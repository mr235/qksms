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
import androidx.room.PrimaryKey

/**
 * Room storage mirror of [com.moez.qksms.model.Conversation].
 *
 * The Realm `lastMessage: Message?` object reference becomes the nullable [lastMessageId] column;
 * the Message is re-attached at read time via a @Relation. The `recipients: RealmList<Recipient>`
 * becomes the [ConversationRecipientCrossRef] junction table.
 */
@Entity(
    tableName = "conversation",
    indices = [Index("archived"), Index("blocked"), Index("pinned")]
)
data class ConversationEntity(
    @PrimaryKey val id: Long = 0,
    val archived: Boolean = false,
    val blocked: Boolean = false,
    val pinned: Boolean = false,
    val lastMessageId: Long? = null,
    val draft: String = "",
    val blockingClient: Int? = null,
    val blockReason: String? = null,
    val name: String = ""
)
