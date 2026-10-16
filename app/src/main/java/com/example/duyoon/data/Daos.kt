package com.example.duyoon.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY name COLLATE NOCASE ASC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :personId")
    suspend fun getPersonById(personId: Long): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY createdDate DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE personId = :personId ORDER BY createdDate DESC")
    fun getDebtsForPerson(personId: Long): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :debtId")
    suspend fun getDebtById(debtId: Long): DebtEntity?

    @Query("SELECT * FROM debts WHERE id = :debtId")
    fun getDebtByIdFlow(debtId: Long): Flow<DebtEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE debtId = :debtId ORDER BY paymentDate DESC")
    fun getPaymentsForDebt(debtId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE debtId = :debtId")
    suspend fun getPaymentsForDebtSync(debtId: Long): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE debtId = :debtId")
    fun getRemindersForDebt(debtId: Long): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE debtId = :debtId")
    fun getAttachmentsForDebt(debtId: Long): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)
}
