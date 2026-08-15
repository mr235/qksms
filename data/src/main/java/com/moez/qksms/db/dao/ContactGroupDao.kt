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
import com.moez.qksms.db.entity.ContactGroupContactCrossRef
import com.moez.qksms.db.entity.ContactGroupEntity
import com.moez.qksms.db.relation.ContactGroupFull
import io.reactivex.Flowable

/** Room translation of the ContactGroup queries (ContactRepositoryImpl, SyncRepositoryImpl). */
@Dao
interface ContactGroupDao {

    /** Realm `isNotEmpty("contacts")` — groups with at least one member. */
    @Transaction
    @Query(
        """
        SELECT * FROM contact_group cg
        WHERE EXISTS (
            SELECT 1 FROM contact_group_contact cgc
            WHERE cgc.groupId = cg.id
        )
        """
    )
    fun getContactGroupsWithMembers(): Flowable<List<ContactGroupFull>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(group: ContactGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAll(groups: List<ContactGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCrossRefs(refs: List<ContactGroupContactCrossRef>)

    @Query("DELETE FROM contact_group_contact WHERE groupId = :groupId")
    fun deleteCrossRefsForGroup(groupId: Long)

    @Query("DELETE FROM contact_group")
    fun deleteAll()

    @Query("DELETE FROM contact_group_contact")
    fun deleteAllCrossRefs()
}
