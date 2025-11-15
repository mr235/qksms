package com.moez.QKSMS.feature.blocking.notification

import com.moez.QKSMS.model.BlockedMessageNotification
import io.realm.RealmResults

data class BlockedMessagesNotificationState(
    val messages: RealmResults<BlockedMessageNotification>? = null,
)
