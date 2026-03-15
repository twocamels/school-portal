import express from 'express';
import { supabase, executeQuery } from '../db/supabase.js';
import { authenticateAction } from '../middleware/auth.js';
import { enforceTenancy } from '../middleware/tenancy.js';
// import { processMarksheetOcr } from '../services/ocr.js'; // Future implementation

const router = express.Router();

router.use(authenticateAction);
router.use(enforceTenancy);

/**
 * POST /api/v1/scores/bulk
 * Upload scores manually or as the finalized result of an OCR scan.
 * Expected payload: { term_id: 'uuid', subject_id: 'uuid', class_id: 'uuid', scores: [{ student_id: 'uuid', ca1: 20, ca2: 15, exam: 50 }] }
 */
router.post('/bulk', async (req, res) => {
  const { term_id, subject_id, class_id, scores } = req.body;

  if (!term_id || !subject_id || !class_id || !Array.isArray(scores) || scores.length === 0) {
    return res.status(400).json({
      error: { message: 'term_id, subject_id, class_id and scores array are required', code: 'BAD_REQUEST' }
    });
  }

  // 1. Verify access to the class (Security check)
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

  // 2. Prepare payload for upsert
  const scorePayload = scores.map(s => {
    // Basic validation on limits (ca1/ca2 max 20, exam max 60)
    const ca1 = Math.min(Math.max(s.ca1 || 0, 0), 20);
    const ca2 = Math.min(Math.max(s.ca2 || 0, 0), 20);
    const exam = Math.min(Math.max(s.exam || 0, 0), 60);
    
    // Note: total and grade are computed automatically by PostgreSQL generated columns
    // See schema.sql: scores.total = ca1 + ca2 + exam
    
    return {
      student_id: s.student_id,
      term_id,
      subject_id,
      class_id,
      ca1,
      ca2,
      exam,
      recorded_by: req.user.id,
      school_id: req.user.school_id, // See Attendance routes note on org_admins
      organisation_id: req.user.organisation_id
    };
  });

  // 3. Upsert records (on conflict update scores)
  const { error: upsertError } = await executeQuery(
    supabase
      .from('scores')
      .upsert(scorePayload, { onConflict: 'student_id, term_id, subject_id' })
  );

  if (upsertError) throw upsertError;

  res.status(200).json({
    data: { message: `Successfully recorded scores for ${scores.length} students.` }
  });
});

/**
 * POST /api/v1/ocr/score-sheet
 * Proxy endpoint to pass an uploaded image to Google Cloud Vision API.
 * Keeps the GCP Service Account Key secure on the backend.
 * 
 * Note: Actual implementation depends on multer/body-parser setup for file uploads.
 */
router.post('/ocr-score-sheet', async (req, res) => {
  // 1. Receive Base64 image or Multipart file from req
  // 2. Call Google Cloud Vision API (TEXT_DETECTION)
  // 3. Run heuristic regex parsing across bounding boxes to extract tabular data
  //    Mapping coordinates to rows (Student Name, CA1, CA2, Exam)
  // 4. Return array of `{ student_name, ca1, ca2, exam, confidence: 0.92 }`
  
  // This is a stub for the proxy as per the handoff logic.
  res.status(200).json({
    data: {
      message: "OCR parsing successful",
      results: [
        { student_id: 'uuid-1', ca1: 18, ca2: 15, exam: 50, confidence: 0.95 },
        { student_id: 'uuid-2', ca1: 12, ca2: 10, exam: 45, confidence: 0.82 } // Below 85%, app should flag amber
      ]
    }
  });
});

export default router;
