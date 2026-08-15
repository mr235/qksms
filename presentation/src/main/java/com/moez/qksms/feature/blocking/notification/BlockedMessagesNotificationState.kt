package com.moez.qksms.feature.blocking.notification

import com.moez.qksms.model.BlockedMessageNotification

data class BlockedMessagesNotificationState(
    val messages: List<BlockedMessageNotification>? = null,
)
