// Admin controller - handles product import and price updates
const pool = require('../config/database');
const {
    getAmazonProductDetails,
    getEbayProductDetails,
    getWalmartProductDetails,
    fetchFromWalmart,
    findBestWalmartMatch,
    fetchFromEbay,
    findBestEbayMatch
} = require('../services/platformService');
const { importInitialProducts } = require('../services/importService');
const { extractShortTitle } = require('../utils/helpers');

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
                SELECT p1.pid, p1.idInPlatform, p1.link
                FROM price p1
                INNER JOIN (
                    SELECT pid, MAX(date) AS max_date
                    FROM price
                    WHERE platform = 'Amazon'
                    GROUP BY pid
                ) p2 ON p1.pid = p2.pid AND p1.date = p2.max_date
                WHERE p1.platform = 'Amazon'
            ) pr_amazon ON p.pid = pr_amazon.pid
            LEFT JOIN (
                SELECT p1.pid, p1.link
                FROM price p1
                INNER JOIN (
                    SELECT pid, MAX(date) AS max_date
                    FROM price
                    WHERE platform = 'Walmart'
                    GROUP BY pid
                ) p2 ON p1.pid = p2.pid AND p1.date = p2.max_date
                WHERE p1.platform = 'Walmart'
            ) pr_walmart ON p.pid = pr_walmart.pid
            LEFT JOIN (
                SELECT p1.pid, p1.link
                FROM price p1
                INNER JOIN (
                    SELECT pid, MAX(date) AS max_date
                    FROM price
                    WHERE platform = 'eBay'
                    GROUP BY pid
                ) p2 ON p1.pid = p2.pid AND p1.date = p2.max_date
                WHERE p1.platform = 'eBay'
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
                    console.log(`    Walmart link: ${dbProduct.walmart_link}...`);
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

        console.log(`\nUpdate completed: ${updatedCount} updated, ${failedCount} failed`);

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

async function addWalmartPrices(req, res) {
    try {
        console.log('\n Starting Walmart price supplement...');

        const [dbProducts] = await pool.query('SELECT pid, title, short_title, price FROM products');
        console.log(` Found ${dbProducts.length} products`);

        let addedCount = 0;
        let skippedCount = 0;
        let failedCount = 0;
        const results = [];

        for (const dbProduct of dbProducts) {
            try {
                console.log(`\n [${addedCount + skippedCount + failedCount + 1}/${dbProducts.length}] ${dbProduct.title.substring(0, 60)}...`);

                const [existing] = await pool.query(`
                    SELECT id FROM price
                    WHERE pid = ? AND platform = 'Walmart'
                    ORDER BY date DESC
                    LIMIT 1
                `, [dbProduct.pid]);

                if (existing.length > 0) {
                    console.log(`     Walmart price already exists, skipping...`);
                    skippedCount++;
                    continue;
                }

                console.log(`    Searching Walmart with: "${dbProduct.short_title}"`);
                await new Promise(resolve => setTimeout(resolve, 5000));

                const walmartProducts = await fetchFromWalmart(dbProduct.short_title, 1);

                if (walmartProducts.length > 0) {
                    const walmartProduct = await findBestWalmartMatch(dbProduct, walmartProducts);

                    if (walmartProduct && walmartProduct.price > 0) {
                        await pool.query(`
                            INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                            VALUES (?, ?, ?, ?, ?, NOW(), ?)
                        `, [dbProduct.pid, 'Walmart', walmartProduct.price, walmartProduct.freeShipping, walmartProduct.inStock, walmartProduct.link]);

                        console.log(`   Added Walmart price: $${walmartProduct.price}`);
                        addedCount++;

                        results.push({
                            pid: dbProduct.pid,
                            title: dbProduct.title.substring(0, 50),
                            walmart_price: walmartProduct.price
                        });
                    } else {
                        console.log(`     No suitable match`);
                        failedCount++;
                    }
                } else {
                    console.log(`     No Walmart products found`);
                    failedCount++;
                }

            } catch (error) {
                failedCount++;
                console.error(`    Failed: ${error.message}`);
            }
        }

        console.log(`\n Walmart supplement completed:`);
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
            results: results.slice(0, 10)
        });

    } catch (error) {
        console.error('Walmart supplement failed:', error);
        res.status(500).json({
            error: 'Walmart supplement failed',
            details: error.message
        });
    }
}

