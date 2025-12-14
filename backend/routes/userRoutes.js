// User routes
const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');

router.post('/register', userController.register);
router.post('/login', userController.login);
router.get('/:uid', userController.getUser);
router.put('/:uid', userController.updateUser);
router.delete('/:uid', userController.deleteUser);

module.exports = router;
