// ===================================
// server.js - Node.js 后端优化版
// 完整映射所有字段：title, price, rating, platform, free_shipping, in_stock, information, category, image_url
// ===================================

const express = require('express');
const mysql = require('mysql2/promise');
const axios = require('axios');
const cors = require('cors');
const cron = require('node-cron');
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
        console.log('Database connected successfully');
        connection.release();
    })
    .catch(err => console.error('Database connection failed:', err));

// ===================================
// RapidAPI 配置
// ===================================
const RAPIDAPI_KEY = process.env.RAPIDAPI_KEY;
const RAPIDAPI_HOST = 'real-time-amazon-data.p.rapidapi.com';

/**
 * 从 RapidAPI 获取产品数据
 */
async function fetchFromAmazonAPI(query, page = 1) {
    try {
        console.log(`📡 Fetching from RapidAPI: "${query}" (page ${page})`);

        // 检查 API Key
        if (!RAPIDAPI_KEY || RAPIDAPI_KEY === 'YOUR_RAPIDAPI_KEY_HERE') {
            throw new Error('RapidAPI Key is not configured in .env file');
        }

        const response = await axios.get(`https://${RAPIDAPI_HOST}/search`, {
            params: {
                query: query,
                page: page.toString(),
                country: 'US'
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEY,
                'X-RapidAPI-Host': RAPIDAPI_HOST
            },
            timeout: 30000  // 30 秒超时
        });

        const products = response.data.data?.products || [];
        console.log(`Fetched ${products.length} products from RapidAPI`);
        return products;

    } catch (error) {
        // 详细错误日志
        if (error.response) {
            // API 返回了错误响应
            console.error('RapidAPI error response:', {
                status: error.response.status,
                statusText: error.response.statusText,
                data: error.response.data,
                headers: error.response.headers
            });
            throw new Error(`RapidAPI returned ${error.response.status}: ${JSON.stringify(error.response.data)}`);
        } else if (error.request) {
            // 请求发送了但没有收到响应
            console.error('No response from RapidAPI:', error.message);
            throw new Error(`No response from RapidAPI: ${error.message}`);
        } else {
            // 请求配置错误
            console.error('Request setup error:', error.message);
            throw error;
        }
    }
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
 * 智能分类 - 根据产品标题自动分类（12个分类）
 */
function categorizeProduct(title) {
    const lower = title.toLowerCase();

    // Electronics - 电子产品
    if (lower.match(/phone|laptop|tablet|computer|headphone|speaker|camera|tv|monitor|keyboard|mouse|smartwatch|earbuds|airpods|ipad|macbook|gaming|console|playstation|xbox|nintendo|electronics|cable|charger|adapter|router|printer/)) {
        return 'Electronics';
    }

    // Beauty - 美妆
    if (lower.match(/beauty|makeup|skincare|cosmetic|perfume|fragrance|lipstick|foundation|serum|moisturizer|shampoo|conditioner|lotion|cream|mascara|eyeliner|nail polish/)) {
        return 'Beauty';
    }

    // Home - 家居
    if (lower.match(/furniture|kitchen|home|bedding|decor|lamp|chair|table|sofa|pillow|blanket|curtain|rug|vacuum|appliance|cookware|utensil|storage|organizer/)) {
        return 'Home';
    }

    // Food - 食品
    if (lower.match(/food|snack|coffee|tea|chocolate|candy|grocery|organic|protein|vitamin|supplement|chips|cookies|cereal|pasta|sauce|spice/)) {
        return 'Food';
    }

    // Fashion - 时尚
    if (lower.match(/clothing|shoes|dress|shirt|pants|jacket|coat|boots|sneakers|fashion|bag|wallet|jewelry|sunglasses|hat|scarf|gloves|belt|tie/)) {
        return 'Fashion';
    }

    // Sports - 运动
    if (lower.match(/sports|fitness|gym|yoga|exercise|bike|bicycle|treadmill|dumbbell|weights|running|tennis|basketball|soccer|football|swimming/)) {
        return 'Sports';
    }

    // Books - 图书
    if (lower.match(/book|novel|textbook|kindle|ebook|magazine|comic|manga|cookbook|guide|dictionary|encyclopedia|bestseller|paperback|hardcover/)) {
        return 'Books';
    }

    // Toys - 玩具
    if (lower.match(/toy|doll|lego|puzzle|board game|action figure|stuffed animal|playset|barbie|hot wheels|nerf|pokemon|minecraft/)) {
        return 'Toys';
    }

    // Health - 健康
    if (lower.match(/health|medical|medicine|thermometer|blood pressure|first aid|bandage|supplements|probiotic|immune|pain relief|aspirin|allergy/)) {
        return 'Health';
    }

    // Outdoors - 户外
    if (lower.match(/outdoor|camping|hiking|tent|backpack|sleeping bag|flashlight|lantern|fishing|hunting|survival|compass|binoculars/)) {
        return 'Outdoors';
    }

    // Office - 办公
    if (lower.match(/office|desk|pen|pencil|notebook|paper|stapler|folder|calculator|planner|marker|highlighter|binder|organizer|supplies/)) {
        return 'Office';
    }

    // Pets - 宠物
    if (lower.match(/pet|dog|cat|puppy|kitten|fish|bird|hamster|collar|leash|food|treat|toy|bed|cage|aquarium|litter/)) {
        return 'Pets';
    }

    // 默认分类
    return 'Electronics';
}

/**
 * 生成产品详情信息 - 整合多个字段
 */
function generateInformation(product) {
    const info = [];

    // ASIN
    if (product.asin) {
        info.push(`ASIN: ${product.asin}`);
    }

    // Prime 会员
    if (product.is_prime) {
        info.push('Prime Eligible');
    }

    // Best Seller
    if (product.is_best_seller) {
        info.push('Best Seller');
    }

    // Amazon's Choice
    if (product.is_amazon_choice) {
        info.push("Amazon's Choice");
    }

    // 评价数量
    if (product.product_num_ratings) {
        info.push(`${product.product_num_ratings.toLocaleString()} ratings`);
    }

    // 销量
    if (product.sales_volume) {
        info.push(`Sales: ${product.sales_volume}`);
    }

    // 配送信息
    if (product.delivery) {
        info.push(`Delivery: ${product.delivery}`);
    }

    // Climate Pledge Friendly
    if (product.climate_pledge_friendly) {
        info.push('Climate Pledge Friendly');
    }

    return info.join(' • ') || 'No additional information';
}

/**
 * 转换 RapidAPI 产品数据为数据库格式
 */
function transformProduct(apiProduct) {
    return {
        title: apiProduct.product_title || 'Unknown Product',
        price: parsePrice(apiProduct.product_price),
        rating: parseRating(apiProduct.product_star_rating),
        platform: 'Amazon',  // RapidAPI 只返回 Amazon 数据
        freeShipping: apiProduct.is_prime ? 1 : 0,
        inStock: (apiProduct.product_availability || '').toLowerCase().includes('in stock') ? 1 : 0,
        information: generateInformation(apiProduct),
        category: categorizeProduct(apiProduct.product_title || ''),
        imageUrl: apiProduct.product_photo || ''
    };
}

// ===================================
// API 路由 - Android 调用
// ===================================

// 获取所有产品
app.get('/api/products', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM products ORDER BY pid DESC');
        res.json(rows);
    } catch (error) {
        console.error('Error:', error);
        res.status(500).json({ error: 'Failed to fetch products' });
    }
});