async function syncEBayPrices(req, res) {
    try {
        console.log('\n Starting eBay price sync...');
        console.log('='.repeat(70));

        const [dbProducts] = await pool.query('SELECT pid, title, short_title, price FROM products');
        console.log(` Found ${dbProducts.length} products`);

        let addedCount = 0;
        let skippedCount = 0;
        let failedCount = 0;
        const syncLog = [];

        for (const dbProduct of dbProducts) {
            try {
                console.log(`\n [${addedCount + skippedCount + failedCount + 1}/${dbProducts.length}] ${dbProduct.title.substring(0, 60)}...`);

                const [existing] = await pool.query(`
                    SELECT id FROM price
                    WHERE pid = ? AND platform = 'eBay'
                    ORDER BY date DESC
                    LIMIT 1
                `, [dbProduct.pid]);

                if (existing.length > 0) {
                    console.log(`     eBay price already exists, skipping...`);
                    skippedCount++;
                    continue;
                }

                const searchQuery = dbProduct.short_title;

                console.log(`    Searching eBay with: "${searchQuery}"`);

                await new Promise(resolve => setTimeout(resolve, 10000));

                const ebayProducts = await fetchFromEbay(searchQuery, 1);

                if (ebayProducts.length > 0) {
                    const bestMatch = await findBestEbayMatch({
                        title: dbProduct.title,
                        price: dbProduct.price
                    }, ebayProducts);

                    if (bestMatch && bestMatch.price > 0) {
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

                        console.log(`    Added eBay price: $${bestMatch.price}`);
                        addedCount++;

                        syncLog.push({
                            pid: dbProduct.pid,
                            title: dbProduct.title.substring(0, 40),
                            ebayPrice: bestMatch.price,
                            ebayId: bestMatch.idInPlatform
                        });
                    } else {
                        console.log(`     No suitable match found`);
                        failedCount++;
                    }
                } else {
                    console.log(`     No results from eBay`);
                    failedCount++;
                }

            } catch (error) {
                failedCount++;
                console.error(`    Error syncing ${dbProduct.title}:`, error.message);
            }
        }

        console.log('\n' + '='.repeat(70));
        console.log(` eBay sync completed:`);
        console.log(`   Added: ${addedCount}`);
        console.log(`   Skipped (already exists): ${skippedCount}`);
        console.log(`   Failed: ${failedCount}`);
        console.log(`   Total: ${dbProducts.length}`);
        console.log('='.repeat(70) + '\n');

        res.json({
            success: true,
            message: `Added ${addedCount} eBay prices, skipped ${skippedCount}, failed ${failedCount}`,
            addedCount,
            skippedCount,
            failedCount,
            totalProducts: dbProducts.length,
            syncLog: syncLog.slice(0, 10)
        });

    } catch (error) {
        console.error('eBay sync failed:', error);
        res.status(500).json({
            error: 'eBay sync failed',
            details: error.message
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
                      AND platform IN ('Amazon', 'Walmart')
                    GROUP BY platform
                ) p2
                    ON p1.platform = p2.platform AND p1.date = p2.max_date
                WHERE p1.pid = ?
                  AND p1.platform IN ('Amazon', 'Walmart')
                ORDER BY p1.price ASC
            `, [product.pid, product.pid]);

            if (rows.length === 0) {
                console.log(`  [PID ${product.pid}] No Amazon/Walmart prices found, skipped`);
                skippedCount++;
                continue;
            }

            const best = rows[0];
            const lowestPricePlatforms = rows.filter(r => r.price === best.price);
            const platformNames = lowestPricePlatforms.map(p => p.platform).join(', ');
            const freeShipping = lowestPricePlatforms.some(p => p.free_shipping === 1 || p.free_shipping === true);
            const inStock = lowestPricePlatforms.some(p => p.in_stock === 1 || p.in_stock === true);

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
                platformNames,
                freeShipping ? 1 : 0,
                inStock ? 1 : 0,
                product.pid
            ]);

            updatedCount++;
            console.log(
                `[PID ${product.pid}] -> ${platformNames}, price=$${best.price}, ` +
                `free_shipping=${freeShipping}, in_stock=${inStock}`
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

async function test(req, res) {
    try {
        console.log('\n Starting AI short title update...');
        console.log('='.repeat(100));

        const [products] = await pool.query('SELECT pid, title, short_title FROM products ORDER BY pid');
        console.log(` Found ${products.length} products to update\n`);

        const results = [];
        let updatedCount = 0;
        let failedCount = 0;
        let skippedCount = 0;

        for (let i = 0; i < products.length; i++) {
            const product = products[i];

            console.log(`\n[${ i + 1}/${products.length}] PID: ${product.pid}`);
            console.log('─'.repeat(100));
            console.log(` Full Title:     ${product.title}`);
            console.log(` Current Short:  ${product.short_title || '(empty)'}`);

            try {
                const aiShortTitle = await extractShortTitle(product.title);
                console.log(` AI Short:       ${aiShortTitle}`);

               if (!aiShortTitle || aiShortTitle.trim().length === 0) {
                    console.log(`  AI returned empty, skipping update`);
                    skippedCount++;
                    results.push({
                        pid: product.pid,
                        fullTitle: product.title,
                        oldShort: product.short_title,
                        newShort: null,
                        status: 'skipped',
                        reason: 'AI returned empty'
                    });
                    continue;
               }

                await pool.query(
                    'UPDATE products SET short_title = ? WHERE pid = ?',
                    [aiShortTitle, product.pid]
                );

                console.log(` Updated in database`);
                updatedCount++;

                results.push({
                    pid: product.pid,
                    fullTitle: product.title,
                    oldShort: product.short_title,
                    newShort: aiShortTitle,
                    status: 'updated'
                });

                await new Promise(resolve => setTimeout(resolve, 1000));

            } catch (error) {
                console.log(` AI Failed:      ${error.message}`);
                failedCount++;
                results.push({
                    pid: product.pid,
                    fullTitle: product.title,
                    oldShort: product.short_title,
                    newShort: null,
                    status: 'failed',
                    error: error.message
                });
            }
        }

        console.log('\n' + '='.repeat(100));
        console.log(` Update completed:`);
        console.log(`   Updated: ${updatedCount}`);
        console.log(`   Skipped: ${skippedCount}`);
        console.log(`   Failed: ${failedCount}`);
        console.log(`   Total: ${products.length}`);
        console.log('='.repeat(100) + '\n');

        res.json({
            success: true,
            message: `Updated ${updatedCount} products, skipped ${skippedCount}, failed ${failedCount}`,
            updatedCount,
            skippedCount,
            failedCount,
            totalProducts: products.length,
            results: results.slice(0, 20)
        });

    } catch (error) {
        console.error('Update failed:', error);
        res.status(500).json({
            error: 'Update failed',
            details: error.message
        });
    }
}

module.exports = {
    importInitialProducts: importInitialProductsEndpoint,
    updateAllPrices,
    syncLowestPrices,
    syncLowestPricesEndpoint,
    addWalmartPrices,
    syncEBayPrices,
    test
};