package com.example.duyoon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.duyoon.R
import com.example.duyoon.ui.theme.PrimaryGreen
import com.example.duyoon.ui.theme.SecondaryOrange
import com.example.duyoon.ui.viewmodel.ThimmaViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: ThimmaViewModel,
    onNavigateToAddDebtWithDir: (String) -> Unit,
    onNavigateToPerson: (Long) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val persons by viewModel.persons.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val payments by viewModel.payments.collectAsState()

    val personMap = remember(persons) { persons.associateBy { it.id } }

    val receivableTotals = mutableMapOf<String, Double>()
    val payableTotals = mutableMapOf<String, Double>()

    val receivablePeople = mutableSetOf<Long>()
    val payablePeople = mutableSetOf<Long>()

    debts.forEach { debt ->
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

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Lend FAB (Emerald)
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToAddDebtWithDir("RECEIVABLE") },
                    containerColor = PrimaryGreen,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lend),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.they_owe_me), fontWeight = FontWeight.Bold)
                }

                // Borrow FAB (Coral)
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToAddDebtWithDir("PAYABLE") },
                    containerColor = SecondaryOrange,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_borrow),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.i_owe_them), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Friendly Header
            item {
                Column {
                    Text(
                        text = "$greeting 👋",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Balanced Multi-Currency Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Lend Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.owed_to_me),
                                style = MaterialTheme.typography.labelLarge,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                            if (receivableTotals.isNotEmpty()) {
                                receivableTotals.forEach { (curr, amt) ->
                                    Text(
                                        text = "$curr ${String.format(Locale.US, "%.2f", amt)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                }
                            } else {
                                Text(
                                    text = "$0.00",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                            Text(
                                text = stringResource(R.string.people_count, receivablePeople.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Borrow Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = SecondaryOrange.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.i_owe),
                                style = MaterialTheme.typography.labelLarge,
                                color = SecondaryOrange,
                                fontWeight = FontWeight.Bold
                            )
                            if (payableTotals.isNotEmpty()) {
                                payableTotals.forEach { (curr, amt) ->
                                    Text(
                                        text = "$curr ${String.format(Locale.US, "%.2f", amt)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryOrange
                                    )
                                }
                            } else {
                                Text(
                                    text = "$0.00",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryOrange
                                )
                            }
                            Text(
                                text = stringResource(R.string.people_count, payablePeople.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
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

            // Recent Activity List
            val recentDebts = debts.take(5)
            if (recentDebts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent activity. Use the buttons below to start.",
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
                    val desc = debt.description?.ifEmpty { null } ?: if (isReceivable) "Lend" else "Borrow"

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
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
}
