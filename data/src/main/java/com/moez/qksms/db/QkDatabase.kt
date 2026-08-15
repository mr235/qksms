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
package com.moez.qksms.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.moez.qksms.db.dao.BlockingDao
import com.moez.qksms.db.dao.ContactDao
import com.moez.qksms.db.dao.ContactGroupDao
import com.moez.qksms.db.dao.ConversationDao
import com.moez.qksms.db.dao.MessageDao
import com.moez.qksms.db.dao.MmsPartDao
import com.moez.qksms.db.dao.RecipientDao
import com.moez.qksms.db.dao.ScheduledMessageDao
import com.moez.qksms.db.dao.SyncLogDao
import com.moez.qksms.db.entity.BlockedMessageNotificationEntity
import com.moez.qksms.db.entity.BlockedNumberEntity
import com.moez.qksms.db.entity.ContactEntity
import com.moez.qksms.db.entity.ContactGroupContactCrossRef
import com.moez.qksms.db.entity.ContactGroupEntity
import com.moez.qksms.db.entity.ConversationEntity
import com.moez.qksms.db.entity.ConversationRecipientCrossRef
import com.moez.qksms.db.entity.MessageEntity
import com.moez.qksms.db.entity.MmsPartEntity
import com.moez.qksms.db.entity.PhoneNumberEntity
import com.moez.qksms.db.entity.RecipientEntity
import com.moez.qksms.db.entity.ScheduledMessageAttachmentEntity
import com.moez.qksms.db.entity.ScheduledMessageEntity
import com.moez.qksms.db.entity.ScheduledMessageRecipientEntity
import com.moez.qksms.db.entity.SyncLogEntity

/**
 * The app's persistent store: messages and MMS parts, conversations and their recipients,
 * contacts and phone numbers, contact groups, scheduled messages, blocked numbers and
 * notifications, and the sync log.
 *
 * `version = 1` is the initial schema.
 */
@Database(
    entities = [
        MessageEntity::class,
        MmsPartEntity::class,
        ConversationEntity::class,
        ConversationRecipientCrossRef::class,
        RecipientEntity::class,
        ContactEntity::class,
        PhoneNumberEntity::class,
        ContactGroupEntity::class,
        ContactGroupContactCrossRef::class,
        ScheduledMessageEntity::class,
        ScheduledMessageRecipientEntity::class,
        ScheduledMessageAttachmentEntity::class,
        BlockedNumberEntity::class,
        BlockedMessageNotificationEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class QkDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun mmsPartDao(): MmsPartDao
    abstract fun conversationDao(): ConversationDao
    abstract fun recipientDao(): RecipientDao
    abstract fun contactDao(): ContactDao
    abstract fun contactGroupDao(): ContactGroupDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun blockingDao(): BlockingDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        const val DATABASE_NAME = "qksms.db"

        @Volatile private var instance: QkDatabase? = null

        fun getInstance(context: Context): QkDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QkDatabase::class.java,
                    DATABASE_NAME
                )
                    .build().also { instance = it }
            }
    }
}
