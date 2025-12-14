// Platform API services for fetching product data from Amazon, eBay, and Walmart
const axios = require('axios');
const { RAPIDAPI_KEYS, API_HOSTS } = require('../config/api');
const { parsePrice, parseRating, calculateSimilarity, extractShortTitle, categorizeProduct, generateInformation, isUsedProduct } = require('../utils/helpers');

// Fetch Amazon products by search query
// Returns array of raw Amazon API products
async function fetchFromAmazon(query, page = 1) {
    try {
        console.log(`[Amazon] Searching: "${query}" (page ${page})`);

        if (!RAPIDAPI_KEYS.amazon || RAPIDAPI_KEYS.amazon === 'YOUR_RAPIDAPI_KEY_HERE') {
            throw new Error('Amazon RapidAPI Key is not configured');
        }

        const response = await axios.get('https://real-time-amazon-data.p.rapidapi.com/search', {
            params: {
                query: query,
                page: page.toString(),
                country: 'US'
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.amazon,
                'X-RapidAPI-Host': 'real-time-amazon-data.p.rapidapi.com'
            }
        });

        const products = response.data.data?.products || [];
        console.log(`[Amazon] Found ${products.length} products`);
        return products;

    } catch (error) {
        console.error('[Amazon] API Error:', error.message);
        return [];
    }
}

// Fetch eBay products by search query
// Returns array of raw eBay API products
async function fetchFromEbay(query, page = 1) {
    try {
        console.log(`[eBay] Searching: "${query}" (page ${page})`);

        if (!RAPIDAPI_KEYS.ebay || RAPIDAPI_KEYS.ebay === 'YOUR_RAPIDAPI_KEY_HERE') {
            console.log('[eBay] API key not configured, skipping...');
            return [];
        }

        const response = await axios.get('https://ebay-data-api.p.rapidapi.com/search', {
            params: {
                query: query,
                page: page.toString(),
                countryIso: 'us',
                minPrice: 0
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.ebay,
                'X-RapidAPI-Host': 'ebay-data-api.p.rapidapi.com'
            }
        });

        const products = response.data?.data?.items || [];
        console.log(`[eBay] Found ${products.length} products`);
        return products;

    } catch (error) {
        if (error.response) {
            console.error('[eBay] API Error:', {
                status: error.response.status,
                message: error.response.data
            });
        } else {
            console.error('[eBay] Request Error:', error.message);
        }
        return [];
    }
}

// Fetch Walmart products by search query
// Returns array of raw Walmart API products
async function fetchFromWalmart(query, page = 1) {
    try {
        console.log(`[Walmart] Searching: "${query}"`);

        if (!RAPIDAPI_KEYS.walmart) {
            console.log('[Walmart] API key not configured, skipping...');
            return [];
        }

        const response = await axios.get('https://walmart-api4.p.rapidapi.com/search', {
            params: {
                q: query,
                page: page.toString()
            },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.walmart,
                'X-RapidAPI-Host': 'walmart-api4.p.rapidapi.com'
            }
        });

        const searchResultArray = response.data?.searchResult || [];

        let allProducts = [];
        for (const item of searchResultArray) {
            if (Array.isArray(item)) {
                allProducts = allProducts.concat(item);
            }
        }

        console.log(`[Walmart] Found ${allProducts.length} products`);
        return allProducts;

    } catch (error) {
        if (error.response) {
            console.error('[Walmart] API Error:', {
                status: error.response.status,
                statusText: error.response.statusText,
                url: error.config?.url,
                params: error.config?.params
            });
        } else {
            console.error('[Walmart] Error:', error.message);
        }
        return [];
    }
}

