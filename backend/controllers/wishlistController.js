// Wishlist controller - handles user wishlists and price alerts
const pool = require('../config/database');

// Get user's wishlist items
// GET /api/wishlist?uid=4
async function getWishlist(req, res) {
    try {
        const uid = parseInt(req.query.uid);
        if (!uid) {
            return res.status(400).json({ error: 'uid is required' });
        }

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
}

// Add or update wishlist item
// POST /api/wishlist
// Body: { uid, pid, target_price? }
async function addToWishlist(req, res) {
    try {
        const { uid, pid, target_price } = req.body;

        if (!uid || !pid) {
            return res.status(400).json({ error: 'uid and pid are required' });
        }

        let tp = null;

        if (target_price != null) {
            tp = parseFloat(target_price);
            if (isNaN(tp) || tp <= 0) {
                return res.status(400).json({ error: 'Invalid target_price' });
            }
        }

        // Insert or update (upsert based on uid+pid unique constraint)
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
}

// Remove item from wishlist
// DELETE /api/wishlist
// Body: { uid, pid }
async function removeFromWishlist(req, res) {
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
}

// Get price alerts (products below target price)
// GET /api/wishlist/alerts?uid=4
async function getPriceAlerts(req, res) {
    try {
        const uid = parseInt(req.query.uid);
        if (!uid) {
            return res.status(400).json({ error: 'uid is required' });
        }

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
}

// Update target price (deprecated - use POST /api/wishlist instead)
async function updateTargetPrice(req, res) {
    try {
        const { uid, pid, target_price } = req.body;

        if (!uid || !pid || target_price === undefined) {
            return res.status(400).json({ error: 'uid, pid, and target_price are required' });
        }

        const [result] = await pool.query(
            'UPDATE wishlist SET target_price = ? WHERE uid = ? AND pid = ?',
            [target_price, uid, pid]
        );

        if (result.affectedRows > 0) {
            res.json({ success: true, message: 'Target price updated' });
        } else {
            res.status(404).json({ success: false, error: 'Wishlist item not found' });
        }
    } catch (error) {
        console.error('Update target price error:', error);
        res.status(500).json({ error: error.message });
    }
}

module.exports = {
    getWishlist,
    addToWishlist,
    removeFromWishlist,
    getPriceAlerts,
    updateTargetPrice
};