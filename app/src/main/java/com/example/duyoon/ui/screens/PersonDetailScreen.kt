package com.example.duyoon.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
import androidx.core.content.FileProvider
import com.example.duyoon.R
import com.example.duyoon.data.DebtEntity
import com.example.duyoon.data.PaymentEntity
import com.example.duyoon.data.PersonEntity
import com.example.duyoon.ui.theme.PrimaryGreen
import com.example.duyoon.ui.theme.SecondaryOrange
import com.example.duyoon.ui.viewmodel.ThimmaViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    viewModel: ThimmaViewModel,
    onBack: () -> Unit,
    onNavigateToAddDebtForPerson: (Long) -> Unit,
    onNavigateToEditDebt: (Long) -> Unit
) {
    val context = LocalContext.current
    val persons by viewModel.persons.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val payments by viewModel.payments.collectAsState()

    val person = persons.find { it.id == personId }
    val personDebts = debts.filter { it.personId == personId }

    var selectedDebtForPaymentId by remember { mutableStateOf<Long?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    var debtToDelete by remember { mutableStateOf<DebtEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    // Edit contact dialog state
    var showEditContactDialog by remember { mutableStateOf(false) }
    var editName by remember(person) { mutableStateOf(person?.name ?: "") }
    var editPhone by remember(person) { mutableStateOf(person?.phone ?: "") }
    var editPhotoUri by remember(person) { mutableStateOf(person?.photoUri) }

    LaunchedEffect(person) {
        if (person != null) {
            editName = person.name
            editPhone = person.phone ?: ""
            editPhotoUri = person.photoUri
        }
    }

    // Share options dialog state
    var showShareDialog by remember { mutableStateOf(false) }
    var showWhatsAppAppChooserDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            editPhotoUri = uri.toString()
        }
    }

    // Link Contact Launcher
    val linkContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null && person != null) {
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

                val finalName = name?.trim()?.ifEmpty { person.name } ?: person.name
                val finalPhone = phone?.trim() ?: person.phone

                editName = finalName
                editPhone = finalPhone ?: ""

                val updated = person.copy(
                    name = finalName,
                    phone = finalPhone,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.updatePerson(updated)
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
            linkContactLauncher.launch(null)
        }
    }

    val launchLinkContact = {
        val permissionState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            linkContactLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Helper to build statement text
    val buildStatementText: () -> String = {
        val statementBuilder = StringBuilder()
        statementBuilder.append("📊 Account Statement — ${person?.name ?: ""}\n")
        statementBuilder.append("Thimma (ذمّة) Debt Tracker\n\n")

        val lendDebts = personDebts.filter { it.direction == "RECEIVABLE" }
        val borrowDebts = personDebts.filter { it.direction == "PAYABLE" }

        val lendTotalsByCurrency = mutableMapOf<String, Double>()
        val borrowTotalsByCurrency = mutableMapOf<String, Double>()

        if (lendDebts.isNotEmpty()) {
            statementBuilder.append("🟢 Lend List (إقراض):\n")
            lendDebts.forEach { debt ->
                val debtPayments = payments.filter { it.debtId == debt.id }
                val remaining = debt.originalAmount - debtPayments.sumOf { it.amount }
                if (remaining > 0.0) {
                    lendTotalsByCurrency[debt.currency] = (lendTotalsByCurrency[debt.currency] ?: 0.0) + remaining
                }
                val desc = debt.description?.ifEmpty { null } ?: "General"
                statementBuilder.append("· ${debt.currency} ${String.format(Locale.US, "%.2f", debt.originalAmount)} ($desc)\n")
            }
            if (lendTotalsByCurrency.isNotEmpty()) {
                val totalLendStr = lendTotalsByCurrency.entries.joinToString(", ") { "${it.key} ${String.format(Locale.US, "%.2f", it.value)}" }
                statementBuilder.append("👉 Total Remaining Lend (إقراض): $totalLendStr\n\n")
            } else {
                statementBuilder.append("👉 Total Remaining Lend (إقراض): Fully Settled\n\n")
            }
        }

        if (borrowDebts.isNotEmpty()) {
            statementBuilder.append("🔴 Borrow List (استقراض):\n")
            borrowDebts.forEach { debt ->
                val debtPayments = payments.filter { it.debtId == debt.id }
                val remaining = debt.originalAmount - debtPayments.sumOf { it.amount }
                if (remaining > 0.0) {
                    borrowTotalsByCurrency[debt.currency] = (borrowTotalsByCurrency[debt.currency] ?: 0.0) + remaining
                }
                val desc = debt.description?.ifEmpty { null } ?: "General"
                statementBuilder.append("· ${debt.currency} ${String.format(Locale.US, "%.2f", debt.originalAmount)} ($desc)\n")
            }
            if (borrowTotalsByCurrency.isNotEmpty()) {
                val totalBorrowStr = borrowTotalsByCurrency.entries.joinToString(", ") { "${it.key} ${String.format(Locale.US, "%.2f", it.value)}" }
                statementBuilder.append("👉 Total Remaining Borrow (استقراض): $totalBorrowStr\n\n")
            } else {
                statementBuilder.append("👉 Total Remaining Borrow (استقراض): Fully Settled\n\n")
            }
        }

        if (lendDebts.isEmpty() && borrowDebts.isEmpty()) {
            statementBuilder.append("Fully Settled (0.00)\n\n")
        }

        statementBuilder.append("Generated via Thimma (ذمّة)")
        statementBuilder.toString()
    }

    // PDF Statement Generator
    val generatePdfStatement: (PersonEntity, List<DebtEntity>) -> File = { p, pDebts ->
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint()
        val titlePaint = android.graphics.Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val subtitlePaint = android.graphics.Paint().apply {
            textSize = 12f
            color = android.graphics.Color.DKGRAY
        }
        val textPaint = android.graphics.Paint().apply {
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val boldTextPaint = android.graphics.Paint().apply {
            textSize = 11f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }

        var y = 40f
        canvas.drawText("Thimma (ذمّة) — Account Statement", 40f, y, titlePaint)
        y += 24f
        canvas.drawText("Contact: ${p.name} ${p.phone ?: ""}", 40f, y, subtitlePaint)
        y += 16f
        canvas.drawText("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}", 40f, y, subtitlePaint)
        y += 24f

        paint.strokeWidth = 1f
        paint.color = android.graphics.Color.LTGRAY
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        canvas.drawText("Date", 40f, y, boldTextPaint)
        canvas.drawText("Type", 140f, y, boldTextPaint)
        canvas.drawText("Amount", 240f, y, boldTextPaint)
        canvas.drawText("Description", 360f, y, boldTextPaint)
        y += 18f

        pDebts.forEach { debt ->
            val typeStr = if (debt.direction == "RECEIVABLE") "Lend" else "Borrow"
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(debt.createdDate))
            val amtStr = "${debt.currency} ${String.format(Locale.US, "%.2f", debt.originalAmount)}"
            val descStr = debt.description ?: "General"

            canvas.drawText(dateStr, 40f, y, textPaint)
            canvas.drawText(typeStr, 140f, y, textPaint)
            canvas.drawText(amtStr, 240f, y, textPaint)
            canvas.drawText(if (descStr.length > 25) descStr.substring(0, 25) + "..." else descStr, 360f, y, textPaint)
            y += 20f
        }

        pdfDocument.finishPage(page)

        val pdfFile = File(context.cacheDir, "statement_${p.name.replace(" ", "_")}.pdf")
        pdfFile.outputStream().use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        pdfFile
    }

    // Detect installed WhatsApp packages
    val isPackageInstalled: (String) -> Boolean = { pkgName ->
        try {
            context.packageManager.getPackageInfo(pkgName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = person?.name ?: "Person Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (person != null) {
                        IconButton(onClick = { showEditContactDialog = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Contact")
                        }
                        IconButton(onClick = { showShareDialog = true }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share Statement")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onNavigateToAddDebtForPerson(personId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = stringResource(R.string.add_new_debt))
                    }
                }
            }
        }
    ) { padding ->
        if (person == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = "Person not found")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!person.phone.isNullOrEmpty()) {
                            Text(
                                text = person.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            OutlinedButton(onClick = { launchLinkContact() }) {
                                Text(text = "Link to Phone Contact", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Debts List Header
            item {
                Text(
                    text = "Debts & History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (personDebts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No debts recorded with this person yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            } else {
                items(personDebts) { debt ->
                    val debtPayments = payments.filter { it.debtId == debt.id }
                    val totalPaid = debtPayments.sumOf { it.amount }
                    val remaining = debt.originalAmount - totalPaid
                    val isReceivable = debt.direction == "RECEIVABLE"
                    val color = if (isReceivable) PrimaryGreen else SecondaryOrange

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = debt.description?.ifEmpty { null } ?: if (isReceivable) "Lend" else "Borrow",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${debt.currency} ${String.format(Locale.US, "%.2f", debt.originalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(onClick = { onNavigateToEditDebt(debt.id) }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Debt", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { debtToDelete = debt }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Debt", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Remaining: ${debt.currency} ${String.format(Locale.US, "%.2f", remaining)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = dateFormat.format(Date(debt.createdDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            if (debtPayments.isNotEmpty()) {
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                                Text(text = "Payments:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                debtPayments.forEach { payment ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "· Paid ${payment.currency} ${String.format(Locale.US, "%.2f", payment.amount)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = dateFormat.format(Date(payment.paymentDate)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            IconButton(
                                                onClick = { paymentToDelete = payment },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Payment",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (remaining > 0.0) {
                                OutlinedButton(
                                    onClick = {
                                        selectedDebtForPaymentId = debt.id
                                        showPaymentDialog = true
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(text = stringResource(R.string.record_payment))
                                }
                            } else {
                                Text(
                                    text = "Fully Settled",
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Contact Dialog
    if (showEditContactDialog) {
        AlertDialog(
            onDismissRequest = { showEditContactDialog = false },
            title = { Text(text = "Edit Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { launchLinkContact() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Link to Phone Contact")
                    }
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (editPhotoUri != null) "Change Picture" else "Add Picture")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            val updatedPerson = person!!.copy(
                                name = editName.trim(),
                                phone = editPhone.trim().ifEmpty { null },
                                photoUri = editPhotoUri,
                                updatedAt = System.currentTimeMillis()
                            )
                            viewModel.updatePerson(updatedPerson)
                            showEditContactDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditContactDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Share Options Dialog (WhatsApp, SMS, General Share, PDF)
    if (showShareDialog) {
        val statementText = buildStatementText()
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text(text = "Share Statement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Choose how to share statement with ${person?.name}:")
                    if (!person?.phone.isNullOrEmpty()) {
                        Button(
                            onClick = {
                                showShareDialog = false
                                val hasStd = isPackageInstalled("com.whatsapp")
                                val hasBiz = isPackageInstalled("com.whatsapp.w4b")

                                if (hasStd && hasBiz) {
                                    showWhatsAppAppChooserDialog = true
                                } else {
                                    try {
                                        val targetPkg = if (hasBiz) "com.whatsapp.w4b" else "com.whatsapp"
                                        val url = "https://api.whatsapp.com/send?phone=${person?.phone}&text=${Uri.encode(statementText)}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            setPackage(targetPkg)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, statementText)
                                            type = "text/plain"
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Statement").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Send via WhatsApp")
                        }
                        Button(
                            onClick = {
                                showShareDialog = false
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${person?.phone}")).apply {
                                    putExtra("sms_body", statementText)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(text = "Send via SMS")
                        }
                    }

                    // Share PDF Statement Button
                    Button(
                        onClick = {
                            showShareDialog = false
                            if (person != null) {
                                try {
                                    val pdfFile = generatePdfStatement(person, personDebts)
                                    val contentUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pdfFile
                                    )
                                    val pdfIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(Intent.createChooser(pdfIntent, "Share PDF Statement").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(text = "Share PDF Statement 📄")
                    }

                    OutlinedButton(
                        onClick = {
                            showShareDialog = false
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, statementText)
                                type = "text/plain"
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Statement").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Other Apps (General Share)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Multi-WhatsApp App Chooser Dialog
    if (showWhatsAppAppChooserDialog) {
        val statementText = buildStatementText()
        AlertDialog(
            onDismissRequest = { showWhatsAppAppChooserDialog = false },
            title = { Text(text = "Select WhatsApp App") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showWhatsAppAppChooserDialog = false
                            try {
                                val url = "https://api.whatsapp.com/send?phone=${person?.phone}&text=${Uri.encode(statementText)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    setPackage("com.whatsapp")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "WhatsApp Messenger")
                    }
                    Button(
                        onClick = {
                            showWhatsAppAppChooserDialog = false
                            try {
                                val url = "https://api.whatsapp.com/send?phone=${person?.phone}&text=${Uri.encode(statementText)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    setPackage("com.whatsapp.w4b")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(text = "WhatsApp Business")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWhatsAppAppChooserDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Payment Dialog
    if (showPaymentDialog && selectedDebtForPaymentId != null) {
        val targetDebt = debts.find { it.id == selectedDebtForPaymentId }
        if (targetDebt != null) {
            val debtPayments = payments.filter { it.debtId == targetDebt.id }
            val remaining = targetDebt.originalAmount - debtPayments.sumOf { it.amount }

            var paymentAmountText by remember { mutableStateOf(remaining.toString()) }
            var paymentNotes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                title = { Text(text = stringResource(R.string.record_payment)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Remaining: ${targetDebt.currency} ${String.format(Locale.US, "%.2f", remaining)}")
                        OutlinedTextField(
                            value = paymentAmountText,
                            onValueChange = { paymentAmountText = it },
                            label = { Text(stringResource(R.string.amount)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = paymentNotes,
                            onValueChange = { paymentNotes = it },
                            label = { Text(stringResource(R.string.notes)) },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = paymentAmountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                viewModel.recordPayment(targetDebt.id, amt, targetDebt.currency, paymentNotes.trim().ifEmpty { null })
                                showPaymentDialog = false
                                selectedDebtForPaymentId = null
                            }
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }

    // Debt Delete Confirmation Dialog
    if (debtToDelete != null) {
        val hasPayments = payments.any { it.debtId == debtToDelete!!.id }
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text(text = "Delete Debt") },
            text = {
                val warningText = if (hasPayments) {
                    "This debt has recorded payments attached. Deleting it will also remove all associated payment history. Are you sure you want to delete this debt?"
                } else {
                    "Are you sure you want to delete this debt?"
                }
                Text(text = warningText)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt(debtToDelete!!)
                        debtToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Payment Delete Confirmation Dialog
    if (paymentToDelete != null) {
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text(text = "Delete Payment") },
            text = { Text(text = "Are you sure you want to delete this payment record?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(paymentToDelete!!)
                        paymentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
