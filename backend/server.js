// ===================================
// server.js - 合并版 Node.js 后端
// 功能: 产品管理 + 用户管理 + 价格查询
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
const RAPIDAPI_KEY = process.env.RAPIDAPI_KEY;
const RAPIDAPI_HOST = 'real-time-amazon-data.p.rapidapi.com';

/**
 * 从 RapidAPI 获取产品数据
 */
async function fetchFromAmazonAPI(query, page = 1) {
    try {
        console.log(`📡 Fetching from RapidAPI: "${query}" (page ${page})`);

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
            timeout: 30000
        });

        const products = response.data.data?.products || [];
        console.log(`✅ Fetched ${products.length} products from RapidAPI`);
        return products;

    } catch (error) {
        if (error.response) {
            console.error('RapidAPI error response:', {
                status: error.response.status,
                data: error.response.data
            });
            throw new Error(`RapidAPI returned ${error.response.status}`);
        } else if (error.request) {
            console.error('No response from RapidAPI:', error.message);
            throw new Error(`No response from RapidAPI: ${error.message}`);
        } else {
            console.error('Request setup error:', error.message);
            throw error;
        }
    }
}

/**
 * 转换 RapidAPI 产品数据为数据库格式
 */
function transformProduct(apiProduct) {
    return {
        title: apiProduct.product_title || 'Unknown Product',
        price: parsePrice(apiProduct.product_price),
        rating: parseRating(apiProduct.product_star_rating),
        platform: 'Amazon',
        freeShipping: apiProduct.is_prime ? 1 : 0,
        inStock: apiProduct.product_availability?.toLowerCase().includes('in stock') ? 1 : 0,
        information: generateInformation(apiProduct),
        category: categorizeProduct(apiProduct.product_title || ''),
        imageUrl: apiProduct.product_photo || ''
    };
}

// ===================================
// API: 用户管理
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

// 获取产品的最新价格（按平台分组）
app.get('/price/:pid', async (req, res) => {
    try {
        const pid = parseInt(req.params.pid);

        const [rows] = await pool.query(`
            SELECT p1.id, p1.pid, p1.price, p1.date, p1.platform, p1.idInPlatform, p1.link
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
            query += ' AND title LIKE ?';
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

        const [rows] = await pool.query(query, params);
        res.json(rows);
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

// 导入初始产品
app.post('/api/admin/import-initial', async (req, res) => {
    try {
        console.log('\n🚀 Starting initial product import...');

        const queries = [
            'phone', 'laptop', 'headphones', 'smartwatch',
            'makeup', 'skincare', 'furniture', 'kitchen',
            'coffee', 'chocolate', 'shoes', 'bag',
            'fitness', 'yoga mat', 'book', 'toy',
            'vitamin', 'camping', 'office', 'dog food'
        ];

        const importedProducts = [];
        let totalImported = 0;

        for (const query of queries) {
            console.log(`\n📦 Searching for: "${query}"`);

            const apiProducts = await fetchFromAmazonAPI(query, 1);

            if (apiProducts.length === 0) {
                console.log(`⚠️  No products found for "${query}"`);
                continue;
            }

            const product = transformProduct(apiProducts[0]);

            if (product.price === 0) {
                console.log(`⚠️  Skipping product with price 0`);
                continue;
            }

            try {
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

                console.log(`✅ [${totalImported}] Imported: ${product.title.substring(0, 50)}... ($${product.price})`);

            } catch (error) {
                console.error(`❌ Failed to import product:`, error.message);
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

// 更新所有产品的价格
app.post('/api/admin/update-all-prices', async (req, res) => {
    try {
        console.log('🔄 Starting price update for all products...');

        const [dbProducts] = await pool.query('SELECT pid, title FROM products');
        console.log(`📊 Found ${dbProducts.length} products to update`);

        let updatedCount = 0;
        let failedCount = 0;
        const updateLog = [];

        for (const dbProduct of dbProducts) {
            try {
                const apiProducts = await fetchFromAmazonAPI(dbProduct.title, 1);

                if (apiProducts && apiProducts.length > 0) {
                    const latestProduct = transformProduct(apiProducts[0]);

                    if (latestProduct.price > 0) {
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

                        console.log(`✅ Updated [${updatedCount}/${dbProducts.length}] ${dbProduct.title.substring(0, 40)}: $${latestProduct.price}`);
                    }
                } else {
                    console.log(`⚠️  No results for: ${dbProduct.title}`);
                }

                await new Promise(resolve => setTimeout(resolve, 2000));

            } catch (error) {
                failedCount++;
                console.error(`❌ Failed to update ${dbProduct.title}:`, error.message);
            }
        }

        console.log(`\n✅ Update completed: ${updatedCount} updated, ${failedCount} failed`);

        res.json({
            success: true,
            message: `Updated ${updatedCount}/${dbProducts.length} products`,
            updatedCount,
            failedCount,
            totalProducts: dbProducts.length,
            updates: updateLog.slice(0, 10)
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
    console.log('\n⏰ [Scheduled Task] Starting daily price update...');
    console.log(`📅 ${new Date().toLocaleString()}`);

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

                await new Promise(resolve => setTimeout(resolve, 2000));

            } catch (error) {
                console.error(`❌ Failed to update ${dbProduct.title}:`, error.message);
            }
        }

        console.log(`✅ [Scheduled Task] Completed: ${updatedCount}/${dbProducts.length} products updated`);

    } catch (error) {
        console.error('❌ [Scheduled Task] Failed:', error);
    }
}, {
    timezone: "America/New_York"
});

// ===================================
// 健康检查和测试端点
// ===================================

app.get('/health', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT 1');
        res.json({
            status: 'OK',
            timestamp: new Date().toISOString(),
            database: 'Connected',
            rapidApiKey: RAPIDAPI_KEY ? 'Configured' : 'Missing'
        });
    } catch (error) {
        res.status(500).json({
            status: 'Error',
            database: 'Disconnected',
            error: error.message
        });
    }
});

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
    console.log(`   POST   /api/admin/import-initial        - Import 20 products`);
    console.log(`   POST   /api/admin/update-all-prices     - Update all prices`);
    console.log('\n🔧 System:');
    console.log(`   GET    /health                 - Health check`);
    console.log(`   GET    /api/test/transform     - Test data transformation`);
    console.log('\n⏰ Scheduled Tasks:');
    console.log(`   Daily price update at 3:00 AM EST`);
    console.log('\n' + '='.repeat(70));
});