// Wishlist routes
const express = require('express');
const router = express.Router();
const wishlistController = require('../controllers/wishlistController');

// More specific routes first
router.get('/alerts', wishlistController.getPriceAlerts);
router.get('/', wishlistController.getWishlist);
router.post('/', wishlistController.addToWishlist);
router.delete('/', wishlistController.removeFromWishlist);

module.exports = router;