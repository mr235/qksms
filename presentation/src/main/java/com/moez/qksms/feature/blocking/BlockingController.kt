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
package com.moez.qksms.feature.blocking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.RouterTransaction
import com.jakewharton.rxbinding3.view.clicks
import com.moez.qksms.R
import com.moez.qksms.common.QkChangeHandler
import com.moez.qksms.common.base.QkController
import com.moez.qksms.common.util.Colors
import com.moez.qksms.common.util.extensions.animateLayoutChanges
import com.moez.qksms.common.widget.QkSwitch
import com.moez.qksms.databinding.BlockingControllerBinding
import com.moez.qksms.feature.blocking.manager.BlockingManagerController
import com.moez.qksms.feature.blocking.messages.BlockedMessagesController
import com.moez.qksms.feature.blocking.notification.BlockedMessagesNotificationController
import com.moez.qksms.feature.blocking.numbers.BlockedNumbersController
import com.moez.qksms.injection.appComponent
import javax.inject.Inject

class BlockingController : QkController<BlockingView, BlockingState, BlockingPresenter>(), BlockingView {

    override val blockingManagerIntent by lazy { binding.blockingManager.clicks() }
    override val blockedNumbersIntent by lazy { binding.blockedNumbers.clicks() }
    override val blockedMessagesIntent by lazy { binding.blockedMessages.clicks() }
    override val dropClickedIntent by lazy { binding.drop.clicks() }
    override val blockedMessagesNotificationIntent by lazy { binding.ignoreNotifications.clicks() }

    private lateinit var binding: BlockingControllerBinding

    @Inject lateinit var colors: Colors
    @Inject override lateinit var presenter: BlockingPresenter

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = BlockingControllerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated() {
        super.onViewCreated()
        binding.parent.postDelayed({ binding.parent?.animateLayoutChanges = true }, 100)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocking_title)
        showBackButton(true)
    }

    override fun render(state: BlockingState) {
        binding.blockingManager.summary = state.blockingManager
        binding.drop.findViewById<QkSwitch>(R.id.checkbox).isChecked = state.dropEnabled
        binding.blockedMessages.isEnabled = !state.dropEnabled
    }

    override fun openBlockedNumbers() {
        router.pushController(RouterTransaction.with(BlockedNumbersController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun openBlockedMessagesNotification() {
        router.pushController(RouterTransaction.with(BlockedMessagesNotificationController())
            .pushChangeHandler(QkChangeHandler())
            .popChangeHandler(QkChangeHandler()))
    }

    override fun openBlockedMessages() {
        router.pushController(RouterTransaction.with(BlockedMessagesController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

    override fun openBlockingManager() {
        router.pushController(RouterTransaction.with(BlockingManagerController())
                .pushChangeHandler(QkChangeHandler())
                .popChangeHandler(QkChangeHandler()))
    }

}
