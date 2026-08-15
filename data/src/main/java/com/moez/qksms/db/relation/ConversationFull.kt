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
import androidx.room.Junction
import androidx.room.Relation
import com.moez.qksms.db.entity.ContactEntity
import com.moez.qksms.db.entity.ConversationEntity
import com.moez.qksms.db.entity.ConversationRecipientCrossRef
import com.moez.qksms.db.entity.MessageEntity
import com.moez.qksms.db.entity.PhoneNumberEntity
import com.moez.qksms.db.entity.RecipientEntity

/**
 * Full assembly of a [ConversationEntity] with its recipients (each with their contact and
 * phone numbers) and its last message (with its MMS parts).
 *
 * One round trip yields everything the presentation layer needs to render a conversation row.
 */
data class ConversationFull(
    @Embedded val conversation: ConversationEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ConversationRecipientCrossRef::class,
            parentColumn = "conversationId",
            entityColumn = "recipientId"
        ),
        entity = RecipientEntity::class
    )
    val recipients: List<RecipientFull>,

    @Relation(
        parentColumn = "lastMessageId",
        entityColumn = "id",
        entity = MessageEntity::class
    )
    val lastMessage: MessageWithParts?,

    /**
     * The junction rows themselves, loaded only so that [orderedRecipients] can restore the
     * stored order — @Relation cannot project the junction's `seq` column onto
     * [recipients], and conversation titles are built by joining recipient names in order.
     */
    @Relation(parentColumn = "id", entityColumn = "conversationId")
    val recipientRefs: List<ConversationRecipientCrossRef> = emptyList()
) {

    /** Recipients in their stored order. */
    fun orderedRecipients(): List<RecipientFull> {
        if (recipientRefs.isEmpty()) return recipients

        val seqById = recipientRefs.associate { ref -> ref.recipientId to ref.seq }
        return recipients.sortedBy { recipient -> seqById[recipient.recipient.id] ?: Int.MAX_VALUE }
    }
}

/** Contact joined with its phone numbers via [PhoneNumberEntity.contactLookupKey]. */
data class ContactFull(
    @Embedded val contact: ContactEntity,
    @Relation(parentColumn = "lookupKey", entityColumn = "contactLookupKey")
    val numbers: List<PhoneNumberEntity>
)
