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
import com.moez.qksms.db.entity.RecipientEntity
import com.moez.qksms.db.relation.RecipientFull
import io.reactivex.Flowable

/** Room translation of the Recipient queries (RoomConversationRepositoryImpl, RoomSyncRepositoryImpl). */
@Dao
interface RecipientDao {

    @Transaction
    @Query("SELECT * FROM recipient")
    fun getRecipients(): Flowable<List<RecipientFull>>

    /** Only recipients that resolve to a contact. */
    @Transaction
    @Query("SELECT * FROM recipient WHERE contactLookupKey IS NOT NULL")
    fun getRecipientsWithContact(): Flowable<List<RecipientFull>>

    /** One-shot version of [getRecipients] — RoomConversationRepositoryImpl.getRecipients(). */
    @Transaction
    @Query("SELECT * FROM recipient")
    fun getRecipientsSnapshot(): List<RecipientFull>

    @Transaction
    @Query("SELECT * FROM recipient WHERE id = :recipientId")
    fun getRecipient(recipientId: Long): RecipientFull?

    @Query("SELECT * FROM recipient")
    fun getAllRecipientEntities(): List<RecipientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(recipient: RecipientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAll(recipients: List<RecipientEntity>)

    @Query("UPDATE recipient SET contactLookupKey = :contactLookupKey WHERE id = :recipientId")
    fun updateContact(recipientId: Long, contactLookupKey: String?)

    @Query("DELETE FROM recipient")
    fun deleteAll()
}
