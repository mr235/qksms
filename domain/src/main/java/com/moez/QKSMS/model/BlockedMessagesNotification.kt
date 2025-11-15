package com.moez.QKSMS.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class BlockedMessageNotification(
    @PrimaryKey var id: Long = 0,

    var content: String = ""
) : RealmObject()