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
import android.provider.Telephony.Sms

/**
 * Room storage mirror of [com.moez.qksms.model.Message].
 *
 * The MMS `parts` relation is not stored here — it is resolved through [MmsPartEntity.messageId].
 */
@Entity(
    tableName = "message",
    indices = [Index("threadId")]
)
data class MessageEntity(
    @PrimaryKey val id: Long = 0,
    val threadId: Long = 0,
    val contentId: Long = 0,
    val address: String = "",
    val boxId: Int = 0,
    val type: String = "",
    val date: Long = 0,
    val dateSent: Long = 0,
    val seen: Boolean = false,
    val read: Boolean = false,
    val locked: Boolean = false,
    val subId: Int = -1,

    // SMS only
    val body: String = "",
    val errorCode: Int = 0,
    val deliveryStatus: Int = Sms.STATUS_NONE,

    // MMS only
    val attachmentTypeString: String = "NOT_LOADED",
    val mmsDeliveryStatusString: String = "",
    val readReportString: String = "",
    val errorType: Int = 0,
    val messageSize: Int = 0,
    val messageType: Int = 0,
    val mmsStatus: Int = 0,
    val subject: String = "",
    val textContentType: String = ""
)
