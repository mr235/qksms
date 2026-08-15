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
import com.moez.qksms.db.entity.ConversationEntity
import com.moez.qksms.db.entity.ConversationRecipientCrossRef
import com.moez.qksms.db.relation.ConversationFull
import io.reactivex.Flowable

/**
 * Room translation of the Conversation queries in ConversationRepositoryImpl,
 * MessageRepositoryImpl and SyncRepositoryImpl.
 *
 * Realm's `isNotNull("lastMessage")` becomes a LEFT JOIN with an `m.id IS NOT NULL` test rather
 * than `lastMessageId IS NOT NULL` — that way a dangling id (message deleted without the
 * conversation being updated) is treated as "no last message", matching Realm link semantics.
 * The `sort("draft", DESCENDING)` tier is kept verbatim so ordering does not shift.
 */
@Dao
interface ConversationDao {

    /** Realm getConversations(archived) — findAllAsync, so this stays reactive. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM conversation c
        LEFT JOIN message m ON m.id = c.lastMessageId
        WHERE c.id != 0
          AND c.archived = :archived
          AND c.blocked = 0
          AND EXISTS (SELECT 1 FROM conversation_recipient cr WHERE cr.conversationId = c.id)
          AND (m.id IS NOT NULL OR c.draft != '')
        ORDER BY c.pinned DESC, c.draft DESC, m.date DESC
        """
    )
    fun getConversations(archived: Boolean): Flowable<List<ConversationFull>>

    /** Same predicate as [getConversations], one-shot — used by the widget on a binder thread. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM conversation c
        LEFT JOIN message m ON m.id = c.lastMessageId
        WHERE c.id != 0
          AND c.archived = :archived
          AND c.blocked = 0
          AND EXISTS (SELECT 1 FROM conversation_recipient cr WHERE cr.conversationId = c.id)
          AND (m.id IS NOT NULL OR c.draft != '')
        ORDER BY c.pinned DESC, c.draft DESC, m.date DESC
        """
    )
    fun getConversationsSnapshot(archived: Boolean): List<ConversationFull>

    /** Realm getTopConversations — pinned OR active in the last week. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM conversation c
        JOIN message m ON m.id = c.lastMessageId
        WHERE c.id != 0
          AND c.archived = 0
          AND c.blocked = 0
          AND (c.pinned = 1 OR m.date > :sinceDate)
          AND EXISTS (SELECT 1 FROM conversation_recipient cr WHERE cr.conversationId = c.id)
        """
    )
    fun getTopConversations(sinceDate: Long): List<ConversationFull>

    /** Realm searchConversations Conversation half — filtering by body happens in MessageDao. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM conversation c
        JOIN message m ON m.id = c.lastMessageId
        WHERE c.id != 0
          AND c.blocked = 0
          AND EXISTS (SELECT 1 FROM conversation_recipient cr WHERE cr.conversationId = c.id)
        ORDER BY c.pinned DESC, m.date DESC
        """
    )
    fun getSearchableConversations(): List<ConversationFull>

    @Transaction
    @Query("SELECT * FROM conversation WHERE blocked = 1")
    fun getBlockedConversations(): List<ConversationFull>

    @Transaction
    @Query("SELECT * FROM conversation WHERE blocked = 1")
    fun getBlockedConversationsFlowable(): Flowable<List<ConversationFull>>

    @Transaction
    @Query("SELECT * FROM conversation WHERE id = :threadId")
    fun getConversation(threadId: Long): ConversationFull?

    @Transaction
    @Query("SELECT * FROM conversation WHERE id = :threadId")
    fun getConversationFlowable(threadId: Long): Flowable<List<ConversationFull>>

    @Transaction
    @Query("SELECT * FROM conversation WHERE id IN (:threadIds)")
    fun getConversations(threadIds: List<Long>): List<ConversationFull>

    /** Realm getUnmanagedConversations — the `limit(5)` recents list on the compose screen. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM conversation c
        JOIN message m ON m.id = c.lastMessageId
        WHERE c.id != 0
          AND c.archived = 0
          AND c.blocked = 0
          AND EXISTS (SELECT 1 FROM conversation_recipient cr WHERE cr.conversationId = c.id)
        ORDER BY m.date DESC
        LIMIT 5
        """
    )
    fun getRecentConversations(): Flowable<List<ConversationFull>>

    /** Backs getThreadId(recipients) — Realm scanned every conversation and matched in Kotlin. */
    @Transaction
    @Query("SELECT * FROM conversation")
    fun getAllConversations(): List<ConversationFull>

    /** MessageRepositoryImpl.getUnreadCount — `equalTo("lastMessage.read", false)`. */
    @Query(
        """
        SELECT COUNT(*) FROM conversation c
        JOIN message m ON m.id = c.lastMessageId
        WHERE c.archived = 0 AND c.blocked = 0 AND m.read = 0
        """
    )
    fun getUnreadCount(): Long

    /** MessageRepositoryImpl.markUnread — returns the message ids to flip to unread. */
    @Query(
        """
        SELECT m.id FROM conversation c
        JOIN message m ON m.id = c.lastMessageId
        WHERE c.id IN (:threadIds) AND m.read = 1
        """
    )
    fun getReadLastMessageIds(threadIds: List<Long>): List<Long>

    /** SyncRepositoryImpl preserves these user-set fields across a full re-sync. */
    @Transaction
    @Query(
        """
        SELECT * FROM conversation
        WHERE archived = 1 OR blocked = 1 OR pinned = 1
           OR name != '' OR blockingClient IS NOT NULL
           OR (blockReason IS NOT NULL AND blockReason != '')
        """
    )
    fun getPersistedConversations(): List<ConversationFull>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAll(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecipientRefs(refs: List<ConversationRecipientCrossRef>)

    @Query("DELETE FROM conversation_recipient WHERE conversationId = :conversationId")
    fun deleteRecipientRefs(conversationId: Long)

    @Query("UPDATE conversation SET name = :name WHERE id = :id")
    fun setName(id: Long, name: String)

    @Query("UPDATE conversation SET draft = :draft WHERE id = :id")
    fun saveDraft(id: Long, draft: String)

    @Query("UPDATE conversation SET lastMessageId = :lastMessageId WHERE id = :id")
    fun updateLastMessage(id: Long, lastMessageId: Long?)

    @Query("UPDATE conversation SET archived = :archived WHERE id IN (:threadIds)")
    fun setArchived(threadIds: List<Long>, archived: Boolean)

    @Query("UPDATE conversation SET pinned = :pinned WHERE id IN (:threadIds)")
    fun setPinned(threadIds: List<Long>, pinned: Boolean)

    @Query(
        """
        UPDATE conversation
        SET blocked = 1, blockingClient = :blockingClient, blockReason = :blockReason
        WHERE id IN (:threadIds) AND blocked = 0
        """
    )
    fun markBlocked(threadIds: List<Long>, blockingClient: Int, blockReason: String?)

    @Query(
        """
        UPDATE conversation
        SET blocked = 0, blockingClient = NULL, blockReason = NULL
        WHERE id IN (:threadIds)
        """
    )
    fun markUnblocked(threadIds: List<Long>)

    @Query("DELETE FROM conversation WHERE id IN (:threadIds)")
    fun deleteConversations(threadIds: List<Long>)

    @Query("DELETE FROM conversation")
    fun deleteAll()

    @Query("DELETE FROM conversation_recipient")
    fun deleteAllRecipientRefs()
}
