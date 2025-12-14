// Admin routes
const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');

router.post('/import-initial', adminController.importInitialProducts);
router.post('/update-all-prices', adminController.updateAllPrices);
router.post('/sync-lowest-prices', adminController.syncLowestPricesEndpoint);
router.post('/add-walmart-prices', adminController.addWalmartPrices);
router.post('/sync-ebay-prices', adminController.syncEBayPrices);

module.exports = router;
