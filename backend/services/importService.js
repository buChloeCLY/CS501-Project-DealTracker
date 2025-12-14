// Product import service - handles initial product import from multiple platforms
const pool = require('../config/database');
const { fetchFromAmazon, fetchFromEbay, fetchFromWalmart, transformAmazonProduct, findBestEbayMatch, findBestWalmartMatch } = require('./platformService');

// Default product queries for initial import
const DEFAULT_QUERIES = [
    // Electronics (10 products)
    'Samsung Galaxy S24',
    'iPhone 15',
    'iPad Air',
    'MacBook Pro',
    'Dell XPS laptop',
    'HP laptop',
    'Sony WH-1000XM5 headphones',
    'Bose QuietComfort headphones',
    'LG OLED TV',
    'Samsung 4K TV',

    // Beauty (4 products)
    'CeraVe moisturizer',
    'Neutrogena sunscreen',
    'Maybelline mascara',
    'L\'Oreal foundation',

    // Home (5 products)
    'Dyson vacuum cleaner',
    'Shark vacuum',
    'KitchenAid stand mixer',
    'Ninja blender',
    'Instant Pot',

    // Food (3 products)
    'Starbucks coffee beans',
    'Ghirardelli chocolate',
    'KIND protein bars',

    // Fashion (4 products)
    'Nike running shoes',
    'Adidas sneakers',
    'Levi\'s jeans',
    'North Face jacket',

    // Sports (4 products)
    'Fitbit fitness tracker',
    'Garmin smartwatch',
    'yoga mat',
    'resistance bands',

    // Books (3 products)
    'Atomic Habits book',
    'Harry Potter book set',
    'a song of ice and fire book set',

    // Toys (4 products)
    'LEGO Star Wars set',
    'Hot Wheels track',
    'Barbie doll',
    'Rubik\'s cube',

    // Health (3 products)
    'Omron blood pressure monitor',
    'Braun thermometer',
    'multivitamin gummies',

    // Sports/Outdoors (3 products - changed from Outdoors category)
    'Coleman camping tent',
    'Yeti cooler',
    'Stanley thermos',

    // Office (4 products)
    'Logitech wireless mouse',
    'mechanical keyboard',
    'office chair',
    'standing desk',

    // Pets (3 products)
    'dog food',
    'cat litter',
    'pet carrier'
];

// Import products from multiple platforms
// Uses queries array or default queries
async function importInitialProducts(queries = DEFAULT_QUERIES) {
    console.log('\nStarting multi-platform product import...');

    const importedProducts = [];
    let totalImported = 0;

    for (const query of queries) {
        console.log(`\nProcessing: "${query}"`);

        // Step 1: Fetch from Amazon
        const amazonProducts = await fetchFromAmazon(query, 1);
        if (amazonProducts.length === 0) {
            console.log(`No Amazon products found for "${query}"`);
            continue;
        }

        const amazonProduct = transformAmazonProduct(amazonProducts[0]);

        if (amazonProduct.price === 0) {
            console.log(`Skipping product with price 0`);
            continue;
        }

        try {
            // Step 2: Insert product into products table
            const [productResult] = await pool.query(`
                INSERT INTO products
                (short_title, title, price, rating, platform, free_shipping, in_stock, information, category, image_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            `, [
                amazonProduct.shortTitle,
                amazonProduct.fullTitle,
                amazonProduct.price,
                amazonProduct.rating,
                'Amazon',
                amazonProduct.freeShipping,
                amazonProduct.inStock,
                amazonProduct.information,
                amazonProduct.category,
                amazonProduct.imageUrl
            ]);

            const pid = productResult.insertId;
            console.log(`[${totalImported + 1}] Created product: ${amazonProduct.shortTitle} (pid=${pid})`);

            // Step 3: Insert Amazon price into price table
            await pool.query(`
                INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, idInPlatform, link)
                VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)
            `, [pid, 'Amazon', amazonProduct.price, amazonProduct.freeShipping, amazonProduct.inStock, amazonProduct.idInPlatform, amazonProduct.link]);

            console.log(`  Amazon: $${amazonProduct.price}`);

            // Step 4: Search eBay
            console.log(`  Searching eBay with: "${amazonProduct.shortTitle}"`);
            await new Promise(resolve => setTimeout(resolve, 5000));

            const ebayProducts = await fetchFromEbay(amazonProduct.shortTitle, 1);

            if (ebayProducts.length > 0) {
                const ebayProduct = findBestEbayMatch({ title: amazonProduct.fullTitle, price: amazonProduct.price }, ebayProducts);

                if (ebayProduct && ebayProduct.price > 0) {
                    await pool.query(`
                        INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, idInPlatform, link)
                        VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)
                    `, [pid, 'eBay', ebayProduct.price, ebayProduct.freeShipping, ebayProduct.inStock, ebayProduct.idInPlatform, ebayProduct.link]);

                    console.log(`  eBay: $${ebayProduct.price}`);
                }
            }

            // Step 5: Search Walmart
            console.log(`  Searching Walmart with: "${amazonProduct.shortTitle}"`);
            await new Promise(resolve => setTimeout(resolve, 5000));

            const walmartProducts = await fetchFromWalmart(amazonProduct.shortTitle, 1);

            if (walmartProducts.length > 0) {
                const walmartProduct = findBestWalmartMatch({ title: amazonProduct.fullTitle, price: amazonProduct.price }, walmartProducts);

                if (walmartProduct && walmartProduct.price > 0) {
                    await pool.query(`
                        INSERT INTO price (pid, platform, price, free_shipping, in_stock, date, link)
                        VALUES (?, ?, ?, ?, ?, NOW(), ?)
                    `, [pid, 'Walmart', walmartProduct.price, 1, 1, walmartProduct.link]);

                    console.log(`  Walmart: $${walmartProduct.price}`);
                } else {
                    console.log(`  No suitable Walmart match found`);
                }
            }

            totalImported++;
            importedProducts.push({
                pid: pid,
                short_title: amazonProduct.shortTitle,
                amazon_price: amazonProduct.price,
                category: amazonProduct.category
            });

            // Prevent API rate limiting
            await new Promise(resolve => setTimeout(resolve, 5000));

        } catch (error) {
            console.error(`Failed to import product:`, error.message);
        }
    }

    console.log(`\nImport completed: ${totalImported} products imported`);

    return {
        success: true,
        message: `Successfully imported ${totalImported} products with multi-platform prices`,
        totalImported,
        products: importedProducts
    };
}

module.exports = {
    importInitialProducts,
    DEFAULT_QUERIES
};