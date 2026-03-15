package com.schoolms.app.ui.students

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolms.app.domain.model.Student
import com.schoolms.app.ui.common.ErrorState
import com.schoolms.app.ui.common.LoadingState

/**
 * Screen displaying a paginated list of students.
 * Supports a "Search" context and navigation to details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: StudentListViewModel = hiltViewModel(),
    onNavigateToDetail: (studentId: String) -> Unit,
    onNavigateToAddStudent: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students") },
                actions = {
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search students")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddStudent) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Student")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.students.isEmpty() -> {
                    // Initial load state
                    LoadingState("Fetching students...")
                }
                uiState.error != null && uiState.students.isEmpty() -> {
                    // Critical failure, no offline data available
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.refreshData() }
                    )
                }
                else -> {
                    // Render the list of students natively cached from Room
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.students) { student ->
                            StudentListItem(student = student, onClick = { onNavigateToDetail(it.id) })
                        }
                        
                        if (uiState.isLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual row for a student in the list.
 */
@Composable
fun StudentListItem(
    student: Student,
    onClick: (Student) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(student) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${student.firstName} ${student.lastName}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            // e.g "JSS 1A • Admission: 2026/0123"
            Text(
                text = "${student.className ?: "Unassigned"} • Admission: ${student.admissionNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
