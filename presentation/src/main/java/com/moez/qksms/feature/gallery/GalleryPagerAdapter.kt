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
package com.moez.qksms.feature.gallery

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.mms.ContentType
import com.moez.qksms.common.base.QkRealmAdapter
import com.moez.qksms.common.base.QkViewHolder
import com.moez.qksms.databinding.GalleryImagePageBinding
import com.moez.qksms.databinding.GalleryInvalidPageBinding
import com.moez.qksms.databinding.GalleryVideoPageBinding
import com.moez.qksms.extensions.isImage
import com.moez.qksms.extensions.isVideo
import com.moez.qksms.model.MmsPart
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*
import javax.inject.Inject

class GalleryPagerAdapter @Inject constructor(private val context: Context) : QkRealmAdapter<MmsPart, ViewBinding>() {

    companion object {
        private const val VIEW_TYPE_INVALID = 0
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_VIDEO = 2
    }

    val clicks: Subject<View> = PublishSubject.create()

    private val contentResolver = context.contentResolver
    private val exoPlayers = Collections.newSetFromMap(WeakHashMap<ExoPlayer?, Boolean>())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        var binding: ViewBinding
        if (viewType == VIEW_TYPE_IMAGE) {
            binding = GalleryImagePageBinding.inflate(inflater, parent, false).apply {

                // When calling the public setter, it doesn't allow the midscale to be the same as the
                // maxscale or the minscale. We don't want 3 levels and we don't want to modify the library
                // so let's celebrate the invention of reflection!
                image.attacher.run {
                    javaClass.getDeclaredField("mMinScale").run {
                        isAccessible = true
                        setFloat(image.attacher, 1f)
                    }
                    javaClass.getDeclaredField("mMidScale").run {
                        isAccessible = true
                        setFloat(image.attacher, 1f)
                    }
                    javaClass.getDeclaredField("mMaxScale").run {
                        isAccessible = true
                        setFloat(image.attacher, 3f)
                    }
                }
            }
        } else if (viewType == VIEW_TYPE_VIDEO) {
            binding = GalleryVideoPageBinding.inflate(inflater, parent, false)
        } else {
            binding = GalleryInvalidPageBinding.inflate(inflater, parent, false)
        }
        return QkViewHolder(binding).apply { itemView.setOnClickListener(clicks::onNext) }
    }

    override fun onBindViewHolder(holder: QkViewHolder<ViewBinding>, position: Int) {
        val part = getItem(position) ?: return
        when (getItemViewType(position)) {
            VIEW_TYPE_IMAGE -> {
                val binding = holder.binding as GalleryImagePageBinding
                // We need to explicitly request a gif from glide for animations to work
                when (part.getUri().let(contentResolver::getType)) {
                    ContentType.IMAGE_GIF -> Glide.with(context)
                            .asGif()
                            .load(part.getUri())
                            .into(binding.image)

                    else -> Glide.with(context)
                            .asBitmap()
                            .load(part.getUri())
                            .into(binding.image)
                }
            }

            VIEW_TYPE_VIDEO -> {
                val binding = holder.binding as GalleryVideoPageBinding

                val trackSelector = DefaultTrackSelector(context)

                val exoPlayer = ExoPlayer.Builder(context).setTrackSelector(trackSelector).build()
                binding.video.player = exoPlayer
                exoPlayers.add(exoPlayer)

                exoPlayer.setMediaItem(MediaItem.fromUri(part.getUri()))
                exoPlayer.prepare()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val part = getItem(position)
        return when {
            part?.isImage() == true -> VIEW_TYPE_IMAGE
            part?.isVideo() == true -> VIEW_TYPE_VIDEO
            else -> VIEW_TYPE_INVALID
        }
    }

    fun destroy() {
        exoPlayers.forEach { exoPlayer -> exoPlayer?.release() }
    }

}
