package com.example.duyoon.data

import kotlinx.coroutines.flow.Flow

class ThimmaRepository(
    private val personDao: PersonDao,
    private val debtDao: DebtDao,
    private val paymentDao: PaymentDao,
    private val reminderDao: ReminderDao,
    private val attachmentDao: AttachmentDao
) {
    val allPersons: Flow<List<PersonEntity>> = personDao.getAllPersons()
    val allDebts: Flow<List<DebtEntity>> = debtDao.getAllDebts()
    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    suspend fun insertPerson(person: PersonEntity): Long = personDao.insertPerson(person)
    suspend fun updatePerson(person: PersonEntity) = personDao.updatePerson(person)
    suspend fun deletePerson(person: PersonEntity) = personDao.deletePerson(person)
    suspend fun getPersonById(id: Long): PersonEntity? = personDao.getPersonById(id)

    suspend fun insertDebt(debt: DebtEntity): Long = debtDao.insertDebt(debt)
    suspend fun updateDebt(debt: DebtEntity) {
        debtDao.updateDebt(debt)
        updateDebtStatus(debt.id)
    }
    suspend fun deleteDebt(debt: DebtEntity) {
        val payments = paymentDao.getPaymentsForDebtSync(debt.id)
        payments.forEach { paymentDao.deletePayment(it) }
        debtDao.deleteDebt(debt)
    }

    suspend fun getDebtById(id: Long): DebtEntity? = debtDao.getDebtById(id)
    fun getDebtsForPerson(personId: Long): Flow<List<DebtEntity>> = debtDao.getDebtsForPerson(personId)

    fun getPaymentsForDebt(debtId: Long): Flow<List<PaymentEntity>> = paymentDao.getPaymentsForDebt(debtId)
    
    suspend fun recordPayment(payment: PaymentEntity): Long {
        val paymentId = paymentDao.insertPayment(payment)
        updateDebtStatus(payment.debtId)
        return paymentId
    }

    suspend fun updatePayment(payment: PaymentEntity) {
        paymentDao.updatePayment(payment)
        updateDebtStatus(payment.debtId)
    }

    suspend fun deletePayment(payment: PaymentEntity) {
        paymentDao.deletePayment(payment)
        updateDebtStatus(payment.debtId)
    }

    private suspend fun updateDebtStatus(debtId: Long) {
        val debt = debtDao.getDebtById(debtId) ?: return
        val payments = paymentDao.getPaymentsForDebtSync(debtId)
        val totalPaid = payments.sumOf { it.amount }
        val remaining = debt.originalAmount - totalPaid

        val newStatus = when {
            remaining <= 0.0 -> "PAID"
            totalPaid > 0.0 -> "PARTIALLY_PAID"
            else -> "UNPAID"
        }

        val updatedDebt = debt.copy(status = newStatus, updatedAt = System.currentTimeMillis())
        debtDao.updateDebt(updatedDebt)
    }
}
