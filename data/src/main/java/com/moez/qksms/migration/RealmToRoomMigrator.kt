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
package com.moez.qksms.migration

import com.moez.qksms.db.QkDatabase
import com.moez.qksms.db.entity.ContactGroupContactCrossRef
import com.moez.qksms.db.entity.ConversationRecipientCrossRef
import com.moez.qksms.db.entity.PhoneNumberEntity
import com.moez.qksms.db.mapper.attachmentEntities
import com.moez.qksms.db.mapper.contactCrossRefs
import com.moez.qksms.db.mapper.partEntities
import com.moez.qksms.db.mapper.phoneNumberEntities
import com.moez.qksms.db.mapper.recipientCrossRefs
import com.moez.qksms.db.mapper.recipientEntities
import com.moez.qksms.db.mapper.toEntity
import com.moez.qksms.model.BlockedMessageNotification
import com.moez.qksms.model.BlockedNumber
import com.moez.qksms.model.Contact
import com.moez.qksms.model.ContactGroup
import com.moez.qksms.model.Conversation
import com.moez.qksms.model.Message
import com.moez.qksms.model.MmsPart
import com.moez.qksms.model.Recipient
import com.moez.qksms.model.ScheduledMessage
import com.moez.qksms.model.SyncLog
import com.moez.qksms.util.Preferences
import io.realm.Realm
import io.realm.RealmObject
import io.realm.Sort
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time copy of the Realm database into Room.
 *
 * Runs before the Room repositories are allowed to serve reads — [Preferences.useRoomStorage] must
 * stay false until [Preferences.realmMigrationDone] flips to true. Deliberately *not* incremental:
 * each table is truncated and refilled inside a single Room transaction, so a process death
 * mid-migration simply leaves the done-flag unset and the whole thing reruns on next launch.
 *
 * Tables are written in foreign-key dependency order so that a reader observing a partial state
 * (which cannot happen inside the transaction, but is the invariant we design for) never sees a
 * dangling reference: Contact → PhoneNumber → ContactGroup → Recipient → MmsPart → Message →
 * Conversation → ScheduledMessage → BlockedNumber → Notification → SyncLog.
 */
