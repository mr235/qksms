package com.moez.qksms.feature.blocking.notification

import com.moez.qksms.common.base.QkViewContract
import io.reactivex.Observable

interface BlockedMessagesNotificationView : QkViewContract<BlockedMessagesNotificationState> {

    fun unblockMessageNotification(): Observable<Long>
    fun addBlockedMessageNotification(): Observable<*>
    fun saveMessage(): Observable<String>

    fun showAddDialog()
}