// Transform Amazon API product to standardized format
function transformAmazonProduct(apiProduct) {
    const fullTitle = apiProduct.product_title || 'Unknown Product';
    const shortTitle = extractShortTitle(fullTitle);

    let freeShipping = false;

    if (apiProduct.is_prime === true) {
        freeShipping = true;
        console.log(`[Free Shipping] Detected via is_prime`);
    }

    if (!freeShipping && apiProduct.delivery) {
        const deliveryText = apiProduct.delivery.toLowerCase();
        if (deliveryText.includes('free')) {
            freeShipping = true;
            console.log(`[Free Shipping] Detected via delivery: "${apiProduct.delivery}"`);
        }
    }

    let inStock = false;

    if (apiProduct.product_availability) {
        const availability = apiProduct.product_availability.toLowerCase();
        if (availability.includes('in stock') || availability.includes('available')) {
            inStock = true;
            console.log(`[In Stock] Detected via product_availability: "${apiProduct.product_availability}"`);
        }
    }

    if (!inStock && apiProduct.product_num_offers) {
        const numOffers = typeof apiProduct.product_num_offers === 'number'
            ? apiProduct.product_num_offers
            : parseInt(apiProduct.product_num_offers);

        if (numOffers > 0) {
            inStock = true;
            console.log(`[In Stock] Detected via product_num_offers: ${numOffers}`);
        }
    }

    return {
        shortTitle: shortTitle,
        fullTitle: fullTitle,
        price: parsePrice(apiProduct.product_price),
        rating: parseRating(apiProduct.product_star_rating),
        platform: 'Amazon',
        freeShipping: apiProduct.is_prime ? 1 : 0,
        inStock: apiProduct.product_availability?.toLowerCase().includes('in stock') ? 1 : 0,
        information: generateInformation(apiProduct),
        category: categorizeProduct(fullTitle, apiProduct),
        imageUrl: apiProduct.product_photo || '',
        idInPlatform: apiProduct.asin || '',
        link: apiProduct.product_url || ''
    };
}

// Transform eBay API product to standardized format
function transformEbayProduct(apiProduct) {
    const price = apiProduct.total || apiProduct.price || 0;

    const freeShipping = apiProduct.shipping === 0 ||
                        apiProduct['delivery-date']?.toLowerCase().includes('free');

    const inStock = !apiProduct.condition?.toLowerCase().includes('sold out');

    const info = [];

    if (apiProduct.itemId) {
        info.push(`eBay ID: ${apiProduct.itemId}`);
    }

    if (apiProduct['bid-count']) {
        info.push(`Bids: ${apiProduct['bid-count']}`);
    }

    if (apiProduct['time-left']) {
        info.push(`Time left: ${apiProduct['time-left']}`);
    }

    if (apiProduct.condition) {
        info.push(`Condition: ${apiProduct.condition}`);
    }

    if (apiProduct['delivery-date']) {
        info.push(`Delivery: ${apiProduct['delivery-date']}`);
    }

    return {
        title: apiProduct.title || 'Unknown Product',
        price: price,
        platform: 'eBay',
        freeShipping: freeShipping ? 1 : 0,
        inStock: inStock ? 1 : 0,
        information: info.join(' • ') || 'No additional information',
        imageUrl: apiProduct.imageUrl || '',
        idInPlatform: apiProduct.itemId || '',
        link: apiProduct.url || '',
        condition: apiProduct.condition || 'Unknown'
    };
}

// Transform Walmart API product to standardized format
function transformWalmartProduct(apiProduct) {
    const title = apiProduct.name || apiProduct.title || 'Unknown Product';
    const price = parseFloat(apiProduct.primaryOffer?.minPrice || apiProduct.price || 0);

    const info = [];

    if (apiProduct.rating) {
        info.push(`Rating: ${apiProduct.rating}/5`);
    }

    if (apiProduct.numberOfReviews) {
        info.push(`${apiProduct.numberOfReviews} reviews`);
    }

    if (apiProduct.brand) {
        info.push(`Brand: ${apiProduct.brand}`);
    }

    if (apiProduct.primaryOffer?.offerType) {
        info.push(`Type: ${apiProduct.primaryOffer.offerType}`);
    }

    return {
        title: title,
        price: price,
        platform: 'Walmart',
        freeShipping: 1,
        inStock: 1,
        information: info.join(' • ') || 'No additional information',
        imageUrl: apiProduct.imageInfo?.thumbnailUrl || apiProduct.image || '',
        idInPlatform: apiProduct.itemId || apiProduct.usItemId || '',
        link: apiProduct.productPageUrl || apiProduct.canonicalUrl || ''
    };
}

