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
import com.moez.qksms.db.entity.MessageEntity
import com.moez.qksms.db.entity.MmsPartEntity

/**
 * Reassembles the Realm `Message.parts: RealmList<MmsPart>` relation.
 *
 * [MmsPartEntity] rows are matched on [MmsPartEntity.messageId] == [MessageEntity.id].
 */
data class MessageWithParts(
    @Embedded val message: MessageEntity,
    @Relation(parentColumn = "id", entityColumn = "messageId")
    val parts: List<MmsPartEntity>
)
