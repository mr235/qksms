package com.moez.QKSMS.feature.blocking.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import com.moez.QKSMS.common.base.QkRealmAdapter
import com.moez.QKSMS.common.base.QkViewHolder
import com.moez.QKSMS.databinding.BlockedMessagesNotificationListItemBinding
import com.moez.QKSMS.model.BlockedMessageNotification
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class BlockedMessagesNotificationAdapter : QkRealmAdapter<BlockedMessageNotification, BlockedMessagesNotificationListItemBinding>() {

    val unblockMessageNotification: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkViewHolder<BlockedMessagesNotificationListItemBinding> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BlockedMessagesNotificationListItemBinding.inflate(layoutInflater, parent, false)
        return QkViewHolder(binding).apply {
            binding.unblock.setOnClickListener {
                val item = getItem(adapterPosition) ?: return@setOnClickListener
                unblockMessageNotification.onNext(item.id)
            }
        }
    }

    override fun onBindViewHolder(
        holder: QkViewHolder<BlockedMessagesNotificationListItemBinding>,
        position: Int,
    ) {
        val item = getItem(position)!!

        holder.binding.content.text = item.content
    }
}