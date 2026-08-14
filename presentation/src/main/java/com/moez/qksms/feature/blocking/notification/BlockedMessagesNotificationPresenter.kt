package com.moez.qksms.feature.blocking.notification

import android.content.Context
import com.moez.qksms.common.base.QkPresenter
import com.moez.qksms.common.util.extensions.makeToast
import com.moez.qksms.repository.BlockingRepository
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class BlockedMessagesNotificationPresenter @Inject constructor(
    private val blockingRepo: BlockingRepository,
    private val context: Context) :
    QkPresenter<BlockedMessagesNotificationView, BlockedMessagesNotificationState>(
        BlockedMessagesNotificationState(blockingRepo.getBlockedMessagesNotification())
    ) {

    override fun bindIntents(view: BlockedMessagesNotificationView) {
        super.bindIntents(view)

        view.addBlockedMessageNotification()
            .autoDispose(view.scope())
            .subscribe { view.showAddDialog() }

        view.saveMessage()
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe {
                if (it.trim().isNotEmpty()) {
                    blockingRepo.blockMessageNotification(it.trim())
                } else {
                    context.makeToast("请输入内容")
                }
            }

        view.unblockMessageNotification()
            .autoDispose(view.scope())
            .subscribe { blockingRepo.unblockMessageNotification(it) }

    }
}