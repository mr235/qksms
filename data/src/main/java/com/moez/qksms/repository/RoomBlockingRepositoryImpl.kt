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
package com.moez.qksms.repository

import com.moez.qksms.db.dao.BlockingDao
import com.moez.qksms.db.entity.BlockedMessageNotificationEntity
import com.moez.qksms.db.entity.BlockedNumberEntity
import com.moez.qksms.db.mapper.toDomain
import com.moez.qksms.model.BlockedMessageNotification
import com.moez.qksms.model.BlockedNumber
import com.moez.qksms.util.PhoneNumberUtils
import io.reactivex.Flowable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [BlockingRepository].
 *
 * `isBlocked` / `unblockNumbers` read the full number list and filter in memory with
 * [PhoneNumberUtils.compare] — there is no SQL equivalent for that fuzzy match (see [BlockingDao]).
 */
@Singleton
class RoomBlockingRepositoryImpl @Inject constructor(
    private val blockingDao: BlockingDao,
    private val phoneNumberUtils: PhoneNumberUtils
) : BlockingRepository {

    override fun blockNumber(vararg addresses: String) {
        val existing = blockingDao.getBlockedNumbersSnapshot()
        val newAddresses = addresses.filter { address ->
            existing.none { number -> phoneNumberUtils.compare(number.address, address) }
        }

        val maxId = blockingDao.getMaxBlockedNumberId()
        blockingDao.insertBlockedNumbers(newAddresses.mapIndexed { index, address ->
            BlockedNumberEntity(id = maxId + 1 + index, address = address)
        })
    }

    override fun getBlockedNumbers(): Flowable<List<BlockedNumber>> = blockingDao
        .getBlockedNumbers()
        .map { numbers -> numbers.map { it.toDomain() } }

    override fun getBlockedMessagesNotification(): Flowable<List<BlockedMessageNotification>> = blockingDao
        .getBlockedMessageNotifications()
        .map { notifications -> notifications.map { it.toDomain() } }

    override fun getBlockedNotificationContents(): List<String> = blockingDao
        .getBlockedMessageNotificationsSnapshot()
        .map { it.content }

    override fun blockMessageNotification(vararg contents: String) {
        val existing = blockingDao.getBlockedMessageNotificationsSnapshot()
        val newMessages = contents.filter { content ->
            existing.none { message -> message.content.equals(content, true) }
        }

        val maxId = blockingDao.getMaxBlockedMessageNotificationId()
        blockingDao.insertBlockedMessageNotifications(newMessages.mapIndexed { index, content ->
            BlockedMessageNotificationEntity(id = maxId + 1 + index, content = content)
        })
    }

    override fun unblockMessageNotification(id: Long) =
        blockingDao.deleteBlockedMessageNotifications(listOf(id))

    override fun getBlockedNumber(id: Long): BlockedNumber? =
        blockingDao.getBlockedNumber(id)?.toDomain()

    override fun isBlocked(address: String): Boolean = blockingDao
        .getBlockedNumbersSnapshot()
        .any { number -> phoneNumberUtils.compare(number.address, address) }

    override fun unblockNumber(id: Long) = blockingDao.deleteBlockedNumbers(listOf(id))

    override fun unblockNumbers(vararg addresses: String) {
        val ids = blockingDao.getBlockedNumbersSnapshot()
            .filter { number ->
                addresses.any { address -> phoneNumberUtils.compare(number.address, address) }
            }
            .map { it.id }

        blockingDao.deleteBlockedNumbers(ids)
    }
}
