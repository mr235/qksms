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
package com.moez.qksms.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * One row per entry in the Realm `ScheduledMessage.recipients: RealmList<String>`.
 *
 * [seq] preserves list order; the row is deleted with its parent (see ScheduledMessageDao).
 */
@Entity(
    tableName = "scheduled_message_recipient",
    primaryKeys = ["messageId", "seq"],
    indices = [Index("messageId")]
)
data class ScheduledMessageRecipientEntity(
    val messageId: Long,
    val seq: Int,
    val value: String
)
