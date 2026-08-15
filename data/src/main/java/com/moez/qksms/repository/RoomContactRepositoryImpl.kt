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
import com.moez.qksms.db.dao.ContactDao
import com.moez.qksms.db.dao.ContactGroupDao
import com.moez.qksms.model.Contact
import com.moez.qksms.model.ContactGroup
import com.moez.qksms.util.Preferences
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed stub of [ContactRepository]. Method bodies will be filled in as part of the
 * Realm → Room migration.
 */
@Singleton
class RoomContactRepositoryImpl @Inject constructor(
    private val context: Context,
    private val contactDao: ContactDao,
    private val contactGroupDao: ContactGroupDao,
    private val prefs: Preferences
) : ContactRepository {

    override fun findContactUri(address: String): Single<Uri> = TODO("Room path")

    override fun getContacts(): List<Contact> = TODO("Room path")

    override fun getUnmanagedContact(lookupKey: String): Contact? = TODO("Room path")

    override fun getUnmanagedContacts(starred: Boolean): Observable<List<Contact>> = TODO("Room path")

    override fun getUnmanagedContactGroups(): Observable<List<ContactGroup>> = TODO("Room path")

    override fun setDefaultPhoneNumber(lookupKey: String, phoneNumberId: Long) = TODO("Room path")
}
