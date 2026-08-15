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
import com.moez.qksms.db.entity.MmsPartEntity
import io.reactivex.Flowable

/** Room translation of the MmsPart queries (RoomMessageRepositoryImpl, RoomSyncRepositoryImpl). */
@Dao
interface MmsPartDao {

    @Query("SELECT * FROM mms_part WHERE id = :id")
    fun getPart(id: Long): MmsPartEntity?

    /** Image and video parts in a thread, found by joining through Message. */
    @Query(
        """
        SELECT p.* FROM mms_part p
        JOIN message m ON p.messageId = m.id
        WHERE m.threadId = :threadId AND (p.type LIKE 'image/%' OR p.type LIKE 'video/%')
        ORDER BY p.id DESC
        """
    )
    fun getPartsForConversation(threadId: Long): Flowable<List<MmsPartEntity>>

    /** RoomSyncRepositoryImpl re-attaches parts to their MMS message by contentId. */
    @Query("SELECT * FROM mms_part WHERE messageId = :contentId")
    fun getPartsByMessageId(contentId: Long): List<MmsPartEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(part: MmsPartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAll(parts: List<MmsPartEntity>)

    @Query("DELETE FROM mms_part WHERE messageId = :messageId")
    fun deletePartsForMessage(messageId: Long)

    @Query("DELETE FROM mms_part")
    fun deleteAll()
}
