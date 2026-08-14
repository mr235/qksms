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
package com.moez.qksms.feature.compose.part

import android.content.Context
import com.moez.qksms.R
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.common.util.Colors
import com.moez.qksms.common.util.extensions.setVisible
import com.moez.qksms.common.widget.BubbleImageView
import com.moez.qksms.databinding.MmsPreviewListItemBinding
import com.moez.qksms.extensions.isImage
import com.moez.qksms.extensions.isVideo
import com.moez.qksms.model.Message
import com.moez.qksms.model.MmsPart
import com.moez.qksms.util.GlideApp
import javax.inject.Inject

class MediaBinder @Inject constructor(colors: Colors, private val context: Context) : PartBinder<MmsPreviewListItemBinding>() {

    override val partLayout = R.layout.mms_preview_list_item
    override var theme = colors.theme()

    override fun canBindPart(part: MmsPart) = part.isImage() || part.isVideo()

    override fun bindPart(
        holder: QkViewHolder<in MmsPreviewListItemBinding>,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ) {
        if (holder.binding !is MmsPreviewListItemBinding) {
            return
        }
        holder as QkViewHolder<MmsPreviewListItemBinding>
        holder.binding.video.setVisible(part.isVideo())
        holder.containerView.setOnClickListener { clicks.onNext(part.id) }

        holder.binding.thumbnail.bubbleStyle = when {
            !canGroupWithPrevious && canGroupWithNext -> if (message.isMe()) BubbleImageView.Style.OUT_FIRST else BubbleImageView.Style.IN_FIRST
            canGroupWithPrevious && canGroupWithNext -> if (message.isMe()) BubbleImageView.Style.OUT_MIDDLE else BubbleImageView.Style.IN_MIDDLE
            canGroupWithPrevious && !canGroupWithNext -> if (message.isMe()) BubbleImageView.Style.OUT_LAST else BubbleImageView.Style.IN_LAST
            else -> BubbleImageView.Style.ONLY
        }

        GlideApp.with(context).load(part.getUri()).fitCenter().into(holder.binding.thumbnail)
    }

}