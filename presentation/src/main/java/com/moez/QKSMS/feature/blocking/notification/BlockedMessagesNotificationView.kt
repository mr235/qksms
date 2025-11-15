package com.moez.QKSMS.feature.blocking.notification

import androidx.lifecycle.Lifecycle
import com.moez.QKSMS.common.base.QkViewContract
import io.reactivex.Observable

interface BlockedMessagesNotificationView : QkViewContract<BlockedMessagesNotificationState> {

    fun unblockMessageNotification(): Observable<Long>
    fun addBlockedMessageNotification(): Observable<*>
    fun saveMessage(): Observable<String>

    fun showAddDialog()
}