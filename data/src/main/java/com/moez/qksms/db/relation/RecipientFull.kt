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
import com.moez.qksms.db.entity.ContactEntity
import com.moez.qksms.db.entity.PhoneNumberEntity
import com.moez.qksms.db.entity.RecipientEntity

/**
 * Reassembles the Realm `Recipient.contact: Contact?` object reference, joined on
 * [RecipientEntity.contactLookupKey] == [ContactEntity.lookupKey].
 *
 * The contact carries its own phone numbers so `Contact.getDefaultNumber()` keeps working.
 */
data class RecipientFull(
    @Embedded val recipient: RecipientEntity,
    @Relation(
        parentColumn = "contactLookupKey",
        entityColumn = "lookupKey",
        entity = ContactEntity::class
    )
    val contact: ContactFull?
)
