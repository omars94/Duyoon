package com.example.duyoon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
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
import com.example.duyoon.data.DebtEntity
import com.example.duyoon.data.PaymentEntity
import com.example.duyoon.ui.theme.PrimaryGreen
import com.example.duyoon.ui.theme.SecondaryOrange
import com.example.duyoon.ui.viewmodel.ThimmaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: ThimmaViewModel) {
    val persons by viewModel.persons.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Lend (إلي), 1 = Borrow (عليّ)

    val personMap = remember(persons) { persons.associateBy { it.id } }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

    val activities = remember(debts, payments, selectedTab) {
        val list = mutableListOf<Triple<Long, String, Any>>()
        val targetDirection = if (selectedTab == 0) "RECEIVABLE" else "PAYABLE"

        debts.filter { it.direction == targetDirection }.forEach {
            list.add(Triple(it.createdDate, "DEBT", it))
        }

        payments.forEach { payment ->
            val parentDebt = debts.find { it.id == payment.debtId }
            if (parentDebt != null && parentDebt.direction == targetDirection) {
                list.add(Triple(payment.paymentDate, "PAYMENT", payment))
            }
        }

        list.sortedByDescending { it.first }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.nav_history)) }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.tab_lend)) },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lend),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_borrow)) },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_borrow),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = if (selectedTab == 0) "No Lend history recorded yet." else "No Borrow history recorded yet.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activities) { (timestamp, type, item) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val titleText: String

                                if (type == "DEBT") {
                                    val debt = item as DebtEntity
                                    val person = personMap[debt.personId]
                                    val personName = person?.name ?: "Unknown"
                                    titleText = if (selectedTab == 0) "Lent to $personName" else "Borrowed from $personName"
                                } else {
                                    val payment = item as PaymentEntity
                                    val parentDebt = debts.find { it.id == payment.debtId }
                                    val person = parentDebt?.let { personMap[it.personId] }
                                    val personName = person?.name ?: "Unknown"
                                    titleText = "Payment with $personName"
                                }

                                Text(
                                    text = titleText,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = dateFormat.format(Date(timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            val isReceivable = selectedTab == 0
                            val color = if (isReceivable) PrimaryGreen else SecondaryOrange
                            val sign = if (isReceivable) "+" else "−"

                            val displayAmt = if (type == "DEBT") {
                                val d = item as DebtEntity
                                "$sign${d.currency} ${String.format(Locale.US, "%.2f", d.originalAmount)}"
                            } else {
                                val p = item as PaymentEntity
                                "−${p.currency} ${String.format(Locale.US, "%.2f", p.amount)}"
                            }

                            Text(
                                text = displayAmt,
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
