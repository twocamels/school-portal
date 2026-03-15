import express from 'express';
import { supabase, executeQuery } from '../db/supabase.js';
import { authenticateAction } from '../middleware/auth.js';
import { enforceTenancy } from '../middleware/tenancy.js';

const router = express.Router();

// Apply auth and tenancy middleware to all student routes
router.use(authenticateAction);
router.use(enforceTenancy);

/**
 * GET /api/v1/students
 * Fetch paginated list of students.
 * Enforces `school_id` filtering if user is bound to a school,
 * or `organisation_id` if the user is an org_admin.
 */
router.get('/', async (req, res) => {
  const page = parseInt(req.query.page) || 1;
  const perPage = parseInt(req.query.per_page) || 30;
  const offset = (page - 1) * perPage;

  let query = supabase.from('students').select('*', { count: 'exact' });

  // Tenancy Enforcement
  if (req.user.school_id) {
    // Admin, Teacher, Accountant, etc - Locked to one school
    query = query.eq('school_id', req.user.school_id);
  } else if (req.user.role === 'org_admin') {
    // Proprietor - Sees all students across all their schools
    // Note: The `students` table has an `organisation_id` column for easier querying
    // matching the spec's multitenancy architecture.
    query = query.eq('organisation_id', req.user.organisation_id);
  }

  // Apply Pagination
  query = query.range(offset, offset + perPage - 1).order('created_at', { ascending: false });

  const { data: students, count, error } = await executeQuery(query);

  if (error) throw error; // Caught by Express global handler

  res.status(200).json({
    data: students,
    meta: {
      page,
      per_page: perPage,
      total: count,
      total_pages: Math.ceil((count || 0) / perPage)
    }
  });
});

/**
 * GET /api/v1/students/:id
 * Retrieve a single student by ID, ensuring tenancy bounds.
 */
router.get('/:id', async (req, res) => {
  const studentId = req.params.id;

  let query = supabase.from('students').select('*').eq('id', studentId).limit(1);

  // Tenancy Enforcement
  if (req.user.school_id) {
    query = query.eq('school_id', req.user.school_id);
  } else if (req.user.role === 'org_admin') {
    query = query.eq('organisation_id', req.user.organisation_id);
  }

  const { data, error } = await executeQuery(query);

  if (error) throw error;

  const student = data?.[0];

  if (!student) {
    return res.status(404).json({
      error: { message: 'Student not found or access denied', code: 'NOT_FOUND' }
    });
  }

  res.status(200).json({
    data: student
  });
});

// Stubs for future routes (e.g., POST, PUT, DELETE, /import)
// ...

export default router;
