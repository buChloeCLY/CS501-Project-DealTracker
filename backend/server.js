// ===================================
// server.js v2.0 - 多平台价格比较版
// 功能: 产品管理 + 用户管理 + 多平台价格查询
// ===================================

const express = require('express');
const mysql = require('mysql2/promise');
const axios = require('axios');
const cors = require('cors');
const cron = require('node-cron');
const crypto = require('crypto');
require('dotenv').config();

const app = express();

// 中间件
app.use(cors());
app.use(express.json());

// ===================================
// 数据库连接池
// ===================================
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT || 3306,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// 测试连接
pool.getConnection()
    .then(connection => {
        console.log('✅ Database connected successfully');
        connection.release();
    })
    .catch(err => console.error('❌ Database connection failed:', err));

// ===================================
// 工具函数
// ===================================

/**
 * 密码哈希函数 (SHA-256)
 */
function hashPassword(password) {
    return crypto.createHash('sha256').update(password).digest('hex');
}

/**
 * 解析价格 - 从 "$99.99" 或 "$1,299.99" 转为 99.99
 */
function parsePrice(priceStr) {
    if (!priceStr) return 0;
    const cleaned = priceStr.replace(/[$,]/g, '').trim();
    return parseFloat(cleaned) || 0;
}

/**
 * 解析评分 - 从 "4.5 out of 5 stars" 转为 4.5
 */
function parseRating(ratingStr) {
    if (!ratingStr) return 0;
    const match = ratingStr.match(/(\d+\.?\d*)/);
    return match ? parseFloat(match[1]) : 0;
}

/**
 * 🆕 精确标题匹配算法（优化版）
 *
 * 匹配策略：
 * 1. 品牌匹配（Apple, Samsung, Sony 等）
 * 2. 型号匹配（iPhone 11, Galaxy S23 等）
 * 3. 关键规格匹配（64GB, 256GB, Pro Max 等）
 * 4. 颜色匹配（Red, Black, Blue 等）
 * 5. Levenshtein 距离（编辑距离）
 *
 * 返回：0-1 之间的相似度分数，1 表示完全匹配
 */
function calculateSimilarity(str1, str2) {
    if (!str1 || !str2) return 0;

    // 转为小写并移除多余符号
    const clean1 = cleanTitle(str1);
    const clean2 = cleanTitle(str2);

    // 完全匹配
    if (clean1 === clean2) return 1.0;

    // 提取关键信息
    const info1 = extractKeyInfo(str1);
    const info2 = extractKeyInfo(str2);

    // 计算各项匹配分数
    let score = 0;
    let weights = 0;

    // 1. 品牌匹配（权重 30%）
    if (info1.brand && info2.brand) {
        if (info1.brand === info2.brand) {
            score += 0.3;
        }
        weights += 0.3;
    }

    // 2. 型号匹配（权重 40%）
    if (info1.model && info2.model) {
        const modelSimilarity = compareModels(info1.model, info2.model);
        score += modelSimilarity * 0.4;
        weights += 0.4;
    }

    // 3. 关键规格匹配（权重 20%）
    const specScore = compareSpecs(info1, info2);
    score += specScore * 0.2;
    weights += 0.2;

    // 4. 词汇重叠度（权重 10%）
    const wordScore = compareWords(clean1, clean2);
    score += wordScore * 0.1;
    weights += 0.1;

    // 归一化分数
    const finalScore = weights > 0 ? score / weights : 0;

    console.log(`📊 Similarity: "${str1.substring(0, 40)}" vs "${str2.substring(0, 40)}" = ${(finalScore * 100).toFixed(1)}%`);

    return finalScore;
}

/**
 * 清理标题 - 移除营销词汇和多余符号
 */
function cleanTitle(title) {
    return title
        .toLowerCase()
        .replace(/\(.*?\)/g, '')  // 移除括号内容
        .replace(/[-–—]/g, ' ')  // 替换连字符为空格
        .replace(/[,;:]/g, ' ')  // 替换标点为空格
        .replace(/\s+/g, ' ')    // 合并多个空格
        .trim();
}

/**
 * 提取关键信息（品牌、型号、规格）
 */
function extractKeyInfo(title) {
    const lower = title.toLowerCase();
    const info = {
        brand: null,
        model: null,
        storage: null,
        color: null,
        specs: []
    };

    // 品牌匹配（常见品牌）
    const brands = [
        'apple', 'samsung', 'google', 'sony', 'lg', 'motorola', 'oneplus',
        'dell', 'hp', 'lenovo', 'asus', 'acer', 'microsoft',
        'bose', 'beats', 'jbl', 'airpods',
        'nike', 'adidas', 'puma'
    ];

    for (const brand of brands) {
        if (lower.includes(brand)) {
            info.brand = brand;
            break;
        }
    }

    // 型号匹配（iPhone, Galaxy, Pixel 等）
    const modelPatterns = [
        /iphone\s*(\d+\s*pro\s*max|\d+\s*pro|\d+\s*plus|\d+)/i,
        /galaxy\s*s\d+\s*(ultra|plus)?/i,
        /pixel\s*\d+\s*(pro|xl)?/i,
        /macbook\s*(pro|air)/i,
        /ipad\s*(pro|air|mini)?/i,
        /airpods\s*(pro|max)?/i,
        /echo\s*(dot|show|studio)?/i,
        /kindle\s*(paperwhite|oasis)?/i
    ];

    for (const pattern of modelPatterns) {
        const match = title.match(pattern);
        if (match) {
            info.model = match[0].toLowerCase().trim();
            break;
        }
    }

    // 存储容量匹配
    const storageMatch = title.match(/(\d+)\s*(gb|tb)/i);
    if (storageMatch) {
        info.storage = storageMatch[0].toLowerCase();
        info.specs.push(info.storage);
    }

    // 颜色匹配
    const colors = [
        'red', 'black', 'white', 'blue', 'green', 'yellow', 'purple',
        'silver', 'gold', 'rose gold', 'space gray', 'midnight', 'starlight'
    ];

    for (const color of colors) {
        if (lower.includes(color)) {
            info.color = color;
            info.specs.push(color);
            break;
        }
    }

    // 其他关键规格
    const specPatterns = [
        /pro max/i, /pro/i, /plus/i, /mini/i, /ultra/i,
        /unlocked/i, /renewed/i, /refurbished/i,
        /5g/i, /wifi/i, /cellular/i
    ];

    for (const pattern of specPatterns) {
        const match = title.match(pattern);
        if (match) {
            info.specs.push(match[0].toLowerCase());
        }
    }

    return info;
}

/**
 * 比较型号相似度
 */
function compareModels(model1, model2) {
    if (model1 === model2) return 1.0;

    // 移除空格后比较
    const m1 = model1.replace(/\s+/g, '');
    const m2 = model2.replace(/\s+/g, '');

    if (m1 === m2) return 0.95;

    // 使用 Levenshtein 距离
    const distance = levenshteinDistance(m1, m2);
    const maxLen = Math.max(m1.length, m2.length);
    const similarity = 1 - (distance / maxLen);

    return Math.max(0, similarity);
}

/**
 * 比较规格相似度
 */
function compareSpecs(info1, info2) {
    let matchCount = 0;
    let totalSpecs = 0;

    // 存储容量
    if (info1.storage && info2.storage) {
        matchCount += info1.storage === info2.storage ? 1 : 0;
        totalSpecs++;
    }

    // 颜色
    if (info1.color && info2.color) {
        matchCount += info1.color === info2.color ? 1 : 0;
        totalSpecs++;
    }

    // 其他规格
    const specs1 = new Set(info1.specs);
    const specs2 = new Set(info2.specs);
    const commonSpecs = [...specs1].filter(s => specs2.has(s));

    if (specs1.size > 0 || specs2.size > 0) {
        const specSimilarity = commonSpecs.length / Math.max(specs1.size, specs2.size);
        matchCount += specSimilarity;
        totalSpecs++;
    }

    return totalSpecs > 0 ? matchCount / totalSpecs : 0;
}

/**
 * 比较词汇重叠度
 */
function compareWords(str1, str2) {
    const words1 = str1.split(/\s+/).filter(w => w.length > 2);
    const words2 = str2.split(/\s+/).filter(w => w.length > 2);

    if (words1.length === 0 || words2.length === 0) return 0;

    const set1 = new Set(words1);
    const set2 = new Set(words2);

    const intersection = [...set1].filter(w => set2.has(w));
    const union = new Set([...set1, ...set2]);

    return intersection.length / union.size;  // Jaccard 相似度
}

/**
 * Levenshtein 距离（编辑距离）
 */
function levenshteinDistance(str1, str2) {
    const len1 = str1.length;
    const len2 = str2.length;

    // 创建矩阵
    const matrix = Array(len1 + 1).fill(null).map(() => Array(len2 + 1).fill(0));

    // 初始化第一行和第一列
    for (let i = 0; i <= len1; i++) matrix[i][0] = i;
    for (let j = 0; j <= len2; j++) matrix[0][j] = j;

    // 填充矩阵
    for (let i = 1; i <= len1; i++) {
        for (let j = 1; j <= len2; j++) {
            const cost = str1[i - 1] === str2[j - 1] ? 0 : 1;
            matrix[i][j] = Math.min(
                matrix[i - 1][j] + 1,      // 删除
                matrix[i][j - 1] + 1,      // 插入
                matrix[i - 1][j - 1] + cost // 替换
            );
        }
    }

    return matrix[len1][len2];
}

/**
 * 🆕 提取短标题 - 保留品牌+型号+关键配置（限制 10 词以内）
 * 例如: "Apple iPhone 15 Pro Max, 256GB, Blue - Unlocked (Renewed)"
 *   → "Apple iPhone 15 Pro Max 256GB Blue"
 */
