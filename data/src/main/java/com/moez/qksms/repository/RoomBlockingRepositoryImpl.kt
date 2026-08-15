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
import com.moez.qksms.model.BlockedMessageNotification
import com.moez.qksms.model.BlockedNumber
import com.moez.qksms.util.PhoneNumberUtils
import io.reactivex.Flowable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed stub of [BlockingRepository]. Method bodies will be filled in as part of the
 * Realm → Room migration; for now they throw so the module compiles against the new DAOs.
 */
@Singleton
class RoomBlockingRepositoryImpl @Inject constructor(
    private val blockingDao: BlockingDao,
    private val phoneNumberUtils: PhoneNumberUtils
) : BlockingRepository {

    override fun blockNumber(vararg addresses: String) = TODO("Room path")

    override fun getBlockedNumbers(): Flowable<List<BlockedNumber>> = TODO("Room path")

    override fun getBlockedMessagesNotification(): Flowable<List<BlockedMessageNotification>> = TODO("Room path")

    override fun getBlockedNotificationContents(): List<String> = TODO("Room path")

    override fun blockMessageNotification(vararg contents: String) = TODO("Room path")

    override fun unblockMessageNotification(id: Long) = TODO("Room path")

    override fun getBlockedNumber(id: Long): BlockedNumber? = TODO("Room path")

    override fun isBlocked(address: String): Boolean = TODO("Room path")

    override fun unblockNumber(id: Long) = TODO("Room path")

    override fun unblockNumbers(vararg addresses: String) = TODO("Room path")
}
