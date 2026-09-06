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
package com.moez.qksms.model

open class Conversation(
    var id: Long = 0,
    var archived: Boolean = false,
    var blocked: Boolean = false,
    var pinned: Boolean = false,
    var recipients: MutableList<Recipient> = mutableListOf(),
    var lastMessage: Message? = null,
    var draft: String = "",

    var blockingClient: Int? = null,
    var blockReason: String? = null,

    var name: String = "" // For group chats, the user is allowed to set a custom title for the conversation
) {

    val date: Long get() = lastMessage?.date ?: 0
    val snippet: String? get() = lastMessage?.getSummary()
    val unread: Boolean get() = lastMessage?.read == false
    val me: Boolean get() = lastMessage?.isMe() == true

    fun getTitle(): String {
        return name.takeIf { it.isNotBlank() } ?: recipients.joinToString { recipient -> recipient.getDisplayName() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Conversation) return false

        if (id != other.id) return false
        if (archived != other.archived) return false
        if (blocked != other.blocked) return false
        if (pinned != other.pinned) return false
        if (recipients != other.recipients) return false
        if (lastMessage != other.lastMessage) return false
        if (draft != other.draft) return false
        if (blockingClient != other.blockingClient) return false
        if (blockReason != other.blockReason) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + archived.hashCode()
        result = 31 * result + blocked.hashCode()
        result = 31 * result + pinned.hashCode()
        result = 31 * result + recipients.hashCode()
        result = 31 * result + (lastMessage?.hashCode() ?: 0)
        result = 31 * result + draft.hashCode()
        result = 31 * result + (blockingClient?.hashCode() ?: 0)
        result = 31 * result + (blockReason?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        return result
    }

}

