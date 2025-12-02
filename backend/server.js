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
 * 🆕 计算两个字符串的相似度（Levenshtein距离）
 * 返回 0-1 之间的值，1表示完全相同
 */
function calculateSimilarity(str1, str2) {
    if (!str1 || !str2) return 0;

    // 转为小写并移除多余空格
    const s1 = str1.toLowerCase().trim();
    const s2 = str2.toLowerCase().trim();

    if (s1 === s2) return 1;

    // 简单的相似度算法：计算公共词数量
    const words1 = s1.split(/\s+/);
    const words2 = s2.split(/\s+/);

    let matchCount = 0;
    for (const word1 of words1) {
        if (word1.length > 2) { // 只计算长度>2的词
            for (const word2 of words2) {
                if (word1 === word2) {
                    matchCount++;
                    break;
                }
            }
        }
    }

    const maxWords = Math.max(words1.length, words2.length);
    return matchCount / maxWords;
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
        .trim();

    // 分词，过滤无意义词汇
    const words = cleaned.split(/[\s,]+/).filter(w =>
        w.length > 1 &&
        !/^(the|and|or|with|for|by|in|on|at|to|from|of)$/i.test(w)
    );

    // 限制最多 10 个词
    const maxWords = 10;
    const shortWords = words.slice(0, maxWords);

    // 如果有内存/存储信息，确保包含（如果还没超过 10 词）
    const storageMatch = fullTitle.match(/\b(\d+\s*(?:GB|TB|MB))\b/i);
    if (storageMatch && shortWords.length < maxWords && !shortWords.join(' ').includes(storageMatch[1])) {
        shortWords.push(storageMatch[1]);
    }

    const result = shortWords.join(' ');

    // 限制长度不超过 150 字符
    return result.length > 150 ? result.substring(0, 147) + '...' : result;
}

/**
 * 智能分类 - 根据产品标题自动分类
 */
