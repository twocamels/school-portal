package com.schoolms.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolms.app.ui.common.LoadingState
import java.text.NumberFormat
import java.util.Locale

/**
 * The main overview screen for Admin (location) and OrgAdmin (multi-location).
 * Heavily relies on dashboard KPIs detailed in the spec for demo purposes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userRole: String = "admin", // Passed from DataStore via NavGraph args
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToDebtors: () -> Unit,
    onNavigateToAttendanceDetails: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userRole) {
        viewModel.loadDashboard(userRole)
    }

    val formatCurrency = { amount: Double -> 
        val format = NumberFormat.getCurrencyInstance(Locale("en", "NG"))
        format.format(amount).replace("NGN", "₦")
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (uiState.isOrgAdmin) "Proprietor Portal" else "Principal Portal") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingState("Fetching latest school insights...")
                uiState.error != null -> {
                    // Retry state
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.error!!)
                    }
                }
                else -> {
                    // The main dashboard grid/list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Grand Totals Row (Visible to all)
                        item {
                            DashboardMetricCard(
                                title = "Total Students Enrolled",
                                value = uiState.totalStudents.toString()
                            )
                        }

                        if (!uiState.isOrgAdmin) {
                            // Single School View Elements
                            item {
                                DashboardMetricCard(
                                    title = "Today's Attendance Rate",
                                    value = "${((uiState.todayAttendance.toDouble() / uiState.totalStudents) * 100).toInt()}% • (${uiState.todayAttendance} Present)",
                                    onClick = onNavigateToAttendanceDetails
                                )
                            }
                        }

                        // Financial Overview
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                DashboardMetricCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Fees Collected",
                                    value = formatCurrency(uiState.totalCollected)
                                )
                                DashboardMetricCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Outstanding (Debtors)",
                                    value = formatCurrency(uiState.totalOutstanding),
                                    isWarning = true,
                                    onClick = onNavigateToDebtors
                                )
                            }
                        }

                        // OrgAdmin Specific Views: Comparison list
                        if (uiState.isOrgAdmin) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Campus Breakdown", style = MaterialTheme.typography.titleLarge)
                            }
                            
                            items(uiState.locations) { location ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(location.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Students: ${location.students}", style = MaterialTheme.typography.bodyMedium)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Paid: " + formatCurrency(location.collected), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Owed: " + formatCurrency(location.outstanding), color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    } // end LazyColumn
                }
            } // end when
        }
    }
}

/**
 * Reusable layout block for major metrics (Students / Cash)
 */
@Composable
fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    isWarning: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isWarning) Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                if (isWarning) Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
