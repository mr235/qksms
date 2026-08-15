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
package com.moez.qksms.db.mapper

import com.moez.qksms.db.entity.ScheduledMessageAttachmentEntity
import com.moez.qksms.db.entity.ScheduledMessageEntity
import com.moez.qksms.db.entity.ScheduledMessageRecipientEntity
import com.moez.qksms.db.relation.ScheduledMessageFull
import com.moez.qksms.model.ScheduledMessage
import io.realm.RealmList

/**
 * Entity/Relation ↔ domain-model mappers for [ScheduledMessage].
 *
 * The two `RealmList<String>` fields are split into ordered child tables. [ScheduledMessageFull]
 * already exposes `orderedRecipients()`/`orderedAttachments()` to restore list order.
 */

fun ScheduledMessageFull.toDomain(): ScheduledMessage = ScheduledMessage(
    id = message.id,
    date = message.date,
    subId = message.subId,
    recipients = RealmList<String>().apply { addAll(orderedRecipients()) },
    sendAsGroup = message.sendAsGroup,
    body = message.body,
    attachments = RealmList<String>().apply { addAll(orderedAttachments()) }
)

fun ScheduledMessage.toEntity(): ScheduledMessageEntity = ScheduledMessageEntity(
    id = id,
    date = date,
    subId = subId,
    sendAsGroup = sendAsGroup,
    body = body
)

fun ScheduledMessage.recipientEntities(): List<ScheduledMessageRecipientEntity> =
    recipients.mapIndexed { seq, value ->
        ScheduledMessageRecipientEntity(messageId = id, seq = seq, value = value)
    }

fun ScheduledMessage.attachmentEntities(): List<ScheduledMessageAttachmentEntity> =
    attachments.mapIndexed { seq, value ->
        ScheduledMessageAttachmentEntity(messageId = id, seq = seq, value = value)
    }
