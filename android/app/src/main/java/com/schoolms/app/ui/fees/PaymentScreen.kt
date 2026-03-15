package com.schoolms.app.ui.fees

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolms.app.domain.model.Invoice
import com.schoolms.app.domain.model.PaymentRequest

/**
 * Screen for the Accountant Dashboard to accept and record a payment 
 * manually or by triggering the OCR Receipt Flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    invoiceId: String = "inv-1", // Passed via Navigation route args
    viewModel: PaymentViewModel = hiltViewModel(), // Omitted ViewModel for brevity, behaves similarly to others
    onNavigateBack: () -> Unit
) {
    // Simplified State
    var amountText by remember { mutableStateOf("") }
    var referenceText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Bank Transfer") }
    
    // Simulate finding invoice on load
    val mockInvoice = Invoice(
        id = invoiceId,
        studentId = "stu-1",
        amountDue = 45000.0,
        amountPaid = 15000.0,
        balance = 30000.0,
        studentName = "Bola Tinubu",
        admissionNumber = "JSS1/001"
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Record Payment") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Invoice Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = mockInvoice.studentName, style = MaterialTheme.typography.titleLarge)
                    Text(text = "Student ID: ${mockInvoice.admissionNumber}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Outstanding Balance:", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "₦${mockInvoice.balance}", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // The OCR Highlight Button (From Spec)
            OutlinedButton(
                onClick = {
                    // Triggers hardware camera -> Cropper -> FeeRepository.scanReceipt()
                    // Then auto-fills the fields below
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan Teller")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Bank Teller (Auto-fill)")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // Manual Entry Fields
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount Paid (₦)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = referenceText,
                onValueChange = { referenceText = it },
                label = { Text("Reference / Teller No.") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // Triggers API submission via ViewModel
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = amountText.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) <= mockInvoice.balance
            ) {
                Text("Confirm Payment")
            }
        }
    }
}
