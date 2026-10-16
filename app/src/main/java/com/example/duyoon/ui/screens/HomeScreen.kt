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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duyoon.R
import com.example.duyoon.data.DebtEntity
import com.example.duyoon.data.PaymentEntity
import com.example.duyoon.data.PersonEntity
import com.example.duyoon.ui.theme.PrimaryGreen
import com.example.duyoon.ui.theme.SecondaryOrange
import com.example.duyoon.ui.viewmodel.ThimmaViewModel
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: ThimmaViewModel,
    onNavigateToAddDebt: () -> Unit,
    onNavigateToPerson: (Long) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val persons by viewModel.persons.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val payments by viewModel.payments.collectAsState()

    val personMap = remember(persons) { persons.associateBy { it.id } }

    // Calculate totals by currency for receivables and payables
    val receivableTotals = mutableMapOf<String, Double>()
    val payableTotals = mutableMapOf<String, Double>()

    val receivablePeople = mutableSetOf<Long>()
    val payablePeople = mutableSetOf<Long>()

    debts.forEach { debt ->
        // Calculate remaining amount for this debt
        val debtPayments = payments.filter { it.debtId == debt.id }
        val totalPaid = debtPayments.sumOf { it.amount }
        val remaining = debt.originalAmount - totalPaid

        if (remaining > 0.0) {
            if (debt.direction == "RECEIVABLE") {
                receivableTotals[debt.currency] = (receivableTotals[debt.currency] ?: 0.0) + remaining
                receivablePeople.add(debt.personId)
            } else {
                payableTotals[debt.currency] = (payableTotals[debt.currency] ?: 0.0) + remaining
                payablePeople.add(debt.personId)
            }
        }
    }

    val primaryReceivableText = receivableTotals.entries.joinToString(", ") { "${it.key} ${String.format(Locale.US, "%.2f", it.value)}" }
        .ifEmpty { "$0.00" }

    val primaryPayableText = payableTotals.entries.joinToString(", ") { "${it.key} ${String.format(Locale.US, "%.2f", it.value)}" }
        .ifEmpty { "$0.00" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar / Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "My financial overview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = { /* Notifications */ }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Summary Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Owed to me Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.owed_to_me),
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryGreen
                        )
                        Text(
                            text = primaryReceivableText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Text(
                            text = stringResource(R.string.people_count, receivablePeople.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                // I owe Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SecondaryOrange.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.i_owe),
                            style = MaterialTheme.typography.labelMedium,
                            color = SecondaryOrange
                        )
                        Text(
                            text = primaryPayableText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryOrange
                        )
                        Text(
                            text = stringResource(R.string.people_count, payablePeople.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Add Debt Quick Button
        item {
            Button(
                onClick = onNavigateToAddDebt,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(text = stringResource(R.string.add_new_debt), fontWeight = FontWeight.Bold)
            }
        }

        // Recent Activity Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_activity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToHistory) {
                    Text(text = stringResource(R.string.view_all), color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Recent Activity List (Combined Debts & Payments)
        val recentDebts = debts.take(5)
        if (recentDebts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent activity yet. Tap Add New Debt to start.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(recentDebts) { debt ->
                val person = personMap[debt.personId]
                val personName = person?.name ?: "Unknown"
                val isReceivable = debt.direction == "RECEIVABLE"
                val sign = if (isReceivable) "+" else "−"
                val color = if (isReceivable) PrimaryGreen else SecondaryOrange
                val desc = debt.description?.ifEmpty { null } ?: if (isReceivable) "Lent money" else "Borrowed money"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPerson(debt.personId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
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
                                text = personName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "$desc · ${debt.currency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = "$sign${debt.currency} ${String.format(Locale.US, "%.2f", debt.originalAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = color,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
