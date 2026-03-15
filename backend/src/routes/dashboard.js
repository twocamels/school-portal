import express from 'express';
import { supabase, executeQuery } from '../db/supabase.js';
import { authenticateAction } from '../middleware/auth.js';
import { enforceTenancy } from '../middleware/tenancy.js';

const router = express.Router();

router.use(authenticateAction);
router.use(enforceTenancy);

/**
 * GET /api/v1/dashboard/stats
 * Overview dashboard for a specific location (Admin/Principal).
 */
router.get('/stats', async (req, res) => {
  // Ensure we are scoping to a single location
  const schoolId = req.user.school_id;
  
  if (!schoolId) {
    return res.status(403).json({
      error: { message: 'This endpoint requires a specific school context.', code: 'FORBIDDEN' }
    });
  }

  // In a real production SQL setup, this would be a single call to the `v_org_dashboard`
  // or `v_attendance_summary` view as mentioned in the handoff.
  // For brevity, we simulate the 4 distinct queries hitting the db in parallel.
  
  const queries = await Promise.all([
    // 1. Total Students
    executeQuery(supabase.from('students').select('*', { count: 'exact', head: true }).eq('school_id', schoolId).eq('status', 'active')),
    
    // 2. Fees Outstanding (Using invoices balance column)
    executeQuery(supabase.from('invoices').select('balance').eq('school_id', schoolId).gt('balance', 0)),
    
    // 3. Today's Attendance
    executeQuery(supabase.from('attendance_records').select('id', { count: 'exact', head: true }).eq('school_id', schoolId).eq('date', new Date().toISOString().split('T')[0]).eq('status', 'present')),
    
    // 4. Recent Payments
    executeQuery(supabase.from('payments').select('amount, payment_date, students(first_name)').eq('school_id', schoolId).order('payment_date', { ascending: false }).limit(5))
  ]);

  const [studentsRes, feesRes, attendanceRes, recentPaymentsRes] = queries;

  // Aggregate fee debtors
  const totalDebtors = feesRes.data?.length || 0;
  const totalDebtAmount = feesRes.data?.reduce((sum, inv) => sum + (inv.balance || 0), 0) || 0;

  res.status(200).json({
    data: {
      total_students: studentsRes.count || 0,
      attendance_today_count: attendanceRes.count || 0,
      fees: {
        total_debtors: totalDebtors,
        total_outstanding: totalDebtAmount
      },
      recent_payments: recentPaymentsRes.data || []
    }
  });
});

/**
 * GET /api/v1/dashboard/org-stats
 * Overview dashboard for the Proprietor (Org Admin).
 * Shows insights across ALL campuses.
 */
router.get('/org-stats', async (req, res) => {
  if (req.user.role !== 'org_admin') {
    return res.status(403).json({
      error: { message: 'Org Admin access required.', code: 'FORBIDDEN' }
    });
  }

  const orgId = req.user.organisation_id;

  // We query the `v_org_dashboard` view directly here as specified in `schema.sql`.
  // This view pre-aggregates total students, total collected, and total outstanding grouped by `school_id`.
  
  const { data: locationStats, error } = await executeQuery(
    supabase
      .from('v_org_dashboard')
      .select('*')
      .eq('organisation_id', orgId)
  );

  if (error) throw error;

  // Provide a grand total summary across all locations for the top of the dashboard
  const grandTotals = (locationStats || []).reduce((acc, loc) => {
    acc.total_students += (loc.student_count || 0);
    acc.total_collected += (loc.total_collected || 0);
    acc.total_outstanding += (loc.total_outstanding || 0);
    return acc;
  }, { total_students: 0, total_collected: 0, total_outstanding: 0 });

  res.status(200).json({
    data: {
      grand_totals: grandTotals,
      locations: locationStats || []
    }
  });
});

export default router;
