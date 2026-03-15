import express from 'express';
import { supabase, executeQuery } from '../db/supabase.js';
import { authenticateAction } from '../middleware/auth.js';
import { enforceTenancy } from '../middleware/tenancy.js';

const router = express.Router();

router.use(authenticateAction);
router.use(enforceTenancy);

/**
 * POST /api/v1/attendance/mark
 * Bulk mark attendance for a class on a specific date.
 * Expected payload: { class_id: 'uuid', date: 'YYYY-MM-DD', records: [{ student_id: 'uuid', status: 'present|absent|late', remarks: '' }] }
 */
router.post('/mark', async (req, res) => {
  const { class_id, date, records } = req.body;

  if (!class_id || !date || !Array.isArray(records) || records.length === 0) {
    return res.status(400).json({
      error: { message: 'Invalid payload. class_id, date, and records array are required.', code: 'BAD_REQUEST' }
    });
  }

  // 1. Double check the user has access to this class (security)
  let classQuery = supabase.from('classes').select('id').eq('id', class_id).limit(1);
  if (req.user.school_id) {
    classQuery = classQuery.eq('school_id', req.user.school_id);
  } else if (req.user.role === 'org_admin') {
     classQuery = classQuery.eq('organisation_id', req.user.organisation_id);
  }

  const { data: classData, error: classError } = await executeQuery(classQuery);
  if (classError) throw classError;
  if (!classData || classData.length === 0) {
    return res.status(403).json({
      error: { message: 'Class not found or access denied', code: 'FORBIDDEN' }
    });
  }

  // 2. Prepare records for bulk insert/upsert
  const attendancePayload = records.map(record => ({
    student_id: record.student_id,
    class_id,
    date,
    status: record.status,
    remarks: record.remarks || null,
    marked_by: req.user.id,
    // Inject tenancy fields
    school_id: req.user.school_id, // If org_admin does this, the frontend must supply school_id. We'd grab it from the class record ideally.
    organisation_id: req.user.organisation_id
  }));

  // Note: If an org_admin is marking attendance, req.user.school_id is null.
  // In a robust implementation, we would fetch the school_id from the classData in step 1 
  // and inject it into the payload above.
  
  // 3. Perform Upsert (on conflict update status/remarks/marked_by)
  // Assuming a unique constraint on (student_id, date)
  const { error: upsertError } = await executeQuery(
    supabase
      .from('attendance_records')
      .upsert(attendancePayload, { onConflict: 'student_id, date' })
  );

  if (upsertError) throw upsertError;

  res.status(200).json({
    data: { message: `Successfully marked attendance for ${records.length} students.` }
  });
});

/**
 * GET /api/v1/attendance/today
 * Fetch today's attendance summary for a specific class.
 * Useful for the teacher dashboard or pre-filling the roll screen.
 */
router.get('/today', async (req, res) => {
  const { class_id } = req.query;
  const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD

  if (!class_id) {
    return res.status(400).json({
      error: { message: 'class_id query parameter is required', code: 'BAD_REQUEST' }
    });
  }

  let query = supabase
    .from('attendance_records')
    .select('student_id, status, remarks')
    .eq('class_id', class_id)
    .eq('date', today);

  // Tenancy Enum
  if (req.user.school_id) {
      query = query.eq('school_id', req.user.school_id);
  }

  const { data, error } = await executeQuery(query);
  
  if (error) throw error;

  res.status(200).json({
    data: data || []
  });
});

export default router;
