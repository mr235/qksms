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
import androidx.room.Query
import com.moez.qksms.db.entity.SyncLogEntity

/**
 * Room translation of the SyncLog access in RoomSyncRepositoryImpl.
 *
 * The sync pipeline only ever appends one row per sync and later reads the most recent date,
 * so a minimal insert + latest-date query is enough.
 */
@Dao
interface SyncLogDao {

    @Insert
    fun insert(entry: SyncLogEntity)

    @Query("SELECT COALESCE(MAX(date), 0) FROM sync_log")
    fun getLatestSyncDate(): Long

    @Query("DELETE FROM sync_log")
    fun deleteAll()
}
