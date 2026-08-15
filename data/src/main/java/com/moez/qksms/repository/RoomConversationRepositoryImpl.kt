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
package com.moez.qksms.repository

import android.content.Context
import com.moez.qksms.db.dao.ConversationDao
import com.moez.qksms.db.dao.MessageDao
import com.moez.qksms.db.dao.RecipientDao
import com.moez.qksms.filter.ConversationFilter
import com.moez.qksms.mapper.CursorToConversation
import com.moez.qksms.mapper.CursorToRecipient
import com.moez.qksms.model.Conversation
import com.moez.qksms.model.Recipient
import com.moez.qksms.model.SearchResult
import com.moez.qksms.util.PhoneNumberUtils
import io.reactivex.Flowable
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed stub of [ConversationRepository]. Method bodies will be filled in as part of the
 * Realm → Room migration.
 */
@Singleton
class RoomConversationRepositoryImpl @Inject constructor(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val recipientDao: RecipientDao,
    private val conversationFilter: ConversationFilter,
    private val cursorToConversation: CursorToConversation,
    private val cursorToRecipient: CursorToRecipient,
    private val phoneNumberUtils: PhoneNumberUtils
) : ConversationRepository {

    override fun getConversations(archived: Boolean): Flowable<List<Conversation>> = TODO("Room path")

    override fun getConversationsSnapshot(): List<Conversation> = TODO("Room path")

    override fun getTopConversations(): List<Conversation> = TODO("Room path")

    override fun setConversationName(id: Long, name: String) = TODO("Room path")

    override fun searchConversations(query: CharSequence): List<SearchResult> = TODO("Room path")

    override fun getBlockedConversations(): List<Conversation> = TODO("Room path")

    override fun getBlockedConversationsAsync(): Flowable<List<Conversation>> = TODO("Room path")

    override fun getConversationAsync(threadId: Long): Flowable<Conversation> = TODO("Room path")

    override fun getConversation(threadId: Long): Conversation? = TODO("Room path")

    override fun getConversations(vararg threadIds: Long): List<Conversation> = TODO("Room path")

    override fun getUnmanagedConversations(): Observable<List<Conversation>> = TODO("Room path")

    override fun getRecipients(): List<Recipient> = TODO("Room path")

    override fun getUnmanagedRecipients(): Observable<List<Recipient>> = TODO("Room path")

    override fun getRecipient(recipientId: Long): Recipient? = TODO("Room path")

    override fun getThreadId(recipient: String): Long? = TODO("Room path")

    override fun getThreadId(recipients: Collection<String>): Long? = TODO("Room path")

    override fun getOrCreateConversation(threadId: Long): Conversation? = TODO("Room path")

    override fun getOrCreateConversation(address: String): Conversation? = TODO("Room path")

    override fun getOrCreateConversation(addresses: List<String>): Conversation? = TODO("Room path")

    override fun saveDraft(threadId: Long, draft: String) = TODO("Room path")

    override fun updateConversations(vararg threadIds: Long) = TODO("Room path")

    override fun markArchived(vararg threadIds: Long) = TODO("Room path")

    override fun markUnarchived(vararg threadIds: Long) = TODO("Room path")

    override fun markPinned(vararg threadIds: Long) = TODO("Room path")

    override fun markUnpinned(vararg threadIds: Long) = TODO("Room path")

    override fun markBlocked(threadIds: List<Long>, blockingClient: Int, blockReason: String?) = TODO("Room path")

    override fun markUnblocked(vararg threadIds: Long) = TODO("Room path")

    override fun deleteConversations(vararg threadIds: Long) = TODO("Room path")
}
