package com.schoolms.app.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolms.app.ui.common.ErrorState
import com.schoolms.app.ui.common.LoadingState

/**
 * Screen for teachers to quickly mark attendance.
 * Designed to be "Phone-first" as per the MVP plan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceRollScreen(
    classId: String = "JSS1A", // Provided via navigation args usually
    viewModel: AttendanceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(classId) {
        viewModel.loadClass(classId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("Attendance saved successfully!")
            // Optionally navigate back or disable the save button
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roll Call - $classId") },
                actions = {
                    TextButton(onClick = { viewModel.submitAttendance() }) {
                        Text("Save", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState("Loading class roster...")
                }
                uiState.studentsList.isEmpty() && uiState.error == null -> {
                    // Empty state
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No students found in this class.")
                    }
                }
                else -> {
                    Column {
                        // Header Date Picker (Simplified to static text for MVP)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Date: ${uiState.date}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        // List of Students with Radio Buttons
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(uiState.studentsList) { item ->
                                AttendanceRowItem(
                                    item = item,
                                    onStatusChanged = { newStatus ->
                                        viewModel.updateStudentStatus(item.student.id, newStatus)
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual row for a student with quick-tap status buttons.
 */
@Composable
fun AttendanceRowItem(
    item: StudentAttendanceItem,
    onStatusChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Name and details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.student.firstName} ${item.student.lastName}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = item.student.admissionNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Segmented Control for Status (Present, Absent, Late)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusButton(
                text = "P",
                isSelected = item.status == "present",
                onClick = { onStatusChanged("present") }
            )
            StatusButton(
                text = "A",
                isSelected = item.status == "absent",
                onClick = { onStatusChanged("absent") }
            )
            StatusButton(
                text = "L",
                isSelected = item.status == "late",
                onClick = { onStatusChanged("late") }
            )
        }
    }
}

@Composable
fun StatusButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = if (isSelected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.outlinedButtonColors()
    }

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        contentPadding = PaddingValues(0.dp),
        colors = colors
    ) {
        Text(text)
    }
}
