// Product controller - handles product queries and management
const pool = require('../config/database');

// Get all products with optional filters
// GET /api/products?category=&search=&min_price=&max_price=&in_stock=&free_shipping=
async function getAllProducts(req, res) {
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

        const [products] = await pool.query(query, params);
        res.json(products);
    } catch (error) {
        console.error('Get products error:', error);
        res.status(500).json({ error: error.message });
    }
}

// Get single product by ID
// GET /api/products/:pid
async function getProductById(req, res) {
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
}

// Search products by keyword (deprecated - use getAllProducts with search param)
// GET /api/products/search?q=keyword
async function searchProducts(req, res) {
    const { query, q, page = 1, size = 20 } = req.query;

    // Support both 'query' and 'q' parameters
    const searchQuery = query || q;

    if (!searchQuery) {
        return res.status(400).json({ error: 'Search query is required' });
    }

    try {
        const currentPage = parseInt(page);
        const pageSize = parseInt(size);
        const offset = (currentPage - 1) * pageSize;

        // Get total count for pagination
        const [countResult] = await pool.query(
            'SELECT COUNT(*) as total FROM products WHERE title LIKE ? OR short_title LIKE ?',
            [`%${searchQuery}%`, `%${searchQuery}%`]
        );
        const total = countResult[0].total;

        // Get paginated results
        const [products] = await pool.query(
            'SELECT * FROM products WHERE title LIKE ? OR short_title LIKE ? ORDER BY created_at DESC LIMIT ? OFFSET ?',
            [`%${searchQuery}%`, `%${searchQuery}%`, pageSize, offset]
        );

        // Calculate total pages
        const totalPages = Math.ceil(total / pageSize);

        // Return wrapped response matching Android's SearchResponseDTO
        res.json({
            products: products,
            page: currentPage,
            totalPages: totalPages,
            total: total
        });
    } catch (error) {
        console.error('Search products error:', error);
        res.status(500).json({ error: error.message });
    }
}

// Filter products by platform (deprecated - use getAllProducts)
// GET /api/products/platform/:platform
async function getByPlatform(req, res) {
    const { platform } = req.params;

    try {
        const [products] = await pool.query(
            'SELECT * FROM products WHERE platform = ? ORDER BY created_at DESC',
            [platform]
        );

        res.json(products);
    } catch (error) {
        console.error('Filter by platform error:', error);
        res.status(500).json({ error: error.message });
    }
}

// Filter products by price range (deprecated - use getAllProducts)
// GET /api/products/price?min=100&max=500
async function getByPriceRange(req, res) {
    const { min, max } = req.query;

    if (!min || !max) {
        return res.status(400).json({ error: 'Min and max prices are required' });
    }

    try {
        const [products] = await pool.query(
            'SELECT * FROM products WHERE price BETWEEN ? AND ? ORDER BY price ASC',
            [parseFloat(min), parseFloat(max)]
        );

        res.json(products);
    } catch (error) {
        console.error('Filter by price error:', error);
        res.status(500).json({ error: error.message });
    }
}

module.exports = {
    getAllProducts,
    getProductById,
    searchProducts,
    getByPlatform,
    getByPriceRange
};