function categorizeProduct(title) {
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

    return 'Electronics';
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
    bestbuy: process.env.RAPIDAPI_KEY_BESTBUY || RAPIDAPI_KEY,
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
 * 🆕 从 BestBuy RapidAPI 获取产品数据
 * API: bestbuy-usa by belchiorarkad
 */
async function fetchFromBestBuy(query, page = 1) {
    try {
        console.log(`🔍 [BestBuy] Searching: "${query}"`);

        if (!RAPIDAPI_KEYS.bestbuy) {
            console.log('⚠️  BestBuy API key not configured, skipping...');
            return [];
        }

        // 使用 BestBuy USA API 的搜索端点
        const response = await axios.get('https://bestbuy-usa.p.rapidapi.com/search', {
            params: {
                query: query,
                page: page.toString()
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.bestbuy,
                'X-RapidAPI-Host': 'bestbuy-usa.p.rapidapi.com'
            }
            // 移除 timeout 限制
        });

        // BestBuy USA API 返回的数据结构
        const products = response.data?.products || response.data?.data?.products || [];
        console.log(`✅ [BestBuy] Found ${products.length} products`);
        return products;

    } catch (error) {
        console.error('❌ [BestBuy] API Error:', error.message);
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
            // 移除 timeout 限制，让请求有足够时间完成
        });

        // searchResult 是一个数组，包含多个 item
        // 每个 item 是一个产品数组，需要遍历所有 item 并合并
        const searchResultArray = response.data?.searchResult || [];

        // 合并所有 item 中的产品
        let allProducts = [];
        for (const item of searchResultArray) {
            if (Array.isArray(item)) {
                allProducts = allProducts.concat(item);
            }
        }

        console.log(`✅ [Walmart] Found ${allProducts.length} products (from ${searchResultArray.length} result groups)`);

        // 如果没找到产品，输出调试信息
        if (allProducts.length === 0) {
            console.log(`🔍 [Walmart] Debug - Response structure:`, JSON.stringify(Object.keys(response.data || {})));
            if (searchResultArray.length > 0) {
                searchResultArray.forEach((item, index) => {
                    console.log(`   searchResult[${index}] length: ${Array.isArray(item) ? item.length : 'not an array'}`);
                });
            }
        }

        // 🆕 如果有多个产品，选择与搜索标题最匹配的
        if (allProducts.length > 1) {
            const productsWithScore = allProducts
                .filter(p => p.name && p.price?.current) // 只保留有名称和价格的
                .map(p => ({
                    product: p,
                    similarity: calculateSimilarity(query, p.name)
                }))
                .sort((a, b) => b.similarity - a.similarity); // 按相似度降序

            if (productsWithScore.length > 0) {
                const best = productsWithScore[0];
                console.log(`📊 [Walmart] Best match: "${best.product.name.substring(0, 60)}..." (similarity: ${(best.similarity * 100).toFixed(1)}%)`);
                console.log(`   Price: $${best.product.price.current}`);
                return [best.product]; // 返回最匹配的产品
            }
        }

        return allProducts;

    } catch (error) {
        if (error.response) {
            console.error('❌ [Walmart] API Error:', {
                status: error.response.status,
                statusText: error.response.statusText,
                url: error.config?.url,
                params: error.config?.params
            });

            // 如果是 404，可能是端点路径错误
            if (error.response.status === 404) {
                console.error('⚠️  [Walmart] 404 Error - Check API endpoint URL');
                console.error('   Current URL: https://walmart-api4.p.rapidapi.com/v1/search');
                console.error('   Make sure you are subscribed to the correct Walmart API');
            }
        } else {
            console.error('❌ [Walmart] API Error:', error.message);
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

    return {
        shortTitle: shortTitle,
        fullTitle: fullTitle,
        price: parsePrice(apiProduct.product_price),
        rating: parseRating(apiProduct.product_star_rating),
        platform: 'Amazon',
        freeShipping: apiProduct.is_prime ? 1 : 0,
        inStock: apiProduct.product_availability?.toLowerCase().includes('in stock') ? 1 : 0,
        information: generateInformation(apiProduct),
        category: categorizeProduct(fullTitle),
        imageUrl: apiProduct.product_photo || '',
        link: apiProduct.product_url || ''
    };
}

/**
 * 🆕 转换 BestBuy 产品数据
 * BestBuy USA API 字段映射：
 * - name/title -> title
 * - price/salePrice/regularPrice -> price
 * - url/productUrl -> link
 * - inStock/availability -> in_stock
 * - freeShipping/shipping -> free_shipping
 */
function transformBestBuyProduct(apiProduct) {
    // 处理价格（可能是 price, salePrice, regularPrice）
    const price = parsePrice(
        apiProduct.price ||
        apiProduct.salePrice ||
        apiProduct.regularPrice ||
        apiProduct.current_price ||
        0
    );

    // 处理包邮（可能是 freeShipping, shipping, shippingCost）
    let freeShipping = false;
    if (apiProduct.freeShipping !== undefined) {
        freeShipping = apiProduct.freeShipping === true;
    } else if (apiProduct.shipping !== undefined) {
        freeShipping = apiProduct.shipping === 'Free' || apiProduct.shipping === 0;
    } else if (apiProduct.shippingCost !== undefined) {
        freeShipping = apiProduct.shippingCost === 0 || apiProduct.shippingCost === '0' || apiProduct.shippingCost === 'Free';
    }

    // 处理库存（可能是 inStock, availability, stock）
    let inStock = true; // 默认有货
    if (apiProduct.inStock !== undefined) {
        inStock = apiProduct.inStock === true;
    } else if (apiProduct.availability !== undefined) {
        const avail = String(apiProduct.availability).toLowerCase();
        inStock = avail.includes('in stock') || avail.includes('available');
    } else if (apiProduct.stock !== undefined) {
        inStock = apiProduct.stock > 0 || apiProduct.stock === 'In Stock';
    }

    // 处理链接
    const link = apiProduct.url || apiProduct.productUrl || apiProduct.link || '';

    return {
        price: price,
        platform: 'BestBuy',
        freeShipping: freeShipping ? 1 : 0,
        inStock: inStock ? 1 : 0,
        link: link,
        information: '' // BestBuy 不需要 information，用完整标题
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
    const link = apiProduct.canonicalUrl || apiProduct.url || apiProduct.productUrl || '';

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
 * 🆕 计算两个标题的相似度（简单版本）
 */
function calculateSimilarity(title1, title2) {
    const words1 = title1.toLowerCase().split(/\s+/);
    const words2 = title2.toLowerCase().split(/\s+/);

    const commonWords = words1.filter(w => words2.includes(w));
    const similarity = commonWords.length / Math.max(words1.length, words2.length);

    return similarity;
}

// ===================================
// API: 用户管理（保持不变）
// ===================================

// 获取用户信息
app.get('/user/:uid', async (req, res) => {
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
app.post('/user/login', async (req, res) => {
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
app.post('/user/register', async (req, res) => {
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
app.put('/user/:uid', async (req, res) => {
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
app.delete('/user/:uid', async (req, res) => {
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
app.get('/price/:pid', async (req, res) => {
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
app.get('/history/:pid', async (req, res) => {
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
            'iPhone 15 Pro', 'MacBook Air', 'AirPods Pro', 'Apple Watch',
            'Samsung Galaxy', 'Dell laptop', 'Sony headphones', 'LG TV',
            'Dyson vacuum', 'KitchenAid mixer', 'Ninja blender', 'Instant Pot',
            'Lego set', 'Nintendo Switch', 'PlayStation 5', 'Xbox Series',
            'Fitbit', 'Kindle', 'Ring doorbell', 'Echo Dot'
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
                    INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                    VALUES (?, ?, ?, ?, ?, NOW(), ?)
                `, [pid, 'Amazon', amazonProduct.price, amazonProduct.freeShipping, amazonProduct.inStock, amazonProduct.link]);

                console.log(`   💰 Amazon: $${amazonProduct.price}`);

                // Step 4: 用原始完整标题搜索 BestBuy
                console.log(`   🔍 Searching BestBuy with: "${amazonProduct.fullTitle}"`);

                await new Promise(resolve => setTimeout(resolve, 2000)); // 防止 API 限流
                const bestbuyProducts = await fetchFromBestBuy(amazonProduct.fullTitle, 1);

                if (bestbuyProducts.length > 0) {
                    const bestbuyProduct = transformBestBuyProduct(bestbuyProducts[0]);

                    if (bestbuyProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [pid, 'BestBuy', bestbuyProduct.price, bestbuyProduct.freeShipping, bestbuyProduct.inStock, bestbuyProduct.link]);

                        console.log(`   💰 BestBuy: $${bestbuyProduct.price}`);
                    }
                }

                // Step 5: 用原始完整标题搜索 Walmart 价格
                console.log(`   🔍 Searching Walmart with: "${amazonProduct.fullTitle}"`);

                await new Promise(resolve => setTimeout(resolve, 2000));
                const walmartProducts = await fetchFromWalmart(amazonProduct.fullTitle, 1);

                if (walmartProducts.length > 0) {
                    const walmartProduct = transformWalmartProduct(walmartProducts[0]);

                    if (walmartProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [pid, 'Walmart', walmartProduct.price, walmartProduct.freeShipping, walmartProduct.inStock, walmartProduct.link]);

                        console.log(`   💰 Walmart: $${walmartProduct.price}`);
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
                await new Promise(resolve => setTimeout(resolve, 2000));

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
        console.log('🔄 Starting multi-platform price update...');

        const [dbProducts] = await pool.query('SELECT pid, title FROM products');
        console.log(`📊 Found ${dbProducts.length} products to update`);

        let updatedCount = 0;
        let failedCount = 0;

        for (const dbProduct of dbProducts) {
            try {
                console.log(`\n📦 [${updatedCount + 1}/${dbProducts.length}] ${dbProduct.title.substring(0, 60)}...`);
                console.log(`   🔍 Searching with: "${dbProduct.title}"`);

                // 更新 Amazon 价格
                const amazonProducts = await fetchFromAmazon(dbProduct.title, 1);
                if (amazonProducts.length > 0) {
                    const amazonProduct = transformAmazonProduct(amazonProducts[0]);
                    if (amazonProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Amazon', amazonProduct.price, amazonProduct.freeShipping, amazonProduct.inStock, amazonProduct.link]);

                        console.log(`   💰 Amazon: $${amazonProduct.price}`);
                    }
                }

                await new Promise(resolve => setTimeout(resolve, 2000));

                // 更新 BestBuy 价格
                const bestbuyProducts = await fetchFromBestBuy(dbProduct.title, 1);
                if (bestbuyProducts.length > 0) {
                    const bestbuyProduct = transformBestBuyProduct(bestbuyProducts[0]);
                    if (bestbuyProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'BestBuy', bestbuyProduct.price, bestbuyProduct.freeShipping, bestbuyProduct.inStock, bestbuyProduct.link]);

                        console.log(`   💰 BestBuy: $${bestbuyProduct.price}`);
                    }
                }

                await new Promise(resolve => setTimeout(resolve, 2000));

                // 更新 Walmart 价格
                const walmartProducts = await fetchFromWalmart(dbProduct.title, 1);
                if (walmartProducts.length > 0) {
                    const walmartProduct = transformWalmartProduct(walmartProducts[0]);
                    if (walmartProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Walmart', walmartProduct.price, walmartProduct.freeShipping, walmartProduct.inStock, walmartProduct.link]);

                        console.log(`   💰 Walmart: $${walmartProduct.price}`);
                    }
                }

                updatedCount++;
                await new Promise(resolve => setTimeout(resolve, 2000));

            } catch (error) {
                failedCount++;
                console.error(`❌ Failed to update ${dbProduct.title.substring(0, 40)}:`, error.message);
            }
        }

        console.log(`\n✅ Update completed: ${updatedCount} updated, ${failedCount} failed`);

        res.json({
            success: true,
            message: `Updated prices for ${updatedCount}/${dbProducts.length} products`,
            updatedCount,
            failedCount,
            totalProducts: dbProducts.length
        });

    } catch (error) {
        console.error('Batch update failed:', error);
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
                await new Promise(resolve => setTimeout(resolve, 2000)); // 防止限流

                const walmartProducts = await fetchFromWalmart(dbProduct.title, 1);

                if (walmartProducts.length > 0) {
                    const walmartProduct = transformWalmartProduct(walmartProducts[0]);

                    if (walmartProduct.price > 0) {
                        // 插入到 price 表
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Walmart', walmartProduct.price, walmartProduct.freeShipping, walmartProduct.inStock, walmartProduct.link]);

                        console.log(`   ✅ Added Walmart price: $${walmartProduct.price}`);
                        addedCount++;

                        results.push({
                            pid: dbProduct.pid,
                            title: dbProduct.title.substring(0, 50),
                            walmart_price: walmartProduct.price,
                            free_shipping: walmartProduct.freeShipping === 1,
                            in_stock: walmartProduct.inStock === 1
                        });
                    } else {
                        console.log(`   ⚠️  Invalid price (0), skipping...`);
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
// 抽出来的通用函数：同步最低价到 products 表
// =============================
async function syncLowestPrices() {
    console.log('\n🔄 Starting to sync lowest prices to products table...');

    // 1. 获取所有产品
    const [products] = await pool.query('SELECT pid FROM products');
    console.log(`📦 Found ${products.length} products to sync`);

    let updatedCount = 0;
    let skippedCount = 0;

    for (const product of products) {
        try {
            // 2. 获取该产品所有平台的最新价格（按价格升序）
            const [prices] = await pool.query(`
                SELECT p1.platform, p1.price, p1.free_shipping, p1.in_stock, p1.link
                FROM price p1
                INNER JOIN (
                    SELECT platform, MAX(date) AS max_date
                    FROM price
                    WHERE pid = ?
                    GROUP BY platform
                ) p2 ON p1.platform = p2.platform AND p1.date = p2.max_date
                WHERE p1.pid = ?
                ORDER BY p1.price ASC
            `, [product.pid, product.pid]);

            if (prices.length === 0) {
                console.log(`⚠️  [PID ${product.pid}] No prices found, skipping...`);
                skippedCount++;
                continue;
            }

            // 3. 找到最低价（第一条就是最低价）
            const lowestPrice = prices[0];

            // 4. 更新 products 表的 price / platform / free_shipping / in_stock
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
                lowestPrice.price,
                lowestPrice.platform,
                lowestPrice.free_shipping ? 1 : 0, // 保证是 0/1
                lowestPrice.in_stock ? 1 : 0,       // 保证是 0/1
                product.pid
            ]);

            updatedCount++;
            console.log(`✅ [PID ${product.pid}] Updated: $${lowestPrice.price} from ${lowestPrice.platform} (FS=${lowestPrice.free_shipping}, IS=${lowestPrice.in_stock})`);

        } catch (error) {
            console.error(`❌ [PID ${product.pid}] Failed:`, error.message);
        }
    }

    console.log(`\n✅ Sync completed: ${updatedCount} updated, ${skippedCount} skipped\n`);

    // 返回给调用方使用
    return {
        updatedCount,
        skippedCount,
        totalProducts: products.length
    };
}

// ===================================
// 定时任务：每天凌晨 3 点更新所有产品
// ===================================
cron.schedule('0 3 * * *', async () => {
    console.log('\n⏰ [Scheduled Task] Starting daily price update...');
    console.log(`📅 ${new Date().toLocaleString()}`);

    try {
        const [dbProducts] = await pool.query('SELECT pid, title FROM products');

        let updatedCount = 0;

        for (const dbProduct of dbProducts) {
            try {
                // 1) 更新 Amazon 价格
                const amazonProducts = await fetchFromAmazon(dbProduct.title, 1);
                if (amazonProducts.length > 0) {
                    const amazonProduct = transformAmazonProduct(amazonProducts[0]);
                    if (amazonProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Amazon', amazonProduct.price, amazonProduct.freeShipping, amazonProduct.inStock, amazonProduct.link]);
                    }
                }

                await new Promise(resolve => setTimeout(resolve, 2000));

                // 2) 更新 BestBuy
                const bestbuyProducts = await fetchFromBestBuy(dbProduct.title, 1);
                if (bestbuyProducts.length > 0) {
                    const bestbuyProduct = transformBestBuyProduct(bestbuyProducts[0]);
                    if (bestbuyProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'BestBuy', bestbuyProduct.price, bestbuyProduct.freeShipping, bestbuyProduct.inStock, bestbuyProduct.link]);
                    }
                }

                await new Promise(resolve => setTimeout(resolve, 2000));

                // 3) 更新 Walmart
                const walmartProducts = await fetchFromWalmart(dbProduct.title, 1);
                if (walmartProducts.length > 0) {
                    const walmartProduct = transformWalmartProduct(walmartProducts[0]);
                    if (walmartProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Walmart', walmartProduct.price, walmartProduct.freeShipping, walmartProduct.inStock, walmartProduct.link]);
                    }
                }

                updatedCount++;
                await new Promise(resolve => setTimeout(resolve, 2000));

            } catch (error) {
                console.error(`❌ Failed to update ${dbProduct.title.substring(0, 40)}:`, error.message);
            }
        }

        console.log(`✅ [Scheduled Task] Completed: ${updatedCount}/${dbProducts.length} products updated`);

        // ⭐ 更新完 price 表之后，再同步 products 的最低价 / 包邮 / 库存字段
        try {
            const syncResult = await syncLowestPrices();
            console.log(`✅ [Scheduled Task] Sync lowest prices done: ${syncResult.updatedCount}/${syncResult.totalProducts} products updated`);
        } catch (syncError) {
            console.error('❌ [Scheduled Task] Sync lowest prices failed:', syncError);
        }

    } catch (error) {
        console.error('❌ [Scheduled Task] Failed:', error);
    }
}, {
    timezone: "America/New_York"
});

// ===================================
// 新增接口：更新 products 表的最低价信息
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
        console.error('Sync failed:', error);
        res.status(500).json({
            error: 'Sync failed',
            details: error.message
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
                                RAPIDAPI_KEYS.bestbuy === RAPIDAPI_KEY &&
                                RAPIDAPI_KEYS.walmart === RAPIDAPI_KEY;

        res.json({
            status: 'OK',
            timestamp: new Date().toISOString(),
            database: 'Connected',
            apiKey: RAPIDAPI_KEY ? 'Configured' : 'Missing',
            apiKeyMode: usingUnifiedKey ? 'Unified (Recommended)' : 'Separate Keys',
            platforms: {
                amazon: RAPIDAPI_KEYS.amazon ? 'Configured' : 'Missing',
                bestbuy: RAPIDAPI_KEYS.bestbuy ? 'Configured' : 'Missing',
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
    console.log(`   GET    /user/:uid              - Get user info`);
    console.log(`   POST   /user/login             - User login`);
    console.log(`   POST   /user/register          - User registration`);
    console.log(`   PUT    /user/:uid              - Update user`);
    console.log(`   DELETE /user/:uid              - Delete user`);
    console.log('\n💰 Price API:');
    console.log(`   GET    /price/:pid             - Get latest prices by platform`);
    console.log(`   GET    /history/:pid?days=7    - Get price history`);
    console.log('\n📦 Product Management:');
    console.log(`   GET    /api/products           - Get all products`);
    console.log(`   GET    /api/products/:pid      - Get single product`);
    console.log(`   POST   /api/admin/import-initial        - Import products (multi-platform)`);
    console.log(`   POST   /api/admin/update-all-prices     - Update all prices (multi-platform)`);
    console.log('\n🔧 System:');
    console.log(`   GET    /health                 - Health check`);
    console.log(`   GET    /api/test/extract-title - Test title extraction`);
    console.log('\n⏰ Scheduled Tasks:');
    console.log(`   Daily price update at 3:00 AM EST`);
    console.log('\n' + '='.repeat(70));
});