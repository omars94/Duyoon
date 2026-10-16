package com.example.duyoon.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.duyoon.R
import com.example.duyoon.ui.viewmodel.ThimmaViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDebtScreen(
    debtId: Long,
    viewModel: ThimmaViewModel,
    onBack: () -> Unit
) {
    val debts by viewModel.debts.collectAsState()
    val debt = debts.find { it.id == debtId }

    var direction by remember(debt) { mutableStateOf(debt?.direction ?: "RECEIVABLE") }
    var amountText by remember(debt) { mutableStateOf(debt?.originalAmount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var currency by remember(debt) { mutableStateOf(debt?.currency ?: "USD") }
    var description by remember(debt) { mutableStateOf(debt?.description ?: "") }

    val currencies = listOf("USD", "LBP", "EUR", "SAR", "AED")
    var expandedCurrency by remember { mutableStateOf(false) }

    if (debt == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Debt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Debt Type Selector
            Text(text = stringResource(R.string.debt_type), fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { direction = "RECEIVABLE" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (direction == "RECEIVABLE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                    Text(text = stringResource(R.string.they_owe_me), color = if (direction == "RECEIVABLE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { direction = "PAYABLE" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (direction == "PAYABLE") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                    Text(text = stringResource(R.string.i_owe_them), color = if (direction == "PAYABLE") MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Amount & Currency
            Text(text = stringResource(R.string.amount), fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("0.00") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Box {
                    OutlinedButton(
                        onClick = { expandedCurrency = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = currency)
                    }
                    DropdownMenu(
                        expanded = expandedCurrency,
                        onDismissRequest = { expandedCurrency = false }
                    ) {
                        currencies.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    currency = c
                                    expandedCurrency = false
                                }
                            )
                        }
                    }
                }
            }

            // Description / Reason
            Text(text = stringResource(R.string.description), fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Restaurant payment, Loan, etc.") },
                leadingIcon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        val updatedDebt = debt.copy(
                            direction = direction,
                            originalAmount = amount,
                            currency = currency,
                            description = description.trim().ifEmpty { null },
                            updatedAt = System.currentTimeMillis()
                        )
                        viewModel.updateDebt(updatedDebt)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        }
    }
}
