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

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.Telephony
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.moez.qksms.db.QkDatabase
import com.moez.qksms.db.mapper.contactCrossRefs
import com.moez.qksms.db.mapper.partEntities
import com.moez.qksms.db.mapper.phoneNumberEntities
import com.moez.qksms.db.mapper.recipientCrossRefs
import com.moez.qksms.db.mapper.toDomain
import com.moez.qksms.db.mapper.toEntity
import com.moez.qksms.extensions.forEach
import com.moez.qksms.extensions.map
import com.moez.qksms.manager.KeyManager
import com.moez.qksms.mapper.CursorToContact
import com.moez.qksms.mapper.CursorToContactGroup
import com.moez.qksms.mapper.CursorToContactGroupMember
import com.moez.qksms.mapper.CursorToConversation
import com.moez.qksms.mapper.CursorToMessage
import com.moez.qksms.mapper.CursorToPart
import com.moez.qksms.mapper.CursorToRecipient
import com.moez.qksms.model.Contact
import com.moez.qksms.model.ContactGroup
import com.moez.qksms.model.Conversation
import com.moez.qksms.model.Message
import com.moez.qksms.model.MmsPart
import com.moez.qksms.model.PhoneNumber
import com.moez.qksms.model.SyncLog
import com.moez.qksms.util.PhoneNumberUtils
import com.moez.qksms.util.tryOrNull
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.realm.RealmList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [SyncRepository]. Mirrors [SyncRepositoryImpl] semantics.
 *
 * [syncMessages] is a full truncate-and-refill of the message/conversation/recipient/contact
 * tables, so the whole thing runs inside a single [QkDatabase.runInTransaction] block — the Realm
 * path relied on `beginTransaction`/`commitTransaction` for the same all-or-nothing guarantee.
 *
 * Two Realm behaviours are reproduced in Kotlin rather than SQL:
 *  - the recipient → contact join uses [PhoneNumberUtils.compare], which has no SQL equivalent;
 *  - MMS parts are attached to their message by matching `part.messageId == message.contentId`,
 *    which is how the Realm path linked them (parts are keyed on the *content* id, not our own).
 */
