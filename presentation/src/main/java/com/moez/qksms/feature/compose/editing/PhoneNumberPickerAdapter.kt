/*
 * Copyright (C) 2019 Moez Bhatti <moez.bhatti@gmail.com>
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
package com.moez.qksms.feature.compose.editing

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.moez.qksms.R
import com.moez.qksms.common.base.QkAdapter
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.common.util.extensions.forwardTouches
import com.moez.qksms.databinding.PhoneNumberListItemBinding
import com.moez.qksms.extensions.Optional
import com.moez.qksms.model.PhoneNumber
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class PhoneNumberPickerAdapter @Inject constructor(
    private val context: Context
) : QkAdapter<PhoneNumber, PhoneNumberListItemBinding>() {

    val selectedItemChanges: Subject<Optional<Long>> = BehaviorSubject.create()

    private var selectedItem: Long? = null
        set(value) {
            data.indexOfFirst { number -> number.id == field }.takeIf { it != -1 }?.run(::notifyItemChanged)
            field = value
            data.indexOfFirst { number -> number.id == field }.takeIf { it != -1 }?.run(::notifyItemChanged)
            selectedItemChanges.onNext(Optional(value))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder<PhoneNumberListItemBinding> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PhoneNumberListItemBinding.inflate(inflater, parent, false)
        return QkViewHolder(binding).apply {
            binding.number.getRadioButton().forwardTouches(itemView)

            itemView.setOnClickListener {
                val phoneNumber = getItem(adapterPosition)
                selectedItem = phoneNumber.id
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder<PhoneNumberListItemBinding>, position: Int) {
        val phoneNumber = getItem(position)

        holder.binding.number.getRadioButton().isChecked = phoneNumber.id == selectedItem
        holder.binding.number.getTitleView().text = phoneNumber.address
        holder.binding.number.getSummaryView().text = when (phoneNumber.isDefault) {
            true -> context.getString(R.string.compose_number_picker_default, phoneNumber.type)
            false -> phoneNumber.type
        }
    }

    override fun onDatasetChanged() {
        super.onDatasetChanged()
        selectedItem = data.find { number -> number.isDefault }?.id ?: data.firstOrNull()?.id
    }

}
