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
package com.moez.qksms.common.util.extensions

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager

var ViewGroup.animateLayoutChanges: Boolean
    get() = layoutTransition != null
    set(value) {
        layoutTransition = if (value) LayoutTransition() else null
    }

fun EditText.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun EditText.hideKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun ImageView.setTint(color: Int) {
    imageTintList = ColorStateList.valueOf(color)
}

fun ProgressBar.setTint(color: Int) {
    indeterminateTintList = ColorStateList.valueOf(color)
    progressTintList = ColorStateList.valueOf(color)
}

fun View.setBackgroundTint(color: Int) {

    // API 21 doesn't support this

    backgroundTintList = ColorStateList.valueOf(color)
}

fun View.setPadding(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    setPadding(left ?: paddingLeft, top ?: paddingTop, right ?: paddingRight, bottom ?: paddingBottom)
}

fun View.setVisible(visible: Boolean, invisible: Int = View.GONE) {
    visibility = if (visible) View.VISIBLE else invisible
}

/**
 * If a view captures clicks at all, then the parent won't ever receive touch events. This is a
 * problem when we're trying to capture link clicks, but tapping or long pressing other areas of
 * the view no longer work. Also problematic when we try to long press on an image in the message
 * view
 */
fun View.forwardTouches(parent: View) {
    var isLongClick = false

    setOnLongClickListener {
        isLongClick = true
        true
    }

    setOnTouchListener { v, event ->
        parent.onTouchEvent(event)

        when {
            event.action == MotionEvent.ACTION_UP && isLongClick -> {
                isLongClick = true
                true
            }

            event.action == MotionEvent.ACTION_DOWN -> {
                isLongClick = false
                v.onTouchEvent(event)
            }

            else -> v.onTouchEvent(event)
        }
    }
}

fun ViewPager.addOnPageChangeListener(listener: (Int) -> Unit) {
    addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            listener(position)
        }
    })
}

fun RecyclerView.scrapViews() {
    recycledViewPool.clear()
    adapter?.notifyDataSetChanged()
}

/*
 * Window inset helpers.
 *
 * Since targetSdk 37, Android 15+ forces edge-to-edge: the window no longer shrinks to avoid the
 * status and navigation bars, so views anchored to the top or bottom of the screen end up
 * underneath them. These helpers offset the affected views by the inset amount.
 *
 * They are deliberately additive rather than switching the window into edge-to-edge mode: on
 * API 23-34 the window still fits the system bars, the reported inset is 0, and every helper
 * becomes a no-op. Each listener captures the view's original padding/margin so repeated inset
 * dispatches (rotation, IME show/hide) don't accumulate, and none of them consume the insets —
 * sibling views still receive their own dispatch.
 */

/**
 * Pads the top of this view by the status bar height. For toolbars pinned to the top.
 *
 * If the view has a fixed height (as `@style/Toolbar`'s 56dp does), the height grows by the same
 * amount — otherwise the padding would eat into the toolbar's content area and push its title and
 * search field out of bounds. The extra height also lets the toolbar's background fill the status
 * bar region instead of leaving a gap.
 */
fun View.applyStatusBarTopPadding() {
    val initialPadding = paddingTop
    val initialHeight = layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        view.setPadding(top = initialPadding + top)
        if (initialHeight > 0) {
            view.layoutParams = view.layoutParams.also { it.height = initialHeight + top }
        }
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * Pads the bottom of this view by the navigation bar height, and by the keyboard height when
 * [includeIme] is set. For scrolling containers — they should set `clipToPadding=false` so the
 * content can still scroll into the inset area.
 */
fun View.applyNavBarBottomPadding(includeIme: Boolean = false) {
    val initial = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        view.setPadding(bottom = initial + insets.bottomInset(includeIme))
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * Same as [applyNavBarBottomPadding], but grows the bottom *margin* instead. For views whose
 * padding is part of their appearance — a circular FAB would squash its icon if padded.
 */
fun View.applyNavBarBottomMargin(includeIme: Boolean = false) {
    val initial = (layoutParams as? MarginLayoutParams)?.bottomMargin ?: return
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        (view.layoutParams as? MarginLayoutParams)?.let { params ->
            params.bottomMargin = initial + insets.bottomInset(includeIme)
            view.layoutParams = params
        }
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * Pads both the top and the bottom of this view by the corresponding system bar insets. A view can
 * only hold one inset listener, so containers that span the full screen height — the navigation
 * drawer, for one — need this instead of combining the single-edge helpers.
 */
fun View.applySystemBarsVerticalPadding() {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(top = initialTop + systemBars.top, bottom = initialBottom + systemBars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * The navigation bar inset, or the keyboard inset when it's taller. The keyboard is drawn over the
 * navigation bar, so the two must not be summed.
 */
private fun WindowInsetsCompat.bottomInset(includeIme: Boolean): Int {
    val navBar = getInsets(WindowInsetsCompat.Type.systemBars()).bottom
    if (!includeIme) return navBar
    return maxOf(navBar, getInsets(WindowInsetsCompat.Type.ime()).bottom)
}
