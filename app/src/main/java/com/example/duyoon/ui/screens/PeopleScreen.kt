package com.example.duyoon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.duyoon.R
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
    val persons by viewModel.persons.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var newPersonName by remember { mutableStateOf("") }
    var newPersonPhone by remember { mutableStateOf("") }

    val filteredPersons = persons.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                (it.phone != null && it.phone.contains(searchQuery))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_people)) },
                actions = {
                    IconButton(onClick = { showAddPersonDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Person")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPersonDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Person")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPersons) { person ->
                    val personDebts = debts.filter { it.personId == person.id }
                    
                    var receivableSummary = mutableMapOf<String, Double>()
                    var payableSummary = mutableMapOf<String, Double>()

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
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text(text = "Add Person") },
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
                            showAddPersonDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
