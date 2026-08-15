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
 * Room storage mirror of [com.moez.qksms.model.Recipient].
 *
 * A recipient references its contact through the nullable [contactLookupKey] column
 * (Contact's primary key is its lookupKey); the Contact is re-attached at read time via @Relation.
 */
@Entity(
    tableName = "recipient",
    indices = [Index("contactLookupKey")]
)
data class RecipientEntity(
    @PrimaryKey val id: Long = 0,
    val address: String = "",
    val contactLookupKey: String? = null,
    val lastUpdate: Long = 0
)
