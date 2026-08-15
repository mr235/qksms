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
package com.moez.qksms.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.moez.qksms.db.entity.ScheduledMessageAttachmentEntity
import com.moez.qksms.db.entity.ScheduledMessageEntity
import com.moez.qksms.db.entity.ScheduledMessageRecipientEntity

/**
 * Reassembles the two `RealmList<String>` fields of
 * [com.moez.qksms.model.ScheduledMessage] from their ordered child tables.
 *
 * Rows come back unordered from @Relation, so callers must sort by `seq` — see
 * ScheduledMessageDao's mapping helpers.
 */
data class ScheduledMessageFull(
    @Embedded val message: ScheduledMessageEntity,

    @Relation(parentColumn = "id", entityColumn = "messageId")
    val recipients: List<ScheduledMessageRecipientEntity>,

    @Relation(parentColumn = "id", entityColumn = "messageId")
    val attachments: List<ScheduledMessageAttachmentEntity>
) {

    /** Recipient addresses in their original list order. */
    fun orderedRecipients(): List<String> = recipients.sortedBy { it.seq }.map { it.value }

    /** Attachment URIs in their original list order. */
    fun orderedAttachments(): List<String> = attachments.sortedBy { it.seq }.map { it.value }
}
