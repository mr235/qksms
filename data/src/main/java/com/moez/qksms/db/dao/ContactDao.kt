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
import com.moez.qksms.db.entity.ContactEntity
import com.moez.qksms.db.entity.PhoneNumberEntity
import com.moez.qksms.db.relation.ContactFull
import io.reactivex.Flowable

/**
 * Room translation of the Contact / PhoneNumber queries (RoomContactRepositoryImpl,
 * RoomSyncRepositoryImpl, RoomConversationRepositoryImpl).
 *
 * Note: `ORDER BY name` is a plain lexicographic sort. RoomContactRepositoryImpl re-sorts in
 * Kotlin (letters before non-letters, case-insensitive) for the picker, so that comparator stays
 * in the repository — only the raw ordering is reproduced here.
 */
@Dao
interface ContactDao {

    @Transaction
    @Query("SELECT * FROM contact ORDER BY name ASC")
    fun getContacts(): Flowable<List<ContactFull>>

    @Transaction
    @Query("SELECT * FROM contact WHERE lookupKey = :lookupKey")
    fun getContact(lookupKey: String): ContactFull?

    @Transaction
    @Query("SELECT * FROM contact")
    fun getAllContacts(): List<ContactFull>

    /**
     * Contacts filtered by having at least one mobile-labelled number and/or being starred.
     * Both filters are pushed into SQL; pass mobileLabel = null to skip the mobile-only filter
     * and starredOnly = false to skip the starred filter.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM contact c
        WHERE (:starredOnly = 0 OR c.starred = 1)
          AND (:mobileLabel IS NULL
               OR EXISTS (SELECT 1 FROM phone_number p
                          WHERE p.contactLookupKey = c.lookupKey
                            AND p.type LIKE '%' || :mobileLabel || '%'))
        ORDER BY c.name ASC
        """
    )
    fun getFilteredContacts(starredOnly: Boolean, mobileLabel: String?): Flowable<List<ContactFull>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAll(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhoneNumbers(numbers: List<PhoneNumberEntity>)

    @Query("SELECT * FROM phone_number WHERE contactLookupKey = :lookupKey")
    fun getPhoneNumbers(lookupKey: String): List<PhoneNumberEntity>

    /** RoomSyncRepositoryImpl.getContacts() collects the ids of all default numbers. */
    @Query("SELECT id FROM phone_number WHERE isDefault = 1")
    fun getDefaultPhoneNumberIds(): List<Long>

    /**
     * RoomContactRepositoryImpl.setDefaultPhoneNumber — exactly one number per contact ends up
     * default, so this is a single conditional UPDATE rather than a read-modify-write loop.
     */
    @Query(
        """
        UPDATE phone_number
        SET isDefault = (id = :phoneNumberId)
        WHERE contactLookupKey = :lookupKey
        """
    )
    fun setDefaultPhoneNumber(lookupKey: String, phoneNumberId: Long)

    @Query("DELETE FROM phone_number WHERE contactLookupKey = :lookupKey")
    fun deletePhoneNumbers(lookupKey: String)

    @Query("DELETE FROM contact")
    fun deleteAll()

    @Query("DELETE FROM phone_number")
    fun deleteAllPhoneNumbers()
}
