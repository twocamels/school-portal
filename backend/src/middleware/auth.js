import jwt from 'jsonwebtoken';
import { supabase, executeQuery } from '../db/supabase.js';

/**
 * Authentication Middleware
 * Validates the JWT Bearer token and attaches the decoded payload to req.user.
 */
export const authenticateAction = async (req, res, next) => {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      error: {
        message: 'Missing or malformed Authorization header',
        code: 'UNAUTHORIZED'
      }
    });
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = decoded; // Attach the decoded payload to the request object
    
    // As per the handoff, the JWT should contain at least:
    // id (user\_id), organisation_id, school_id, scope (role)
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return res.status(401).json({
        error: {
          message: 'Token has expired',
          code: 'TOKEN_EXPIRED'
        }
      });
    }

    return res.status(403).json({
      error: {
        message: 'Invalid token',
        code: 'FORBIDDEN'
      }
    });
  }
};
