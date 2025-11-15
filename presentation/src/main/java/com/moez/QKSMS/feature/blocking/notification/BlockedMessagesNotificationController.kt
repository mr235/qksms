package com.moez.QKSMS.feature.blocking.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.jakewharton.rxbinding2.view.clicks
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkController
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.extensions.setBackgroundTint
import com.moez.QKSMS.common.util.extensions.setTint
import com.moez.QKSMS.common.widget.QkEditText
import com.moez.QKSMS.databinding.BlockedNotificationControllerBinding
import com.moez.QKSMS.injection.appComponent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * 屏蔽通知栏消息
 */
class BlockedMessagesNotificationController :
    QkController<BlockedMessagesNotificationView, BlockedMessagesNotificationState, BlockedMessagesNotificationPresenter>(),
    BlockedMessagesNotificationView{

    @Inject override lateinit var presenter: BlockedMessagesNotificationPresenter
    @Inject lateinit var colors: Colors

    lateinit var binding: BlockedNotificationControllerBinding

    private val adapter = BlockedMessagesNotificationAdapter()
    private val saveContentSubject: Subject<String> = PublishSubject.create()

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = BlockedNotificationControllerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocked_messages_notification_title)
        showBackButton(true)
    }

    override fun onViewCreated() {
        super.onViewCreated()
        binding.add.setBackgroundTint(colors.theme().theme)
        binding.add.setTint(colors.theme().textPrimary)
        adapter.emptyView = binding.empty
        binding.messages.adapter = adapter
    }

    override fun render(state: BlockedMessagesNotificationState) {
        adapter.updateData(state.messages)
    }

    override fun unblockMessageNotification() = adapter.unblockMessageNotification

    override fun addBlockedMessageNotification() = binding.add.clicks()

    override fun saveMessage(): Observable<String> = saveContentSubject

    override fun showAddDialog() {
        val layout = LayoutInflater.from(activity).inflate(R.layout.blocked_messages_notification_add_dialog, null)
        val dialog = AlertDialog.Builder(activity!!)
            .setView(layout)
            .setPositiveButton(R.string.blocked_messages_notification_dialog_block) { _, _ ->
                saveContentSubject.onNext(layout.findViewById<QkEditText>(R.id.input).text.toString())
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> }
        dialog.show()
    }
}