function extractShortTitle(fullTitle) {
    if (!fullTitle) return 'Unknown Product';

    // 移除常见的营销词汇和多余符号
    let cleaned = fullTitle
        .replace(/\(.*?\)/g, '')           // 移除括号内容 (Renewed), (Brand New)
        .replace(/[-–—]\s*(Unlocked|GSM|CDMA|Certified|Refurbished|Pre-Owned|Factory|International|US Version).*/gi, '')
        .replace(/\s*,\s*(Free Shipping|Fast Delivery|Best Price|Top Rated|Best Seller).*/gi, '')
        .replace(/\s+(with|for|by)\s+.*/gi, '')  // 移除 "with accessories" 之类
        .replace(/\b(Limited Edition|Special Edition|Exclusive)\b/gi, '')
        // 移除运营商信息（保留 Unlocked）
        .replace(/\b(Verizon|AT&T|T-Mobile|Sprint|US Cellular)\b(?!\s*Unlocked)/gi, '')
        // 移除版本信息（除非是关键配置）
        .replace(/\b(US Version|International Version|Global Version)\b/gi, '')
        // 移除多余符号
        .replace(/[•●○▪▫]/g, ' ')
        .replace(/[-–—]/g, ' ')
        .replace(/[,;:]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();

    // 分词，过滤无意义词汇
    const words = cleaned.split(/[\s,]+/).filter(w =>
        w.length > 1 &&
        !/^(the|and|or|with|for|by|in|on|at|to|from|of)$/i.test(w)
    );

    // 限制最多 15 个词
    const maxWords = 15;
    let shortWords = words.slice(0, maxWords);

    // ⭐ 新增：提取并确保包含颜色（多词颜色优先）
    const color = extractColorDetailed(fullTitle);
    if (color && shortWords.length < maxWords) {
        const colorWords = color.split(/\s+/);
        const colorInTitle = colorWords.every(cw =>
            shortWords.some(sw => sw.toLowerCase() === cw.toLowerCase())
        );

        if (!colorInTitle) {
            // 如果颜色不在短标题中，添加它
            if (shortWords.length + colorWords.length <= maxWords) {
                shortWords.push(...colorWords);
            }
        }
    }

    // ⭐ 优化：提取所有存储信息（包括内存）
    const storageMatches = [...fullTitle.matchAll(/\b(\d+)\s*(GB|TB|MB)(?:\s*RAM)?\b/gi)];
    const storageInfo = [];

    for (const match of storageMatches) {
        const value = match[1];
        const unit = match[2].toUpperCase();
        const isRAM = match[0].toLowerCase().includes('ram');

        const storageStr = isRAM ? `${value}${unit} RAM` : `${value}${unit}`;

        // 检查是否已在短标题中
        const alreadyIncluded = shortWords.some(w =>
            w.toLowerCase().includes(value.toLowerCase()) &&
            w.toLowerCase().includes(unit.toLowerCase())
        );

        if (!alreadyIncluded && shortWords.length < maxWords) {
            storageInfo.push(storageStr);
        }
    }

    // 添加存储信息（去重）
    const uniqueStorage = [...new Set(storageInfo)];
    for (const storage of uniqueStorage) {
        if (shortWords.length < maxWords) {
            const storageWords = storage.split(/\s+/);
            if (shortWords.length + storageWords.length <= maxWords) {
                shortWords.push(...storageWords);
            }
        }
    }

    // ⭐ 新增：提取并添加关键配置
    const configs = extractConfigs(fullTitle);
    for (const config of configs) {
        if (shortWords.length < maxWords) {
            const configWords = config.split(/\s+/);

            // 检查配置是否已在短标题中
            const configInTitle = configWords.every(cw =>
                shortWords.some(sw => sw.toLowerCase() === cw.toLowerCase())
            );

            if (!configInTitle && shortWords.length + configWords.length <= maxWords) {
                shortWords.push(...configWords);
            }
        }
    }

    const result = shortWords.join(' ');

    // 限制长度不超过 250 字符
    return result.length > 250 ? result.substring(0, 247) + '...' : result;
}

/**
 * ⭐ 提取颜色（详细版，包含多词颜色）
 */
function extractColorDetailed(title) {
    // 多词颜色（优先匹配）
    const multiWordColors = [
        'Natural Titanium', 'Blue Titanium', 'White Titanium', 'Black Titanium',
        'Space Gray', 'Space Black', 'Rose Gold', 'Midnight Green', 'Pacific Blue',
        'Sierra Blue', 'Alpine Green', 'Deep Purple', 'Starlight', 'Midnight',
        'Product Red', 'Jet Black', 'Matte Black', 'Graphite Black'
    ];

    for (const color of multiWordColors) {
        const regex = new RegExp(`\\b${color}\\b`, 'i');
        if (regex.test(title)) {
            return color;
        }
    }

    // 单词颜色
    const singleWordColors = [
        'Red', 'Black', 'White', 'Blue', 'Green', 'Yellow', 'Purple',
        'Pink', 'Orange', 'Gray', 'Grey', 'Silver', 'Gold', 'Bronze',
        'Titanium', 'Graphite', 'Coral', 'Lavender'
    ];

    for (const color of singleWordColors) {
        const regex = new RegExp(`\\b${color}\\b`, 'i');
        if (regex.test(title)) {
            return color;
        }
    }

    return null;
}

/**
 * ⭐ 提取关键配置
 */
function extractConfigs(title) {
    const configs = [];

    const keywords = [
        'Unlocked', '5G', '4G', 'LTE', 'WiFi', 'Wi-Fi', 'WiFi 6', 'Bluetooth',
        'Dual SIM', 'eSIM', 'Touchscreen', 'Retina', 'OLED', 'AMOLED',
        'Water Resistant', 'Waterproof', 'Wireless Charging', 'Fast Charging',
        'Noise Cancelling', 'Active Noise Cancelling', 'ANC'
    ];

    for (const keyword of keywords) {
        const regex = new RegExp(`\\b${keyword}\\b`, 'i');
        if (regex.test(title)) {
            configs.push(keyword);
        }
    }

    // 限制配置数量（最多 3 个，避免超过 15 词）
    return configs.slice(0, 3);
}

/**
 * 🆕 优化版：智能分类函数
 *
 * 优先级：
 * 1. 使用 Amazon 的 category_path（最准确）
 * 2. 回退到标题关键词匹配（兼容其他平台）
 *
 * @param {string} title - 产品标题
 * @param {Object} apiProduct - 完整的 API 响应对象（可选）
 * @returns {string} - 分类名称
 */
function categorizeProduct(title, apiProduct = null) {
    // ⭐ 优先级 1：使用 Amazon 的 category_path
    if (apiProduct && apiProduct.category_path && Array.isArray(apiProduct.category_path)) {
        const mappedCategory = mapAmazonCategory(apiProduct.category_path);
        if (mappedCategory) {
            console.log(`📂 [Category] Using Amazon path: ${mappedCategory}`);
            return mappedCategory;
        }
    }

    // ⭐ 优先级 2：回退到标题关键词匹配
    console.log(`📂 [Category] Fallback to keyword matching`);
    return categorizeByKeywords(title);
}

/**
 * 🆕 映射 Amazon category_path 到我们的分类系统
 *
 * Amazon category_path 示例：
 * [
 *   { id: "2335752011", name: "Cell Phones & Accessories" },
 *   { id: "7072561011", name: "Cell Phones" }
 * ]
 */
function mapAmazonCategory(categoryPath) {
    // 从最后一个分类开始检查（最具体的分类）
    for (let i = categoryPath.length - 1; i >= 0; i--) {
        const category = categoryPath[i];
        const categoryName = category.name.toLowerCase();

        // Electronics 相关
        if (categoryName.includes('cell phone') ||
            categoryName.includes('smartphone') ||
            categoryName.includes('electronics') ||
            categoryName.includes('computer') ||
            categoryName.includes('tablet') ||
            categoryName.includes('laptop') ||
            categoryName.includes('camera') ||
            categoryName.includes('tv') ||
            categoryName.includes('audio') ||
            categoryName.includes('headphone') ||
            categoryName.includes('wearable') ||
            categoryName.includes('smart home') ||
            categoryName.includes('video game')) {
            return 'Electronics';
        }

        // Beauty 相关
        if (categoryName.includes('beauty') ||
            categoryName.includes('makeup') ||
            categoryName.includes('skincare') ||
            categoryName.includes('cosmetic') ||
            categoryName.includes('fragrance') ||
            categoryName.includes('personal care')) {
            return 'Beauty';
        }

        // Home 相关
        if (categoryName.includes('home') ||
            categoryName.includes('kitchen') ||
            categoryName.includes('furniture') ||
            categoryName.includes('bedding') ||
            categoryName.includes('appliance') ||
            categoryName.includes('garden') ||
            categoryName.includes('patio')) {
            return 'Home';
        }

        // Food 相关
        if (categoryName.includes('grocery') ||
            categoryName.includes('food') ||
            categoryName.includes('beverage') ||
            categoryName.includes('snack') ||
            categoryName.includes('gourmet')) {
            return 'Food';
        }

        // Fashion 相关
        if (categoryName.includes('clothing') ||
            categoryName.includes('shoes') ||
            categoryName.includes('fashion') ||
            categoryName.includes('jewelry') ||
            categoryName.includes('watch') ||
            categoryName.includes('accessories') ||
            categoryName.includes('handbag') ||
            categoryName.includes('luggage')) {
            return 'Fashion';
        }

        // Sports 相关
        if (categoryName.includes('sport') ||
            categoryName.includes('fitness') ||
            categoryName.includes('outdoor') ||
            categoryName.includes('exercise') ||
            categoryName.includes('athletic')) {
            return 'Sports';
        }

        // Books 相关
        if (categoryName.includes('book') ||
            categoryName.includes('kindle') ||
            categoryName.includes('magazine') ||
            categoryName.includes('textbook')) {
            return 'Books';
        }

        // Toys 相关
        if (categoryName.includes('toy') ||
            categoryName.includes('game') ||
            categoryName.includes('puzzle')) {
            return 'Toys';
        }

        // Health 相关
        if (categoryName.includes('health') ||
            categoryName.includes('medical') ||
            categoryName.includes('vitamin') ||
            categoryName.includes('supplement') ||
            categoryName.includes('wellness')) {
            return 'Health';
        }

        // Office 相关
        if (categoryName.includes('office') ||
            categoryName.includes('school') ||
            categoryName.includes('stationery')) {
            return 'Office';
        }

        // Pets 相关
        if (categoryName.includes('pet') ||
            categoryName.includes('dog') ||
            categoryName.includes('cat') ||
            categoryName.includes('animal')) {
            return 'Pets';
        }
    }

    return null; // 未找到匹配，返回 null 触发回退
}

/**
 * 🆕 基于关键词的分类（原有逻辑，作为回退方案）
 */
function categorizeByKeywords(title) {
    const lower = title.toLowerCase();

    if (lower.match(/phone|laptop|tablet|computer|headphone|speaker|camera|tv|monitor|keyboard|mouse|smartwatch|earbuds|airpods|ipad|macbook|gaming|console|playstation|xbox|nintendo|electronics|cable|charger|adapter|router|printer/)) {
        return 'Electronics';
    }
    if (lower.match(/beauty|makeup|skincare|cosmetic|perfume|fragrance|lipstick|foundation|serum|moisturizer|shampoo|conditioner|lotion|cream|mascara|eyeliner|nail polish/)) {
        return 'Beauty';
    }
    if (lower.match(/furniture|kitchen|home|bedding|decor|lamp|chair|table|sofa|pillow|blanket|curtain|rug|vacuum|appliance|cookware|utensil|storage|organizer/)) {
        return 'Home';
    }
    if (lower.match(/food|snack|coffee|tea|chocolate|candy|grocery|organic|protein|vitamin|supplement|chips|cookies|cereal|pasta|sauce|spice/)) {
        return 'Food';
    }
    if (lower.match(/clothing|shoes|dress|shirt|pants|jacket|coat|boots|sneakers|fashion|bag|wallet|jewelry|sunglasses|hat|scarf|gloves|belt|tie/)) {
        return 'Fashion';
    }
    if (lower.match(/sports|fitness|gym|yoga|exercise|bike|bicycle|treadmill|dumbbell|weights|running|tennis|basketball|soccer|football|swimming/)) {
        return 'Sports';
    }
    if (lower.match(/book|novel|textbook|kindle|ebook|magazine|comic|manga|cookbook|guide|dictionary|encyclopedia|bestseller|paperback|hardcover/)) {
        return 'Books';
    }
    if (lower.match(/toy|doll|lego|puzzle|board game|action figure|stuffed animal|playset|barbie|hot wheels|nerf|pokemon|minecraft/)) {
        return 'Toys';
    }
    if (lower.match(/health|medical|medicine|thermometer|blood pressure|first aid|bandage|supplements|probiotic|immune|pain relief|aspirin|allergy/)) {
        return 'Health';
    }
    if (lower.match(/outdoor|camping|hiking|tent|backpack|sleeping bag|flashlight|lantern|fishing|hunting|survival|compass|binoculars/)) {
        return 'Outdoors';
    }
    if (lower.match(/office|desk|pen|pencil|notebook|paper|stapler|folder|calculator|planner|marker|highlighter|binder|organizer|supplies/)) {
        return 'Office';
    }
    if (lower.match(/pet|dog|cat|puppy|kitten|fish|bird|hamster|collar|leash|food|treat|toy|bed|cage|aquarium|litter/)) {
        return 'Pets';
    }

    return 'Electronics'; // 默认分类
}

/**
 * 生成产品详情信息
 */
function generateInformation(product) {
    const info = [];

    if (product.asin) {
        info.push(`ASIN: ${product.asin}`);
    }
    if (product.is_prime) {
        info.push('Prime Eligible');
    }
    if (product.is_best_seller) {
        info.push('Best Seller');
    }
    if (product.is_amazon_choice) {
        info.push("Amazon's Choice");
    }
    if (product.product_num_ratings) {
        info.push(`${product.product_num_ratings.toLocaleString()} ratings`);
    }
    if (product.sales_volume) {
        info.push(`Sales: ${product.sales_volume}`);
    }
    if (product.delivery) {
        info.push(`Delivery: ${product.delivery}`);
    }
    if (product.climate_pledge_friendly) {
        info.push('Climate Pledge Friendly');
    }

    return info.join(' • ') || 'No additional information';
}

// ===================================
// RapidAPI 配置
// ===================================
const RAPIDAPI_KEY = process.env.RAPIDAPI_KEY || process.env.RAPIDAPI_KEY_AMAZON;

// 向后兼容：如果设置了单独的 Key，使用单独的；否则都用同一个
const RAPIDAPI_KEYS = {
    amazon: process.env.RAPIDAPI_KEY_AMAZON || RAPIDAPI_KEY,
    ebay: process.env.RAPIDAPI_KEY_EBAY || RAPIDAPI_KEY,
    walmart: process.env.RAPIDAPI_KEY_WALMART || RAPIDAPI_KEY
};

/**
 * 🆕 从 Amazon RapidAPI 获取产品数据
 */
async function fetchFromAmazon(query, page = 1) {
    try {
        console.log(`🔍 [Amazon] Searching: "${query}" (page ${page})`);

        if (!RAPIDAPI_KEYS.amazon || RAPIDAPI_KEYS.amazon === 'YOUR_RAPIDAPI_KEY_HERE') {
            throw new Error('Amazon RapidAPI Key is not configured');
        }

        const response = await axios.get('https://real-time-amazon-data.p.rapidapi.com/search', {
            params: {
                query: query,
                page: page.toString(),
                country: 'US'
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.amazon,
                'X-RapidAPI-Host': 'real-time-amazon-data.p.rapidapi.com'
            }
            // 移除 timeout 限制
        });

        const products = response.data.data?.products || [];
        console.log(`✅ [Amazon] Found ${products.length} products`);
        return products;

    } catch (error) {
        console.error('❌ [Amazon] API Error:', error.message);
        return [];
    }
}

/**
 * 从 eBay RapidAPI 获取产品数据
 *
 * @param {string} query - 搜索关键词（使用 short_title）
 * @param {number} page - 页码
 * @returns {Array} - 产品列表
 */
async function fetchFromEbay(query, page = 1) {
    try {
        console.log(`🔍 [eBay] Searching: "${query}" (page ${page})`);

        if (!RAPIDAPI_KEYS.ebay || RAPIDAPI_KEYS.ebay === 'YOUR_RAPIDAPI_KEY_HERE') {
            console.log('⚠️  eBay API key not configured, skipping...');
            return [];
        }

        const response = await axios.get('https://ebay-data-api.p.rapidapi.com/search', {
            params: {
                query: query,
                page: page.toString(),
                countryIso: 'us',
                minPrice: 0
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.ebay,
                'X-RapidAPI-Host': 'ebay-data-api.p.rapidapi.com'
            }
        });

        const products = response.data?.data?.items || [];
        console.log(`✅ [eBay] Found ${products.length} products`);
        return products;

    } catch (error) {
        if (error.response) {
            console.error('❌ [eBay] API Error:', {
                status: error.response.status,
                message: error.response.data
            });
        } else {
            console.error('❌ [eBay] Request Error:', error.message);
        }
        return [];
    }
}

/**
 * 🆕 从 Walmart RapidAPI 获取产品数据
 * API: walmart-api4 by mahmudulhasandev
 * URL: https://rapidapi.com/mahmudulhasandev/api/walmart-api4
 */
async function fetchFromWalmart(query, page = 1) {
    try {
        console.log(`🔍 [Walmart] Searching: "${query}"`);

        if (!RAPIDAPI_KEYS.walmart) {
            console.log('⚠️  Walmart API key not configured, skipping...');
            return [];
        }

        // 使用 Walmart API4 的 /search 端点
        const response = await axios.get('https://walmart-api4.p.rapidapi.com/search', {
            params: {
                q: query,
                page: page.toString()
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.walmart,
                'X-RapidAPI-Host': 'walmart-api4.p.rapidapi.com'
            }
        });

        // searchResult 是一个数组，包含多个 item
        const searchResultArray = response.data?.searchResult || [];

        // 合并所有 item 中的产品
        let allProducts = [];
        for (const item of searchResultArray) {
            if (Array.isArray(item)) {
                allProducts = allProducts.concat(item);
            }
        }

        console.log(`✅ [Walmart] Found ${allProducts.length} products`);
        return allProducts;

    } catch (error) {
        if (error.response) {
            console.error('❌ [Walmart] API Error:', {
                status: error.response.status,
                statusText: error.response.statusText,
                url: error.config?.url,
                params: error.config?.params
            });
        } else {
            console.error('❌ [Walmart] Error:', error.message);
        }
        return [];
    }
}

/**
 * 🆕 转换 Amazon 产品数据
 */
function transformAmazonProduct(apiProduct) {
    const fullTitle = apiProduct.product_title || 'Unknown Product';
    const shortTitle = extractShortTitle(fullTitle);

    // ⭐ 优化 1: Free Shipping 判断
        let freeShipping = false;

        // 方法 1: is_prime
        if (apiProduct.is_prime === true) {
            freeShipping = true;
            console.log(`📦 [Free Shipping] Detected via is_prime`);
        }

        // 方法 2: delivery 字段包含 "free" (不区分大小写)
        if (!freeShipping && apiProduct.delivery) {
            const deliveryText = apiProduct.delivery.toLowerCase();
            if (deliveryText.includes('free')) {
                freeShipping = true;
                console.log(`📦 [Free Shipping] Detected via delivery: "${apiProduct.delivery}"`);
            }
        }

        // ⭐ 优化 2: In Stock 判断
        let inStock = false;

        // 方法 1: product_availability 包含 "in stock"
        if (apiProduct.product_availability) {
            const availability = apiProduct.product_availability.toLowerCase();
            if (availability.includes('in stock') || availability.includes('available')) {
                inStock = true;
                console.log(`✅ [In Stock] Detected via product_availability: "${apiProduct.product_availability}"`);
            }
        }

        // 方法 2: product_num_offers > 0
        if (!inStock && apiProduct.product_num_offers) {
            const numOffers = typeof apiProduct.product_num_offers === 'number'
                ? apiProduct.product_num_offers
                : parseInt(apiProduct.product_num_offers);

            if (numOffers > 0) {
                inStock = true;
                console.log(`✅ [In Stock] Detected via product_num_offers: ${numOffers}`);
            }
        }

    return {
        shortTitle: shortTitle,
        fullTitle: fullTitle,
        price: parsePrice(apiProduct.product_price),
        rating: parseRating(apiProduct.product_star_rating),
        platform: 'Amazon',
        freeShipping: apiProduct.is_prime ? 1 : 0,
        inStock: apiProduct.product_availability?.toLowerCase().includes('in stock') ? 1 : 0,
        information: generateInformation(apiProduct),
        category: categorizeProduct(fullTitle, apiProduct),
        imageUrl: apiProduct.product_photo || '',
        idInPlatform: apiProduct.asin || '',
        link: apiProduct.product_url || ''
    };
}

/**
 * 转换 eBay 产品数据
 *
 * eBay API 返回示例：
 * {
 *   "itemId": "366033421295",
 *   "title": "APPLE MACBOOK PRO MLL42LL/A | CORE I5-6360U 2.0GHZ | 256GB | 8GB",
 *   "price": 64,
 *   "currency": "USD",
 *   "shipping": 0,
 *   "total": 64,
 *   "soldQuantity": 0,
 *   "imageUrl": "https://i.ebayimg.com/...",
 *   "time-left": "11 bids · Time left18h 29m left",
 *   "bid-count": 11,
 *   "condition": "Pre-Owned · 13 in",
 *   "delivery-date": "Free delivery",
 *   "url": "https://www.ebay.com/itm/..."
 * }
 */
function transformEbayProduct(apiProduct) {
    // 处理价格
    const price = apiProduct.total || apiProduct.price || 0;

    // 处理包邮（shipping = 0 表示免费配送）
    const freeShipping = apiProduct.shipping === 0 ||
                        apiProduct['delivery-date']?.toLowerCase().includes('free');

    // 处理库存（eBay 通常有货，除非已售罄）
    const inStock = !apiProduct.condition?.toLowerCase().includes('sold out');

    // 生成详情信息
    const info = [];

    if (apiProduct.itemId) {
        info.push(`eBay ID: ${apiProduct.itemId}`);
    }

    if (apiProduct.condition) {
        info.push(apiProduct.condition);
    }

    if (apiProduct['bid-count'] && apiProduct['bid-count'] > 0) {
        info.push(`${apiProduct['bid-count']} bids`);
    }

    if (apiProduct.soldQuantity && apiProduct.soldQuantity > 0) {
        info.push(`${apiProduct.soldQuantity} sold`);
    }

    if (apiProduct['time-left']) {
        info.push(apiProduct['time-left']);
    }

    return {
        price: price,
        platform: 'eBay',
        freeShipping: freeShipping ? 1 : 0,
        inStock: inStock ? 1 : 0,
        link: apiProduct.url || '',
        idInPlatform: apiProduct.itemId || '',
        title: apiProduct.title || '',
        condition: apiProduct.condition || ''  // ⭐ 新增：用于二手判断
    };
}

/**
 * 🆕 转换 Walmart 产品数据
 * Walmart API4 字段映射：
 * - name -> title
 * - price.current -> price
 * - availabilityStatusDisplayValue -> in_stock
 * - fulfillmentType -> free_shipping (检查是否包含 "2-day" 或 "Free")
 */
function transformWalmartProduct(apiProduct) {
    // 处理价格（Walmart API4 使用 price.current）
    const priceValue = apiProduct.price?.current || apiProduct.price || 0;
    const price = typeof priceValue === 'string' ? parsePrice(priceValue) : parseFloat(priceValue);

    // 处理包邮（根据 fulfillmentBadgeGroups）
    let freeShipping = false;

    // 优先检查 fulfillmentBadgeGroups
    if (apiProduct.fulfillmentBadgeGroups && Array.isArray(apiProduct.fulfillmentBadgeGroups)) {
        for (const badge of apiProduct.fulfillmentBadgeGroups) {
            if (badge.text && typeof badge.text === 'string') {
                const text = badge.text.toLowerCase();
                if (text.includes('free shipping')) {
                    freeShipping = true;
                    break;
                }
            }
        }
    }

    // 备用：检查 fulfillmentType
    if (!freeShipping && apiProduct.fulfillmentType) {
        const fulfillment = String(apiProduct.fulfillmentType).toLowerCase();
        freeShipping = fulfillment.includes('free') || fulfillment.includes('2-day');
    }

    // 备用：检查 shippingOption
    if (!freeShipping && apiProduct.shippingOption) {
        const shipping = String(apiProduct.shippingOption).toLowerCase();
        freeShipping = shipping.includes('free');
    }

    // 处理库存（isOutOfStock 或 availabilityStatusDisplayValue）
    let inStock = true; // 默认有货

    // 优先检查 isOutOfStock
    if (apiProduct.isOutOfStock !== undefined) {
        inStock = apiProduct.isOutOfStock === false;
    }
    // 备用：检查 availabilityStatusDisplayValue
    else if (apiProduct.availabilityStatusDisplayValue) {
        const avail = String(apiProduct.availabilityStatusDisplayValue).toLowerCase();
        inStock = avail.includes('in stock') || avail.includes('available');
    }
    // 备用：检查 availabilityStatus
    else if (apiProduct.availabilityStatus) {
        const avail = String(apiProduct.availabilityStatus).toLowerCase();
        inStock = avail.includes('in_stock') || avail.includes('available');
    }

    // 处理链接（canonicalUrl）
    const link = apiProduct.productLink || '';

    return {
        price: price,
        platform: 'Walmart',
        freeShipping: freeShipping ? 1 : 0,
        inStock: inStock ? 1 : 0,
        link: link,
        information: '' // Walmart 不需要 information，用完整标题
    };
}

/**
 * 检测标题是否包含二手/翻新信息
 */
function isUsedProduct(title) {
    if (!title) return false;

    const lowerTitle = title.toLowerCase();
    const usedKeywords = [
        'renewed', 'refurbished', 'pre-owned', 'used', 'open box',
        'certified refurbished', 'like new', 'second hand', 'secondhand',
        'reconditioned', 'remanufactured'
    ];

    return usedKeywords.some(keyword => lowerTitle.includes(keyword));
}

function findBestWalmartMatch(dbProduct, walmartProducts) {
    if (!walmartProducts || walmartProducts.length === 0) {
        return null;
    }

    console.log(`   🔍 [Walmart] Processing ${walmartProducts.length} products`);

    // ⭐ 步骤 1: 使用 transformWalmartProduct 转换所有商品
    const transformed = walmartProducts.map(p => {
        const product = transformWalmartProduct(p);
        product.title = p.name || p.title || '';
        return product;
    });

    // ⭐ 步骤 2: 检测原标题是否为二手
    const originalIsUsed = isUsedProduct(dbProduct.title);
    console.log(`   📋 [Walmart] Original is used: ${originalIsUsed}`);

    let candidates = transformed;

    // ⭐ 步骤 3: 如果原标题非二手，过滤二手商品
    if (!originalIsUsed) {
        const filtered = transformed.filter(p => !isUsedProduct(p.title));

        if (filtered.length > 0) {
            candidates = filtered;
            console.log(`   ✅ [Walmart] Filtered: ${transformed.length} → ${filtered.length} (removed used)`);
        } else {
            console.log(`   ⚠️  [Walmart] All products are used, using original list`);
        }
    }

    // ⭐ 步骤 4: 价格过滤（移到外面，独立执行）
    if (dbProduct.price) {
        const referencePrice = dbProduct.price;
        const minPrice = referencePrice * 0.3;
        const maxPrice = referencePrice * 2.5;

        const priceFiltered = candidates.filter(p => {
            if (p.price < minPrice || p.price > maxPrice) {
                console.log(`   ⏭️  [Walmart] Price out of range: $${p.price} (ref: $${referencePrice})`);
                return false;
            }
            return true;
        });

        if (priceFiltered.length > 0) {
            candidates = priceFiltered;
            console.log(`   ✅ [Walmart] Price filtered: ${candidates.length} products in range`);
        } else {
            console.log(`   ⚠️  [Walmart] All prices out of range, using original list`);
        }
    }

    // ⭐ 步骤 5: 计算相似度
    const scored = candidates.map(product => ({
        product: product,
        similarity: calculateSimilarity(dbProduct.title, product.title),
        price: product.price
    }));

    // ⭐ 步骤 6: 按相似度降序排序
    scored.sort((a, b) => b.similarity - a.similarity);

    // ⭐ 步骤 7: 找出最高相似度
    const topSimilarity = scored[0].similarity;

    // ⭐ 步骤 8: 找出所有相近匹配（差距 <= 0.03）
    const topMatches = scored.filter(s => s.similarity >= topSimilarity - 0.03);

    // ⭐ 步骤 9: 如果有多个，选最便宜的
    if (topMatches.length > 1) {
        topMatches.sort((a, b) => a.price - b.price);
        console.log(`   ✅ [Walmart] ${topMatches.length} similar matches, cheapest: $${topMatches[0].price}`);
    }

    const best = topMatches[0];
    console.log(`   ✅ [Walmart] Best: similarity=${best.similarity.toFixed(2)}, price=$${best.price}`);

    return best.product;
}

/**
 * 从 eBay 搜索结果中找到最佳匹配
 *
 * @param {Object} dbProduct - 数据库商品 {pid, title, short_title}
 * @param {Array} ebayProducts - eBay 搜索结果
 * @returns {Object|null} - 最佳匹配的 eBay 商品
 */
function findBestEbayMatch(dbProduct, ebayProducts) {
    if (!ebayProducts || ebayProducts.length === 0) {
        return null;
    }

    console.log(`   🔍 [eBay] Processing ${ebayProducts.length} products`);

    // ⭐ 步骤 1: 使用 transformEbayProduct 转换所有商品
    const transformed = ebayProducts.map(p => transformEbayProduct(p));

    // ⭐ 步骤 2: 检测原标题是否为二手
    const originalIsUsed = isUsedProduct(dbProduct.title);
    console.log(`   📋 [eBay] Original is used: ${originalIsUsed}`);

    let candidates = transformed;

    // ⭐ 步骤 3: 如果原标题非二手，过滤二手商品
    if (!originalIsUsed) {
        const filtered = transformed.filter(p => {
            // 检查标题
            const titleIsUsed = isUsedProduct(p.title);

            // 检查 condition 字段
            const conditionIsUsed = p.condition && (
                p.condition.toLowerCase().includes('pre-owned') ||
                p.condition.toLowerCase().includes('used') ||
                p.condition.toLowerCase().includes('refurbished')
            );

            return !titleIsUsed && !conditionIsUsed;
        });

        // 如果过滤后还有商品，使用过滤后的
        if (filtered.length > 0) {
            candidates = filtered;
            console.log(`   ✅ [eBay] Filtered: ${transformed.length} → ${filtered.length} (removed used)`);
        } else {
            console.log(`   ⚠️  [eBay] All products are used, using original list`);
        }
    }

    // ⭐ 步骤 4: 价格过滤（移到外面，独立执行）
    if (dbProduct.price) {
        const referencePrice = dbProduct.price;
        const minPrice = referencePrice * 0.2;  // 最低不能低于参考价的 20%
        const maxPrice = referencePrice * 3.0;  // 最高不能超过参考价的 300%

        const priceFiltered = candidates.filter(p => {
            if (p.price < minPrice || p.price > maxPrice) {
                console.log(`   ⏭️  [eBay] Price out of range: $${p.price} (ref: $${referencePrice}, range: $${minPrice.toFixed(0)}-$${maxPrice.toFixed(0)})`);
                return false;
            }
            return true;
        });

        if (priceFiltered.length > 0) {
            candidates = priceFiltered;
            console.log(`   ✅ [eBay] Price filtered: ${candidates.length} products in reasonable range`);
        } else {
            console.log(`   ⚠️  [eBay] All prices out of range, using original list`);
        }
    }

    // ⭐ 步骤 5: 计算相似度
    const scored = candidates.map(product => ({
        product: product,
        similarity: calculateSimilarity(dbProduct.title, product.title),
        price: product.price
    }));

    // ⭐ 步骤 6: 按相似度降序排序
    scored.sort((a, b) => b.similarity - a.similarity);

    // ⭐ 步骤 7: 找出最高相似度
    const topSimilarity = scored[0].similarity;

    // ⭐ 步骤 8: 找出所有相近匹配（差距 <= 0.03）
    const topMatches = scored.filter(s => s.similarity >= topSimilarity - 0.03);

    // ⭐ 步骤 9: 如果有多个，选最便宜的
    if (topMatches.length > 1) {
        topMatches.sort((a, b) => a.price - b.price);
        console.log(`   ✅ [eBay] ${topMatches.length} similar matches, cheapest: $${topMatches[0].price}`);
    }

    const best = topMatches[0];
    console.log(`   ✅ [eBay] Best: similarity=${best.similarity.toFixed(2)}, price=$${best.price}`);

    return best.product;
}

// ===================================
// 3. 直接获取 Product Details 的函数
// ===================================

/**
 * 通过 Walmart link 获取 product details
 */
async function getWalmartProductDetails(productLink) {
    try {
        console.log(`   🔗 [Walmart] Fetching details from link`);

        if (!RAPIDAPI_KEYS.walmart) {
            console.log('   ⚠️  Walmart API key not configured');
            return null;
        }

        const response = await axios.get('https://walmart-api4.p.rapidapi.com/details', {
            params: {
                url: productLink
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.walmart,
                'X-RapidAPI-Host': 'walmart-api4.p.rapidapi.com'
            }
        });

        const rawData = response.data;

        if (!rawData) {
            console.log('   ⚠️  No data returned');
            return null;
        }

        // ⭐ 步骤 1: 找到 ProductGroup 对象
        let productGroup = null;

        // 情况 1: 直接是数组 [[{...}], {...}]
        if (Array.isArray(rawData)) {
            for (const item of rawData) {
                if (Array.isArray(item)) {
                    // 嵌套数组
                    const found = item.find(obj => obj['@type'] === 'ProductGroup');
                    if (found) {
                        productGroup = found;
                        break;
                    }
                } else if (item['@type'] === 'ProductGroup') {
                    productGroup = item;
                    break;
                }
            }
        } else if (rawData['@type'] === 'ProductGroup') {
            productGroup = rawData;
        }

        if (!productGroup) {
            console.log('   ⚠️  ProductGroup not found in response');
            return null;
        }

        // ⭐ 步骤 2: 收集所有 offers（可能在多个 variant 里）
        const allOffers = [];

        if (productGroup.hasVariant && Array.isArray(productGroup.hasVariant)) {
            for (const variant of productGroup.hasVariant) {
                // 跳过 url-only 变体
                if (variant.url && !variant.offers) continue;

                // 提取 offers
                if (variant.offers && Array.isArray(variant.offers)) {
                    allOffers.push(...variant.offers);
                }
            }
        }

        // ⭐ 步骤 3: 提取所有价格
        const prices = allOffers
            .map(offer => offer.price)
            .filter(p => p && p > 0);

        if (prices.length === 0) {
            console.log('   ⚠️  No valid prices found');
            return null;
        }

        // ⭐ 步骤 4: 取最低价
        const lowestPrice = Math.min(...prices);

        // ⭐ 步骤 5: 找到对应的 offer
        const bestOffer = allOffers.find(offer => offer.price === lowestPrice);

        // ⭐ 步骤 6: 提取其他字段
        const inStock = bestOffer.availability === 'https://schema.org/InStock';
        const freeShipping = bestOffer.shippingDetails?.shippingRate?.value === 0;

        console.log(`   ✅ [Walmart] Details: price=$${lowestPrice}, inStock=${inStock}, freeShipping=${freeShipping}`);

        if (prices.length > 1) {
            console.log(`   💡 [Walmart] Found ${prices.length} prices, selected lowest: $${lowestPrice}`);
        }

        return {
            price: lowestPrice,
            freeShipping: freeShipping ? 1 : 0,
            inStock: inStock ? 1 : 0
        };

    } catch (error) {
        console.error(`   ❌ [Walmart] Failed to get details:`, error.message);
        return null;
    }
}

/**
 * 通过 Amazon ASIN 获取 product details
 */
async function getAmazonProductDetails(asin) {
    try {
        console.log(`   🔗 [Amazon] Fetching details for ASIN: ${asin}`);

        if (!RAPIDAPI_KEYS.amazon) {
            console.log('   ⚠️  Amazon API key not configured');
            return null;
        }

        const response = await axios.get('https://real-time-amazon-data.p.rapidapi.com/product-details', {
            params: {
                asin: asin,
                country: 'US'
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.amazon,
                'X-RapidAPI-Host': 'real-time-amazon-data.p.rapidapi.com'
            }
        });

        const data = response.data?.data;

        if (!data) {
            console.log('   ⚠️  No data returned');
            return null;
        }

        // 解析字段
        const price = parsePrice(data.product_price);
        const freeShipping = data.is_prime || (data.delivery && data.delivery.toLowerCase().includes('free'));
        const inStock = data.product_availability && (
            data.product_availability.toLowerCase().includes('in stock') ||
            data.product_availability.toLowerCase().includes('available')
        );

        console.log(`   ✅ [Amazon] Details: price=$${price}, inStock=${inStock}, freeShipping=${freeShipping}`);

        return {
            price: price,
            freeShipping: freeShipping ? 1 : 0,
            inStock: inStock ? 1 : 0
        };

    } catch (error) {
        console.error(`   ❌ [Amazon] Failed to get details:`, error.message);
        return null;
    }
}

/**
 * 通过 eBay link 获取 product details
 */
async function getEbayProductDetails(productLink) {
    try {
        console.log(`   🔗 [eBay] Fetching details from link`);

        if (!RAPIDAPI_KEYS.ebay) {
            console.log('   ⚠️  eBay API key not configured');
            return null;
        }

        const response = await axios.get('https://ebay-data-api.p.rapidapi.com/item/description', {
            params: {
                itemUrl: productLink
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.ebay,
                'X-RapidAPI-Host': 'ebay-data-api.p.rapidapi.com'
            }
        });

        const data = response.data?.data;

        if (!data) {
            console.log('   ⚠️  No data returned');
            return null;
        }

        // 解析字段
        const price = data.price || 0;
        const freeShipping = data.shippingOptions && data.shippingOptions.some(opt =>
            opt.shippingCost && opt.shippingCost.price === null || opt.shippingCost.price === 0
        );
        const inStock = data.condition && !data.condition.toLowerCase().includes('sold out');

        console.log(`   ✅ [eBay] Details: price=$${price}, inStock=${inStock}, freeShipping=${freeShipping}`);

        return {
            price: price,
            freeShipping: freeShipping ? 1 : 0,
            inStock: inStock ? 1 : 0
        };

    } catch (error) {
        console.error(`   ❌ [eBay] Failed to get details:`, error.message);
        return null;
    }
}

// ===================================
// API: 用户管理（保持不变）
// ===================================

// 获取用户信息
app.get('/api/user/:uid', async (req, res) => {
    try {
        const uid = parseInt(req.params.uid);

        const [rows] = await pool.query(
            'SELECT uid, name, email, gender, created_at, updated_at FROM user WHERE uid = ?',
            [uid]
        );

        if (rows.length > 0) {
            res.json(rows[0]);
        } else {
            res.status(404).json({ error: 'User not found' });
        }
    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({ error: error.message });
    }
});

// 用户登录
app.post('/api/user/login', async (req, res) => {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            return res.status(400).json({ error: 'Email and password required' });
        }

        const hashedPassword = hashPassword(password);

        const [rows] = await pool.query(
            'SELECT uid, name, email, gender FROM user WHERE email = ? AND password = ?',
            [email, hashedPassword]
        );

        if (rows.length > 0) {
            res.json({ success: true, user: rows[0] });
        } else {
            res.status(401).json({ success: false, error: 'Invalid credentials' });
        }
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ error: error.message });
    }
});

