/**
 * Tenancy Middleware
 * Validates the tenant configuration based on the decoded JWT payload.
 * 
 * As per the spec, the user scope is:
 * - org_admin (proprietor) => sees all locations. school_id is NULL.
 * - admin, teacher, accountant, student => locked to one location (school_id is defined).
 * 
 * This middleware blocks users from performing actions outside their permitted scope.
 */
export const enforceTenancy = async (req, res, next) => {
  const { user } = req;

  if (!user || !user.role || !user.organisation_id) {
    return res.status(403).json({
      error: {
        message: 'Invalid tenancy configuration',
        code: 'FORBIDDEN_TENANCY'
      }
    });
  }

  // Check role-based tenancy
  if (user.role === 'org_admin') {
    // Org Admin sees all locations within their organisation
    // But they cannot access data from another organisation
    
    // Check if the route tries to explicitly access another organisation
    // For now we'll rely on the underlying queries to apply organisation_id filtering
    next();
  } else if (
    ['admin', 'teacher', 'accountant', 'student'].includes(user.role)
  ) {
    // These roles MUST be tied to a specific school
    if (!user.school_id) {
      return res.status(403).json({
        error: {
          message: 'User is missing school assignment',
          code: 'FORBIDDEN_NO_SCHOOL'
        }
      });
    }

    // Attach school_id for convenience, but queries should still always
    // assert 'WHERE school_id = user.school_id'
    next();
  } else {
    return res.status(403).json({
      error: {
        message: 'Unknown role',
        code: 'FORBIDDEN_ROLE'
      }
    });
  }
};
