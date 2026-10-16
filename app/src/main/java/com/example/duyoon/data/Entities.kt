package com.example.duyoon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val phone: String? = null,
    val photoUri: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val personId: Long,
    val direction: String, // "RECEIVABLE" (They owe me) or "PAYABLE" (I owe them)
    val originalAmount: Double,
    val currency: String, // USD, LBP, EUR, SAR, AED, etc.
    val description: String? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val status: String = "UNPAID", // UNPAID, PARTIALLY_PAID, PAID
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val debtId: Long,
    val amount: Double,
    val currency: String,
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val debtId: Long,
    val reminderDate: Long,
    val enabled: Boolean = true,
    val lastTriggeredAt: Long? = null
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val debtId: Long,
    val localFilePath: String,
    val fileType: String,
    val createdAt: Long = System.currentTimeMillis()
)