// 用户注册
app.post('/api/user/register', async (req, res) => {
    try {
        const { name, email, password, gender = 'Prefer not to say' } = req.body;

        if (!name || !email || !password) {
            return res.status(400).json({ error: 'Name, email and password required' });
        }

        const hashedPassword = hashPassword(password);

        // 检查邮箱是否已存在
        const [existing] = await pool.query(
            'SELECT uid FROM user WHERE email = ?',
            [email]
        );

        if (existing.length > 0) {
            return res.status(409).json({ error: 'Email already exists' });
        }

        // 插入新用户
        const [result] = await pool.query(
            'INSERT INTO user (name, email, password, gender) VALUES (?, ?, ?, ?)',
            [name, email, hashedPassword, gender]
        );

        res.status(201).json({
            success: true,
            user: {
                uid: result.insertId,
                name,
                email,
                gender
            }
        });
    } catch (error) {
        console.error('Register error:', error);
        res.status(500).json({ error: error.message });
    }
});

// 更新用户信息
app.put('/api/user/:uid', async (req, res) => {
    try {
        const uid = parseInt(req.params.uid);
        const { name, email, gender, password } = req.body;

        // 检查用户是否存在
        const [existing] = await pool.query(
            'SELECT uid FROM user WHERE uid = ?',
            [uid]
        );

        if (existing.length === 0) {
            return res.status(404).json({ error: 'User not found' });
        }

        // 构建更新语句
        const updates = [];
        const params = [];

        if (name) {
            updates.push('name = ?');
            params.push(name);
        }
        if (email) {
            updates.push('email = ?');
            params.push(email);
        }
        if (gender) {
            updates.push('gender = ?');
            params.push(gender);
        }
        if (password) {
            updates.push('password = ?');
            params.push(hashPassword(password));
        }

        if (updates.length === 0) {
            return res.status(400).json({ error: 'No fields to update' });
        }

        params.push(uid);
        const query = `UPDATE user SET ${updates.join(', ')} WHERE uid = ?`;

        await pool.query(query, params);

        // 返回更新后的用户信息
        const [rows] = await pool.query(
            'SELECT uid, name, email, gender, updated_at FROM user WHERE uid = ?',
            [uid]
        );

        res.json({ success: true, user: rows[0] });
    } catch (error) {
        if (error.code === 'ER_DUP_ENTRY') {
            res.status(409).json({ error: 'Email already exists' });
        } else {
            console.error('Update user error:', error);
            res.status(500).json({ error: error.message });
        }
    }
});

