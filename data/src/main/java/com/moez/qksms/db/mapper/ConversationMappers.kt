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

import com.moez.qksms.db.entity.ConversationEntity
import com.moez.qksms.db.entity.ConversationRecipientCrossRef
import com.moez.qksms.db.relation.ConversationFull
import com.moez.qksms.model.Conversation
import com.moez.qksms.model.Recipient

/**
 * Entity/Relation ↔ domain-model mappers for [Conversation].
 *
 * [ConversationEntity.lastMessageId] is a plain nullable column; the domain model wants the
 * fully-populated `lastMessage: Message?` object, which only [ConversationFull] (the @Relation
 * result) can supply. The bare-[ConversationEntity] overload is for call sites that already have
 * the message and recipients in hand (e.g. after an insert) and don't want to issue a query.
 */

fun ConversationFull.toDomain(): Conversation = Conversation(
    id = conversation.id,
    archived = conversation.archived,
    blocked = conversation.blocked,
    pinned = conversation.pinned,
    recipients = orderedRecipients().map { it.toDomain() }.toMutableList(),
    lastMessage = lastMessage?.toDomain(),
    draft = conversation.draft,
    blockingClient = conversation.blockingClient,
    blockReason = conversation.blockReason,
    name = conversation.name
)

fun ConversationEntity.toDomain(
    recipients: List<Recipient> = emptyList(),
    lastMessage: com.moez.qksms.model.Message? = null
): Conversation = Conversation(
    id = id,
    archived = archived,
    blocked = blocked,
    pinned = pinned,
    recipients = recipients.toMutableList(),
    lastMessage = lastMessage,
    draft = draft,
    blockingClient = blockingClient,
    blockReason = blockReason,
    name = name
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    archived = archived,
    blocked = blocked,
    pinned = pinned,
    lastMessageId = lastMessage?.id,
    draft = draft,
    blockingClient = blockingClient,
    blockReason = blockReason,
    name = name
)

/** Junction rows for this conversation's recipients, preserving list order. */
fun Conversation.recipientCrossRefs(): List<ConversationRecipientCrossRef> =
    recipients.mapIndexed { seq, recipient ->
        ConversationRecipientCrossRef(conversationId = id, recipientId = recipient.id, seq = seq)
    }
