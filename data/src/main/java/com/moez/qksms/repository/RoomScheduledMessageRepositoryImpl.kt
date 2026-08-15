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

import com.moez.qksms.db.dao.ScheduledMessageDao
import com.moez.qksms.db.mapper.attachmentEntities
import com.moez.qksms.db.mapper.recipientEntities
import com.moez.qksms.db.mapper.toDomain
import com.moez.qksms.db.mapper.toEntity
import com.moez.qksms.model.ScheduledMessage
import io.reactivex.Flowable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [ScheduledMessageRepository].
 *
 * The two `MutableList<String>` fields (recipients, attachments) live in ordered child tables
 * instead of being embedded in the row — see [ScheduledMessageDao.save], which replaces those
 * children atomically with the parent.
 */
@Singleton
class RoomScheduledMessageRepositoryImpl @Inject constructor(
    private val scheduledMessageDao: ScheduledMessageDao
) : ScheduledMessageRepository {

    override fun saveScheduledMessage(
        date: Long,
        subId: Int,
        recipients: List<String>,
        sendAsGroup: Boolean,
        body: String,
        attachments: List<String>
    ) {
        // Ids are derived from `max("id") + 1` rather than autoincrement, so they stay dense and
        // comparable.
        val id = scheduledMessageDao.getMaxId() + 1

        val message = ScheduledMessage(
            id = id,
            date = date,
            subId = subId,
            recipients = recipients.toMutableList(),
            sendAsGroup = sendAsGroup,
            body = body,
            attachments = attachments.toMutableList()
        )

        scheduledMessageDao.save(
            message.toEntity(),
            message.recipientEntities(),
            message.attachmentEntities()
        )
    }

    override fun getScheduledMessages(): Flowable<List<ScheduledMessage>> = scheduledMessageDao
        .getScheduledMessages()
        .map { messages -> messages.map { it.toDomain() } }

    override fun getScheduledMessage(id: Long): ScheduledMessage? =
        scheduledMessageDao.getScheduledMessage(id)?.toDomain()

    override fun deleteScheduledMessage(id: Long) = scheduledMessageDao.delete(id)
}
