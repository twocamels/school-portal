import express from 'express';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { supabase, executeQuery } from '../db/supabase.js';

const router = express.Router();

/**
 * POST /api/v1/auth/login
 * Authenticates a user and returns a JWT token.
 * Validates against the user's `role` to ensure they are assigned 
 * to the correct scope (organisation or school).
 */
router.post('/login', async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({
      error: { message: 'Email and password are required', code: 'BAD_REQUEST' }
    });
  }

  // 1. Find user by email
  const { data: users, error: dbError } = await executeQuery(
    supabase.from('users').select('*').eq('email', email).limit(1)
  );

  if (dbError) throw dbError; // Caught by Express 5 global error handler
  
  const user = users?.[0];

  if (!user || user.status === 'inactive') {
    return res.status(401).json({
      error: { message: 'Invalid credentials or inactive account', code: 'UNAUTHORIZED' }
    });
  }

  // 2. Verify password (bcrypt cost 12 as per spec)
  const isMatch = await bcrypt.compare(password, user.password_hash);
  if (!isMatch) {
    return res.status(401).json({
      error: { message: 'Invalid credentials', code: 'UNAUTHORIZED' }
    });
  }

  // 3. Generate JWT
  const payload = {
    id: user.id,
    organisation_id: user.organisation_id,
    school_id: user.school_id, // May be null for org_admin
    role: user.role
  };

  const token = jwt.sign(payload, process.env.JWT_SECRET, {
    expiresIn: '8h' // 8hr access token as per spec
  });

  // 4. Return formatted response envelope
  res.status(200).json({
    data: {
      token,
      user: {
        id: user.id,
        email: user.email,
        first_name: user.first_name,
        last_name: user.last_name,
        role: user.role,
        organisation_id: user.organisation_id,
        school_id: user.school_id
      }
    },
    meta: {
      expires_in: 28800 // 8 hours in seconds
    }
  });
});

export default router;