// 删除用户
app.delete('/api/user/:uid', async (req, res) => {
    try {
        const uid = parseInt(req.params.uid);

        const [result] = await pool.query(
            'DELETE FROM user WHERE uid = ?',
            [uid]
        );

        if (result.affectedRows > 0) {
            res.json({ success: true, message: 'User deleted' });
        } else {
            res.status(404).json({ error: 'User not found' });
        }
    } catch (error) {
        console.error('Delete user error:', error);
        res.status(500).json({ error: error.message });
    }
});

// ===================================
// API: 价格查询
// ===================================

// 获取产品的最新价格(按平台分组)
app.get('/api/price/:pid', async (req, res) => {
    try {
        const pid = parseInt(req.params.pid);

        const [rows] = await pool.query(`
            SELECT p1.id, p1.pid, p1.price, p1.free_shipping, p1.in_stock, p1.date, p1.platform, p1.idInPlatform, p1.link
            FROM price p1
            INNER JOIN (
                SELECT platform, MAX(date) AS max_date
                FROM price
                WHERE pid = ?
                GROUP BY platform
            ) p2 ON p1.platform = p2.platform AND p1.date = p2.max_date
            WHERE p1.pid = ?
            ORDER BY p1.price ASC
        `, [pid, pid]);

        res.json(rows);
    } catch (error) {
        console.error('Get prices error:', error);
        res.json([]);
    }
});

