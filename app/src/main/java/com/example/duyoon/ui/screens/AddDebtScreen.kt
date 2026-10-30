package com.example.duyoon.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.duyoon.R
import com.example.duyoon.ui.viewmodel.ThimmaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDebtScreen(
    initialPersonId: Long? = null,
    initialDirection: String? = null,
    viewModel: ThimmaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val persons by viewModel.persons.collectAsState()

    var direction by remember { mutableStateOf(initialDirection ?: "RECEIVABLE") }
    var selectedPersonId by remember { mutableStateOf(initialPersonId ?: persons.firstOrNull()?.id ?: 0L) }
    var newPersonName by remember { mutableStateOf("") }
    var newPersonPhone by remember { mutableStateOf<String?>(null) }
    var isAddingNewPerson by remember { mutableStateOf(persons.isEmpty()) }
    var showAddPersonOptionDialog by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Transaction Date state (defaults to Today)
    var createdDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCreatedDatePickerDialog by remember { mutableStateOf(false) }

    // Due Date state (optional reminder date)
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDueDatePickerDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val currencies = listOf("USD", "LBP", "EUR", "SAR", "AED")
    var expandedCurrency by remember { mutableStateOf(false) }

    // Contact Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            try {
                var name: String? = null
                var phone: String? = null

                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIdx >= 0) name = c.getString(nameIdx)

                        val phoneIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (phoneIdx >= 0) {
                            phone = c.getString(phoneIdx)
                        } else {
                            val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
                            if (idIdx >= 0) {
                                val contactId = c.getString(idIdx)
                                val pCursor = context.contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                    arrayOf(contactId),
                                    null
                                )
                                pCursor?.use { pc ->
                                    if (pc.moveToFirst()) {
                                        phone = pc.getString(0)
                                    }
                                }
                            }
                        }
                    }
                }

                if (!name.isNullOrBlank()) {
                    viewModel.addPerson(name.trim(), phone?.trim(), null) { newId ->
                        selectedPersonId = newId
                        isAddingNewPerson = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        }
    }

    val launchContactPicker = {
        val permissionState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            contactPickerLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Debt Type Selector Segmented Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = stringResource(R.string.debt_type), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                                modifier = Modifier.size(24.dp).padding(end = 4.dp),
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
                                modifier = Modifier.size(24.dp).padding(end = 4.dp),
                                tint = Color.Unspecified
                            )
                            Text(text = stringResource(R.string.i_owe_them), color = if (direction == "PAYABLE") MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Person Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = stringResource(R.string.person), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (isAddingNewPerson) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPersonName,
                                onValueChange = { newPersonName = it },
                                label = { Text("Person Name") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            if (persons.isNotEmpty()) {
                                IconButton(onClick = { isAddingNewPerson = false }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Cancel")
                                }
                            }
                        }
                    } else {
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
                            FilterChip(
                                selected = false,
                                onClick = { showAddPersonOptionDialog = true },
                                label = { Text(stringResource(R.string.add_person_chip)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Person",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Amount, Currency & Dates Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = stringResource(R.string.amount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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

                    // Compact Dates Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Field
                        OutlinedTextField(
                            value = dateFormat.format(Date(createdDateMillis)),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showCreatedDatePickerDialog = true },
                            enabled = false,
                            label = { Text(stringResource(R.string.date)) },
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

                        // Due Date Field
                        OutlinedTextField(
                            value = if (dueDateMillis != null) dateFormat.format(Date(dueDateMillis!!)) else "No Due Date",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDueDatePickerDialog = true },
                            enabled = false,
                            label = { Text("Due Date") },
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
                    }
                }
            }

            // Description & Notes Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = stringResource(R.string.description), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Restaurant payment, Loan, etc.") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Text(text = stringResource(R.string.notes), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Additional details...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Create, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        if (isAddingNewPerson && newPersonName.isNotBlank()) {
                            viewModel.addPerson(newPersonName.trim(), newPersonPhone, null) { newId ->
                                viewModel.addDebt(
                                    personId = newId,
                                    direction = direction,
                                    amount = amount,
                                    currency = currency,
                                    description = description.trim().ifEmpty { null },
                                    dueDate = dueDateMillis,
                                    createdDate = createdDateMillis
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
                                dueDate = dueDateMillis,
                                createdDate = createdDateMillis
                            )
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = stringResource(R.string.save), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Add Person Option Dialog
    if (showAddPersonOptionDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonOptionDialog = false },
            title = { Text(text = "Add Person") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            showAddPersonOptionDialog = false
                            launchContactPicker()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Import from Contacts")
                    }
                    OutlinedButton(
                        onClick = {
                            showAddPersonOptionDialog = false
                            isAddingNewPerson = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Add Manually")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddPersonOptionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
