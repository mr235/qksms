package com.moez.qksms.feature.blocking.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import com.moez.qksms.common.base.QkListAdapter
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.databinding.BlockedMessagesNotificationListItemBinding
import com.moez.qksms.model.BlockedMessageNotification
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class BlockedMessagesNotificationAdapter : QkListAdapter<BlockedMessageNotification, BlockedMessagesNotificationListItemBinding>() {

    val unblockMessageNotification: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QkViewHolder<BlockedMessagesNotificationListItemBinding> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BlockedMessagesNotificationListItemBinding.inflate(layoutInflater, parent, false)
        return QkViewHolder(binding).apply {
            binding.unblock.setOnClickListener {
                val item = getItemOrNull(adapterPosition) ?: return@setOnClickListener
                unblockMessageNotification.onNext(item.id)
            }
        }
    }

    override fun onBindViewHolder(
        holder: QkViewHolder<BlockedMessagesNotificationListItemBinding>,
        position: Int,
    ) {
        val item = getItem(position)

        holder.binding.content.text = item.content
    }
}