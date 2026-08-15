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
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.moez.qksms.db.dao.ContactDao
import com.moez.qksms.db.dao.ContactGroupDao
import com.moez.qksms.db.mapper.toDomain
import com.moez.qksms.extensions.asFlowable
import com.moez.qksms.extensions.mapNotNull
import com.moez.qksms.model.Contact
import com.moez.qksms.model.ContactGroup
import com.moez.qksms.util.Preferences
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [ContactRepository].
 *
 * [findContactUri] is a pure ContentResolver lookup — no persistence.
 */
@Singleton
class RoomContactRepositoryImpl @Inject constructor(
    private val context: Context,
    private val contactDao: ContactDao,
    private val contactGroupDao: ContactGroupDao,
    private val prefs: Preferences
) : ContactRepository {

    override fun findContactUri(address: String): Single<Uri> {
        return Flowable.just(address)
            .map {
                when {
                    address.contains('@') -> Uri.withAppendedPath(
                        Email.CONTENT_FILTER_URI, Uri.encode(address)
                    )
                    else -> Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address)
                    )
                }
            }
            .mapNotNull { uri -> context.contentResolver.query(uri, arrayOf(BaseColumns._ID), null, null, null) }
            .flatMap { cursor -> cursor.asFlowable() }
            .firstOrError()
            .map { cursor -> cursor.getString(cursor.getColumnIndexOrThrow(BaseColumns._ID)) }
            .map { id -> Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id) }
    }

    override fun getContacts(): List<Contact> = contactDao.getAllContacts().map { it.toDomain() }

    override fun getUnmanagedContact(lookupKey: String): Contact? =
        contactDao.getContact(lookupKey)?.toDomain()

    override fun getUnmanagedContacts(starred: Boolean): Observable<List<Contact>> {
        val mobileOnly = prefs.mobileOnly.get()
        val mobileLabel = if (mobileOnly) {
            Phone.getTypeLabel(context.resources, Phone.TYPE_MOBILE, "Mobile").toString()
        } else null

        return contactDao.getFilteredContacts(starred, mobileLabel)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .map { contacts ->
                var result = contacts.map { it.toDomain() }
                if (mobileOnly && mobileLabel != null) {
                    result = result.map { contact ->
                        val filteredNumbers = contact.numbers.filter { it.type == mobileLabel }
                        contact.numbers.clear()
                        contact.numbers.addAll(filteredNumbers)
                        contact
                    }
                }
                result.sortedWith(Comparator { c1, c2 ->
                    val initial = c1.name.firstOrNull()
                    val other = c2.name.firstOrNull()
                    if (initial?.isLetter() == true && other?.isLetter() != true) -1
                    else if (initial?.isLetter() != true && other?.isLetter() == true) 1
                    else c1.name.compareTo(c2.name, ignoreCase = true)
                })
            }
    }

    override fun getUnmanagedContactGroups(): Observable<List<ContactGroup>> = contactGroupDao
        .getContactGroupsWithMembers()
        .toObservable()
        .subscribeOn(Schedulers.io())
        .map { groups -> groups.map { it.toDomain() } }

    override fun setDefaultPhoneNumber(lookupKey: String, phoneNumberId: Long) =
        contactDao.setDefaultPhoneNumber(lookupKey, phoneNumberId)
}
