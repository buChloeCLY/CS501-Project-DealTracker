// History controller - handles user browsing history
const pool = require('../config/database');

// Get user's browsing history
// GET /api/view-history/:uid
async function getViewHistory(req, res) {
    const { uid } = req.params;

    try {
        const query = `
            SELECT h.hid, h.uid, h.pid,
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
        console.error('Get view history error:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to fetch view history'
        });
    }
}

// Add browsing record
// POST /api/view-history
// Body: { uid, pid }
async function addViewHistory(req, res) {
    const { uid, pid } = req.body;

    if (!uid || !pid) {
        return res.status(400).json({
            success: false,
            error: 'uid and pid are required'
        });
    }

    try {
        const [productCheck] = await pool.query(
            'SELECT pid FROM products WHERE pid = ?',
            [pid]
        );

        if (productCheck.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Product not found'
            });
        }

        const [result] = await pool.query(
            'INSERT INTO history (uid, pid, viewed_at) VALUES (?, ?, NOW())',
            [uid, pid]
        );

        res.json({
            success: true,
            message: 'View history recorded',
            hid: result.insertId
        });
    } catch (error) {
        console.error('Add view history error:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to add view history'
        });
    }
}

// Delete single history record
// DELETE /api/view-history/:hid
async function deleteViewHistory(req, res) {
    const { hid } = req.params;

    try {
        const [result] = await pool.query('DELETE FROM history WHERE hid = ?', [hid]);

        if (result.affectedRows === 0) {
            return res.status(404).json({
                success: false,
                error: 'History record not found'
            });
        }

        res.json({
            success: true,
            message: 'View history deleted'
        });
    } catch (error) {
        console.error('Delete view history error:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to delete view history'
        });
    }
}

// Clear all user history
// DELETE /api/view-history/user/:uid
async function clearUserHistory(req, res) {
    const { uid } = req.params;

    try {
        const [result] = await pool.query('DELETE FROM history WHERE uid = ?', [uid]);

        res.json({
            success: true,
            message: `Deleted ${result.affectedRows} view history records`
        });
    } catch (error) {
        console.error('Clear view history error:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to clear view history'
        });
    }
}

module.exports = {
    getViewHistory,
    addViewHistory,
    deleteViewHistory,
    clearUserHistory
};
