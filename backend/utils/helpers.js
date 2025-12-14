// Utility functions for password, price parsing, and text matching
const crypto = require('crypto');

// Hash password using SHA-256
function hashPassword(password) {
    return crypto.createHash('sha256').update(password).digest('hex');
}

// Parse price from string like "$99.99" or "$1,299.99" to float
function parsePrice(priceStr) {
    if (!priceStr) return 0;
    const cleaned = priceStr.replace(/[$,]/g, '').trim();
    return parseFloat(cleaned) || 0;
}

// Parse rating from string like "4.5 out of 5 stars" to float
function parseRating(ratingStr) {
    if (!ratingStr) return 0;
    const match = ratingStr.match(/(\d+\.?\d*)/);
    return match ? parseFloat(match[1]) : 0;
}

// Clean title by removing marketing phrases and extra symbols
function cleanTitle(title) {
    return title
        .toLowerCase()
        .replace(/\(.*?\)/g, '')
        .replace(/[-–—]/g, ' ')
        .replace(/[,;:]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}

// Extract key information from product title (brand, model, specs)
function extractKeyInfo(title) {
    const lower = title.toLowerCase();
    const info = {
        brand: null,
        model: null,
        storage: null,
        color: null,
        specs: []
    };

    const brands = [
        'apple', 'samsung', 'google', 'sony', 'lg', 'motorola', 'oneplus',
        'dell', 'hp', 'lenovo', 'asus', 'acer', 'microsoft',
        'bose', 'beats', 'jbl', 'airpods',
        'nike', 'adidas', 'puma'
    ];

    for (const brand of brands) {
        if (lower.includes(brand)) {
            info.brand = brand;
            break;
        }
    }

    const modelPatterns = [
        /iphone\s*(\d+\s*pro\s*max|\d+\s*pro|\d+\s*plus|\d+)/i,
        /galaxy\s*s\d+\s*(ultra|plus)?/i,
        /pixel\s*\d+\s*(pro|xl)?/i,
        /macbook\s*(pro|air)/i,
        /ipad\s*(pro|air|mini)?/i,
        /airpods\s*(pro|max)?/i,
        /echo\s*(dot|show|studio)?/i
    ];

    for (const pattern of modelPatterns) {
        const match = title.match(pattern);
        if (match) {
            info.model = match[0].toLowerCase().trim();
            break;
        }
    }

    const storageMatch = title.match(/(\d+)\s*(gb|tb)/i);
    if (storageMatch) {
        info.storage = storageMatch[0].toLowerCase();
        info.specs.push(info.storage);
    }

    const colors = [
        'red', 'black', 'white', 'blue', 'green', 'yellow', 'purple',
        'silver', 'gold', 'rose gold', 'space gray', 'midnight', 'starlight'
    ];

    for (const color of colors) {
        if (lower.includes(color)) {
            info.color = color;
            info.specs.push(color);
            break;
        }
    }

    const specPatterns = [
        /pro max/i, /pro/i, /plus/i, /mini/i, /ultra/i,
        /unlocked/i, /renewed/i, /refurbished/i,
        /5g/i, /wifi/i, /cellular/i
    ];

    for (const pattern of specPatterns) {
        const match = title.match(pattern);
        if (match) {
            info.specs.push(match[0].toLowerCase());
        }
    }

    return info;
}

// Calculate Levenshtein distance between two strings
function levenshteinDistance(str1, str2) {
    const matrix = [];
    for (let i = 0; i <= str2.length; i++) {
        matrix[i] = [i];
    }
    for (let j = 0; j <= str1.length; j++) {
        matrix[0][j] = j;
    }
    for (let i = 1; i <= str2.length; i++) {
        for (let j = 1; j <= str1.length; j++) {
            if (str2.charAt(i - 1) === str1.charAt(j - 1)) {
                matrix[i][j] = matrix[i - 1][j - 1];
            } else {
                matrix[i][j] = Math.min(
                    matrix[i - 1][j - 1] + 1,
                    matrix[i][j - 1] + 1,
                    matrix[i - 1][j] + 1
                );
            }
        }
    }
    return matrix[str2.length][str1.length];
}

// Compare model similarity
function compareModels(model1, model2) {
    if (model1 === model2) return 1.0;
    const m1 = model1.replace(/\s+/g, '');
    const m2 = model2.replace(/\s+/g, '');
    if (m1 === m2) return 0.95;

    const distance = levenshteinDistance(m1, m2);
    const maxLen = Math.max(m1.length, m2.length);
    const similarity = 1 - (distance / maxLen);
    return Math.max(0, similarity);
}

// Compare specification similarity
function compareSpecs(info1, info2) {
    let matchCount = 0;
    let totalSpecs = 0;

    if (info1.storage && info2.storage) {
        matchCount += info1.storage === info2.storage ? 1 : 0;
        totalSpecs++;
    }

    if (info1.color && info2.color) {
        matchCount += info1.color === info2.color ? 1 : 0;
        totalSpecs++;
    }

    const specs1 = new Set(info1.specs);
    const specs2 = new Set(info2.specs);
    const commonSpecs = [...specs1].filter(s => specs2.has(s));

    if (specs1.size > 0 || specs2.size > 0) {
        const specSimilarity = commonSpecs.length / Math.max(specs1.size, specs2.size);
        matchCount += specSimilarity;
        totalSpecs++;
    }

    return totalSpecs > 0 ? matchCount / totalSpecs : 0;
}

// Compare word overlap between two strings
function compareWords(str1, str2) {
    const words1 = str1.split(/\s+/).filter(w => w.length > 2);
    const words2 = str2.split(/\s+/).filter(w => w.length > 2);

    if (words1.length === 0 || words2.length === 0) return 0;

    const set1 = new Set(words1);
    const set2 = new Set(words2);
    const intersection = [...set1].filter(w => set2.has(w));

    return intersection.length / Math.max(set1.size, set2.size);
}

// Calculate similarity score between two product titles (0-1 range)
function calculateSimilarity(str1, str2) {
    if (!str1 || !str2) return 0;

    const clean1 = cleanTitle(str1);
    const clean2 = cleanTitle(str2);

    if (clean1 === clean2) return 1.0;

    const info1 = extractKeyInfo(str1);
    const info2 = extractKeyInfo(str2);

    let score = 0;
    let weights = 0;

    // Brand matching (30% weight)
    if (info1.brand && info2.brand) {
        if (info1.brand === info2.brand) {
            score += 0.3;
        }
        weights += 0.3;
    }

    // Model matching (40% weight)
    if (info1.model && info2.model) {
        const modelSimilarity = compareModels(info1.model, info2.model);
        score += modelSimilarity * 0.4;
        weights += 0.4;
    }

    // Spec matching (20% weight)
    const specScore = compareSpecs(info1, info2);
    score += specScore * 0.2;
    weights += 0.2;

    // Word overlap (10% weight)
    const wordScore = compareWords(clean1, clean2);
    score += wordScore * 0.1;
    weights += 0.1;

    const finalScore = weights > 0 ? score / weights : 0;

    console.log(`Similarity: "${str1.substring(0, 40)}" vs "${str2.substring(0, 40)}" = ${(finalScore * 100).toFixed(1)}%`);

    return finalScore;
}

// Extract short title from full product title
function extractShortTitle(fullTitle) {
    if (!fullTitle) return 'Unknown Product';

    let cleaned = fullTitle
        .replace(/\(.*?\)/g, '')
        .replace(/[-–—]\s*(Unlocked|GSM|CDMA|Certified|Refurbished|Pre-Owned|Factory|International|US Version).*/gi, '')
        .replace(/\s*,\s*(Free Shipping|Fast Delivery|Best Price|Top Rated|Best Seller).*/gi, '')
        .replace(/\s+(with|for|by)\s+.*/gi, '')
        .replace(/\b(Limited Edition|Special Edition|Exclusive)\b/gi, '')
        .replace(/\b(Verizon|AT&T|T-Mobile|Sprint|US Cellular)\b(?!\s*Unlocked)/gi, '')
        .replace(/\b(US Version|International Version|Global Version)\b/gi, '')
        .replace(/[•●○▪▫]/g, ' ')
        .replace(/[-–—]/g, ' ')
        .replace(/[,;:]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();

    const words = cleaned.split(/[\s,]+/).filter(w =>
        w.length > 1 &&
        !/^(the|and|or|with|for|by|in|on|at|to|from|of)$/i.test(w)
    );

    const maxWords = 15;
    let shortWords = words.slice(0, maxWords);

    const color = extractColorDetailed(fullTitle);
    if (color && shortWords.length < maxWords) {
        const colorWords = color.split(/\s+/);
        const colorInTitle = colorWords.every(cw =>
            shortWords.some(sw => sw.toLowerCase() === cw.toLowerCase())
        );

        if (!colorInTitle) {
            if (shortWords.length + colorWords.length <= maxWords) {
                shortWords.push(...colorWords);
            }
        }
    }

    const storageMatches = [...fullTitle.matchAll(/\b(\d+)\s*(GB|TB|MB)(?:\s*RAM)?\b/gi)];
    const storageInfo = [];

    for (const match of storageMatches) {
        const value = match[1];
        const unit = match[2].toUpperCase();
        const isRAM = match[0].toLowerCase().includes('ram');

        const storageStr = isRAM ? `${value}${unit} RAM` : `${value}${unit}`;

        const alreadyIncluded = shortWords.some(w =>
            w.toLowerCase().includes(value.toLowerCase()) &&
            w.toLowerCase().includes(unit.toLowerCase())
        );

        if (!alreadyIncluded && shortWords.length < maxWords) {
            storageInfo.push(storageStr);
        }
    }

    const uniqueStorage = [...new Set(storageInfo)];
    for (const storage of uniqueStorage) {
        if (shortWords.length < maxWords) {
            const storageWords = storage.split(/\s+/);
            if (shortWords.length + storageWords.length <= maxWords) {
                shortWords.push(...storageWords);
            }
        }
    }

    const configs = extractConfigs(fullTitle);
    for (const config of configs) {
        if (shortWords.length < maxWords) {
            const configWords = config.split(/\s+/);

            const configInTitle = configWords.every(cw =>
                shortWords.some(sw => sw.toLowerCase() === cw.toLowerCase())
            );

            if (!configInTitle && shortWords.length + configWords.length <= maxWords) {
                shortWords.push(...configWords);
            }
        }
    }

    const result = shortWords.join(' ');

    return result.length > 250 ? result.substring(0, 247) + '...' : result;
}

// Extract detailed color from title
function extractColorDetailed(title) {
    const multiWordColors = [
        'Natural Titanium', 'Blue Titanium', 'White Titanium', 'Black Titanium',
        'Space Gray', 'Space Black', 'Rose Gold', 'Midnight Green', 'Pacific Blue',
        'Sierra Blue', 'Alpine Green', 'Deep Purple', 'Starlight', 'Midnight',
        'Product Red', 'Jet Black', 'Matte Black', 'Graphite Black'
    ];

    for (const color of multiWordColors) {
        const regex = new RegExp(`\\b${color}\\b`, 'i');
        if (regex.test(title)) {
            return color;
        }
    }

    const singleWordColors = [
        'Red', 'Black', 'White', 'Blue', 'Green', 'Yellow', 'Purple',
        'Pink', 'Orange', 'Gray', 'Grey', 'Silver', 'Gold', 'Bronze',
        'Titanium', 'Graphite', 'Coral', 'Lavender'
    ];

    for (const color of singleWordColors) {
        const regex = new RegExp(`\\b${color}\\b`, 'i');
        if (regex.test(title)) {
            return color;
        }
    }

    return null;
}

// Extract key configurations from title
function extractConfigs(title) {
    const configs = [];

    const keywords = [
        'Unlocked', '5G', '4G', 'LTE', 'WiFi', 'Wi-Fi', 'WiFi 6', 'Bluetooth',
        'Dual SIM', 'eSIM', 'Touchscreen', 'Retina', 'OLED', 'AMOLED',
        'Water Resistant', 'Waterproof', 'Wireless Charging', 'Fast Charging',
        'Noise Cancelling', 'Active Noise Cancelling', 'ANC'
    ];

    for (const keyword of keywords) {
        const regex = new RegExp(`\\b${keyword}\\b`, 'i');
        if (regex.test(title)) {
            configs.push(keyword);
        }
    }

    return configs.slice(0, 3);
}

// Generate product information string
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

// Check if product is used/refurbished
function isUsedProduct(title) {
    if (!title) return false;

    const lowerTitle = title.toLowerCase();
    const usedKeywords = [
        'renewed', 'refurbished', 'pre-owned', 'used', 'open box',
        'certified refurbished', 'like new', 'second hand', 'secondhand',
        'reconditioned', 'remanufactured'
    ];

    return usedKeywords.some(keyword => lowerTitle.includes(keyword));
}

module.exports = {
    hashPassword,
    parsePrice,
    parseRating,
    cleanTitle,
    extractKeyInfo,
    calculateSimilarity,
    extractShortTitle,
    levenshteinDistance,
    extractColorDetailed,
    extractConfigs,
    generateInformation,
    isUsedProduct
};