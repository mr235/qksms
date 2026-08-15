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
import androidx.documentfile.provider.DocumentFile
import com.moez.qksms.db.dao.MessageDao
import com.moez.qksms.model.BackupFile
import com.moez.qksms.util.Preferences
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed stub of [BackupRepository]. Method bodies will be filled in as part of the
 * Realm → Room migration.
 */
@Singleton
class RoomBackupRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val context: Context,
    private val moshi: Moshi,
    private val prefs: Preferences,
    private val syncRepo: SyncRepository
) : BackupRepository {

    private val backupProgress: Subject<BackupRepository.Progress> =
        BehaviorSubject.createDefault(BackupRepository.Progress.Idle())

    private val restoreProgress: Subject<BackupRepository.Progress> =
        BehaviorSubject.createDefault(BackupRepository.Progress.Idle())

    override fun getDefaultBackupPath(): String = TODO("Room path")

    override fun getBackupDocumentTree(): DocumentFile? = TODO("Room path")

    override fun getBackupPathUriForPicker(): Uri = TODO("Room path")

    override fun persistBackupDirectory(directory: Uri) = TODO("Room path")

    override fun performBackup() = TODO("Room path")

    override fun getBackupProgress(): Observable<BackupRepository.Progress> = backupProgress

    override fun parseBackup(uri: Uri): BackupFile = TODO("Room path")

    override fun performRestore(uri: Uri) = TODO("Room path")

    override fun getRestoreProgress(): Observable<BackupRepository.Progress> = restoreProgress

    override fun stopRestore() = TODO("Room path")
}
