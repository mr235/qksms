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

import android.net.Uri
import com.moez.qksms.db.QkDatabase
import com.moez.qksms.model.Message
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed stub of [SyncRepository]. Method bodies will be filled in as part of Phase 2's
 * second half — the DAO layer already has the queries this needs (see [QkDatabase]), what's left
 * is the multi-table `@Transaction` wrapper for [syncMessages]/[syncContacts] and the
 * `PhoneNumberUtils.compare` in-memory join for recipient → contact.
 *
 * Injected via [com.moez.qksms.injection.AppModule.provideSyncRepository]; only reached when
 * [com.moez.qksms.util.Preferences.useRoomStorage] is flipped on, which stays false until this
 * class is real.
 */
@Singleton
class RoomSyncRepositoryImpl @Inject constructor(
    @Suppress("unused") private val db: QkDatabase
) : SyncRepository {

    override val syncProgress: Subject<SyncRepository.SyncProgress> =
        BehaviorSubject.createDefault(SyncRepository.SyncProgress.Idle)

    override fun syncMessages(): Unit = TODO("Room path")

    override fun syncMessage(uri: Uri): Message? = TODO("Room path")

    override fun syncContacts(): Unit = TODO("Room path")
}
