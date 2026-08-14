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
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.moez.qksms.R
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.common.util.Colors
import com.moez.qksms.common.util.extensions.getDisplayName
import com.moez.qksms.common.util.extensions.resolveThemeColor
import com.moez.qksms.common.util.extensions.setBackgroundTint
import com.moez.qksms.common.util.extensions.setTint
import com.moez.qksms.databinding.MmsVcardListItemBinding
import com.moez.qksms.extensions.isVCard
import com.moez.qksms.extensions.mapNotNull
import com.moez.qksms.feature.compose.BubbleUtils
import com.moez.qksms.model.Message
import com.moez.qksms.model.MmsPart
import ezvcard.Ezvcard
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class VCardBinder @Inject constructor(colors: Colors, private val context: Context) : PartBinder<MmsVcardListItemBinding>() {

    override val partLayout = R.layout.mms_vcard_list_item
    override var theme = colors.theme()

    override fun canBindPart(part: MmsPart) = part.isVCard()

    override fun bindPart(
        holder: QkViewHolder<in MmsVcardListItemBinding>,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ) {
        if (holder.binding !is MmsVcardListItemBinding) {
            return
        }
        holder as QkViewHolder<MmsVcardListItemBinding>

        BubbleUtils.getBubble(false, canGroupWithPrevious, canGroupWithNext, message.isMe())
                .let(holder.binding.vCardBackground::setBackgroundResource)

        holder.containerView.setOnClickListener { clicks.onNext(part.id) }

        Observable.just(part.getUri())
                .map(context.contentResolver::openInputStream)
                .mapNotNull { inputStream -> inputStream.use { Ezvcard.parse(it).first() } }
                .map { vcard -> vcard.getDisplayName() ?: "" }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { displayName ->
                    holder.binding.name?.text = displayName
                    holder.binding.name.isVisible = displayName.isNotEmpty()
                }

        val params = holder.binding.vCardBackground.layoutParams as FrameLayout.LayoutParams
        if (!message.isMe()) {
            holder.binding.vCardBackground.layoutParams = params.apply { gravity = Gravity.START }
            holder.binding.vCardBackground.setBackgroundTint(theme.theme)
            holder.binding.vCardAvatar.setTint(theme.textPrimary)
            holder.binding.name.setTextColor(theme.textPrimary)
            holder.binding.label.setTextColor(theme.textTertiary)
        } else {
            holder.binding.vCardBackground.layoutParams = params.apply { gravity = Gravity.END }
            holder.binding.vCardBackground.setBackgroundTint(holder.containerView.context.resolveThemeColor(R.attr.bubbleColor))
            holder.binding.vCardAvatar.setTint(holder.containerView.context.resolveThemeColor(android.R.attr.textColorSecondary))
            holder.binding.name.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorPrimary))
            holder.binding.label.setTextColor(holder.containerView.context.resolveThemeColor(android.R.attr.textColorTertiary))
        }
    }

}
