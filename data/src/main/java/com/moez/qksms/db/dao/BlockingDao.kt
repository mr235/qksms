/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.qksms.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moez.qksms.db.entity.BlockedMessageNotificationEntity
import com.moez.qksms.db.entity.BlockedNumberEntity
import io.reactivex.Flowable

/**
 * Room translation of BlockingRepositoryImpl.
 *
 * `isBlocked(address)` and `unblockNumbers(addresses)` are deliberately absent: they compare
 * numbers with `PhoneNumberUtils.compare`, which has no SQL equivalent, so the repository keeps
 * reading the full list and filtering in memory (unchanged from the Realm implementation).
 */
@Dao
interface BlockingDao {

    @Query("SELECT * FROM blocked_number")
    fun getBlockedNumbers(): Flowable<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_number")
    fun getBlockedNumbersSnapshot(): List<BlockedNumberEntity>

    @Query("SELECT * FROM blocked_number WHERE id = :id")
    fun getBlockedNumber(id: Long): BlockedNumberEntity?

    @Query("SELECT COALESCE(MAX(id), 0) FROM blocked_number")
    fun getMaxBlockedNumberId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlockedNumbers(numbers: List<BlockedNumberEntity>)

    @Query("DELETE FROM blocked_number WHERE id IN (:ids)")
    fun deleteBlockedNumbers(ids: List<Long>)

    @Query("SELECT * FROM blocked_message_notification")
    fun getBlockedMessageNotifications(): Flowable<List<BlockedMessageNotificationEntity>>

    @Query("SELECT * FROM blocked_message_notification")
    fun getBlockedMessageNotificationsSnapshot(): List<BlockedMessageNotificationEntity>

    @Query("SELECT COALESCE(MAX(id), 0) FROM blocked_message_notification")
    fun getMaxBlockedMessageNotificationId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlockedMessageNotifications(notifications: List<BlockedMessageNotificationEntity>)

    @Query("DELETE FROM blocked_message_notification WHERE id IN (:ids)")
    fun deleteBlockedMessageNotifications(ids: List<Long>)

    @Query("DELETE FROM blocked_number")
    fun deleteAllBlockedNumbers()

    @Query("DELETE FROM blocked_message_notification")
    fun deleteAllBlockedMessageNotifications()
}
