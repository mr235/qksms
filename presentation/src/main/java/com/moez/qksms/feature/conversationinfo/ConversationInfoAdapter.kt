package com.moez.qksms.feature.conversationinfo

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.jakewharton.rxbinding2.view.clicks
import com.moez.qksms.R
import com.moez.qksms.common.base.QkAdapter
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.common.util.Colors
import com.moez.qksms.common.util.extensions.setTint
import com.moez.qksms.common.util.extensions.setVisible
import com.moez.qksms.databinding.ConversationInfoSettingsBinding
import com.moez.qksms.databinding.ConversationMediaListItemBinding
import com.moez.qksms.databinding.ConversationRecipientListItemBinding
import com.moez.qksms.extensions.isVideo
import com.moez.qksms.feature.conversationinfo.ConversationInfoItem.*
import com.moez.qksms.util.GlideApp
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class ConversationInfoAdapter @Inject constructor(
    private val context: Context,
    private val colors: Colors
) : QkAdapter<ConversationInfoItem, ViewBinding>() {

    val recipientClicks: Subject<Long> = PublishSubject.create()
    val recipientLongClicks: Subject<Long> = PublishSubject.create()
    val themeClicks: Subject<Long> = PublishSubject.create()
    val nameClicks: Subject<Unit> = PublishSubject.create()
    val notificationClicks: Subject<Unit> = PublishSubject.create()
    val archiveClicks: Subject<Unit> = PublishSubject.create()
    val blockClicks: Subject<Unit> = PublishSubject.create()
    val deleteClicks: Subject<Unit> = PublishSubject.create()
    val mediaClicks: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)

        var binding: ViewBinding
        if (viewType == 0) {
            binding = ConversationRecipientListItemBinding.inflate(inflater, parent, false)
        } else if (viewType == 1) {
            binding = ConversationInfoSettingsBinding.inflate(inflater, parent, false)
        } else {
            binding = ConversationMediaListItemBinding.inflate(inflater, parent, false)
        }
        return when (viewType) {
            0 -> {
                QkViewHolder(binding).apply {
                    binding as ConversationRecipientListItemBinding
                    itemView.setOnClickListener {
                        val item = getItem(adapterPosition) as? ConversationInfoRecipient
                        item?.value?.id?.run(recipientClicks::onNext)
                    }

                    itemView.setOnLongClickListener {
                        val item = getItem(adapterPosition) as? ConversationInfoRecipient
                        item?.value?.id?.run(recipientLongClicks::onNext)
                        true
                    }

                    binding.theme.setOnClickListener {
                        val item = getItem(adapterPosition) as? ConversationInfoRecipient
                        item?.value?.id?.run(themeClicks::onNext)
                    }
                }
            }

            1 -> QkViewHolder(binding).apply {
                binding as ConversationInfoSettingsBinding
                binding.groupName.clicks().subscribe(nameClicks)
                binding.notifications.clicks().subscribe(notificationClicks)
                binding.archive.clicks().subscribe(archiveClicks)
                binding.block.clicks().subscribe(blockClicks)
                binding.delete.clicks().subscribe(deleteClicks)
            }

            2 -> QkViewHolder(binding).apply {
                itemView.setOnClickListener {
                    val item = getItem(adapterPosition) as? ConversationInfoMedia
                    item?.value?.id?.run(mediaClicks::onNext)
                }
            }

            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder<ViewBinding>, position: Int) {
        when (val item = getItem(position)) {
            is ConversationInfoRecipient -> {
                val binding = holder.binding as ConversationRecipientListItemBinding
                val recipient = item.value
                binding.avatar.setRecipient(recipient)

                binding.name.text = recipient.contact?.name ?: recipient.address

                binding.address.text = recipient.address
                binding.address.setVisible(recipient.contact != null)

                binding.add.setVisible(recipient.contact == null)

                val theme = colors.theme(recipient)
                binding.theme.setTint(theme.theme)
            }

            is ConversationInfoSettings -> {
                val binding = holder.binding as ConversationInfoSettingsBinding
                binding.groupName.isVisible = item.recipients.size > 1
                binding.groupName.summary = item.name

                binding.notifications.isEnabled = !item.blocked

                binding.archive.isEnabled = !item.blocked
                binding.archive.title = context.getString(when (item.archived) {
                    true -> R.string.info_unarchive
                    false -> R.string.info_archive
                })

                binding.block.title = context.getString(when (item.blocked) {
                    true -> R.string.info_unblock
                    false -> R.string.info_block
                })
            }

            is ConversationInfoMedia -> {
                val binding = holder.binding as ConversationMediaListItemBinding
                val part = item.value

                GlideApp.with(context)
                        .load(part.getUri())
                        .fitCenter()
                        .into(binding.thumbnail)

                binding.video.isVisible = part.isVideo()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is ConversationInfoRecipient -> 0
            is ConversationInfoSettings -> 1
            is ConversationInfoMedia -> 2
        }
    }

    override fun areItemsTheSame(old: ConversationInfoItem, new: ConversationInfoItem): Boolean {
        return when {
            old is ConversationInfoRecipient && new is ConversationInfoRecipient -> {
               old.value.id == new.value.id
            }

            old is ConversationInfoSettings && new is ConversationInfoSettings -> {
                true
            }

            old is ConversationInfoMedia && new is ConversationInfoMedia -> {
                old.value.id == new.value.id
            }

            else -> false
        }
    }

}
