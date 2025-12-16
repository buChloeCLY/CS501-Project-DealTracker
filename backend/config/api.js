// RapidAPI configuration
require('dotenv').config();

const RAPIDAPI_KEY = process.env.RAPIDAPI_KEY;

const RAPIDAPI_KEYS = {
    amazon: process.env.RAPIDAPI_KEY_AMAZON || RAPIDAPI_KEY,
    ebay: process.env.RAPIDAPI_KEY_EBAY || RAPIDAPI_KEY,
    walmart: process.env.RAPIDAPI_KEY_WALMART || RAPIDAPI_KEY
};

const API_HOSTS = {
    amazon: 'real-time-amazon-data.p.rapidapi.com',
    ebay: 'real-time-ebay-data.p.rapidapi.com',
    walmart: 'walmart-search-and-pricing.p.rapidapi.com'
};

module.exports = {
    RAPIDAPI_KEY,
    RAPIDAPI_KEYS,
    API_HOSTS
};
