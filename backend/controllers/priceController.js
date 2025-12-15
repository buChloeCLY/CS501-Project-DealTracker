// Price controller - handles price queries and history
const pool = require('../config/database');

// Get latest prices by platform for a product
// GET /api/price/:pid
async function getLatestPrices(req, res) {
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
}

// Get price history for a product
// GET /api/history/:pid?days=7
async function getPriceHistory(req, res) {
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
                AND platform != 'eBay'
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
}

module.exports = {
    getLatestPrices,
    getPriceHistory
};