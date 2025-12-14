// Utility functions for password, price parsing, and text matching
const crypto = require('crypto');

const OpenAI = require('openai');
const openai = new OpenAI({
    apiKey: process.env.OPENAI_API_KEY
});

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

// Calculate similarity score between two product titles (0-1 range)
async function calculateSimilarity(title1, title2) {
    const response = await openai.chat.completions.create({
        model: "gpt-5-nano",
        messages: [
            {
                role: "system",
                content: `You are a product matching expert. Compare two product titles and return ONLY a similarity score from 0.0000 to 1.0000 (4 decimal places).

Scoring criteria (by importance):
1. Brand (highest priority): Must match exactly
   - Same brand: continue scoring
   - Different brand: return 0.0000-0.2000

2. Model and product name (high priority): Core product identifier
   - Same: major score boost
   - Different: significant penalty

3. Specifications (medium priority): Storage, color, size and other important features.

Ignore seller info and marketing words.
Return ONLY a decimal number between 0.0000 and 1.0000, nothing else.`
            },
            {
                role: "user",
                content: `Title 1: ${title1}\nTitle 2: ${title2}`
            }
        ]
    });

    const scoreText = response.choices[0].message.content.trim();
    const score = parseFloat(scoreText);

    if (isNaN(score) || score < 0 || score > 1) {
        throw new Error('Invalid AI response');
    }

    console.log(` AI Similarity: "${title1.substring(0, 30)}" vs "${title2.substring(0, 30)}" = ${(score * 100).toFixed(2)}%`);
    return score;
}

// Extract short title from full product title
async function extractShortTitle(fullTitle) {
    const response = await openai.chat.completions.create({
        model: "gpt-5-nano",
        messages: [
            {
                role: "system",
                content: `You are a product title optimizer. Extract the core product information from titles, keeping only:
- Brand name
- Product model/name
- Key specifications/configurations (storage, color, important features)
- Remove marketing words, conditions (renewed, refurbished), seller info, and redundant details
- Maximum 15 words
- Return ONLY the cleaned title, no explanation.`
            },
            {
                role: "user",
                content: `Extract short title from: ${fullTitle}`
            }
        ]
    });

    const shortTitle = response.choices[0].message.content.trim();
    console.log(` AI Short Title: "${fullTitle.substring(0, 50)}..." → "${shortTitle}"`);
    return shortTitle;
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
    calculateSimilarity,
    extractShortTitle,
    generateInformation,
    isUsedProduct
};