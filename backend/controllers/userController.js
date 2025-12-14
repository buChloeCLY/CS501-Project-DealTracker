// User controller - handles user registration, login, profile management
const pool = require('../config/database');
const { hashPassword } = require('../utils/helpers');

// User registration
// POST /api/user/register
// Body: { name, email, password, gender }
async function register(req, res) {
    const { name, email, password, gender } = req.body;

    if (!name || !email || !password) {
        return res.status(400).json({
            success: false,
            error: 'name, email, and password are required'
        });
    }

    try {
        const [existing] = await pool.query(
            'SELECT uid FROM user WHERE email = ?',
            [email]
        );

        if (existing.length > 0) {
            return res.status(409).json({
                success: false,
                error: 'Email already registered'
            });
        }

        const hashedPassword = hashPassword(password);

        const [result] = await pool.query(
            'INSERT INTO user (name, email, password, gender) VALUES (?, ?, ?, ?)',
            [name, email, hashedPassword, gender || null]
        );

        res.status(201).json({
            success: true,
            message: 'User registered successfully',
            uid: result.insertId
        });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({
            success: false,
            error: 'Registration failed'
        });
    }
}

// User login
// POST /api/user/login
// Body: { email, password }
async function login(req, res) {
    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({
            success: false,
            error: 'Email and password are required'
        });
    }

    try {
        const hashedPassword = hashPassword(password);

        const [users] = await pool.query(
            'SELECT uid, name, email, gender FROM user WHERE email = ? AND password = ?',
            [email, hashedPassword]
        );

        if (users.length === 0) {
            return res.status(401).json({
                success: false,
                error: 'Invalid email or password'
            });
        }

        const user = users[0];
        res.json({
            success: true,
            message: 'Login successful',
            user: {
                uid: user.uid,
                name: user.name,
                email: user.email,
                gender: user.gender
            }
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({
            success: false,
            error: 'Login failed'
        });
    }
}

// Get user info by ID
// GET /api/user/:uid
async function getUser(req, res) {
    const { uid } = req.params;

    try {
        const [users] = await pool.query(
            'SELECT uid, name, email, gender FROM user WHERE uid = ?',
            [uid]
        );

        if (users.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'User not found'
            });
        }

        res.json({
            uid: users[0].uid,
            name: users[0].name,
            email: users[0].email,
            gender: users[0].gender
        });
    } catch (error) {
        console.error('Get user error:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to get user'
        });
    }
}

// Update user info
// PUT /api/user/:uid
// Body: { name?, email?, gender?, password? }
async function updateUser(req, res) {
    const { uid } = req.params;
    const { name, email, gender, password } = req.body;

    try {
        const updates = [];
        const values = [];

        if (name) {
            updates.push('name = ?');
            values.push(name);
        }
        if (email) {
            updates.push('email = ?');
            values.push(email);
        }
        if (gender) {
            updates.push('gender = ?');
            values.push(gender);
        }
        if (password) {
            updates.push('password = ?');
            values.push(hashPassword(password));
        }

        if (updates.length === 0) {
            return res.status(400).json({
                success: false,
                error: 'No fields to update'
            });
        }

        values.push(uid);

        await pool.query(
            `UPDATE user SET ${updates.join(', ')} WHERE uid = ?`,
            values
        );

        const [users] = await pool.query(
            'SELECT uid, name, email, gender FROM user WHERE uid = ?',
            [uid]
        );

        res.json({
            success: true,
            message: 'User updated successfully',
            user: users[0]
        });
    } catch (error) {
        console.error('Update user error:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to update user'
        });
    }
}

// Delete user
// DELETE /api/user/:uid
async function deleteUser(req, res) {
    const { uid } = req.params;

    try {
        const [result] = await pool.query('DELETE FROM user WHERE uid = ?', [uid]);

        if (result.affectedRows === 0) {
            return res.status(404).json({
                success: false,
                error: 'User not found'
            });
        }

        res.json({
            success: true,
            message: 'User deleted successfully'
        });
    } catch (error) {
        console.error('Delete user error:', error);
        res.status(500).json({
            success: false,
            error: 'Failed to delete user'
        });
    }
}

module.exports = {
    register,
    login,
    getUser,
    updateUser,
    deleteUser
};
