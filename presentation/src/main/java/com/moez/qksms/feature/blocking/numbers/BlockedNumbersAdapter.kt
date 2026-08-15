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
package com.moez.qksms.feature.blocking.numbers

import android.view.LayoutInflater
import android.view.ViewGroup
import com.moez.qksms.common.base.QkListAdapter
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.databinding.BlockedNumberListItemBinding
import com.moez.qksms.model.BlockedNumber
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class BlockedNumbersAdapter : QkListAdapter<BlockedNumber, BlockedNumberListItemBinding>() {

    val unblockAddress: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder<BlockedNumberListItemBinding> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BlockedNumberListItemBinding.inflate(layoutInflater, parent, false)
        return QkViewHolder(binding).apply {
            binding.unblock.setOnClickListener {
                val number = getItemOrNull(adapterPosition) ?: return@setOnClickListener
                unblockAddress.onNext(number.id)
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder<BlockedNumberListItemBinding>, position: Int) {
        val item = getItem(position)

        holder.binding.number.text = item.address
    }

    // Room re-materialises unmanaged instances on every emission, so the inherited reference
    // equality never matches and DiffUtil would report a full replace. Key on the primary key.
    override fun areItemsTheSame(old: BlockedNumber, new: BlockedNumber) = old.id == new.id

}