// 根据 ID 获取产品
app.get('/api/products/:pid', async (req, res) => {
    try {
        const { pid } = req.params;
        const [rows] = await pool.query('SELECT * FROM products WHERE pid = ?', [pid]);

        if (rows.length === 0) {
            return res.status(404).json({ error: 'Product not found' });
        }

        res.json(rows[0]);
    } catch (error) {
        console.error('Error:', error);
        res.status(500).json({ error: 'Failed to fetch product' });
    }
});

// 搜索产品
app.get('/api/products/search', async (req, res) => {
    try {
        const { query } = req.query;
        const [rows] = await pool.query(
            'SELECT * FROM products WHERE title LIKE ? ORDER BY rating DESC',
            [`%${query}%`]
        );
        res.json(rows);
    } catch (error) {
        console.error('Error:', error);
        res.status(500).json({ error: 'Search failed' });
    }
});

// 按平台筛选
app.get('/api/products/platform/:platform', async (req, res) => {
    try {
        const { platform } = req.params;
        const [rows] = await pool.query(
            'SELECT * FROM products WHERE platform = ? ORDER BY rating DESC',
            [platform]
        );
        res.json(rows);
    } catch (error) {
        console.error('Error:', error);
        res.status(500).json({ error: 'Filter failed' });
    }
});

