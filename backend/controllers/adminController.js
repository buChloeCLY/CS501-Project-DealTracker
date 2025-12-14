// Admin controller - handles product import and price updates
const pool = require('../config/database');
const {
    getAmazonProductDetails,
    getEbayProductDetails,
    getWalmartProductDetails
} = require('../services/platformService');
const { importInitialProducts } = require('../services/importService');

// Import initial products from multiple platforms
// POST /api/admin/import-initial
// Body: { queries?: string[] } (optional, uses default queries if not provided)
async function importInitialProductsEndpoint(req, res) {
    try {
        const { queries } = req.body;
        const result = await importInitialProducts(queries);
        res.json(result);
    } catch (error) {
        console.error('Import error:', error);
        res.status(500).json({
            success: false,
            error: 'Import failed',
            detail: error.message
        });
    }
}

// Update all product prices from platforms
// POST /api/admin/update-all-prices
async function updateAllPrices(req, res) {
    try {
        console.log('Starting multi-platform price update...');

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

        console.log(`Found ${dbProducts.length} products to update`);

        let updatedCount = 0;
        let failedCount = 0;

        for (const dbProduct of dbProducts) {
            try {
                console.log(`\n[${updatedCount + 1}/${dbProducts.length}] ${dbProduct.title.substring(0, 60)}...`);

                // Update Amazon price
                if (dbProduct.amazon_asin) {
                    const amazonDetails = await getAmazonProductDetails(dbProduct.amazon_asin);

                    if (amazonDetails && amazonDetails.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, idInPlatform)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Amazon', amazonDetails.price, amazonDetails.freeShipping,
                            amazonDetails.inStock, dbProduct.amazon_asin]);
                        console.log(`  Amazon: $${amazonDetails.price}`);
                    }
                    await new Promise(resolve => setTimeout(resolve, 5000));
                }

                // Update Walmart price
                if (dbProduct.walmart_link) {
                    const walmartDetails = await getWalmartProductDetails(dbProduct.walmart_link);
                    if (walmartDetails && walmartDetails.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Walmart', walmartDetails.price, 1,
                            walmartDetails.inStock, dbProduct.walmart_link]);
                        console.log(`  Walmart: $${walmartDetails.price}`);
                    }
                    await new Promise(resolve => setTimeout(resolve, 3000));
                }

                // Update eBay price
                if (dbProduct.ebay_link) {
                    const ebayDetails = await getEbayProductDetails(dbProduct.ebay_link);
                    if (ebayDetails && ebayDetails.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'eBay', ebayDetails.price, ebayDetails.freeShipping,
                            ebayDetails.inStock, dbProduct.ebay_link]);
                        console.log(`  eBay: $${ebayDetails.price}`);
                    }
                    await new Promise(resolve => setTimeout(resolve, 5000));
                }

                updatedCount++;
            } catch (error) {
                console.error(`Failed to update ${dbProduct.title}:`, error.message);
                failedCount++;
            }
        }

        // Sync lowest prices to products table
        await syncLowestPrices();

        res.json({
            success: true,
            message: `Updated ${updatedCount}/${dbProducts.length} products`,
            updated: updatedCount,
            failed: failedCount
        });
    } catch (error) {
        console.error('Update prices error:', error);
        res.status(500).json({
            success: false,
            error: 'Update failed',
            detail: error.message
        });
    }
}

// Sync lowest prices from price table to products table
async function syncLowestPrices() {
    console.log('\nStarting to sync lowest prices to products table...');

    const [products] = await pool.query('SELECT pid FROM products');
    console.log(`Found ${products.length} products to sync`);

    let updatedCount = 0;
    let skippedCount = 0;

    for (const product of products) {
        try {
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
                console.log(`[PID ${product.pid}] No price rows, skipped`);
                skippedCount++;
                continue;
            }

            const best = rows[0];

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
                `[PID ${product.pid}] -> ${best.platform}, price=$${best.price}, ` +
                `free_shipping=${best.free_shipping}, in_stock=${best.in_stock}`
            );

        } catch (err) {
            console.error(`[PID ${product.pid}] Sync failed:`, err.message);
        }
    }

    console.log(
        `\nSync completed: ${updatedCount} updated, ${skippedCount} skipped, ` +
        `${products.length} total`
    );

    return {
        totalProducts: products.length,
        updatedCount,
        skippedCount
    };
}

// Manual trigger to sync lowest prices
// POST /api/admin/sync-lowest-prices
async function syncLowestPricesEndpoint(req, res) {
    try {
        const result = await syncLowestPrices();
        res.json({
            success: true,
            message: `Synced ${result.updatedCount}/${result.totalProducts} products`,
            ...result
        });
    } catch (error) {
        console.error('Sync endpoint error:', error);
        res.status(500).json({
            success: false,
            error: 'Sync failed',
            detail: error.message
        });
    }
}

module.exports = {
    importInitialProducts: importInitialProductsEndpoint,
    updateAllPrices,
    syncLowestPrices,
    syncLowestPricesEndpoint
};