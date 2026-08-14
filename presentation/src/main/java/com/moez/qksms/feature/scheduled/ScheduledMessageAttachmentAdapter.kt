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
import com.bumptech.glide.Glide
import com.moez.qksms.common.base.QkAdapter
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.databinding.ScheduledMessageImageListItemBinding
import javax.inject.Inject

class ScheduledMessageAttachmentAdapter @Inject constructor(
    private val context: Context
) : QkAdapter<Uri, ScheduledMessageImageListItemBinding>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder<ScheduledMessageImageListItemBinding> {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ScheduledMessageImageListItemBinding.inflate(layoutInflater, parent, false)
        binding.thumbnail.clipToOutline = true

        return QkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QkViewHolder<ScheduledMessageImageListItemBinding>, position: Int) {
        val attachment = getItem(position)

        Glide.with(context).load(attachment).into(holder.binding.thumbnail)
    }

}
