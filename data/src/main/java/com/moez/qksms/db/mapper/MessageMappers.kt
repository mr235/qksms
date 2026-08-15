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

import com.moez.qksms.db.entity.MessageEntity
import com.moez.qksms.db.entity.MmsPartEntity
import com.moez.qksms.db.relation.MessageWithParts
import com.moez.qksms.model.Message
import com.moez.qksms.model.MmsPart
import io.realm.RealmList

/**
 * Entity/Relation ↔ domain-model mappers for [Message] and [MmsPart].
 *
 * The produced domain objects are *unmanaged* Realm instances — while the Realm implementations
 * still own the app they can be copied into Realm via `realm.copyToRealmOrUpdate`; the Room
 * implementations pass them straight through to the presentation layer. In Phase 5 the domain
 * models become plain data classes and the `RealmList` construction here goes away, but the
 * mapper API stays the same.
 *
 * Note that [MmsPart.messages] is a `@LinkingObjects` reverse link and is intentionally left
 * `null` here — the only two callers that dereference it (`MessageRepositoryImpl` and
 * `GalleryActivity`) are rewritten to look the message up explicitly in the Room path.
 */

/** Row-level: pure part entity → unmanaged [MmsPart]. */
fun MmsPartEntity.toDomain(): MmsPart = MmsPart().apply {
    id = this@toDomain.id
    messageId = this@toDomain.messageId
    type = this@toDomain.type
    seq = this@toDomain.seq
    name = this@toDomain.name
    text = this@toDomain.text
}

/** Unmanaged domain [MmsPart] → entity row. */
fun MmsPart.toEntity(): MmsPartEntity = MmsPartEntity(
    id = id,
    messageId = messageId,
    type = type,
    seq = seq,
    name = name,
    text = text
)

/** Pure message entity → unmanaged [Message] with an empty parts list. */
fun MessageEntity.toDomain(parts: List<MmsPart> = emptyList()): Message = Message().apply {
    id = this@toDomain.id
    threadId = this@toDomain.threadId
    contentId = this@toDomain.contentId
    address = this@toDomain.address
    boxId = this@toDomain.boxId
    type = this@toDomain.type
    date = this@toDomain.date
    dateSent = this@toDomain.dateSent
    seen = this@toDomain.seen
    read = this@toDomain.read
    locked = this@toDomain.locked
    subId = this@toDomain.subId
    body = this@toDomain.body
    errorCode = this@toDomain.errorCode
    deliveryStatus = this@toDomain.deliveryStatus
    attachmentTypeString = this@toDomain.attachmentTypeString
    mmsDeliveryStatusString = this@toDomain.mmsDeliveryStatusString
    readReportString = this@toDomain.readReportString
    errorType = this@toDomain.errorType
    messageSize = this@toDomain.messageSize
    messageType = this@toDomain.messageType
    mmsStatus = this@toDomain.mmsStatus
    subject = this@toDomain.subject
    textContentType = this@toDomain.textContentType
    // Realm-backed field: wrap the incoming list in a fresh RealmList so this instance can still
    // be handed to `realm.copyToRealmOrUpdate` under the legacy Realm path.
    this.parts = RealmList<MmsPart>().apply { addAll(parts) }
}

/** [MessageWithParts] relation → unmanaged [Message] with its parts attached. */
fun MessageWithParts.toDomain(): Message =
    message.toDomain(parts.sortedBy { it.seq }.map { it.toDomain() })

/** Unmanaged domain [Message] → entity row (parts are extracted separately, see [partEntities]). */
fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    threadId = threadId,
    contentId = contentId,
    address = address,
    boxId = boxId,
    type = type,
    date = date,
    dateSent = dateSent,
    seen = seen,
    read = read,
    locked = locked,
    subId = subId,
    body = body,
    errorCode = errorCode,
    deliveryStatus = deliveryStatus,
    attachmentTypeString = attachmentTypeString,
    mmsDeliveryStatusString = mmsDeliveryStatusString,
    readReportString = readReportString,
    errorType = errorType,
    messageSize = messageSize,
    messageType = messageType,
    mmsStatus = mmsStatus,
    subject = subject,
    textContentType = textContentType
)

/**
 * Extract the MMS part rows for a domain [Message], stamping each part's `messageId` with the
 * parent id. `MmsPart.messageId` is Realm's `contentId`-backed field so it may or may not match
 * the parent's primary key already — we force the linkage here.
 */
fun Message.partEntities(): List<MmsPartEntity> = parts.map { part ->
    MmsPartEntity(
        id = part.id,
        messageId = id,
        type = part.type,
        seq = part.seq,
        name = part.name,
        text = part.text
    )
}
