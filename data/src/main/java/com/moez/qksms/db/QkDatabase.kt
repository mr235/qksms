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
 * Room replacement for the Realm default instance configured in
 * `presentation/common/QKApplication.kt`.
 *
 * Table structure mirrors the 11 Realm model classes 1:1 (see the Realm→Room migration plan);
 * this is version 1 because there is no prior Room schema to migrate from — the one-time
 * RealmToRoomMigrator (Phase 3) populates these tables from the existing .realm file.
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
                    // The app was originally architected around Realm, which permits cheap
                    // synchronous reads on the main thread (e.g. QkThemedActivity.theme →
                    // getConversation). Room forbids that by default. There are 27+ such call
                    // sites across ~11 files (ComposeViewModel, MainViewModel, etc.) that still
                    // read synchronously; migrating them to an async API is Phase 5 of the
                    // Realm→Room migration and is deliberately deferred — until it lands, this
                    // stays in place so the app doesn't crash on every synchronous read.
                    .allowMainThreadQueries()
                    .build().also { instance = it }
            }
    }
}
