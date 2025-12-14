// View history routes
const express = require('express');
const router = express.Router();
const historyController = require('../controllers/historyController');

router.get('/:uid', historyController.getViewHistory);
router.post('/', historyController.addViewHistory);
router.delete('/user/:uid', historyController.clearUserHistory);
router.delete('/:hid', historyController.deleteViewHistory);

module.exports = router;
