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
import android.net.Uri
import com.moez.qksms.db.dao.ConversationDao
import com.moez.qksms.db.dao.MessageDao
import com.moez.qksms.db.dao.MmsPartDao
import com.moez.qksms.manager.ActiveConversationManager
import com.moez.qksms.manager.KeyManager
import com.moez.qksms.model.Attachment
import com.moez.qksms.model.Message
import com.moez.qksms.model.MmsPart
import com.moez.qksms.util.PhoneNumberUtils
import com.moez.qksms.util.Preferences
import io.reactivex.Flowable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed stub of [MessageRepository]. Method bodies will be filled in as part of the
 * Realm → Room migration.
 */
@Singleton
class RoomMessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val mmsPartDao: MmsPartDao,
    private val conversationDao: ConversationDao,
    private val activeConversationManager: ActiveConversationManager,
    private val context: Context,
    private val messageIds: KeyManager,
    private val phoneNumberUtils: PhoneNumberUtils,
    private val prefs: Preferences,
    private val syncRepository: SyncRepository
) : MessageRepository {

    override fun getMessages(threadId: Long, query: String): Flowable<List<Message>> = TODO("Room path")

    override fun getMessage(id: Long): Message? = TODO("Room path")

    override fun getMessageForPart(id: Long): Message? = TODO("Room path")

    override fun getLastIncomingMessage(threadId: Long): Flowable<List<Message>> = TODO("Room path")

    override fun getUnreadCount(): Long = TODO("Room path")

    override fun getPart(id: Long): MmsPart? = TODO("Room path")

    override fun getPartsForConversation(threadId: Long): Flowable<List<MmsPart>> = TODO("Room path")

    override fun savePart(id: Long): Uri? = TODO("Room path")

    override fun getUnreadUnseenMessages(threadId: Long): List<Message> = TODO("Room path")

    override fun getUnreadMessages(threadId: Long): Flowable<List<Message>> = TODO("Room path")

    override fun getUnreadMessagesSnapshot(threadId: Long): List<Message> = TODO("Room path")

    override fun markAllSeen() = TODO("Room path")

    override fun markSeen(threadId: Long) = TODO("Room path")

    override fun markRead(vararg threadIds: Long) = TODO("Room path")

    override fun markUnread(vararg threadIds: Long) = TODO("Room path")

    override fun sendMessage(
        subId: Int,
        threadId: Long,
        addresses: List<String>,
        body: String,
        attachments: List<Attachment>,
        delay: Int
    ) = TODO("Room path")

    override fun sendSms(message: Message) = TODO("Room path")

    override fun resendMms(message: Message) = TODO("Room path")

    override fun cancelDelayedSms(id: Long) = TODO("Room path")

    override fun insertSentSms(subId: Int, threadId: Long, address: String, body: String, date: Long): Message =
        TODO("Room path")

    override fun insertReceivedSms(subId: Int, address: String, body: String, sentTime: Long): Message =
        TODO("Room path")

    override fun markSending(id: Long) = TODO("Room path")

    override fun markSent(id: Long) = TODO("Room path")

    override fun markFailed(id: Long, resultCode: Int) = TODO("Room path")

    override fun markDelivered(id: Long) = TODO("Room path")

    override fun markDeliveryFailed(id: Long, resultCode: Int) = TODO("Room path")

    override fun deleteMessages(vararg messageIds: Long) = TODO("Room path")

    override fun getOldMessageCounts(maxAgeDays: Int): Map<Long, Int> = TODO("Room path")

    override fun deleteOldMessages(maxAgeDays: Int) = TODO("Room path")
}
