// DealTracker Backend Server
// Main entry point for the application

const express = require('express');
const cors = require('cors');
const cron = require('node-cron');
require('dotenv').config();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Import routes
const userRoutes = require('./routes/userRoutes');
const productRoutes = require('./routes/productRoutes');
const priceRoutes = require('./routes/priceRoutes');
const historyRoutes = require('./routes/historyRoutes');
const wishlistRoutes = require('./routes/wishlistRoutes');
const viewHistoryRoutes = require('./routes/viewHistoryRoutes');
const adminRoutes = require('./routes/adminRoutes');

// Register routes
app.use('/api/user', userRoutes);
app.use('/api/products', productRoutes);
app.use('/api/price', priceRoutes);
app.use('/api/history', historyRoutes);
app.use('/api/wishlist', wishlistRoutes);
app.use('/api/view-history', viewHistoryRoutes);
app.use('/api/admin', adminRoutes);

// Health check endpoint
// GET /health
app.get('/health', async (req, res) => {
    const pool = require('./config/database');
    const { RAPIDAPI_KEY, RAPIDAPI_KEYS } = require('./config/api');

    try {
        await pool.query('SELECT 1');

        const usingUnifiedKey = RAPIDAPI_KEY &&
            RAPIDAPI_KEYS.amazon === RAPIDAPI_KEY &&
            RAPIDAPI_KEYS.ebay === RAPIDAPI_KEY &&
            RAPIDAPI_KEYS.walmart === RAPIDAPI_KEY;

        res.json({
            status: 'OK',
            timestamp: new Date().toISOString(),
            database: 'Connected',
            apiKey: RAPIDAPI_KEY ? 'Configured' : 'Missing',
            apiKeyMode: usingUnifiedKey ? 'Unified' : 'Separate Keys',
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

async function checkAndResetPriceAlerts() {
    const pool = require('./config/database');

    try {
        console.log('\n Checking for products that reached target price...');

        // AI suggested the sql
        const [reachedProducts] = await pool.query(`
            SELECT
                w.uid,
                w.pid,
                w.target_price,
                w.alert_status,
                p.title,
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
                GROUP BY w2.pid
            ) lp ON lp.pid = w.pid
            WHERE w.target_price IS NOT NULL
              AND lp.current_price <= w.target_price
        `);

        if (reachedProducts.length === 0) {
            console.log(' No products reached target price');
            return { resetCount: 0 };
        }

        console.log(` Found ${reachedProducts.length} products that reached target price`);

        let resetCount = 0;
        for (const product of reachedProducts) {
            await pool.query(
                'UPDATE wishlist SET alert_status = 0 WHERE uid = ? AND pid = ?',
                [product.uid, product.pid]
            );

            console.log(`    Reset alert for uid=${product.uid}, pid=${product.pid}: ${product.title}`);
            console.log(`      Current: $${product.current_price}, Target: $${product.target_price}`);
            resetCount++;
        }

        console.log(` Reset ${resetCount} price alerts\n`);
        return { resetCount, products: reachedProducts };

    } catch (error) {
        console.error(' Failed to check price alerts:', error);
        return { resetCount: 0, error: error.message };
    }
}

// Scheduled task: Update all prices daily at 3:00 AM EST
cron.schedule('0 3 * * *', async () => {
    console.log('Running scheduled price update...');
    const adminController = require('./controllers/adminController');

    try {
        await adminController.updateAllPrices({ body: {} }, {
            json: (data) => console.log('Price update result:', data),
            status: () => ({ json: (data) => console.error('Price update error:', data) })
        });

        const syncResult = await adminController.syncLowestPrices();
        console.log(`Synced lowest prices: ${syncResult.updatedCount}/${syncResult.totalProducts}`);

        const alertResult = await checkAndResetPriceAlerts();
        console.log(` Price alerts reset: ${alertResult.resetCount}`);
    } catch (error) {
        console.error('Scheduled task failed:', error);
    }
}, {
    timezone: "America/New_York"
});

// Favicon handler
app.get('/favicon.ico', (req, res) => {
    res.status(204).end();
});

// 404 handler
app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint not found' });
});

// Global error handler
app.use((err, req, res, next) => {
    console.error('Uncaught error:', err);
    res.status(500).json({ error: 'Internal server error' });
});

// Start server
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
    console.log('='.repeat(70));
    console.log(`Server running on http://localhost:${PORT}`);
    console.log('='.repeat(70));
    console.log('\nAvailable Endpoints:');
    console.log('\nUser Management:');
    console.log('   GET    /api/user/:uid              - Get user info');
    console.log('   POST   /api/user/login             - User login');
    console.log('   POST   /api/user/register          - User registration');
    console.log('   PUT    /api/user/:uid              - Update user');
    console.log('   DELETE /api/user/:uid              - Delete user');
    console.log('\nPrice API:');
    console.log('   GET    /api/price/:pid             - Get latest prices by platform');
    console.log('   GET    /api/history/:pid?days=7    - Get price history');
    console.log('\nProduct Management:');
    console.log('   GET    /api/products               - Get all products');
    console.log('   GET    /api/products/:pid          - Get single product');
    console.log('   GET    /api/products/search?q=     - Search products');
    console.log('   GET    /api/products/platform/:p   - Filter by platform');
    console.log('   GET    /api/products/price?min&max - Filter by price');
    console.log('\nWishlist:');
    console.log('   GET    /api/wishlist/:uid          - Get user wishlist');
    console.log('   GET    /api/wishlist/alerts/:uid   - Get price alerts');
    console.log('   POST   /api/wishlist               - Add to wishlist');
    console.log('   PUT    /api/wishlist/:wid          - Update target price');
    console.log('   DELETE /api/wishlist/:wid          - Remove from wishlist');
    console.log('\nView History:');
    console.log('   GET    /api/view-history/:uid      - Get browsing history');
    console.log('   POST   /api/view-history           - Add view record');
    console.log('   DELETE /api/view-history/:hid      - Delete single record');
    console.log('   DELETE /api/view-history/user/:uid - Clear all history');
    console.log('\nAdmin:');
    console.log('   POST   /api/admin/import-initial        - Import products');
    console.log('   POST   /api/admin/update-all-prices     - Update all prices');
    console.log('   POST   /api/admin/sync-lowest-prices    - Sync lowest prices');
    console.log('\nSystem:');
    console.log('   GET    /health                     - Health check');
    console.log('\nScheduled Tasks:');
    console.log('   Daily price update at 3:00 AM EST');
    console.log('\n' + '='.repeat(70));
});
