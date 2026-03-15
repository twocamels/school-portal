package com.schoolms.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.schoolms.app.ui.attendance.AttendanceRollScreen
import com.schoolms.app.ui.auth.LoginScreen
import com.schoolms.app.ui.dashboard.DashboardScreen
import com.schoolms.app.ui.fees.PaymentScreen
import com.schoolms.app.ui.scores.ScoreEntryScreen
import com.schoolms.app.ui.students.StudentListScreen

/**
 * Defines all navigation destinations in the application.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    
    // Role-specific Dashboards
    object OrgDashboard : Screen("org_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
    object TeacherDashboard : Screen("teacher_dashboard")
    object AccountantDashboard : Screen("accountant_dashboard")

    // Feature Screens
    object StudentsList : Screen("students_list")
    object AttendanceRoll : Screen("attendance_roll/{classId}") {
        fun createRoute(classId: String) = "attendance_roll/$classId"
    }
    object ScoreEntry : Screen("score_entry/{classId}/{subjectId}/{termId}") {
        fun createRoute(classId: String, subjectId: String, termId: String) = 
            "score_entry/$classId/$subjectId/$termId"
    }
    object PaymentEntry : Screen("payment_entry/{invoiceId}") {
        fun createRoute(invoiceId: String) = "payment_entry/$invoiceId"
    }
}

/**
 * Central NavHost for Jetpack Compose.
 */
@Composable
fun SchoolMsNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        
        // --- Auth Flow ---
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = { role ->
                    val dest = when (role) {
                        "org_admin" -> Screen.OrgDashboard.route
                        "admin" -> Screen.AdminDashboard.route
                        "teacher" -> Screen.TeacherDashboard.route
                        "accountant" -> Screen.AccountantDashboard.route
                        else -> Screen.Login.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true } // Clear backstack
                    }
                }
            )
        }

        // --- Dashboards ---
        composable(route = Screen.OrgDashboard.route) {
            DashboardScreen(
                userRole = "org_admin",
                onNavigateToDebtors = { /* Navigate to debtors list */ },
                onNavigateToAttendanceDetails = { /* Not applicable to Org, but needs a lambda */ }
            )
        }

        composable(route = Screen.AdminDashboard.route) {
            DashboardScreen(
                userRole = "admin",
                onNavigateToDebtors = { /* Navigate to debtors list */ },
                onNavigateToAttendanceDetails = { /* Navigate to attendance reports */ }
            )
        }

        composable(route = Screen.TeacherDashboard.route) {
            // Placeholder: A teacher dash usually just lists their assigned classes to tap 
            // and trigger the AttendanceRoll or ScoreEntry screens.
        }

        composable(route = Screen.AccountantDashboard.route) {
            // Placeholder: Accountant dash usually has a search bar to find a student invoice 
            // and navigate to PaymentEntry.
        }

        // --- Features ---
        composable(route = Screen.StudentsList.route) {
            StudentListScreen(
                onNavigateToDetail = { studentId -> 
                    /* Navigate to detail screen */ 
                },
                onNavigateToAddStudent = {
                    /* Navigate to form */
                }
            )
        }

        composable(route = Screen.AttendanceRoll.route) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: "Unknown"
            AttendanceRollScreen(
                classId = classId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ScoreEntry.route) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: "Unknown"
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: "Unknown"
            val termId = backStackEntry.arguments?.getString("termId") ?: "Unknown"
            
            ScoreEntryScreen(
                classId = classId,
                subjectId = subjectId,
                termId = termId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.PaymentEntry.route) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: "Unknown"
            PaymentScreen(
                invoiceId = invoiceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