// 获取产品价格历史
app.get('/api/history/:pid', async (req, res) => {
    try {
        const pid = parseInt(req.params.pid);
        const days = parseInt(req.query.days) || 7;

        const [rows] = await pool.query(`
            SELECT *
            FROM (
                SELECT
                    DATE(date) AS d,
                    DATE_FORMAT(date, '%m/%d') AS date,
                    MIN(price) AS price
                FROM price
                WHERE pid = ?
                GROUP BY d
                ORDER BY d DESC
                LIMIT ?
            ) AS tmp
            ORDER BY d ASC
        `, [pid, days]);

        const result = rows.map(r => ({
            date: r.date,
            price: parseFloat(r.price)
        }));

        res.json(result);
    } catch (error) {
        console.error('Get history error:', error);
        res.json([]);
    }
});

// 🆕 获取产品的最低价信息（包括所有并列最低价的平台）
app.get('/api/products/:pid/lowest-price', async (req, res) => {
    try {
        const pid = parseInt(req.params.pid);

        // 获取所有平台的最新价格
        const [allPrices] = await pool.query(`
            SELECT p1.platform, p1.price, p1.free_shipping, p1.in_stock, p1.link
            FROM price p1
            INNER JOIN (
                SELECT platform, MAX(date) AS max_date
                FROM price
                WHERE pid = ?
                GROUP BY platform
            ) p2 ON p1.platform = p2.platform AND p1.date = p2.max_date
            WHERE p1.pid = ?
        `, [pid, pid]);

        if (allPrices.length === 0) {
            return res.json({
                lowestPrice: 0,
                platforms: [],
                allPrices: []
            });
        }

        // 找出最低价
        const lowestPrice = Math.min(...allPrices.map(p => p.price));

        // 找出所有最低价的平台
        const lowestPricePlatforms = allPrices
            .filter(p => p.price === lowestPrice)
            .map(p => ({
                platform: p.platform,
                price: p.price,
                freeShipping: p.free_shipping === 1,
                inStock: p.in_stock === 1,
                link: p.link
            }));

        // 返回完整信息
        res.json({
            lowestPrice: lowestPrice,
            platforms: lowestPricePlatforms,  // 所有最低价平台
            allPrices: allPrices.map(p => ({
                platform: p.platform,
                price: p.price,
                freeShipping: p.free_shipping === 1,
                inStock: p.in_stock === 1,
                link: p.link
            }))
        });

    } catch (error) {
        console.error('Get lowest price error:', error);
        res.status(500).json({ error: error.message });
    }
});

// ===================================
// API: 产品管理
// ===================================

// 获取所有产品
app.get('/api/products', async (req, res) => {
    try {
        const { category, search, min_price, max_price, in_stock, free_shipping } = req.query;

        let query = 'SELECT * FROM products WHERE 1=1';
        const params = [];

        if (category && category !== 'All') {
            query += ' AND category = ?';
            params.push(category);
        }

        if (search) {
            query += ' AND title LIKE ?';  // 只搜索 title
            params.push(`%${search}%`);
        }

        if (min_price) {
            query += ' AND price >= ?';
            params.push(parseFloat(min_price));
        }

        if (max_price) {
            query += ' AND price <= ?';
            params.push(parseFloat(max_price));
        }

        if (in_stock === 'true') {
            query += ' AND in_stock = 1';
        }

        if (free_shipping === 'true') {
            query += ' AND free_shipping = 1';
        }

        query += ' ORDER BY created_at DESC';

        const [products] = await pool.query(query, params);

        // 直接返回，不查询 price 表
        res.json(products);

    } catch (error) {
        console.error('Get products error:', error);
        res.status(500).json({ error: error.message });
    }
});

// 获取单个产品
app.get('/api/products/:pid', async (req, res) => {
    try {
        const pid = parseInt(req.params.pid);
        const [rows] = await pool.query('SELECT * FROM products WHERE pid = ?', [pid]);

        if (rows.length > 0) {
            res.json(rows[0]);
        } else {
            res.status(404).json({ error: 'Product not found' });
        }
    } catch (error) {
        console.error('Get product error:', error);
        res.status(500).json({ error: error.message });
    }
});

