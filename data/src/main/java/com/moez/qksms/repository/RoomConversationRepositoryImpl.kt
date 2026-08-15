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

import android.content.ContentUris
import android.content.Context
import android.provider.Telephony
import com.moez.qksms.compat.TelephonyCompat
import com.moez.qksms.db.dao.ContactDao
import com.moez.qksms.db.dao.ConversationDao
import com.moez.qksms.db.dao.MessageDao
import com.moez.qksms.db.dao.RecipientDao
import com.moez.qksms.db.mapper.recipientCrossRefs
import com.moez.qksms.db.mapper.toDomain
import com.moez.qksms.db.mapper.toEntity
import com.moez.qksms.extensions.map
import com.moez.qksms.extensions.removeAccents
import com.moez.qksms.filter.ConversationFilter
import com.moez.qksms.mapper.CursorToConversation
import com.moez.qksms.mapper.CursorToRecipient
import com.moez.qksms.model.Contact
import com.moez.qksms.model.Conversation
import com.moez.qksms.model.Recipient
import com.moez.qksms.model.SearchResult
import com.moez.qksms.util.PhoneNumberUtils
import com.moez.qksms.util.tryOrNull
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [ConversationRepository].
 *
 * `getOrCreateConversation` falls back to [CursorToConversation]/[CursorToRecipient] against the
 * native ContentProvider when Room has no row yet, and persists the result via [conversationDao]/
 * [recipientDao].
 */
