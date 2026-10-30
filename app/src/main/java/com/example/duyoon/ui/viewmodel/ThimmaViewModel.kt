package com.example.duyoon.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.duyoon.data.DebtEntity
import com.example.duyoon.data.PaymentEntity
import com.example.duyoon.data.PersonEntity
import com.example.duyoon.data.ThimmaDatabase
import com.example.duyoon.data.ThimmaRepository
import com.example.duyoon.receiver.ReminderBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThimmaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ThimmaDatabase.getDatabase(application)
    private val repository = ThimmaRepository(
        database.personDao(),
        database.debtDao(),
        database.paymentDao(),
        database.reminderDao(),
        database.attachmentDao()
    )

    private val prefs = application.getSharedPreferences("thimma_prefs", Context.MODE_PRIVATE)
    private val _languageCode = MutableStateFlow(prefs.getString("lang_code", "en") ?: "en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    fun setLanguage(lang: String) {
        prefs.edit().putString("lang_code", lang).apply()
        _languageCode.value = lang
        val locale = Locale(lang)
        Locale.setDefault(locale)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
    }

    // Primary Currency Preference
    private val _primaryCurrency = MutableStateFlow(prefs.getString("primary_currency", "USD") ?: "USD")
    val primaryCurrency: StateFlow<String> = _primaryCurrency.asStateFlow()

    fun setPrimaryCurrency(currency: String) {
        prefs.edit().putString("primary_currency", currency).apply()
        _primaryCurrency.value = currency
    }

    // App Lock & Passcode
    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean("app_lock_enabled", false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", true))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
        _isAppLockEnabled.value = enabled
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setAppPin(pin: String) {
        prefs.edit().putString("app_pin_code", pin).apply()
    }

    fun verifyAppPin(pin: String): Boolean {
        val savedPin = prefs.getString("app_pin_code", "") ?: ""
        return savedPin.isNotEmpty() && savedPin == pin
    }

    fun hasAppPin(): Boolean {
        val savedPin = prefs.getString("app_pin_code", "") ?: ""
        return savedPin.length == 4
    }

    val persons: StateFlow<List<PersonEntity>> = repository.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<DebtEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPerson(name: String, phone: String?, notes: String?, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertPerson(PersonEntity(name = name, phone = phone, notes = notes))
            onComplete(id)
        }
    }

    fun updatePerson(person: PersonEntity) {
        viewModelScope.launch {
            repository.updatePerson(person)
        }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            repository.deletePerson(person)
        }
    }

    fun addDebt(
        personId: Long,
        direction: String,
        amount: Double,
        currency: String,
        description: String?,
        dueDate: Long?,
        createdDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val debtId = repository.insertDebt(
                DebtEntity(
                    personId = personId,
                    direction = direction,
                    originalAmount = amount,
                    currency = currency,
                    description = description,
                    createdDate = createdDate,
                    dueDate = dueDate
                )
            )

            // Schedule notification if due date is set in the future
            if (dueDate != null && dueDate > System.currentTimeMillis()) {
                val personName = persons.value.find { it.id == personId }?.name ?: "Contact"
                scheduleReminder(debtId, personName, "$currency ${String.format(Locale.US, "%.2f", amount)}", dueDate)
            }
        }
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.updateDebt(debt)
            if (debt.dueDate != null && debt.dueDate > System.currentTimeMillis()) {
                val personName = persons.value.find { it.id == debt.personId }?.name ?: "Contact"
                scheduleReminder(debt.id, personName, "${debt.currency} ${String.format(Locale.US, "%.2f", debt.originalAmount)}", debt.dueDate)
            }
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    fun recordPayment(debtId: Long, amount: Double, currency: String, notes: String?) {
        viewModelScope.launch {
            repository.recordPayment(
                PaymentEntity(
                    debtId = debtId,
                    amount = amount,
                    currency = currency,
                    notes = notes
                )
            )
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }

    // Schedule System Notification Reminder
    fun scheduleReminder(debtId: Long, personName: String, amountStr: String, triggerTimeMillis: Long) {
        val app = getApplication<Application>()
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(app, ReminderBroadcastReceiver::class.java).apply {
            putExtra("debt_id", debtId)
            putExtra("person_name", personName)
            putExtra("amount_str", amountStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            app,
            debtId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        } catch (_: Exception) {}
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pList = persons.first()
            val dList = debts.first()
            val pMap = pList.associateBy { it.id }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val csvBuilder = StringBuilder()
            csvBuilder.append("Type,Person,Phone,Amount,Currency,Description,Date\n")

            dList.forEach { debt ->
                val personName = pMap[debt.personId]?.name ?: "Unknown"
                val personPhone = pMap[debt.personId]?.phone ?: ""
                val type = if (debt.direction == "RECEIVABLE") "Lend" else "Borrow"
                val dateStr = dateFormat.format(Date(debt.createdDate))
                val desc = debt.description?.replace(",", " ") ?: ""

                csvBuilder.append("$type,$personName,$personPhone,${debt.originalAmount},${debt.currency},$desc,$dateStr\n")
            }

            val exportFile = File(context.cacheDir, "thimma_debts.csv")
            exportFile.writeText(csvBuilder.toString())

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun importCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val reader = BufferedReader(InputStreamReader(inputStream))
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

                var line = reader.readLine() // Header line
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line?.split(",") ?: continue
                    if (tokens.size >= 6) {
                        val typeStr = tokens[0].trim()
                        val personName = tokens[1].trim()
                        val personPhone = tokens[2].trim().ifEmpty { null }
                        val amount = tokens[3].trim().toDoubleOrNull() ?: 0.0
                        val currency = tokens[4].trim()
                        val desc = tokens[5].trim().ifEmpty { null }
                        val dateStr = if (tokens.size >= 7) tokens[6].trim() else ""

                        if (personName.isNotBlank() && amount > 0.0) {
                            val person = repository.allPersons.first().find { it.name.equals(personName, ignoreCase = true) }
                            val personId = if (person != null) {
                                person.id
                            } else {
                                repository.insertPerson(PersonEntity(name = personName, phone = personPhone))
                            }

                            val direction = if (typeStr.contains("Lend", ignoreCase = true) || typeStr.contains("RECEIVABLE", ignoreCase = true)) "RECEIVABLE" else "PAYABLE"
                            val timestamp = try { dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }

                            repository.insertDebt(
                                DebtEntity(
                                    personId = personId,
                                    direction = direction,
                                    originalAmount = amount,
                                    currency = currency,
                                    description = desc,
                                    createdDate = timestamp
                                )
                            )
                        }
                    }
                }
                reader.close()
            } catch (_: Exception) {}
        }
    }
}