@Singleton
class RoomSyncRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val db: QkDatabase,
    private val conversationRepo: ConversationRepository,
    private val cursorToConversation: CursorToConversation,
    private val cursorToMessage: CursorToMessage,
    private val cursorToPart: CursorToPart,
    private val cursorToRecipient: CursorToRecipient,
    private val cursorToContact: CursorToContact,
    private val cursorToContactGroup: CursorToContactGroup,
    private val cursorToContactGroupMember: CursorToContactGroupMember,
    private val keys: KeyManager,
    private val phoneNumberUtils: PhoneNumberUtils,
    private val rxPrefs: RxSharedPreferences
) : SyncRepository {

    override val syncProgress: Subject<SyncRepository.SyncProgress> =
        BehaviorSubject.createDefault(SyncRepository.SyncProgress.Idle)

    override fun syncMessages() {
        // If the sync is already running, don't try to do another one
        if (syncProgress.blockingFirst() is SyncRepository.SyncProgress.Running) return
        syncProgress.onNext(SyncRepository.SyncProgress.Running(0, 0, true))

        // Preserve the user-set conversation fields across the wipe
        val persistedData = db.conversationDao().getPersistedConversations()
            .map { it.toDomain() }
            .associateBy { conversation -> conversation.id }
            .toMutableMap()

        // Migrate blocked conversations from 2.7.3
        val oldBlockedSenders = rxPrefs.getStringSet("pref_key_blocked_senders")
        oldBlockedSenders.get()
            .map { threadIdString -> threadIdString.toLong() }
            .filter { threadId -> !persistedData.contains(threadId) }
            .forEach { threadId -> persistedData[threadId] = Conversation(id = threadId, blocked = true) }

        val partsCursor = cursorToPart.getPartsCursor()
        val messageCursor = cursorToMessage.getMessagesCursor()
        val conversationCursor = cursorToConversation.getConversationsCursor()
        val recipientCursor = cursorToRecipient.getRecipientCursor()

        val max = (partsCursor?.count ?: 0) +
                (messageCursor?.count ?: 0) +
                (conversationCursor?.count ?: 0) +
                (recipientCursor?.count ?: 0)

        var progress = 0

        // Read every cursor into memory first. The Realm path interleaved reads and writes inside
        // its transaction; here we keep the transaction as short as possible and do the
        // ContentProvider traversal outside of it.
        val parts = mutableListOf<MmsPart>()
        partsCursor?.use {
            it.forEach { cursor ->
                tryOrNull {
                    progress++
                    parts += cursorToPart.map(cursor)
                }
            }
        }

        // Parts grouped by the content id they belong to — this is the Realm `messageId` linkage
        val partsByContentId = parts.groupBy { part -> part.messageId }

        val messages = mutableListOf<Message>()
        messageCursor?.use {
            val messageColumns = CursorToMessage.MessageColumns(it)
            it.forEach { cursor ->
                tryOrNull {
                    progress++
                    syncProgress.onNext(SyncRepository.SyncProgress.Running(max, progress, false))
                    messages += cursorToMessage.map(Pair(cursor, messageColumns)).apply {
                        if (isMms()) {
                            this.parts = RealmList<MmsPart>().apply {
                                addAll(partsByContentId[contentId].orEmpty())
                            }
                        }
                    }
                }
            }
        }

        // Last message per thread, so conversations can point at it
        val lastMessageByThread = messages
            .groupBy { message -> message.threadId }
            .mapValues { (_, threadMessages) -> threadMessages.maxByOrNull { it.date } }

        val conversations = mutableListOf<Conversation>()
        conversationCursor?.use {
            it.forEach { cursor ->
                tryOrNull {
                    progress++
                    syncProgress.onNext(SyncRepository.SyncProgress.Running(max, progress, false))
                    conversations += cursorToConversation.map(cursor).apply {
                        persistedData[id]?.let { persisted ->
                            archived = persisted.archived
                            blocked = persisted.blocked
                            pinned = persisted.pinned
                            name = persisted.name
                            blockingClient = persisted.blockingClient
                            blockReason = persisted.blockReason
                        }
                        lastMessage = lastMessageByThread[id]
                    }
                }
            }
        }

        val contacts = getContacts()

        val recipients = mutableListOf<com.moez.qksms.model.Recipient>()
        recipientCursor?.use {
            it.forEach { cursor ->
                tryOrNull {
                    progress++
                    syncProgress.onNext(SyncRepository.SyncProgress.Running(max, progress, false))
                    recipients += cursorToRecipient.map(cursor).apply {
                        contact = findContact(contacts, address)
                    }
                }
            }
        }

        syncProgress.onNext(SyncRepository.SyncProgress.Running(0, 0, true))

        db.runInTransaction {
            db.contactDao().deleteAllPhoneNumbers()
            db.contactDao().deleteAll()
            db.contactGroupDao().deleteAllCrossRefs()
            db.contactGroupDao().deleteAll()
            db.conversationDao().deleteAllRecipientRefs()
            db.conversationDao().deleteAll()
            db.mmsPartDao().deleteAll()
            db.messageDao().deleteAll()
            db.recipientDao().deleteAll()

            keys.reset()

            // Contacts first — recipients reference them by lookup key
            db.contactDao().insertOrUpdateAll(contacts.map { it.toEntity() })
            db.contactDao().insertPhoneNumbers(contacts.flatMap { it.phoneNumberEntities() })

            db.messageDao().insertOrUpdateAll(messages.map { it.toEntity() })
            // Parts must be stamped with `messageId = message.id` so the Room `@Relation` join
            // (MessageWithParts) resolves. `MmsPart.messageId` under Realm is the MMS *content id*,
            // not our own primary key — going through `messages.flatMap { it.partEntities() }`
            // rewrites that link. Do NOT swap this for `parts.map { it.toEntity() }`: that would
            // preserve the raw contentId and every MMS attachment silently disappears.
            db.mmsPartDao().insertOrUpdateAll(messages.flatMap { it.partEntities() })

            db.recipientDao().insertOrUpdateAll(recipients.map { it.toEntity() })

            db.conversationDao().insertOrUpdateAll(conversations.map { it.toEntity() })
            db.conversationDao().insertRecipientRefs(conversations.flatMap { it.recipientCrossRefs() })

            db.syncLogDao().insert(SyncLog().toEntity())
        }

        // Only delete this after the sync has successfully completed
        oldBlockedSenders.delete()

        syncProgress.onNext(SyncRepository.SyncProgress.Idle)
    }

    override fun syncMessage(uri: Uri): Message? {
        // If we don't have a valid type, return null
        val type = when {
            uri.toString().contains("mms") -> "mms"
            uri.toString().contains("sms") -> "sms"
            else -> return null
        }

        // If we don't have a valid id, return null
        val id = tryOrNull(false) { ContentUris.parseId(uri) } ?: return null

        // Check if the message already exists, so we can reuse the id
        val existingId = db.messageDao().getMessageByContentId(type, id)?.id

        // The uri might be something like content://mms/inbox/id
        // The box might change though, so we should just use the mms/id uri
        val stableUri = when (type) {
            "mms" -> ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, id)
            else -> ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id)
        }

        return contentResolver.query(stableUri, null, null, null, null)?.use { cursor ->
            // If there are no rows, return null. Otherwise, we've moved to the first row
            if (!cursor.moveToFirst()) return null

            val columnsMap = CursorToMessage.MessageColumns(cursor)
            cursorToMessage.map(Pair(cursor, columnsMap)).apply {
                existingId?.let { this.id = it }

                if (isMms()) {
                    parts = RealmList<MmsPart>().apply {
                        addAll(cursorToPart.getPartsCursor(contentId)?.map { cursorToPart.map(it) }.orEmpty())
                    }
                }

                conversationRepo.getOrCreateConversation(threadId)

                db.runInTransaction {
                    db.messageDao().insertOrUpdate(toEntity())
                    db.mmsPartDao().deletePartsForMessage(this.id)
                    db.mmsPartDao().insertOrUpdateAll(partEntities())
                }
            }
        }
    }

    override fun syncContacts() {
        // Load all the contacts
        val contacts = getContacts()
        val groups = getContactGroups(contacts)

        // Recipients survive a contact sync — only their contact link is recomputed
        val recipients = db.recipientDao().getAllRecipientEntities()

        db.runInTransaction {
            db.contactDao().deleteAllPhoneNumbers()
            db.contactDao().deleteAll()
            db.contactGroupDao().deleteAllCrossRefs()
            db.contactGroupDao().deleteAll()

            db.contactDao().insertOrUpdateAll(contacts.map { it.toEntity() })
            db.contactDao().insertPhoneNumbers(contacts.flatMap { it.phoneNumberEntities() })

            db.contactGroupDao().insertOrUpdateAll(groups.map { it.toEntity() })
            db.contactGroupDao().insertCrossRefs(groups.flatMap { it.contactCrossRefs() })

            // Update all the recipients with the new contacts
            recipients.forEach { recipient ->
                val lookupKey = findContact(contacts, recipient.address)?.lookupKey
                db.recipientDao().updateContact(recipient.id, lookupKey)
            }
        }
    }

    private fun findContact(contacts: List<Contact>, address: String): Contact? =
        contacts.firstOrNull { contact ->
            contact.numbers.any { number -> phoneNumberUtils.compare(address, number.address) }
        }

    private fun getContacts(): List<Contact> {
        val defaultNumberIds = db.contactDao().getDefaultPhoneNumberIds()

        return cursorToContact.getContactsCursor()
            ?.map { cursor -> cursorToContact.map(cursor) }
            ?.groupBy { contact -> contact.lookupKey }
            ?.map { contacts ->
                // Sometimes, contacts providers on the phone will create duplicate phone number entries. This
                // commonly happens with Whatsapp. Let's try to detect these duplicate entries and filter them out
                val uniqueNumbers = mutableListOf<PhoneNumber>()
                contacts.value
                    .flatMap { it.numbers }
                    .forEach { number ->
                        number.isDefault = defaultNumberIds.any { id -> id == number.id }
                        val duplicate = uniqueNumbers.find { other ->
                            phoneNumberUtils.compare(number.address, other.address)
                        }

                        if (duplicate == null) {
                            uniqueNumbers += number
                        } else if (!duplicate.isDefault && number.isDefault) {
                            duplicate.isDefault = true
                        }
                    }

                contacts.value.first().apply {
                    numbers.clear()
                    numbers.addAll(uniqueNumbers)
                }
            } ?: listOf()
    }

    private fun getContactGroups(contacts: List<Contact>): List<ContactGroup> {
        val groupMembers = cursorToContactGroupMember.getGroupMembersCursor()
            ?.map(cursorToContactGroupMember::map)
            .orEmpty()

        val groups = cursorToContactGroup.getContactGroupsCursor()
            ?.map(cursorToContactGroup::map)
            .orEmpty()

        groups.forEach { group ->
            group.contacts.addAll(groupMembers
                .filter { member -> member.groupId == group.id }
                .mapNotNull { member -> contacts.find { contact -> contact.lookupKey == member.lookupKey } })
        }

        return groups
    }
}