@Singleton
class RealmToRoomMigrator @Inject constructor(
    private val db: QkDatabase,
    private val prefs: Preferences
) {

    companion object {
        /** Rows read out of Realm per chunk. Keeps peak heap bounded on 200k-message databases. */
        private const val BATCH_SIZE = 500
    }

    /**
     * Copies every table. Blocking and slow (minutes on a large database) — call from a background
     * thread with a progress indicator up. Returns true if the data is now in Room.
     */
    fun migrate(): Boolean {
        if (prefs.realmMigrationDone.get()) return true

        return try {
            Realm.getDefaultInstance().use { realm ->
                db.runInTransaction {
                    clearRoom()
                    migrateContacts(realm)
                    migrateContactGroups(realm)
                    migrateRecipients(realm)
                    migrateParts(realm)
                    migrateMessages(realm)
                    migrateConversations(realm)
                    migrateScheduledMessages(realm)
                    migrateBlocking(realm)
                    migrateSyncLog(realm)
                }
            }

            prefs.realmMigrationDone.set(true)
            Timber.i("Realm → Room migration complete")
            true
        } catch (e: Exception) {
            // Leave the flag unset so the next launch retries from scratch. Room is left in
            // whatever state the failed transaction rolled back to, which is the pre-migration one.
            Timber.e(e, "Realm → Room migration failed; will retry on next launch")
            false
        }
    }

    private fun clearRoom() {
        // Reverse dependency order.
        db.syncLogDao().deleteAll()
        db.blockingDao().deleteAllBlockedMessageNotifications()
        db.blockingDao().deleteAllBlockedNumbers()
        db.scheduledMessageDao().deleteAllAttachments()
        db.scheduledMessageDao().deleteAllRecipients()
        db.scheduledMessageDao().deleteAll()
        db.conversationDao().deleteAllRecipientRefs()
        db.conversationDao().deleteAll()
        db.messageDao().deleteAll()
        db.mmsPartDao().deleteAll()
        db.recipientDao().deleteAll()
        db.contactGroupDao().deleteAllCrossRefs()
        db.contactGroupDao().deleteAll()
        db.contactDao().deleteAllPhoneNumbers()
        db.contactDao().deleteAll()
    }

    /**
     * Reads every row of [clazz] out of Realm in [BATCH_SIZE] chunks, handing each chunk to [write].
     *
     * Realm results are lazy, so slicing the RealmResults keeps only the current chunk's objects
     * materialised. `sort("id")` gives a stable window; without it a concurrent write could shuffle
     * rows between chunk reads. SyncLog has no id column, so [orderBy] is overridable.
     */
    private fun <T : RealmObject> batched(
        realm: Realm,
        clazz: Class<T>,
        orderBy: String? = "id",
        write: (List<T>) -> Unit
    ) {
        val results = realm.where(clazz).let { query ->
            when (orderBy) {
                null -> query.findAll()
                else -> query.sort(orderBy, Sort.ASCENDING).findAll()
            }
        }

        var offset = 0
        while (offset < results.size) {
            val chunk = results.subList(offset, minOf(offset + BATCH_SIZE, results.size)).toList()
            write(chunk)
            offset += chunk.size
        }

        Timber.v("Migrated %d rows of %s", results.size, clazz.simpleName)
    }

    /**
     * Contact carries a String primary key ([Contact.lookupKey]) that every downstream table
     * references. Its `numbers` RealmList is flattened here rather than by a standalone PhoneNumber
     * pass, because Realm stores the linkage on the parent — a bare `where(PhoneNumber)` query
     * would lose which contact each number belongs to.
     */
    private fun migrateContacts(realm: Realm) {
        batched(realm, Contact::class.java, orderBy = "lookupKey") { contacts ->
            db.contactDao().insertOrUpdateAll(contacts.map { it.toEntity() })

            val numbers = contacts.flatMap<Contact, PhoneNumberEntity> { it.phoneNumberEntities() }
            if (numbers.isNotEmpty()) db.contactDao().insertPhoneNumbers(numbers)
        }
    }

    private fun migrateContactGroups(realm: Realm) {
        batched(realm, ContactGroup::class.java) { groups ->
            db.contactGroupDao().insertOrUpdateAll(groups.map { it.toEntity() })

            val refs = groups.flatMap<ContactGroup, ContactGroupContactCrossRef> { it.contactCrossRefs() }
            if (refs.isNotEmpty()) db.contactGroupDao().insertCrossRefs(refs)
        }
    }

    private fun migrateRecipients(realm: Realm) {
        batched(realm, Recipient::class.java) { recipients ->
            db.recipientDao().insertOrUpdateAll(recipients.map { it.toEntity() })
        }
    }

    /**
     * MmsPart is migrated standalone: the authoritative parent link is the scalar
     * [MmsPart.messageId] column, not the `Message.parts` RealmList, so this pass also picks up any
     * orphaned parts that a `Message.parts` walk would silently drop.
     */
    private fun migrateParts(realm: Realm) {
        batched(realm, MmsPart::class.java) { parts ->
            db.mmsPartDao().insertOrUpdateAll(parts.map { it.toEntity() })
        }
    }

    private fun migrateMessages(realm: Realm) {
        batched(realm, Message::class.java) { messages ->
            db.messageDao().insertOrUpdateAll(messages.map { it.toEntity() })

            // Defensive: re-stamp parts reachable from the RealmList in case a part's messageId was
            // never written. REPLACE makes this a no-op for the rows migrateParts already inserted.
            val parts = messages.flatMap { it.partEntities() }
            if (parts.isNotEmpty()) db.mmsPartDao().insertOrUpdateAll(parts)
        }
    }

    private fun migrateConversations(realm: Realm) {
        batched(realm, Conversation::class.java) { conversations ->
            db.conversationDao().insertOrUpdateAll(conversations.map { it.toEntity() })

            val refs = conversations.flatMap<Conversation, ConversationRecipientCrossRef> { it.recipientCrossRefs() }
            if (refs.isNotEmpty()) db.conversationDao().insertRecipientRefs(refs)
        }
    }

    /**
     * ScheduledMessageDao has no batch parent insert — the `save` transaction is the only path that
     * keeps a message and its two ordered child lists consistent, so this runs one call per row.
     * Scheduled messages number in the dozens, not the thousands, so the loop is fine.
     */
    private fun migrateScheduledMessages(realm: Realm) {
        batched(realm, ScheduledMessage::class.java) { messages ->
            messages.forEach { message ->
                db.scheduledMessageDao().save(
                    message.toEntity(),
                    message.recipientEntities(),
                    message.attachmentEntities()
                )
            }
        }
    }

    private fun migrateBlocking(realm: Realm) {
        batched(realm, BlockedNumber::class.java) { numbers ->
            db.blockingDao().insertBlockedNumbers(numbers.map { it.toEntity() })
        }

        batched(realm, BlockedMessageNotification::class.java) { notifications ->
            db.blockingDao().insertBlockedMessageNotifications(notifications.map { it.toEntity() })
        }
    }

    /**
     * SyncLog has no primary key in Realm and a synthetic autoGenerate one in Room, so there is no
     * batch insert to use and no conflict strategy to rely on — [clearRoom] having emptied the table
     * is what makes the rerun idempotent.
     */
    private fun migrateSyncLog(realm: Realm) {
        batched(realm, SyncLog::class.java, orderBy = null) { entries ->
            entries.forEach { db.syncLogDao().insert(it.toEntity()) }
        }
    }
}
