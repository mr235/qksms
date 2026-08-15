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
package com.moez.qksms.common.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.moez.qksms.R
import com.moez.qksms.common.util.extensions.applyStatusBarTopPadding
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

abstract class QkActivity : AppCompatActivity() {

    protected val menu: Subject<Menu> = BehaviorSubject.create()

    /**
     * When true, [setContentView] pads the toolbar's top by the status-bar inset so its title
     * doesn't sit under the status bar. Subclasses that don't have a top-aligned toolbar (e.g.
     * `QkReplyActivity`'s centered dialog card) should override this to false.
     */
    protected open val applyToolbarStatusInset: Boolean = true

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onNewIntent(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        wireToolbar()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        wireToolbar()
    }

    private fun wireToolbar() {
        val toolbar = findViewById<View>(R.id.toolbar)
        setSupportActionBar(toolbar as? androidx.appcompat.widget.Toolbar)
        title = title // The title may have been set before layout inflation

        if (!applyToolbarStatusInset || toolbar == null) {
            return
        }

        // Pad the AppBarLayout rather than the toolbar when there is one: the collapsing title lives
        // in the AppBarLayout, so padding the pinned toolbar alone would leave the expanded title
        // under the status bar. `collapsing_toolbar.xml` deliberately has no fitsSystemWindows —
        // AppBarLayout only grows to fit the status bar when its height is wrap_content, and its
        // height there is a fixed 108dp, so the inset would be taken out of the bar's own space.
        val appBar = toolbar.appBarLayoutAncestor()
        (appBar ?: toolbar).applyStatusBarTopPadding()
    }

    private fun View.appBarLayoutAncestor(): AppBarLayout? {
        var node = parent
        while (node is View) {
            if (node is AppBarLayout) return node
            node = node.parent
        }
        return null
    }

    override fun setTitle(titleId: Int) {
        title = getString(titleId)
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        findViewById<TextView>(R.id.toolbarTitle)?.text = title
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        if (menu != null) {
            this.menu.onNext(menu)
        }
        return result
    }

    protected open fun showBackButton(show: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
    }

}