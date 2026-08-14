package com.moez.qksms.feature.blocking.notification

import com.moez.qksms.model.BlockedMessageNotification
import io.realm.RealmResults

data class BlockedMessagesNotificationState(
    val messages: RealmResults<BlockedMessageNotification>? = null,
)
