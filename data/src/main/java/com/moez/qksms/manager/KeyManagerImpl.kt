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
package com.moez.qksms.manager

import com.moez.qksms.db.dao.MessageDao
import com.moez.qksms.model.Message
import com.moez.qksms.util.Preferences
import io.realm.Realm
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class KeyManagerImpl @Inject constructor(
    private val prefs: Preferences,
    /**
     * Lazy so that simply having a KeyManager in the graph does not open the Room database while
     * the Realm path is still the active one.
     */
    private val messageDao: Provider<MessageDao>
) : KeyManager {

    private var initialized = false
    private var maxValue: Long = 0

    /**
     * Should be called when a new sync is being started
     */
    override fun reset() {
        initialized = true
        maxValue = 0L
    }

    /**
     * Returns a valid ID that can be used to store a new message
     */
    override fun newId(): Long {
        if (!initialized) {
            maxValue = when (prefs.useRoomStorage.get()) {
                true -> messageDao.get().getMaxId()
                false -> Realm.getDefaultInstance().use { realm ->
                    realm.where(Message::class.java).max("id")?.toLong() ?: 0L
                }
            }
            initialized = true
        }

        maxValue++
        return maxValue
    }

}