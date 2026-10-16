package com.example.duyoon.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.duyoon.data.DebtEntity
import com.example.duyoon.data.PaymentEntity
import com.example.duyoon.data.PersonEntity
import com.example.duyoon.data.ThimmaDatabase
import com.example.duyoon.data.ThimmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class ActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val isReceivable: Boolean,
    val timestamp: Long
)

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

    fun addDebt(
        personId: Long,
        direction: String,
        amount: Double,
        currency: String,
        description: String?,
        dueDate: Long?
    ) {
        viewModelScope.launch {
            repository.insertDebt(
                DebtEntity(
                    personId = personId,
                    direction = direction,
                    originalAmount = amount,
                    currency = currency,
                    description = description,
                    dueDate = dueDate
                )
            )
        }
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.updateDebt(debt)
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
}