@Singleton
class RoomConversationRepositoryImpl @Inject constructor(
    private val context: Context,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val recipientDao: RecipientDao,
    private val contactDao: ContactDao,
    private val conversationFilter: ConversationFilter,
    private val cursorToConversation: CursorToConversation,
    private val cursorToRecipient: CursorToRecipient,
    private val phoneNumberUtils: PhoneNumberUtils
) : ConversationRepository {

    override fun getConversations(archived: Boolean): Flowable<List<Conversation>> =
        conversationDao.getConversations(archived).map { list -> list.map { it.toDomain() } }

    override fun getConversationsSnapshot(): List<Conversation> =
        conversationDao.getConversationsSnapshot(false).map { it.toDomain() }

    override fun getTopConversations(): List<Conversation> {
        val sinceDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return conversationDao.getTopConversations(sinceDate)
            .map { it.toDomain() }
            .sortedWith(compareByDescending<Conversation> { it.pinned }
                .thenByDescending { conversation ->
                    messageDao.countRecentMessages(conversation.id, sinceDate)
                })
    }

    override fun setConversationName(id: Long, name: String) = conversationDao.setName(id, name)

    override fun searchConversations(query: CharSequence): List<SearchResult> {
        val normalizedQuery = query.removeAccents()

        val conversations = conversationDao.getSearchableConversations().map { it.toDomain() }

        val messagesByConversation = messageDao.searchMessages(normalizedQuery)
            .groupBy { message -> message.threadId }
            .filter { (threadId, _) -> conversations.any { it.id == threadId } }
            .map { (threadId, messages) -> Pair(conversations.first { it.id == threadId }, messages.size) }
            .map { (conversation, messages) -> SearchResult(normalizedQuery.toString(), conversation, messages) }
            .sortedByDescending { result -> result.messages }

        return conversations
            .filter { conversation -> conversationFilter.filter(conversation, normalizedQuery) }
            .map { conversation -> SearchResult(normalizedQuery.toString(), conversation, 0) } + messagesByConversation
    }

    override fun getBlockedConversations(): List<Conversation> =
        conversationDao.getBlockedConversations().map { it.toDomain() }

    override fun getBlockedConversationsAsync(): Flowable<List<Conversation>> =
        conversationDao.getBlockedConversationsFlowable().map { list -> list.map { it.toDomain() } }

    override fun getConversationAsync(threadId: Long): Flowable<Conversation> =
        conversationDao.getConversationFlowable(threadId)
            .filter { it.isNotEmpty() }
            .map { it.first().toDomain() }

    override fun getConversation(threadId: Long): Conversation? =
        conversationDao.getConversation(threadId)?.toDomain()

    override fun getConversations(vararg threadIds: Long): List<Conversation> =
        conversationDao.getConversations(threadIds.toList()).map { it.toDomain() }

    override fun getUnmanagedConversations(): Observable<List<Conversation>> =
        conversationDao.getRecentConversations()
            .map { list -> list.map { it.toDomain() } }
            .toObservable()
            .subscribeOn(Schedulers.io())

    override fun getRecipients(): List<Recipient> = recipientDao.getRecipientsSnapshot().map { it.toDomain() }

    override fun getUnmanagedRecipients(): Observable<List<Recipient>> =
        recipientDao.getRecipientsWithContact()
            .map { list -> list.map { it.toDomain() } }
            .toObservable()
            .subscribeOn(Schedulers.io())

    override fun getRecipient(recipientId: Long): Recipient? = recipientDao.getRecipient(recipientId)?.toDomain()

    override fun getThreadId(recipient: String): Long? = getThreadId(listOf(recipient))

    override fun getThreadId(recipients: Collection<String>): Long? =
        conversationDao.getAllConversations()
            .asSequence()
            .map { it.toDomain() }
            .filter { conversation -> conversation.recipients.size == recipients.size }
            .find { conversation ->
                conversation.recipients.map { it.address }.all { address ->
                    recipients.any { recipient -> phoneNumberUtils.compare(recipient, address) }
                }
            }?.id

    override fun getOrCreateConversation(threadId: Long): Conversation? =
        getConversation(threadId) ?: getConversationFromCp(threadId)

    override fun getOrCreateConversation(address: String): Conversation? =
        getOrCreateConversation(listOf(address))

    override fun getOrCreateConversation(addresses: List<String>): Conversation? {
        if (addresses.isEmpty()) {
            return null
        }

        return (getThreadId(addresses) ?: tryOrNull { TelephonyCompat.getOrCreateThreadId(context, addresses.toSet()) })
            ?.takeIf { threadId -> threadId != 0L }
            ?.let { threadId -> getConversation(threadId) ?: getConversationFromCp(threadId) }
    }

    override fun saveDraft(threadId: Long, draft: String) = conversationDao.saveDraft(threadId, draft)

    override fun updateConversations(vararg threadIds: Long) {
        threadIds.forEach { threadId ->
            val lastMessage = messageDao.getLastMessage(threadId)
            conversationDao.updateLastMessage(threadId, lastMessage?.id)
        }
    }

    override fun markArchived(vararg threadIds: Long) = conversationDao.setArchived(threadIds.toList(), true)

    override fun markUnarchived(vararg threadIds: Long) = conversationDao.setArchived(threadIds.toList(), false)

    override fun markPinned(vararg threadIds: Long) = conversationDao.setPinned(threadIds.toList(), true)

    override fun markUnpinned(vararg threadIds: Long) = conversationDao.setPinned(threadIds.toList(), false)

    override fun markBlocked(threadIds: List<Long>, blockingClient: Int, blockReason: String?) =
        conversationDao.markBlocked(threadIds, blockingClient, blockReason)

    override fun markUnblocked(vararg threadIds: Long) = conversationDao.markUnblocked(threadIds.toList())

    override fun deleteConversations(vararg threadIds: Long) {
        conversationDao.deleteConversations(threadIds.toList())
        messageDao.deleteMessagesForThreads(threadIds.toList())

        threadIds.forEach { threadId ->
            val uri = ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadId)
            context.contentResolver.delete(uri, null, null)
        }
    }

    /**
     * Returns a [Conversation] from the system SMS ContentProvider, based on the [threadId]
     *
     * It should be noted that even if we have a valid [threadId], that does not guarantee that
     * we can return a [Conversation]. On some devices, the ContentProvider won't return the
     * conversation unless it contains at least 1 message
     */
    private fun getConversationFromCp(threadId: Long): Conversation? {
        return cursorToConversation.getConversationsCursor()
            ?.map(cursorToConversation::map)
            ?.firstOrNull { it.id == threadId }
            ?.let { conversation ->
                val contacts = contactDao.getAllContacts().map { it.toDomain() }
                val lastMessage = messageDao.getLastMessage(threadId)?.toDomain()

                val recipients = conversation.recipients
                    .map { recipient -> recipient.id }
                    .map { id -> cursorToRecipient.getRecipientCursor(id) }
                    .mapNotNull { recipientCursor ->
                        recipientCursor?.use { recipientCursor.map { cursorToRecipient.map(recipientCursor) } }
                    }
                    .flatten()
                    .map { recipient ->
                        recipient.apply {
                            contact = findContact(contacts, recipient.address)
                        }
                    }

                conversation.recipients.clear()
                conversation.recipients.addAll(recipients)
                conversation.lastMessage = lastMessage

                conversationDao.insertOrUpdate(conversation.toEntity())
                recipients.forEach { recipient -> recipientDao.insertOrUpdate(recipient.toEntity()) }
                conversationDao.deleteRecipientRefs(conversation.id)
                conversationDao.insertRecipientRefs(conversation.recipientCrossRefs())

                conversation
            }
    }

    private fun findContact(contacts: List<Contact>, address: String): Contact? =
        contacts.firstOrNull { contact -> contact.numbers.any { phoneNumberUtils.compare(address, it.address) } }
}