// 🆕 导入初始产品（支持多平台）
app.post('/api/admin/import-initial', async (req, res) => {
    try {
        console.log('\n🚀 Starting multi-platform product import...');

        const queries = [
            // ========================================
            // Electronics (10 products)
            // ========================================
            'Samsung Galaxy S24',           // 三星手机（多平台）
            'iPhone 15',                     // 苹果手机（多平台）
            'iPad Air',                      // 平板（多平台）
            'MacBook Pro',                   // 笔记本（多平台）
            'Dell XPS laptop',               // 戴尔笔记本
            'HP laptop',                     // 惠普笔记本
            'Sony WH-1000XM5 headphones',    // 索尼耳机
            'Bose QuietComfort headphones',  // Bose 耳机
            'LG OLED TV',                    // LG 电视
            'Samsung 4K TV',                 // 三星电视

            // ========================================
            // Beauty (4 products)
            // ========================================
            'CeraVe moisturizer',            // 护肤品
            'Neutrogena sunscreen',          // 防晒霜
            'Maybelline mascara',            // 美宝莲睫毛膏
            'L\'Oreal foundation',           // 欧莱雅粉底

            // ========================================
            // Home (5 products)
            // ========================================
            'Dyson vacuum cleaner',          // 戴森吸尘器
            'Shark vacuum',                  // Shark 吸尘器
            'KitchenAid stand mixer',        // 厨师机
            'Ninja blender',                 // Ninja 料理机
            'Instant Pot',                   // 电压力锅

            // ========================================
            // Food (3 products)
            // ========================================
            'Starbucks coffee beans',        // 星巴克咖啡豆
            'Ghirardelli chocolate',         // 吉尔德利巧克力
            'KIND protein bars',             // KIND 蛋白棒

            // ========================================
            // Fashion (4 products)
            // ========================================
            'Nike running shoes',            // 耐克跑鞋
            'Adidas sneakers',               // 阿迪达斯运动鞋
            'Levi\'s jeans',                 // Levi's 牛仔裤
            'North Face jacket',             // 北面夹克

            // ========================================
            // Sports (4 products)
            // ========================================
            'Fitbit fitness tracker',        // Fitbit 智能手环
            'Garmin smartwatch',             // Garmin 智能手表
            'yoga mat',                      // 瑜伽垫
            'resistance bands',              // 阻力带

            // ========================================
            // Books (3 products)
            // ========================================
            'Atomic Habits book',            // 畅销书
            'Harry Potter book set',         // 哈利波特套装
            'a song of ice and fire book set',

            // ========================================
            // Toys (4 products)
            // ========================================
            'LEGO Star Wars set',            // 乐高星战
            'Hot Wheels track',              // 风火轮赛道
            'Barbie doll',                   // 芭比娃娃
            'Rubik\'s cube',                 // 魔方

            // ========================================
            // Health (3 products)
            // ========================================
            'Omron blood pressure monitor',  // 欧姆龙血压计
            'Braun thermometer',             // 博朗体温计
            'multivitamin gummies',          // 复合维生素软糖

            // ========================================
            // Outdoors (3 products)
            // ========================================
            'Coleman camping tent',          // Coleman 帐篷
            'Yeti cooler',                   // Yeti 冷藏箱
            'Stanley thermos',               // Stanley 保温杯

            // ========================================
            // Office (4 products)
            // ========================================
            'Logitech wireless mouse',       // 罗技鼠标
            'mechanical keyboard',           // 机械键盘
            'office chair',                  // 办公椅
            'standing desk',                 // 升降桌

            // ========================================
            // Pets (3 products)
            // ========================================
            'dog food',                      // 狗粮
            'cat litter',                    // 猫砂
            'pet carrier'                    // 宠物包
        ];

        const importedProducts = [];
        let totalImported = 0;

        for (const query of queries) {
            console.log(`\n📦 Processing: "${query}"`);

            // Step 1: 从 Amazon 获取基础产品
            const amazonProducts = await fetchFromAmazon(query, 1);
            if (amazonProducts.length === 0) {
                console.log(`⚠️  No Amazon products found for "${query}"`);
                continue;
            }

            const amazonProduct = transformAmazonProduct(amazonProducts[0]);

            if (amazonProduct.price === 0) {
                console.log(`⚠️  Skipping product with price 0`);
                continue;
            }

            try {
                // Step 2: 插入产品到 products 表
                const [productResult] = await pool.query(`
                    INSERT INTO products
                    (short_title, title, price, rating, platform, free_shipping, in_stock, information, category, image_url)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                `, [
                    amazonProduct.shortTitle,
                    amazonProduct.fullTitle,
                    amazonProduct.price,
                    amazonProduct.rating,
                    'Amazon',  // 主平台显示 Amazon
                    amazonProduct.freeShipping,
                    amazonProduct.inStock,
                    amazonProduct.information,
                    amazonProduct.category,
                    amazonProduct.imageUrl
                ]);

                const pid = productResult.insertId;
                console.log(`✅ [${totalImported + 1}] Created product: ${amazonProduct.shortTitle} (pid=${pid})`);

                // Step 3: 插入 Amazon 价格到 price 表
                await pool.query(`
                    INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, idInPlatform, link)
                    VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)
                `, [pid, 'Amazon', amazonProduct.price, amazonProduct.freeShipping, amazonProduct.inStock, amazonProduct.idInPlatform, amazonProduct.link]);

                console.log(`   💰 Amazon: $${amazonProduct.price}`);

                // Step 4: 用短标题搜索 ebay
                console.log(`   🔍 Searching eBay with: "${amazonProduct.shortTitle}"`);
                await new Promise(resolve => setTimeout(resolve, 5000));

                const ebayProducts = await fetchFromEbay(amazonProduct.shortTitle, 1);

                if (ebayProducts.length > 0) {
                    // ⭐ 使用智能匹配
                    const ebayProduct = findBestEbayMatch({ title: amazonProduct.fullTitle, price: amazonProduct.price }, ebayProducts);

                    if (ebayProduct && ebayProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, idInPlatform, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)
                        `, [pid, 'eBay', ebayProduct.price, ebayProduct.freeShipping, ebayProduct.inStock, ebayProduct.idInPlatform, ebayProduct.link]);

                        console.log(`   💰 eBay: $${ebayProduct.price}`);
                    }
                }

                // Step 5: 用短标题搜索 Walmart 价格
                console.log(`   🔍 Searching Walmart with: "${amazonProduct.shortTitle}"`);
                await new Promise(resolve => setTimeout(resolve, 5000));

                const walmartProducts = await fetchFromWalmart(amazonProduct.shortTitle, 1);

                if (walmartProducts.length > 0) {
                    // ⭐ 智能匹配（自动过滤二手）
                    const walmartProduct = findBestWalmartMatch({ title: amazonProduct.fullTitle, price: amazonProduct.price }, walmartProducts);

                    if (walmartProduct && walmartProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [pid, 'Walmart', walmartProduct.price, 1, 1, walmartProduct.link]);

                        console.log(`   💰 Walmart: $${walmartProduct.price}`);
                    } else {
                        console.log(`   ⚠️  No suitable Walmart match found`);
                    }
                }

                totalImported++;
                importedProducts.push({
                    pid: pid,
                    short_title: amazonProduct.shortTitle,
                    amazon_price: amazonProduct.price,
                    category: amazonProduct.category
                });

                // 防止 API 限流
                await new Promise(resolve => setTimeout(resolve, 5000));

            } catch (error) {
                console.error(`❌ Failed to import product:`, error.message);
            }
        }

        console.log(`\n✅ Import completed: ${totalImported} products imported`);

        res.json({
            success: true,
            message: `Successfully imported ${totalImported} products with multi-platform prices`,
            totalImported,
            products: importedProducts
        });

    } catch (error) {
        console.error('Import failed:', error);
        res.status(500).json({
            error: 'Import failed',
            details: error.message
        });
    }
});

// 🆕 更新所有产品的价格（多平台）
app.post('/api/admin/update-all-prices', async (req, res) => {
    try {
        console.log('🔄 Starting multi-platform price update (using direct details)...');

        const [dbProducts] = await pool.query(`
            SELECT p.pid, p.title,
                   pr_amazon.idInPlatform AS amazon_asin,
                   pr_walmart.link AS walmart_link,
                   pr_ebay.link AS ebay_link
            FROM products p
            LEFT JOIN (
                SELECT pid, idInPlatform, link
                FROM price
                WHERE platform = 'Amazon'
                  AND id IN (
                      SELECT MAX(id) FROM price WHERE platform = 'Amazon' GROUP BY pid
                  )
            ) pr_amazon ON p.pid = pr_amazon.pid
            LEFT JOIN (
                SELECT pid, link
                FROM price
                WHERE platform = 'Walmart'
                  AND id IN (
                      SELECT MAX(id) FROM price WHERE platform = 'Walmart' GROUP BY pid
                  )
            ) pr_walmart ON p.pid = pr_walmart.pid
            LEFT JOIN (
                SELECT pid, link
                FROM price
                WHERE platform = 'eBay'
                  AND id IN (
                      SELECT MAX(id) FROM price WHERE platform = 'eBay' GROUP BY pid
                  )
            ) pr_ebay ON p.pid = pr_ebay.pid
        `);

        console.log(`📊 Found ${dbProducts.length} products to update`);

        let updatedCount = 0;
        let failedCount = 0;

        for (const dbProduct of dbProducts) {
            try {
                console.log(`\n📦 [${updatedCount + 1}/${dbProducts.length}] ${dbProduct.title.substring(0, 60)}...`);

                // ⭐ 更新 Amazon 价格（使用 ASIN）
                if (dbProduct.amazon_asin) {
                    const amazonDetails = await getAmazonProductDetails(dbProduct.amazon_asin);

                    if (amazonDetails && amazonDetails.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, idInPlatform, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)
                        `, [dbProduct.pid, 'Amazon', amazonDetails.price, amazonDetails.freeShipping,
                            amazonDetails.inStock, dbProduct.amazon_asin, dbProduct.amazon_link || '']);

                        console.log(`   💰 Amazon: $${amazonDetails.price}`);
                    }

                    await new Promise(resolve => setTimeout(resolve, 5000));
                }

                // ⭐ 更新 Walmart 价格（使用 link）
                if (dbProduct.walmart_link) {
                    const walmartDetails = await getWalmartProductDetails(dbProduct.walmart_link);

                    if (walmartDetails && walmartDetails.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Walmart', walmartDetails.price, walmartDetails.freeShipping,
                            walmartDetails.inStock, dbProduct.walmart_link]);

                        console.log(`   💰 Walmart: $${walmartDetails.price}`);
                    }

                    await new Promise(resolve => setTimeout(resolve, 5000));
                }

                // ⭐ 更新 eBay 价格（使用 link）
                if (dbProduct.ebay_link) {
                    const ebayDetails = await getEbayProductDetails(dbProduct.ebay_link);

                    if (ebayDetails && ebayDetails.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'eBay', ebayDetails.price, ebayDetails.freeShipping,
                            ebayDetails.inStock, dbProduct.ebay_link]);

                        console.log(`   💰 eBay: $${ebayDetails.price}`);
                    }

                    await new Promise(resolve => setTimeout(resolve, 5000));
                }

                updatedCount++;

            } catch (error) {
                failedCount++;
                console.error(`❌ Failed to update ${dbProduct.title.substring(0, 40)}:`, error.message);
            }
        }

        console.log(`\n✅ Update completed: ${updatedCount} updated, ${failedCount} failed`);

        res.json({
            success: true,
            message: `Updated ${updatedCount}/${dbProducts.length} products`,
            updatedCount,
            failedCount,
            totalProducts: dbProducts.length
        });

    } catch (error) {
        console.error('Update failed:', error);
        res.status(500).json({
            error: 'Update failed',
            details: error.message
        });
    }
});

