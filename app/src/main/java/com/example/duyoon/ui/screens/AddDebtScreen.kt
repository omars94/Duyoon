package com.example.duyoon.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(
    initialPersonId: Long? = null,
    viewModel: ThimmaViewModel,
    onBack: () -> Unit
) {
    val persons by viewModel.persons.collectAsState()

    var direction by remember { mutableStateOf("RECEIVABLE") } // RECEIVABLE ("They owe me") or PAYABLE ("I owe them")
    var selectedPersonId by remember { mutableStateOf(initialPersonId ?: persons.firstOrNull()?.id ?: 0L) }
    var newPersonName by remember { mutableStateOf("") }
    var isAddingNewPerson by remember { mutableStateOf(persons.isEmpty()) }

    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val currencies = listOf("USD", "LBP", "EUR", "SAR", "AED")
    var expandedCurrency by remember { mutableStateOf(false) }

    var expandedPerson by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.new_debt)) },
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

            // Person Selection
            Text(text = stringResource(R.string.person), fontWeight = FontWeight.Bold)
            if (isAddingNewPerson) {
                OutlinedTextField(
                    value = newPersonName,
                    onValueChange = { newPersonName = it },
                    label = { Text("Person Name") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                TextButton(onClick = { isAddingNewPerson = false }) {
                    Text("Select from existing persons")
                }
            } else {
                val selectedPerson = persons.find { it.id == selectedPersonId }
                OutlinedButton(
                    onClick = { expandedPerson = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(text = selectedPerson?.name ?: stringResource(R.string.select_person))
                }
                DropdownMenu(
                    expanded = expandedPerson,
                    onDismissRequest = { expandedPerson = false }
                ) {
                    persons.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = {
                                selectedPersonId = p.id
                                expandedPerson = false
                            },
                            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("+ Add New Person") },
                        onClick = {
                            isAddingNewPerson = true
                            expandedPerson = false
                        },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) }
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

            // Notes
            Text(text = stringResource(R.string.notes), fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Additional details...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Create, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        if (isAddingNewPerson && newPersonName.isNotBlank()) {
                            viewModel.addPerson(newPersonName.trim(), null, null) { newId ->
                                viewModel.addDebt(
                                    personId = newId,
                                    direction = direction,
                                    amount = amount,
                                    currency = currency,
                                    description = description.trim().ifEmpty { null },
                                    dueDate = null
                                )
                                onBack()
                            }
                        } else if (!isAddingNewPerson && selectedPersonId != 0L) {
                            viewModel.addDebt(
                                personId = selectedPersonId,
                                direction = direction,
                                amount = amount,
                                currency = currency,
                                description = description.trim().ifEmpty { null },
                                dueDate = null
                            )
                            onBack()
                        }
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
