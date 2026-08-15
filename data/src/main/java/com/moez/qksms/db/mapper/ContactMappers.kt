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

import com.moez.qksms.db.entity.ContactEntity
import com.moez.qksms.db.entity.PhoneNumberEntity
import com.moez.qksms.db.entity.RecipientEntity
import com.moez.qksms.db.relation.ContactFull
import com.moez.qksms.db.relation.RecipientFull
import com.moez.qksms.model.Contact
import com.moez.qksms.model.PhoneNumber
import com.moez.qksms.model.Recipient
import io.realm.RealmList

/**
 * Entity/Relation ↔ domain-model mappers for [Contact], [PhoneNumber], and [Recipient].
 *
 * Key gap bridged here: the domain [PhoneNumber] does not carry a `contactLookupKey` field —
 * that context is only present in [PhoneNumberEntity]. When converting *from* Entity, the
 * parent's `lookupKey` must be passed through the `contactLookupKey` parameter so it can be
 * preserved on the return trip. The field is stored on [PhoneNumberEntity] but invisible to
 * callers holding the domain model.
 */

// ─── PhoneNumber ────────────────────────────────────────────────────────────────

fun PhoneNumberEntity.toDomain(): PhoneNumber = PhoneNumber(
    id = id,
    accountType = accountType,
    address = address,
    type = type,
    isDefault = isDefault
)

fun PhoneNumber.toEntity(contactLookupKey: String): PhoneNumberEntity = PhoneNumberEntity(
    id = id,
    contactLookupKey = contactLookupKey,
    accountType = accountType,
    address = address,
    type = type,
    isDefault = isDefault
)

// ─── Contact ────────────────────────────────────────────────────────────────────

fun ContactFull.toDomain(): Contact = Contact(
    lookupKey = contact.lookupKey,
    numbers = RealmList<PhoneNumber>().apply { addAll(numbers.map { it.toDomain() }) },
    name = contact.name,
    photoUri = contact.photoUri,
    starred = contact.starred,
    lastUpdate = contact.lastUpdate
)

fun ContactEntity.toDomain(numbers: List<PhoneNumber> = emptyList()): Contact = Contact(
    lookupKey = lookupKey,
    numbers = RealmList<PhoneNumber>().apply { addAll(numbers) },
    name = name,
    photoUri = photoUri,
    starred = starred,
    lastUpdate = lastUpdate
)

fun Contact.toEntity(): ContactEntity = ContactEntity(
    lookupKey = lookupKey,
    name = name,
    photoUri = photoUri,
    starred = starred,
    lastUpdate = lastUpdate
)

/** Extract all phone numbers as entities for bulk insert. */
fun Contact.phoneNumberEntities(): List<PhoneNumberEntity> =
    numbers.map { it.toEntity(lookupKey) }

// ─── Recipient ──────────────────────────────────────────────────────────────────

fun RecipientFull.toDomain(): Recipient = Recipient(
    id = recipient.id,
    address = recipient.address,
    contact = contact?.toDomain(),
    lastUpdate = recipient.lastUpdate
)

fun RecipientEntity.toDomain(contact: Contact? = null): Recipient = Recipient(
    id = id,
    address = address,
    contact = contact,
    lastUpdate = lastUpdate
)

fun Recipient.toEntity(): RecipientEntity = RecipientEntity(
    id = id,
    address = address,
    contactLookupKey = contact?.lookupKey,
    lastUpdate = lastUpdate
)
