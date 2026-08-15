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
package com.moez.qksms.extensions

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.RealmResults

fun RealmModel.insertOrUpdate() {
    val realm = Realm.getDefaultInstance()
    realm.executeTransaction { realm.insertOrUpdate(this) }
    realm.close()
}

fun <T : RealmModel> Collection<T>.insertOrUpdate() {
    val realm = Realm.getDefaultInstance()
    realm.executeTransaction { realm.insertOrUpdate(this) }
    realm.close()
}

fun <T : RealmObject> T.asObservable(): Observable<T> {
    return asFlowable<T>().toObservable()
}

fun <T : RealmObject> RealmResults<T>.asObservable(): Observable<RealmResults<T>> {
    return asFlowable().toObservable()
}

/**
 * Bridges a live [RealmResults] to a [Flowable] of unmanaged copies, mirroring the shape the new
 * repository contracts expose (`Flowable<List<T>>`). The [realm] instance must stay open for the
 * lifetime of the subscription, so callers keep it at method scope exactly like the existing
 * `getUnmanaged*` helpers do.
 */
fun <T : RealmObject> RealmResults<T>.asFlowableList(realm: Realm): Flowable<List<T>> {
    return asFlowable()
            .filter { it.isLoaded && it.isValid }
            .map { realm.copyFromRealm(it) }
            .subscribeOn(AndroidSchedulers.mainThread())
}

fun <T : RealmObject> RealmQuery<T>.anyOf(fieldName: String, values: LongArray): RealmQuery<T> {
    return when (values.isEmpty()) {
        true -> equalTo(fieldName, -1L)
        false -> `in`(fieldName, values.toTypedArray())
    }
}