// 🆕 补充 Walmart 价格数据
// 功能：根据 products 表的 title 搜索 Walmart，添加到 price 表
app.post('/api/admin/add-walmart-prices', async (req, res) => {
    try {
        console.log('\n🛒 Starting Walmart price supplement...');

        // 获取所有产品
        const [dbProducts] = await pool.query('SELECT pid, title FROM products');
        console.log(`📊 Found ${dbProducts.length} products`);

        let addedCount = 0;
        let skippedCount = 0;
        let failedCount = 0;
        const results = [];

        for (const dbProduct of dbProducts) {
            try {
                console.log(`\n📦 [${addedCount + skippedCount + failedCount + 1}/${dbProducts.length}] ${dbProduct.title.substring(0, 60)}...`);

                // 检查是否已有 Walmart 价格
                const [existing] = await pool.query(`
                    SELECT id FROM price
                    WHERE pid = ? AND platform = 'Walmart'
                    ORDER BY date DESC
                    LIMIT 1
                `, [dbProduct.pid]);

                if (existing.length > 0) {
                    console.log(`   ⏭️  Walmart price already exists, skipping...`);
                    skippedCount++;
                    continue;
                }

                // 搜索 Walmart
                console.log(`   🔍 Searching Walmart with: "${dbProduct.title}"`);
                await new Promise(resolve => setTimeout(resolve, 5000));

                const walmartProducts = await fetchFromWalmart(dbProduct.title, 1);

                if (walmartProducts.length > 0) {
                    // ⭐ 智能匹配
                    const walmartProduct = findBestWalmartMatch(dbProduct, walmartProducts);

                    if (walmartProduct && walmartProduct.price > 0) {
                        // 插入到 price 表
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Walmart', walmartProduct.price, 1, 1, walmartProduct.link]);

                        console.log(`   ✅ Added Walmart price: $${walmartProduct.price}`);
                        addedCount++;

                        results.push({
                            pid: dbProduct.pid,
                            title: dbProduct.title.substring(0, 50),
                            walmart_price: walmartProduct.price
                        });
                    } else {
                        console.log(`   ⚠️  No suitable match`);
                        failedCount++;
                    }
                } else {
                    console.log(`   ⚠️  No Walmart products found`);
                    failedCount++;
                }

            } catch (error) {
                failedCount++;
                console.error(`   ❌ Failed: ${error.message}`);
            }
        }

        console.log(`\n✅ Walmart supplement completed:`);
        console.log(`   Added: ${addedCount}`);
        console.log(`   Skipped (already exists): ${skippedCount}`);
        console.log(`   Failed: ${failedCount}`);
        console.log(`   Total: ${dbProducts.length}`);

        res.json({
            success: true,
            message: `Added ${addedCount} Walmart prices, skipped ${skippedCount}, failed ${failedCount}`,
            addedCount,
            skippedCount,
            failedCount,
            totalProducts: dbProducts.length,
            results: results.slice(0, 10) // 只返回前10个结果
        });

    } catch (error) {
        console.error('Walmart supplement failed:', error);
        res.status(500).json({
            error: 'Walmart supplement failed',
            details: error.message
        });
    }
});

/**
 * 🆕 同步所有产品的 eBay 价格
 * POST /api/admin/sync-ebay-prices
 */
app.post('/api/admin/sync-ebay-prices', async (req, res) => {
    try {
        console.log('\n🔄 Starting eBay price sync for all products...');
        console.log('='.repeat(70));

        const [dbProducts] = await pool.query('SELECT pid, title, short_title FROM products');
        console.log(`📊 Found ${dbProducts.length} products to sync`);

        let syncedCount = 0;
        let failedCount = 0;
        const syncLog = [];

        for (const dbProduct of dbProducts) {
            try {
                // 使用 short_title 搜索（如果没有则使用 title）
                const searchQuery = dbProduct.short_title || dbProduct.title;

                console.log(`\n🔍 [${syncedCount + failedCount + 1}/${dbProducts.length}] Searching eBay for: "${searchQuery.substring(0, 50)}"`);

                // 查询 eBay
                const ebayProducts = await fetchFromEbay(searchQuery, 1);

                if (ebayProducts.length > 0) {
                    // 智能匹配
                    const bestMatch = findBestEbayMatch({ title: dbProduct.title, price: dbProduct.price }, ebayProducts);

                    if (bestMatch && bestMatch.price > 0) {
                        // 检查是否已存在
                        const [existing] = await pool.query(
                            'SELECT id FROM price WHERE pid = ? AND platform = ? AND date >= DATE_SUB(NOW(), INTERVAL 1 DAY)',
                            [dbProduct.pid, 'eBay']
                        );

                        if (existing.length > 0) {
                            // 更新现有记录
                            await pool.query(`
                                UPDATE price
                                SET price = ?, free_shipping = ?, in_stock = ?, link = ?, idInPlatform = ?, date = NOW()
                                WHERE id = ?
                            `, [
                                bestMatch.price,
                                bestMatch.freeShipping,
                                bestMatch.inStock,
                                bestMatch.link,
                                bestMatch.idInPlatform,
                                existing[0].id
                            ]);
                            console.log(`   ✅ Updated eBay price: $${bestMatch.price}`);
                        } else {
                            // 插入新记录
                            await pool.query(`
                                INSERT INTO price (pid, platform, price, free_shipping, in_stock, link, idInPlatform, date)
                                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                            `, [
                                dbProduct.pid,
                                'eBay',
                                bestMatch.price,
                                bestMatch.freeShipping,
                                bestMatch.inStock,
                                bestMatch.link,
                                bestMatch.idInPlatform
                            ]);
                            console.log(`   ✅ Inserted eBay price: $${bestMatch.price}`);
                        }

                        syncedCount++;
                        syncLog.push({
                            pid: dbProduct.pid,
                            title: dbProduct.title.substring(0, 40),
                            ebayPrice: bestMatch.price,
                            ebayId: bestMatch.idInPlatform
                        });
                    } else {
                        console.log(`   ⚠️  No suitable match found`);
                        failedCount++;
                    }
                } else {
                    console.log(`   ⚠️  No results from eBay`);
                    failedCount++;
                }

                // 延迟避免 API 限流
                await new Promise(resolve => setTimeout(resolve, 5000));

            } catch (error) {
                failedCount++;
                console.error(`   ❌ Error syncing ${dbProduct.title}:`, error.message);
            }
        }

        console.log('\n' + '='.repeat(70));
        console.log(`✅ eBay sync completed: ${syncedCount} synced, ${failedCount} failed`);
        console.log('='.repeat(70) + '\n');

        res.json({
            success: true,
            message: `Synced ${syncedCount}/${dbProducts.length} products`,
            syncedCount,
            failedCount,
            totalProducts: dbProducts.length,
            syncLog: syncLog.slice(0, 10)  // 返回前 10 条
        });

    } catch (error) {
        console.error('eBay sync failed:', error);
        res.status(500).json({
            error: 'eBay sync failed',
            details: error.message
        });
    }
});

// ===================================
// API: Wishlist
// 需求：
//  1) 记录用户想要关注的商品 + target_price
//  2) 返回当前用户的 wishlist 列表（带当前最低价）
//  3) 返回当前用户触发降价条件的商品列表（用于 App 推送）
// ===================================

// 获取用户的 wishlist 列表
app.get('/api/wishlist', async (req, res) => {
    try {
        const uid = parseInt(req.query.uid);
        if (!uid) {
            return res.status(400).json({ error: 'uid is required' });
        }

        // 对每个 wishlist 项，查出商品基本信息 + 当前最低价
        const [rows] = await pool.query(`
            SELECT
                w.uid,
                w.pid,
                w.target_price,
                p.short_title,
                p.title,
                p.rating,
                p.category,
                p.image_url,
                -- 当前最低价（从 price 表每日更新的记录中取）
                (
                    SELECT MIN(p1.price)
                    FROM price p1
                    INNER JOIN (
                        SELECT platform, MAX(date) AS max_date
                        FROM price
                        WHERE pid = w.pid
                        GROUP BY platform
                    ) p2 ON p1.platform = p2.platform AND p1.date = p2.max_date
                    WHERE p1.pid = w.pid
                ) AS current_price
            FROM wishlist w
            JOIN products p ON w.pid = p.pid
            WHERE w.uid = ?
            ORDER BY w.created_at DESC
        `, [uid]);

        res.json(rows);
    } catch (error) {
        console.error('Get wishlist error:', error);
        res.status(500).json({ error: error.message });
    }
});

// 添加或更新 wishlist 项（插入或更新 target_price）
app.post('/api/wishlist', async (req, res) => {
    try {
        const { uid, pid, target_price } = req.body;

        if (!uid || !pid || target_price == null) {
            return res.status(400).json({ error: 'uid, pid and target_price are required' });
        }

        // 确保 target_price 是数字
        const tp = parseFloat(target_price);
        if (isNaN(tp) || tp <= 0) {
            return res.status(400).json({ error: 'Invalid target_price' });
        }

        // 插入或更新（uid+pid 唯一）
        await pool.query(`
            INSERT INTO wishlist (uid, pid, target_price)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                target_price = VALUES(target_price),
                updated_at = CURRENT_TIMESTAMP
        `, [uid, pid, tp]);

        res.json({ success: true });
    } catch (error) {
        console.error('Add/Update wishlist error:', error);
        res.status(500).json({ error: error.message });
    }
});

// 从 wishlist 删除某个商品
app.delete('/api/wishlist', async (req, res) => {
    try {
        const { uid, pid } = req.body;

        if (!uid || !pid) {
            return res.status(400).json({ error: 'uid and pid are required' });
        }

        const [result] = await pool.query(
            'DELETE FROM wishlist WHERE uid = ? AND pid = ?',
            [uid, pid]
        );

        if (result.affectedRows > 0) {
            res.json({ success: true });
        } else {
            res.status(404).json({ success: false, error: 'Wishlist item not found' });
        }
    } catch (error) {
        console.error('Delete wishlist error:', error);
        res.status(500).json({ error: error.message });
    }
});

// 获取“已触发降价条件”的商品，用于 App 端推送
app.get('/api/wishlist/alerts', async (req, res) => {
    try {
        const uid = parseInt(req.query.uid);
        if (!uid) {
            return res.status(400).json({ error: 'uid is required' });
        }

        // 逻辑：
//   1. 对当前 uid 的所有 wishlist 项，算出该 pid 的当前最低价 current_price
//   2. 如果 current_price <= target_price，则返回这条记录
        const [rows] = await pool.query(`
            SELECT
                w.uid,
                w.pid,
                w.target_price,
                p.short_title,
                p.title,
                p.category,
                p.image_url,
                lp.current_price
            FROM wishlist w
            JOIN products p ON w.pid = p.pid
            JOIN (
                SELECT
                    w2.pid,
                    MIN(p1.price) AS current_price
                FROM wishlist w2
                JOIN price p1 ON p1.pid = w2.pid
                JOIN (
                    SELECT pid, platform, MAX(date) AS max_date
                    FROM price
                    GROUP BY pid, platform
                ) latest ON latest.pid = p1.pid
                           AND latest.platform = p1.platform
                           AND latest.max_date = p1.date
                WHERE w2.uid = ?
                GROUP BY w2.pid
            ) lp ON lp.pid = w.pid
            WHERE w.uid = ?
              AND lp.current_price IS NOT NULL
              AND lp.current_price <= w.target_price
        `, [uid, uid]);

        res.json(rows);
    } catch (error) {
        console.error('Get wishlist alerts error:', error);
        res.status(500).json({ error: error.message });
    }
});

