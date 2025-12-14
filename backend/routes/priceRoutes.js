// Price routes
const express = require('express');
const router = express.Router();
const priceController = require('../controllers/priceController');

router.get('/:pid', priceController.getLatestPrices);

module.exports = router;
