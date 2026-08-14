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
package com.moez.qksms.feature.compose.part

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import com.moez.qksms.R
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.common.util.Colors
import com.moez.qksms.common.util.extensions.resolveThemeColor
import com.moez.qksms.common.util.extensions.setBackgroundTint
import com.moez.qksms.common.util.extensions.setTint
import com.moez.qksms.databinding.MmsFileListItemBinding
import com.moez.qksms.feature.compose.BubbleUtils
import com.moez.qksms.model.Message
import com.moez.qksms.model.MmsPart
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class FileBinder @Inject constructor(colors: Colors, private val context: Context) : PartBinder<MmsFileListItemBinding>() {

    override val partLayout = R.layout.mms_file_list_item
    override var theme = colors.theme()

    // This is the last binder we check. If we're here, we can bind the part
    override fun canBindPart(part: MmsPart) = true

    @SuppressLint("CheckResult")
    override fun bindPart(
        holder: QkViewHolder<in MmsFileListItemBinding>,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ) {
        if (holder.binding !is MmsFileListItemBinding) {
            return
        }
        holder as QkViewHolder<MmsFileListItemBinding>
        BubbleUtils.getBubble(false, canGroupWithPrevious, canGroupWithNext, message.isMe())
                .let(holder.binding.fileBackground::setBackgroundResource)

        holder.containerView.setOnClickListener { clicks.onNext(part.id) }

        Observable.just(part.getUri())
                .map(context.contentResolver::openInputStream)
                .map { inputStream -> inputStream.use { it.available() } }
                .map { bytes ->
                    when (bytes) {
                        in 0..999 -> "$bytes B"
                        in 1000..999999 -> "${"%.1f".format(bytes / 1000f)} KB"
                        in 1000000..9999999 -> "${"%.1f".format(bytes / 1000000f)} MB"
                        else -> "${"%.1f".format(bytes / 1000000000f)} GB"
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { size -> holder.binding.size.text = size }

        holder.binding.filename.text = part.name

        val params = holder.binding.fileBackground.layoutParams as FrameLayout.LayoutParams
        if (!message.isMe()) {
            holder.binding.fileBackground.layoutParams = params.apply { gravity = Gravity.START }
            holder.binding.fileBackground.setBackgroundTint(theme.theme)
            holder.binding.icon.setTint(theme.textPrimary)
            holder.binding.filename.setTextColor(theme.textPrimary)
            holder.binding.size.setTextColor(theme.textTertiary)
        } else {
            holder.binding.fileBackground.layoutParams = params.apply { gravity = Gravity.END }
            holder.binding.fileBackground.setBackgroundTint(holder.containerView.context.resolveThemeColor(R.attr.bubbleColor))
            holder.binding.icon.setTint(holder.containerView.context.resolveThemeColor(android.R.attr.textColorSecondary))
            holder.binding.filename.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorPrimary))
            holder.binding.size.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorTertiary))
        }
    }

}