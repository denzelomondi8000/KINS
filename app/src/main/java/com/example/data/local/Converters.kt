package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.PaymentMethod
import com.example.data.model.StaffRole
import com.example.data.model.SyncStatus

class Converters {
    @TypeConverter
    fun fromStaffRole(value: StaffRole): String = value.name

    @TypeConverter
    fun toStaffRole(value: String): StaffRole = runCatching { StaffRole.valueOf(value) }.getOrDefault(StaffRole.CASHIER)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = runCatching { SyncStatus.valueOf(value) }.getOrDefault(SyncStatus.SYNCED)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = runCatching { PaymentMethod.valueOf(value) }.getOrDefault(PaymentMethod.CASH)
}
