package com.example.duyoon.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.duyoon.R
import com.example.duyoon.data.PersonEntity
import com.example.duyoon.ui.theme.PrimaryGreen
import com.example.duyoon.ui.theme.SecondaryOrange
import com.example.duyoon.ui.viewmodel.ThimmaViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    viewModel: ThimmaViewModel,
    onNavigateToPersonDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val persons by viewModel.persons.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddPersonOptionDialog by remember { mutableStateOf(false) }
    var showManualAddPersonDialog by remember { mutableStateOf(false) }
    var personToDelete by remember { mutableStateOf<PersonEntity?>(null) }

    var newPersonName by remember { mutableStateOf("") }
    var newPersonPhone by remember { mutableStateOf("") }

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
                    viewModel.addPerson(name.trim(), phone?.trim(), null)
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
        val permissionState = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
        if (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            contactPickerLauncher.launch(null)
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }
    }

    val filteredPersons = persons.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                (it.phone != null && it.phone.contains(searchQuery))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_people)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPersonOptionDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Person")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredPersons) { person ->
                    val personDebts = debts.filter { it.personId == person.id }

                    val receivableSummary = mutableMapOf<String, Double>()
                    val payableSummary = mutableMapOf<String, Double>()

                    personDebts.forEach { debt ->
                        val debtPayments = payments.filter { it.debtId == debt.id }
                        val remaining = debt.originalAmount - debtPayments.sumOf { it.amount }
                        if (remaining > 0.0) {
                            if (debt.direction == "RECEIVABLE") {
                                receivableSummary[debt.currency] = (receivableSummary[debt.currency] ?: 0.0) + remaining
                            } else {
                                payableSummary[debt.currency] = (payableSummary[debt.currency] ?: 0.0) + remaining
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPersonDetail(person.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (!person.phone.isNullOrEmpty()) {
                                    Text(
                                        text = person.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (receivableSummary.isNotEmpty()) {
                                    val text = receivableSummary.entries.joinToString(", ") { "${it.key} ${String.format(Locale.US, "%.2f", it.value)}" }
                                    Text(
                                        text = "+$text",
                                        color = PrimaryGreen,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (payableSummary.isNotEmpty()) {
                                    val text = payableSummary.entries.joinToString(", ") { "${it.key} ${String.format(Locale.US, "%.2f", it.value)}" }
                                    Text(
                                        text = "-$text",
                                        color = SecondaryOrange,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (receivableSummary.isEmpty() && payableSummary.isEmpty()) {
                                    Text(
                                        text = "Settled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onNavigateToPersonDetail(person.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Statement",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { personToDelete = person },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Person",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
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
                            showManualAddPersonDialog = true
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

    if (showManualAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showManualAddPersonDialog = false },
            title = { Text(text = "Add Person Manually") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPersonName,
                        onValueChange = { newPersonName = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPersonPhone,
                        onValueChange = { newPersonPhone = it },
                        label = { Text("Phone (Optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPersonName.isNotBlank()) {
                            viewModel.addPerson(newPersonName.trim(), newPersonPhone.trim().ifEmpty { null }, null)
                            newPersonName = ""
                            newPersonPhone = ""
                            showManualAddPersonDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddPersonDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete Person Confirmation Dialog
    if (personToDelete != null) {
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text(text = "Delete Person") },
            text = {
                Text(text = "Are you sure you want to delete ${personToDelete?.name}? This will also delete all associated debts and payment history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePerson(personToDelete!!)
                        personToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