// 按价格区间筛选
app.get('/api/products/price-range', async (req, res) => {
    try {
        const { minPrice, maxPrice } = req.query;
        const [rows] = await pool.query(
            'SELECT * FROM products WHERE price BETWEEN ? AND ? ORDER BY price ASC',
            [minPrice || 0, maxPrice || 99999]
        );
        res.json(rows);
    } catch (error) {
        console.error('Error:', error);
        res.status(500).json({ error: 'Filter failed' });
    }
});

// ===================================
// 管理员路由 - 数据导入和更新
// ===================================

// 首次导入 20 个产品（完整字段映射）
app.post('/api/admin/import-initial', async (req, res) => {
    try {
        console.log('Starting initial import (20 products with complete data)...');

        // 搜索热门产品类别
        const queries = ['electronics bestseller', 'phone'];
        let totalImported = 0;
        const importedProducts = [];

        for (const query of queries) {
            const apiProducts = await fetchFromAmazonAPI(query, 1);

            // 只取前 10 个
            const productsToImport = apiProducts.slice(0, 10);

            for (const apiProduct of productsToImport) {
                try {
                    // 转换数据
                    const product = transformProduct(apiProduct);

                    // 验证必填字段
                    if (!product.title || product.price <= 0) {
                        console.log(`Skipping invalid product: ${product.title}`);
                        continue;
                    }

                    // 插入数据库
                    const [result] = await pool.query(`
                        INSERT INTO products
                        (title, price, rating, platform, free_shipping, in_stock, information, category, image_url)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    `, [
                        product.title,
                        product.price,
                        product.rating,
                        product.platform,
                        product.freeShipping,
                        product.inStock,
                        product.information,
                        product.category,
                        product.imageUrl
                    ]);

                    totalImported++;
                    importedProducts.push({
                        pid: result.insertId,
                        title: product.title.substring(0, 50),
                        price: product.price,
                        category: product.category
                    });

                    console.log(`[${totalImported}] Imported: ${product.title.substring(0, 50)}... ($${product.price})`);

                } catch (error) {
                    console.error(`Failed to import product:`, error.message);
                }
            }

            // 避免 API 限流
            await new Promise(resolve => setTimeout(resolve, 2000));
        }

        res.json({
            success: true,
            message: `Successfully imported ${totalImported} products`,
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

// 更新所有产品的价格和信息
app.post('/api/admin/update-all-prices', async (req, res) => {
    try {
        console.log('Starting price update for all products...');

        // 获取数据库中的所有产品
        const [dbProducts] = await pool.query('SELECT pid, title FROM products');

        console.log(`Found ${dbProducts.length} products to update`);

        let updatedCount = 0;
        let failedCount = 0;
        const updateLog = [];

        for (const dbProduct of dbProducts) {
            try {
                // 搜索产品的最新数据
                const apiProducts = await fetchFromAmazonAPI(dbProduct.title, 1);

                if (apiProducts && apiProducts.length > 0) {
                    const latestProduct = transformProduct(apiProducts[0]);

                    if (latestProduct.price > 0) {
                        // 更新所有字段（除了 title 和 pid）
                        await pool.query(`
                            UPDATE products
                            SET
                                price = ?,
                                rating = ?,
                                free_shipping = ?,
                                in_stock = ?,
                                information = ?,
                                image_url = ?,
                                updated_at = NOW()
                            WHERE pid = ?
                        `, [
                            latestProduct.price,
                            latestProduct.rating,
                            latestProduct.freeShipping,
                            latestProduct.inStock,
                            latestProduct.information,
                            latestProduct.imageUrl,
                            dbProduct.pid
                        ]);

                        updatedCount++;
                        updateLog.push({
                            pid: dbProduct.pid,
                            title: dbProduct.title.substring(0, 40),
                            newPrice: latestProduct.price
                        });

                        console.log(`Updated [${updatedCount}/${dbProducts.length}] ${dbProduct.title.substring(0, 40)}: $${latestProduct.price}`);
                    }
                } else {
                    console.log(`No results for: ${dbProduct.title}`);
                }

                // 避免 API 限流（每个请求间隔 2 秒）
                await new Promise(resolve => setTimeout(resolve, 2000));

            } catch (error) {
                failedCount++;
                console.error(`Failed to update ${dbProduct.title}:`, error.message);
            }
        }

        console.log(`\nUpdate completed: ${updatedCount} updated, ${failedCount} failed`);

        res.json({
            success: true,
            message: `Updated ${updatedCount}/${dbProducts.length} products`,
            updatedCount,
            failedCount,
            totalProducts: dbProducts.length,
            updates: updateLog.slice(0, 10)  // 返回前 10 个更新记录
        });

    } catch (error) {
        console.error('Batch update failed:', error);
        res.status(500).json({
            error: 'Update failed',
            details: error.message
        });
    }
});

// ===================================
// 定时任务：每天凌晨 3 点更新所有产品
// ===================================
cron.schedule('0 3 * * *', async () => {
    console.log('\n[Scheduled Task] Starting daily price update...');
    console.log(`${new Date().toLocaleString()}`);

    try {
        const [dbProducts] = await pool.query('SELECT pid, title FROM products');

        let updatedCount = 0;

        for (const dbProduct of dbProducts) {
            try {
                const apiProducts = await fetchFromAmazonAPI(dbProduct.title, 1);

                if (apiProducts && apiProducts.length > 0) {
                    const latestProduct = transformProduct(apiProducts[0]);

                    if (latestProduct.price > 0) {
                        await pool.query(`
                            UPDATE products
                            SET price = ?, rating = ?, free_shipping = ?, in_stock = ?, information = ?, image_url = ?
                            WHERE pid = ?
                        `, [
                            latestProduct.price,
                            latestProduct.rating,
                            latestProduct.freeShipping,
                            latestProduct.inStock,
                            latestProduct.information,
                            latestProduct.imageUrl,
                            dbProduct.pid
                        ]);
                        updatedCount++;
                    }
                }

                // 避免 API 限流
                await new Promise(resolve => setTimeout(resolve, 2000));

            } catch (error) {
                console.error(`Failed to update ${dbProduct.title}:`, error.message);
            }
        }

        console.log(`[Scheduled Task] Completed: ${updatedCount}/${dbProducts.length} products updated`);

    } catch (error) {
        console.error('[Scheduled Task] Failed:', error);
    }
}, {
    timezone: "America/New_York"
});

// ===================================
// 健康检查
// ===================================
app.get('/health', (req, res) => {
    res.json({
        status: 'OK',
        timestamp: new Date().toISOString(),
        database: pool.pool._allConnections.length > 0 ? 'Connected' : 'Disconnected',
        rapidApiKey: RAPIDAPI_KEY ? 'Configured' : 'Missing'
    });
});

// 测试数据转换
app.get('/api/test/transform', async (req, res) => {
    try {
        const products = await fetchFromAmazonAPI('phone', 1);
        if (products.length > 0) {
            const sample = transformProduct(products[0]);
            res.json({
                original: products[0],
                transformed: sample
            });
        } else {
            res.json({ error: 'No products found' });
        }
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ===================================
// 启动服务器
// ===================================
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
    console.log('='.repeat(60));
    console.log(`Server running on http://localhost:${PORT}`);
    console.log(`API Endpoint: http://localhost:${PORT}/api/products`);
    console.log(`RapidAPI Key: ${RAPIDAPI_KEY ? 'Configured' : 'Missing'}`);
    console.log(`Daily update scheduled at 3:00 AM EST`);
    console.log('='.repeat(60));
    console.log('\nAvailable Commands:');
    console.log('1. Import 20 products:');
    console.log('   POST http://localhost:8080/api/admin/import-initial\n');
    console.log('2. Update all prices:');
    console.log('   POST http://localhost:8080/api/admin/update-all-prices\n');
    console.log('3. Test data transformation:');
    console.log('   GET http://localhost:8080/api/test/transform\n');
    console.log('4. Android access:');
    console.log('   GET http://10.0.2.2:8080/api/products\n');
});