// =============================
// 通用函数：同步最低价到 products 表
// =============================
async function syncLowestPrices() {
    console.log('\n🔄 Starting to sync lowest prices to products table...');

    // 1) 获取所有产品
    const [products] = await pool.query('SELECT pid FROM products');
    console.log(`📦 Found ${products.length} products to sync`);

    let updatedCount = 0;
    let skippedCount = 0;

    for (const product of products) {
        try {
            // 2) 对每个产品，先拿到「每个平台最新一条价格」，再在这些里面选最低价

            const [rows] = await pool.query(`
                SELECT p1.platform,
                       p1.price,
                       p1.free_shipping,
                       p1.in_stock,
                       p1.date,
                       p1.link
                FROM price p1
                INNER JOIN (
                    SELECT platform, MAX(date) AS max_date
                    FROM price
                    WHERE pid = ?
                    GROUP BY platform
                ) p2
                    ON p1.platform = p2.platform AND p1.date = p2.max_date
                WHERE p1.pid = ?
                ORDER BY p1.price ASC
            `, [product.pid, product.pid]);

            if (rows.length === 0) {
                console.log(`⚠️  [PID ${product.pid}] No price rows, skipped`);
                skippedCount++;
                continue;
            }

            // 3) rows[0] 就是「各平台最新价」里最便宜的那一个
            const best = rows[0];

            // 4) 把最低价平台的价格 / 平台 / free_shipping / in_stock 同步回 products
            await pool.query(`
                UPDATE products
                SET
                    price = ?,
                    platform = ?,
                    free_shipping = ?,
                    in_stock = ?,
                    updated_at = NOW()
                WHERE pid = ?
            `, [
                best.price,
                best.platform,
                best.free_shipping ? 1 : 0,
                best.in_stock ? 1 : 0,
                product.pid
            ]);

            updatedCount++;
            console.log(
                `✅ [PID ${product.pid}] -> ${best.platform}, price=$${best.price}, ` +
                `free_shipping=${best.free_shipping}, in_stock=${best.in_stock}`
            );

        } catch (err) {
            console.error(`❌ [PID ${product.pid}] Sync failed:`, err.message);
        }
    }

    console.log(
        `\n✅ Sync completed: updated=${updatedCount}, skipped=${skippedCount}, total=${products.length}\n`
    );

    return { updatedCount, skippedCount, totalProducts: products.length };
}

// ===================================
// 定时任务：每天凌晨 3 点更新所有产品
// ===================================
cron.schedule('0 3 * * *', async () => {
    console.log('\n⏰ [Scheduled Task] Starting daily price update...');
    console.log(`📅 ${new Date().toLocaleString()}`);

    try {
        const [dbProducts] = await pool.query(`
            SELECT p.pid, p.title,
                   pr_amazon.idInPlatform AS amazon_asin,
                   pr_walmart.link AS walmart_link,
                   pr_ebay.link AS ebay_link
            FROM products p
            LEFT JOIN (
                SELECT pid, idInPlatform
                FROM price
                WHERE platform = 'Amazon'
                  AND id IN (SELECT MAX(id) FROM price WHERE platform = 'Amazon' GROUP BY pid)
            ) pr_amazon ON p.pid = pr_amazon.pid
            LEFT JOIN (
                SELECT pid, link
                FROM price
                WHERE platform = 'Walmart'
                  AND id IN (SELECT MAX(id) FROM price WHERE platform = 'Walmart' GROUP BY pid)
            ) pr_walmart ON p.pid = pr_walmart.pid
            LEFT JOIN (
                SELECT pid, link
                FROM price
                WHERE platform = 'eBay'
                  AND id IN (SELECT MAX(id) FROM price WHERE platform = 'eBay' GROUP BY pid)
            ) pr_ebay ON p.pid = pr_ebay.pid
        `);

        let updatedCount = 0;

        for (const dbProduct of dbProducts) {
            try {
                // 1) 更新 Amazon
                if (dbProduct.amazon_asin) {
                    const amazonDetails = await getAmazonProductDetails(dbProduct.amazon_asin);
                    if (amazonDetails && amazonDetails.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, idInPlatform)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Amazon', amazonDetails.price, amazonDetails.freeShipping,
                            amazonDetails.inStock, dbProduct.amazon_asin]);
                    }
                    await new Promise(resolve => setTimeout(resolve, 5000));
                }

                // 2) 更新 Walmart
                if (dbProduct.walmart_link) {
                    const walmartDetails = await getWalmartProductDetails(dbProduct.walmart_link);
                    if (walmartDetails && walmartDetails.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Walmart', walmartDetails.price, walmartDetails.freeShipping,
                            walmartDetails.inStock, dbProduct.walmart_link]);
                    }
                    await new Promise(resolve => setTimeout(resolve, 5000));
                }

                // 3) 更新 eBay
                if (dbProduct.ebay_link) {
                    const ebayDetails = await getEbayProductDetails(dbProduct.ebay_link);
                    if (ebayDetails && ebayDetails.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'eBay', ebayDetails.price, ebayDetails.freeShipping,
                            ebayDetails.inStock, dbProduct.ebay_link]);
                    }
                    await new Promise(resolve => setTimeout(resolve, 5000));
                }

                updatedCount++;

            } catch (error) {
                console.error(`❌ Failed to update ${dbProduct.title}:`, error.message);
            }
        }

        console.log(`✅ [Scheduled Task] Completed: ${updatedCount}/${dbProducts.length} products updated`);

        // ⭐ 同步最低价
        const syncResult = await syncLowestPrices();
        console.log(`✅ [Scheduled Task] Sync lowest prices: ${syncResult.updatedCount}/${syncResult.totalProducts}`);

    } catch (error) {
        console.error('❌ [Scheduled Task] Failed:', error);
    }
}, {
    timezone: "America/New_York"
});

// ===================================
// 管理接口：手动触发同步最低价到 products
// ===================================
app.post('/api/admin/sync-lowest-prices', async (req, res) => {
    try {
        const result = await syncLowestPrices();
        res.json({
            success: true,
            message: `Synced ${result.updatedCount}/${result.totalProducts} products`,
            ...result
        });
    } catch (error) {
        console.error('❌ /api/admin/sync-lowest-prices failed:', error);
        res.status(500).json({
            success: false,
            error: 'Sync failed',
            detail: error.message
        });
    }
});

// ===================================
// 健康检查和测试端点
// ===================================

app.get('/health', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT 1');

        // 检查是否使用统一的 Key
        const usingUnifiedKey = RAPIDAPI_KEY &&
                                RAPIDAPI_KEYS.amazon === RAPIDAPI_KEY &&
                                RAPIDAPI_KEYS.ebay === RAPIDAPI_KEY &&
                                RAPIDAPI_KEYS.walmart === RAPIDAPI_KEY;

        res.json({
            status: 'OK',
            timestamp: new Date().toISOString(),
            database: 'Connected',
            apiKey: RAPIDAPI_KEY ? 'Configured' : 'Missing',
            apiKeyMode: usingUnifiedKey ? 'Unified (Recommended)' : 'Separate Keys',
            platforms: {
                amazon: RAPIDAPI_KEYS.amazon ? 'Configured' : 'Missing',
                ebay: RAPIDAPI_KEYS.ebay ? 'Configured' : 'Missing',
                walmart: RAPIDAPI_KEYS.walmart ? 'Configured' : 'Missing'
            }
        });
    } catch (error) {
        res.status(500).json({
            status: 'Error',
            database: 'Disconnected',
            error: error.message
        });
    }
});

// ==================== View History API (浏览历史) ====================

/**
 * 获取用户浏览历史（包含产品信息）
 * GET /api/view-history/:uid
 */
app.get('/api/view-history/:uid', async (req, res) => {
  const { uid } = req.params;

  try {
    const query = `
      SELECT
        h.hid,
        h.uid,
        h.pid,
        p.title as product_title,
        p.image_url as product_image,
        p.price as product_price,
        p.platform as product_platform,
        h.viewed_at
      FROM history h
      INNER JOIN products p ON h.pid = p.pid
      WHERE h.uid = ?
      ORDER BY h.viewed_at DESC
      LIMIT 100
    `;

    const [rows] = await pool.query(query, [uid]);
    res.json(rows);
  } catch (error) {
    console.error('Error fetching view history:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to fetch view history'
    });
  }
});

/**
 * 添加浏览记录
 * POST /api/view-history
 * Body: { uid, pid }
 */
app.post('/api/view-history', async (req, res) => {
  const { uid, pid } = req.body;

  if (!uid || !pid) {
    return res.status(400).json({
      success: false,
      message: 'uid and pid are required'
    });
  }

  try {
    // 检查产品是否存在
    const [productCheck] = await pool.query(
      'SELECT pid FROM products WHERE pid = ?',
      [pid]
    );

    if (productCheck.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'Product not found'
      });
    }

    // 插入历史记录
    const query = `
      INSERT INTO history (uid, pid, viewed_at)
      VALUES (?, ?, NOW())
    `;

    const [result] = await pool.query(query, [uid, pid]);

    res.json({
      success: true,
      message: 'View history recorded',
      hid: result.insertId
    });
  } catch (error) {
    console.error('Error adding view history:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to add view history'
    });
  }
});

/**
 * 删除单条历史记录
 * DELETE /api/view-history/:hid
 */
app.delete('/api/view-history/:hid', async (req, res) => {
  const { hid } = req.params;

  try {
    const query = 'DELETE FROM history WHERE hid = ?';
    const [result] = await pool.query(query, [hid]);

    if (result.affectedRows === 0) {
      return res.status(404).json({
        success: false,
        message: 'History record not found'
      });
    }

    res.json({
      success: true,
      message: 'View history deleted'
    });
  } catch (error) {
    console.error('Error deleting view history:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to delete view history'
    });
  }
});

/**
 * 清空用户所有历史记录
 * DELETE /api/view-history/user/:uid
 */
app.delete('/api/view-history/user/:uid', async (req, res) => {
  const { uid } = req.params;

  try {
    const query = 'DELETE FROM history WHERE uid = ?';
    const [result] = await pool.query(query, [uid]);

    res.json({
      success: true,
      message: `Deleted ${result.affectedRows} view history records`
    });
  } catch (error) {
    console.error('Error clearing view history:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to clear view history'
    });
  }
});

// 🆕 测试标题提取
app.get('/api/test/extract-title', (req, res) => {
    const testTitles = [
        "Apple iPhone 15 Pro Max, 256GB, Blue Titanium - Unlocked (Renewed Premium)",
        "Samsung Galaxy S24 Ultra, 512GB, Titanium Gray - Factory Unlocked with 5G",
        "Sony WH-1000XM5 Wireless Noise Cancelling Headphones - Black (International Version)",
        "Dell XPS 13 Laptop, Intel Core i7, 16GB RAM, 512GB SSD - Latest Model"
    ];

    const results = testTitles.map(title => ({
        original: title,
        extracted: extractShortTitle(title)
    }));

    res.json(results);
});

// Favicon
app.get('/favicon.ico', (req, res) => {
    res.status(204).end();
});

// 404 处理
app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint not found' });
});

// 全局错误处理
app.use((err, req, res, next) => {
    console.error('UNCAUGHT ERROR:', err);
    res.status(500).json({ error: 'Internal server error' });
});

// ===================================
// 启动服务器
// ===================================
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
    console.log('='.repeat(70));
    console.log(`🚀 Server running on http://localhost:${PORT}`);
    console.log('='.repeat(70));
    console.log('\n📋 Available Endpoints:');
    console.log('\n👤 User Management:');
    console.log(`   GET    /api/user/:uid              - Get user info`);
    console.log(`   POST   /api/user/login             - User login`);
    console.log(`   POST   /api/user/register          - User registration`);
    console.log(`   PUT    /api/user/:uid              - Update user`);
    console.log(`   DELETE /api/user/:uid              - Delete user`);
    console.log('\n💰 Price API:');
    console.log(`   GET    /api/price/:pid             - Get latest prices by platform`);
    console.log(`   GET    /api/history/:pid?days=7    - Get price history`);
    console.log('\n📦 Product Management:');
    console.log(`   GET    /api/products           - Get all products`);
    console.log(`   GET    /api/products/:pid      - Get single product`);
    console.log(`   POST   /api/admin/import-initial        - Import products (multi-platform)`);
    console.log(`   POST   /api/admin/update-all-prices     - Update all prices (multi-platform)`);
    console.log('\n🔧 System:');
    console.log(`   GET    /api/health                 - Health check`);
    console.log(`   GET    /api/test/extract-title - Test title extraction`);
    console.log('\n⏰ Scheduled Tasks:');
    console.log(`   Daily price update at 3:00 AM EST`);
    console.log('\n' + '='.repeat(70));
});