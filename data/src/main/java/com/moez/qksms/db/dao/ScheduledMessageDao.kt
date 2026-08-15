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
package com.moez.qksms.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.moez.qksms.db.entity.ScheduledMessageAttachmentEntity
import com.moez.qksms.db.entity.ScheduledMessageEntity
import com.moez.qksms.db.entity.ScheduledMessageRecipientEntity
import com.moez.qksms.db.relation.ScheduledMessageFull
import io.reactivex.Flowable

/** Room translation of RoomScheduledMessageRepositoryImpl. */
@Dao
interface ScheduledMessageDao {

    @Transaction
    @Query("SELECT * FROM scheduled_message ORDER BY date ASC")
    fun getScheduledMessages(): Flowable<List<ScheduledMessageFull>>

    @Transaction
    @Query("SELECT * FROM scheduled_message WHERE id = :id")
    fun getScheduledMessage(id: Long): ScheduledMessageFull?

    /** RoomScheduledMessageRepositoryImpl generates ids via `max(id) + 1`. */
    @Query("SELECT COALESCE(MAX(id), 0) FROM scheduled_message")
    fun getMaxId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(message: ScheduledMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecipients(recipients: List<ScheduledMessageRecipientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAttachments(attachments: List<ScheduledMessageAttachmentEntity>)

    /** Replaces the child rows for one message; call inside a @Transaction with insertOrUpdate. */
    @Query("DELETE FROM scheduled_message_recipient WHERE messageId = :messageId")
    fun deleteRecipients(messageId: Long)

    @Query("DELETE FROM scheduled_message_attachment WHERE messageId = :messageId")
    fun deleteAttachments(messageId: Long)

    /** Persists a scheduled message and its ordered child rows atomically. */
    @Transaction
    fun save(
        message: ScheduledMessageEntity,
        recipients: List<ScheduledMessageRecipientEntity>,
        attachments: List<ScheduledMessageAttachmentEntity>
    ) {
        insertOrUpdate(message)
        deleteRecipients(message.id)
        deleteAttachments(message.id)
        insertRecipients(recipients)
        insertAttachments(attachments)
    }

    @Transaction
    fun delete(id: Long) {
        deleteRecipients(id)
        deleteAttachments(id)
        deleteMessage(id)
    }

    @Query("DELETE FROM scheduled_message WHERE id = :id")
    fun deleteMessage(id: Long)

    @Query("DELETE FROM scheduled_message")
    fun deleteAll()

    @Query("DELETE FROM scheduled_message_recipient")
    fun deleteAllRecipients()

    @Query("DELETE FROM scheduled_message_attachment")
    fun deleteAllAttachments()
}
