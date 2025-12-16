// History routes
const express = require('express');
const router = express.Router();
const priceController = require('../controllers/priceController');
const historyController = require('../controllers/historyController');

// Price history
router.get('/:pid', priceController.getPriceHistory);

module.exports = router;