// Find best matching Walmart product
function findBestWalmartMatch(dbProduct, walmartProducts) {
    if (!walmartProducts || walmartProducts.length === 0) {
        return null;
    }

    console.log(`  [Walmart] Processing ${walmartProducts.length} products`);

    const transformed = walmartProducts.map(p => {
        const product = transformWalmartProduct(p);
        product.title = p.name || p.title || '';
        return product;
    });

    const originalIsUsed = isUsedProduct(dbProduct.title);
    console.log(`  [Walmart] Original is used: ${originalIsUsed}`);

    let candidates = transformed;

    if (!originalIsUsed) {
        const filtered = transformed.filter(p => !isUsedProduct(p.title));

        if (filtered.length > 0) {
            candidates = filtered;
            console.log(`  [Walmart] Filtered: ${transformed.length} -> ${filtered.length} (removed used)`);
        } else {
            console.log(`  [Walmart] All products are used, using original list`);
        }
    }

    if (dbProduct.price) {
        const referencePrice = dbProduct.price;
        const minPrice = referencePrice * 0.3;
        const maxPrice = referencePrice * 2.5;

        const priceFiltered = candidates.filter(p => {
            if (p.price < minPrice || p.price > maxPrice) {
                console.log(`  [Walmart] Price out of range: $${p.price} (ref: $${referencePrice})`);
                return false;
            }
            return true;
        });

        if (priceFiltered.length > 0) {
            candidates = priceFiltered;
            console.log(`  [Walmart] Price filtered: ${candidates.length} products in range`);
        } else {
            console.log(`  [Walmart] All prices out of range, using original list`);
        }
    }

    const scored = candidates.map(product => ({
        product: product,
        similarity: calculateSimilarity(dbProduct.title, product.title),
        price: product.price
    }));

    scored.sort((a, b) => b.similarity - a.similarity);

    const topSimilarity = scored[0].similarity;

    const topMatches = scored.filter(s => s.similarity >= topSimilarity - 0.03);

    if (topMatches.length > 1) {
        topMatches.sort((a, b) => a.price - b.price);
        console.log(`  [Walmart] ${topMatches.length} similar matches, cheapest: $${topMatches[0].price}`);
    }

    const best = topMatches[0];
    console.log(`  [Walmart] Best: similarity=${best.similarity.toFixed(2)}, price=$${best.price}`);

    return best.product;
}

// Find best matching eBay product
function findBestEbayMatch(dbProduct, ebayProducts) {
    if (!ebayProducts || ebayProducts.length === 0) {
        return null;
    }

    console.log(`  [eBay] Processing ${ebayProducts.length} products`);

    const transformed = ebayProducts.map(p => transformEbayProduct(p));

    const originalIsUsed = isUsedProduct(dbProduct.title);
    console.log(`  [eBay] Original is used: ${originalIsUsed}`);

    let candidates = transformed;

    if (!originalIsUsed) {
        const filtered = transformed.filter(p => {
            const titleIsUsed = isUsedProduct(p.title);

            const conditionIsUsed = p.condition && (
                p.condition.toLowerCase().includes('pre-owned') ||
                p.condition.toLowerCase().includes('used') ||
                p.condition.toLowerCase().includes('refurbished')
            );

            return !titleIsUsed && !conditionIsUsed;
        });

        if (filtered.length > 0) {
            candidates = filtered;
            console.log(`  [eBay] Filtered: ${transformed.length} -> ${filtered.length} (removed used)`);
        } else {
            console.log(`  [eBay] All products are used, using original list`);
        }
    }

    if (dbProduct.price) {
        const referencePrice = dbProduct.price;
        const minPrice = referencePrice * 0.2;
        const maxPrice = referencePrice * 3.0;

        const priceFiltered = candidates.filter(p => {
            if (p.price < minPrice || p.price > maxPrice) {
                console.log(`  [eBay] Price out of range: $${p.price} (ref: $${referencePrice})`);
                return false;
            }
            return true;
        });

        if (priceFiltered.length > 0) {
            candidates = priceFiltered;
            console.log(`  [eBay] Price filtered: ${candidates.length} products in reasonable range`);
        } else {
            console.log(`  [eBay] All prices out of range, using original list`);
        }
    }

    const scored = candidates.map(product => ({
        product: product,
        similarity: calculateSimilarity(dbProduct.title, product.title),
        price: product.price
    }));

    scored.sort((a, b) => b.similarity - a.similarity);

    const topSimilarity = scored[0].similarity;

    const topMatches = scored.filter(s => s.similarity >= topSimilarity - 0.03);

    if (topMatches.length > 1) {
        topMatches.sort((a, b) => a.price - b.price);
        console.log(`  [eBay] ${topMatches.length} similar matches, cheapest: $${topMatches[0].price}`);
    }

    const best = topMatches[0];
    console.log(`  [eBay] Best: similarity=${best.similarity.toFixed(2)}, price=$${best.price}`);

    return best.product;
}

