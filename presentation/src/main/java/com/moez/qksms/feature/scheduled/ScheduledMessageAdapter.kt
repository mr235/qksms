/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.qksms.feature.scheduled

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.moez.qksms.common.base.QkListAdapter
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.common.util.DateFormatter
import com.moez.qksms.databinding.ScheduledMessageListItemBinding
import com.moez.qksms.model.Contact
import com.moez.qksms.model.Recipient
import com.moez.qksms.model.ScheduledMessage
import com.moez.qksms.repository.ContactRepository
import com.moez.qksms.util.PhoneNumberUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject

class ScheduledMessageAdapter @Inject constructor(
    private val context: Context,
    private val contactRepo: ContactRepository,
    private val dateFormatter: DateFormatter,
    private val phoneNumberUtils: PhoneNumberUtils
) : QkListAdapter<ScheduledMessage, ScheduledMessageListItemBinding>() {

    private var contacts: List<Contact> = emptyList()
    private var contactsDisposable: Disposable? = null
    private val contactCache = ContactCache()
    private val imagesViewPool = RecyclerView.RecycledViewPool()

    val clicks: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder<ScheduledMessageListItemBinding> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ScheduledMessageListItemBinding.inflate(layoutInflater, parent, false)

        binding.attachments.adapter = ScheduledMessageAttachmentAdapter(context)
        binding.attachments.setRecycledViewPool(imagesViewPool)

        return QkViewHolder(binding).apply {
            containerView.setOnClickListener {
                val message = getItemOrNull(adapterPosition) ?: return@setOnClickListener
                clicks.onNext(message.id)
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder<ScheduledMessageListItemBinding>, position: Int) {
        val message = getItemOrNull(position) ?: return
        val binding = holder.binding

        // GroupAvatarView only accepts recipients, so map the phone numbers to recipients
        binding.avatars.recipients = message.recipients.map { address -> Recipient(address = address) }

        binding.recipients.text = message.recipients.joinToString(",") { address ->
            contactCache[address]?.name?.takeIf { it.isNotBlank() } ?: address
        }

        binding.date.text = dateFormatter.getScheduledTimestamp(message.date)
        binding.body.text = message.body

        val adapter = binding.attachments.adapter as ScheduledMessageAttachmentAdapter
        adapter.data = message.attachments.map(Uri::parse)
        binding.attachments.isVisible = message.attachments.isNotEmpty()
    }

    // Room re-materialises unmanaged instances on every emission, so the inherited reference
    // equality never matches and DiffUtil would report a full replace. Key on the primary key.
    override fun areItemsTheSame(old: ScheduledMessage, new: ScheduledMessage) = old.id == new.id

    override fun areContentsTheSame(old: ScheduledMessage, new: ScheduledMessage): Boolean {
        return old.date == new.date &&
                old.subId == new.subId &&
                old.body == new.body &&
                old.sendAsGroup == new.sendAsGroup &&
                old.recipients == new.recipients &&
                old.attachments == new.attachments
    }


    /**
     * Loading the contact list queries the database, which used to happen lazily from
     * [onBindViewHolder] and therefore on the main thread. Fetch it once off the main thread when
     * the list is attached instead, and rebind afterwards so the resolved names show up.
     */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        contactsDisposable = Single.fromCallable { contactRepo.getContacts() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ contacts ->
                    this.contacts = contacts
                    contactCache.clear()
                    notifyDataSetChanged()
                }, { error -> Timber.w(error) })
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        contactsDisposable?.dispose()
        contactsDisposable = null
    }

    /**
     * Cache the contacts in a map by the address, because the messages we're binding don't have
     * a reference to the contact.
     */
    private inner class ContactCache : HashMap<String, Contact?>() {

        override fun get(key: String): Contact? {
            if (super.get(key) == null) {
                set(key, contacts.firstOrNull { contact ->
                    contact.numbers.any {
                        phoneNumberUtils.compare(it.address, key)
                    }
                })
            }

            return super.get(key)
        }

    }

}
