// Product routes
const express = require('express');
const router = express.Router();
const productController = require('../controllers/productController');

router.get('/', productController.getAllProducts);
router.get('/search', productController.searchProducts);
router.get('/platform/:platform', productController.getByPlatform);
router.get('/price', productController.getByPriceRange);
router.get('/:pid', productController.getProductById);

module.exports = router;
