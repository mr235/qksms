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
import com.moez.qksms.db.entity.MessageEntity
import com.moez.qksms.db.relation.MessageWithParts
import io.reactivex.Flowable

/**
 * Room translation of the Message queries in MessageRepositoryImpl, ConversationRepositoryImpl,
 * SyncRepositoryImpl, BackupRepositoryImpl and KeyManagerImpl.
 */
@Dao
interface MessageDao {

    /** Realm: `equalTo("threadId").contains("body"|"parts.text").sort("date")` + findAllAsync. */
    @Transaction
    @Query(
        """
        SELECT * FROM message
        WHERE threadId = :threadId
          AND (:query = '' OR body LIKE '%' || :query || '%'
               OR id IN (SELECT messageId FROM mms_part WHERE text LIKE '%' || :query || '%'))
        ORDER BY date ASC
        """
    )
    fun getMessages(threadId: Long, query: String): Flowable<List<MessageWithParts>>

    @Transaction
    @Query("SELECT * FROM message WHERE id = :id")
    fun getMessage(id: Long): MessageWithParts?

    /** Replaces the Realm `equalTo("parts.id", id)` reverse traversal. */
    @Transaction
    @Query("SELECT m.* FROM message m JOIN mms_part p ON p.messageId = m.id WHERE p.id = :partId")
    fun getMessageForPart(partId: Long): MessageWithParts?

    /** Realm's nested `beginGroup()...or()...endGroup()` on (type, boxId). */
    @Transaction
    @Query(
        """
        SELECT * FROM message
        WHERE threadId = :threadId
          AND ((type = 'sms' AND boxId IN (:smsBoxIds))
               OR (type = 'mms' AND boxId IN (:mmsBoxIds)))
        ORDER BY date DESC
        """
    )
    fun getLastIncomingMessage(
        threadId: Long,
        smsBoxIds: List<Int>,
        mmsBoxIds: List<Int>
    ): List<MessageWithParts>

    @Query("SELECT * FROM message WHERE seen = 0 AND read = 0 AND threadId = :threadId ORDER BY date ASC")
    fun getUnreadUnseenMessages(threadId: Long): List<MessageEntity>

    @Query("SELECT * FROM message WHERE read = 0 AND threadId = :threadId ORDER BY date ASC")
    fun getUnreadMessages(threadId: Long): List<MessageEntity>

    /** Backing query for both getOldMessageCounts and deleteOldMessages. */
    @Query("SELECT * FROM message WHERE date < :beforeDate")
    fun getMessagesOlderThan(beforeDate: Long): List<MessageEntity>

    /** Realm: `contains("body"|"parts.text")`, used by searchConversations. */
    @Query(
        """
        SELECT * FROM message
        WHERE body LIKE '%' || :query || '%'
           OR id IN (SELECT messageId FROM mms_part WHERE text LIKE '%' || :query || '%')
        """
    )
    fun searchMessages(query: String): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM message WHERE threadId = :threadId AND date > :sinceDate")
    fun countRecentMessages(threadId: Long, sinceDate: Long): Int

    /** Realm: `sort("date", DESCENDING).equalTo("threadId", id).findFirst()`. */
    @Query("SELECT * FROM message WHERE threadId = :threadId ORDER BY date DESC LIMIT 1")
    fun getLastMessage(threadId: Long): MessageEntity?

    /** Realm: `equalTo("type", type).equalTo("contentId", id).findFirst()` in syncMessage. */
    @Query("SELECT * FROM message WHERE type = :type AND contentId = :contentId LIMIT 1")
    fun getMessageByContentId(type: String, contentId: Long): MessageEntity?

    /** BackupRepositoryImpl.performBackup — `sort("date").findAll().createSnapshot()`. */
    @Transaction
    @Query("SELECT * FROM message ORDER BY date ASC")
    fun getAllMessagesSnapshot(): List<MessageWithParts>

    /** KeyManagerImpl.newId() — replaces Realm `max("id")`. */
    @Query("SELECT COALESCE(MAX(id), 0) FROM message")
    fun getMaxId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAll(messages: List<MessageEntity>)

    @Query("UPDATE message SET seen = 1 WHERE seen = 0")
    fun markAllSeen()

    @Query("UPDATE message SET seen = 1 WHERE threadId = :threadId AND seen = 0")
    fun markSeen(threadId: Long)

    @Query("UPDATE message SET seen = 1, read = 1 WHERE threadId IN (:threadIds) AND (read = 0 OR seen = 0)")
    fun markRead(threadIds: List<Long>)

    /** Covers markSending / markSent — both only touch boxId. */
    @Query("UPDATE message SET boxId = :boxId WHERE id = :id")
    fun updateBoxId(id: Long, boxId: Int)

    @Query("UPDATE message SET boxId = :boxId, errorCode = :errorCode WHERE id = :id")
    fun markFailed(id: Long, boxId: Int, errorCode: Int)

    @Query("UPDATE message SET deliveryStatus = :deliveryStatus, dateSent = :dateSent, read = 1 WHERE id = :id")
    fun markDelivered(id: Long, deliveryStatus: Int, dateSent: Long)

    @Query(
        """
        UPDATE message
        SET deliveryStatus = :deliveryStatus, dateSent = :dateSent, read = 1, errorCode = :errorCode
        WHERE id = :id
        """
    )
    fun markDeliveryFailed(id: Long, deliveryStatus: Int, dateSent: Long, errorCode: Int)

    /** MessageRepositoryImpl backfills contentId after the ContentProvider insert returns. */
    @Query("UPDATE message SET contentId = :contentId WHERE id = :id")
    fun updateContentId(id: Long, contentId: Long)

    /** MessageRepositoryImpl.markUnread flips the conversation's last message back to unread. */
    @Query("UPDATE message SET read = 0 WHERE id IN (:ids)")
    fun markUnread(ids: List<Long>)

    @Query("DELETE FROM message WHERE id IN (:ids)")
    fun deleteMessages(ids: List<Long>)

    @Query("DELETE FROM message WHERE date < :beforeDate")
    fun deleteOldMessages(beforeDate: Long)

    @Query("DELETE FROM message WHERE threadId IN (:threadIds)")
    fun deleteMessagesForThreads(threadIds: List<Long>)

    @Query("DELETE FROM message")
    fun deleteAll()
}
