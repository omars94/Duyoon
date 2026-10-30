package com.example.duyoon.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.duyoon.R
import com.example.duyoon.ui.viewmodel.ThimmaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditDebtScreen(
    debtId: Long,
    viewModel: ThimmaViewModel,
    onBack: () -> Unit
) {
    val persons by viewModel.persons.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val debt = debts.find { it.id == debtId }

    var selectedPersonId by remember(debt) { mutableStateOf(debt?.personId ?: 0L) }
    var direction by remember(debt) { mutableStateOf(debt?.direction ?: "RECEIVABLE") }
    var amountText by remember(debt) { mutableStateOf(debt?.originalAmount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var currency by remember(debt) { mutableStateOf(debt?.currency ?: "USD") }
    var description by remember(debt) { mutableStateOf(debt?.description ?: "") }

    var createdDateMillis by remember(debt) { mutableStateOf(debt?.createdDate ?: System.currentTimeMillis()) }
    var showCreatedDatePickerDialog by remember { mutableStateOf(false) }

    var dueDateMillis by remember(debt) { mutableStateOf(debt?.dueDate) }
    var showDueDatePickerDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

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
                title = { Text(text = "Edit Debt Record") },
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
            // Debt Type Selector (Lend / Borrow Icons)
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
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lend),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).padding(end = 6.dp),
                        tint = Color.Unspecified
                    )
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
                    Icon(
                        painter = painterResource(id = R.drawable.ic_borrow),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).padding(end = 6.dp),
                        tint = Color.Unspecified
                    )
                    Text(text = stringResource(R.string.i_owe_them), color = if (direction == "PAYABLE") MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Person Selector
            Text(text = stringResource(R.string.person), fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 5
            ) {
                persons.forEach { p ->
                    val isSelected = p.id == selectedPersonId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPersonId = p.id },
                        label = { Text(p.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
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
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lend),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    },
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

            // Transaction Date Field
            Text(text = stringResource(R.string.date), fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = dateFormat.format(Date(createdDateMillis)),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreatedDatePickerDialog = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                leadingIcon = {
                    IconButton(onClick = { showCreatedDatePickerDialog = true }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            // Due Date / Reminder Field
            Text(text = "Due Date & Reminder (Optional)", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (dueDateMillis != null) dateFormat.format(Date(dueDateMillis!!)) else "No Due Date (Set Reminder)",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDueDatePickerDialog = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (dueDateMillis != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    leadingIcon = {
                        IconButton(onClick = { showDueDatePickerDialog = true }) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = "Select Due Date")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                if (dueDateMillis != null) {
                    TextButton(onClick = { dueDateMillis = null }) {
                        Text("Clear")
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
                    if (amount > 0.0 && selectedPersonId != 0L) {
                        val updatedDebt = debt.copy(
                            personId = selectedPersonId,
                            direction = direction,
                            originalAmount = amount,
                            currency = currency,
                            description = description.trim().ifEmpty { null },
                            createdDate = createdDateMillis,
                            dueDate = dueDateMillis,
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

    // Created Date Picker Dialog
    if (showCreatedDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = createdDateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showCreatedDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            createdDateMillis = selected
                        }
                        showCreatedDatePickerDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatedDatePickerDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Due Date Picker Dialog
    if (showDueDatePickerDialog) {
        val dueDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDueDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = dueDatePickerState.selectedDateMillis
                        if (selected != null) {
                            dueDateMillis = selected
                        }
                        showDueDatePickerDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePickerDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = dueDatePickerState)
        }
    }
}