// Fetch Amazon product details by ASIN
// Returns: { price, rating, reviews, inStock, freeShipping, title }
async function getAmazonProductDetails(asin) {
    if (!asin || asin === 'N/A') return null;

    try {
        const response = await axios.get('https://real-time-amazon-data.p.rapidapi.com/product-details', {
            params: { asin, country: 'US' },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.amazon,
                'X-RapidAPI-Host': API_HOSTS.amazon
            }
        });

        const data = response.data?.data;
        if (!data) return null;

        return {
            price: parsePrice(data.product_price),
            rating: parseRating(data.product_star_rating),
            reviews: parseInt(data.product_num_ratings) || 0,
            inStock: data.product_availability !== 'Out of Stock',
            freeShipping: data.delivery?.includes('FREE') || false,
            title: data.product_title
        };
    } catch (error) {
        console.error(`Amazon API error for ASIN ${asin}:`, error.message);
        return null;
    }
}

// Fetch eBay product details by item ID
// Returns: { price, shipping, inStock, freeShipping }
async function getEbayProductDetails(itemUrl) {
    if (!itemUrl || itemUrl === 'N/A') return null;

    const itemIdMatch = itemUrl.match(/\/itm\/(\d+)/);
    if (!itemIdMatch) return null;

    const itemId = itemIdMatch[1];

    try {
        const response = await axios.get('https://real-time-ebay-data.p.rapidapi.com/item-details', {
            params: { itemId },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.ebay,
                'X-RapidAPI-Host': API_HOSTS.ebay
            }
        });

        const data = response.data?.data;
        if (!data) return null;

        const price = parsePrice(data.price?.value);
        const shipping = parsePrice(data.shippingCost?.value);

        return {
            price: price + shipping,
            shipping,
            inStock: data.quantity > 0,
            freeShipping: shipping === 0
        };
    } catch (error) {
        console.error(`eBay API error for item ${itemId}:`, error.message);
        return null;
    }
}

// Fetch Walmart product details by item ID
// Returns: { price, inStock }
async function getWalmartProductDetails(itemUrl) {
    if (!itemUrl || itemUrl === 'N/A') return null;

    const itemIdMatch = itemUrl.match(/\/ip\/.*\/(\d+)/);
    if (!itemIdMatch) return null;

    const itemId = itemIdMatch[1];

    try {
        const response = await axios.get('https://walmart-search-and-pricing.p.rapidapi.com/product-details', {
            params: { itemId },
            headers: {
                'X-RapidAPI-Key': RAPIDAPI_KEYS.walmart,
                'X-RapidAPI-Host': API_HOSTS.walmart
            }
        });

        const data = response.data;
        if (!data) return null;

        return {
            price: parsePrice(data.price),
            inStock: data.availability === 'IN_STOCK'
        };
    } catch (error) {
        console.error(`Walmart API error for item ${itemId}:`, error.message);
        return null;
    }
}

module.exports = {
    fetchFromAmazon,
    fetchFromEbay,
    fetchFromWalmart,
    transformAmazonProduct,
    transformEbayProduct,
    transformWalmartProduct,
    findBestWalmartMatch,
    findBestEbayMatch,
    getAmazonProductDetails,
    getEbayProductDetails,
    getWalmartProductDetails
};