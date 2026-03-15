package com.schoolms.app.ui.scores

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import compiled.schoolms.app.ui.theme.AmberWarning // Custom theme color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolms.app.ui.common.LoadingState

/**
 * Screen for teachers to enter CA1, CA2, and Exam scores.
 * Fits into the "Phone-first" MVP strategy. Allows fast numeric keypad entry
 * and highlights low-confidence OCR results in amber.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreEntryScreen(
    classId: String = "JSS1A",
    subjectId: String = "MATH101",
    termId: String = "TERM1_2026",
    viewModel: ScoreEntryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(classId, subjectId, termId) {
        viewModel.loadClassScores(classId, subjectId, termId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("Scores saved securely!")
            // onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Scores") },
                actions = {
                    TextButton(onClick = { viewModel.submitScores() }) {
                        Text("Submit", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (uiState.isLoading) {
                LoadingState("Loading class list & OCR results...")
            } else {
                Column {
                    // Header Row showing column definitions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Student", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium)
                        Text("CA1", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                        Text("CA2", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                        Text("Exam", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                        Text("Total", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium)
                    }

                    // Input Grid
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp) // Space for FABs if any
                    ) {
                        items(uiState.studentsList) { item ->
                            ScoreRowItem(
                                item = item,
                                onScoreChanged = { field, value ->
                                    viewModel.updateStudentScore(item.student.id, field, value)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreRowItem(
    item: StudentScoreItem,
    onScoreChanged: (String, String) -> Unit
) {
    // If OCR confidence is below 85%, tint the background amber to force manual review
    // The "AmberWarning" custom color handles the specific visual requirement from spec.
    val rowModifier = if (item.hasWarning) {
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E1)) // Simulated AmberWarning for standalone file
            .padding(horizontal = 16.dp, vertical = 8.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name & Optional Warning Icon
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = "${item.student.firstName} ${item.student.lastName}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            if (item.hasWarning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Low Confidence", tint = Color(0xFFFFA000), modifier = Modifier.size(14.dp))
                    Text(" Review", color = Color(0xFFFFA000), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // CA1 Input (Max 20)
        OutlinedTextField(
            value = item.ca1,
            onValueChange = { onScoreChanged("ca1", it) },
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // CA2 Input (Max 20)
        OutlinedTextField(
            value = item.ca2,
            onValueChange = { onScoreChanged("ca2", it) },
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Exam Input (Max 60)
        OutlinedTextField(
            value = item.exam,
            onValueChange = { onScoreChanged("exam", it) },
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        // Computed Total (Max 100)
        Text(
            text = item.total.toString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (item.total < 50) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
