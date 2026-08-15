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
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.jakewharton.rxbinding3.view.clicks
import com.moez.qksms.R
import com.moez.qksms.common.base.QkController
import com.moez.qksms.common.util.Colors
import com.moez.qksms.common.util.extensions.setBackgroundTint
import com.moez.qksms.common.util.extensions.applyNavBarBottomMargin
import com.moez.qksms.common.util.extensions.applyNavBarBottomPadding
import com.moez.qksms.common.util.extensions.setTint
import com.moez.qksms.common.widget.QkEditText
import com.moez.qksms.databinding.BlockedNumbersControllerBinding
import com.moez.qksms.injection.appComponent
import com.moez.qksms.util.PhoneNumberUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class BlockedNumbersController : QkController<BlockedNumbersView, BlockedNumbersState, BlockedNumbersPresenter>(),
    BlockedNumbersView {

    @Inject override lateinit var presenter: BlockedNumbersPresenter
    @Inject lateinit var colors: Colors
    @Inject lateinit var phoneNumberUtils: PhoneNumberUtils

    private val adapter = BlockedNumbersAdapter()
    private val saveAddressSubject: Subject<String> = PublishSubject.create()

    private lateinit var binding: BlockedNumbersControllerBinding

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = BlockedNumbersControllerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocked_numbers_title)
        showBackButton(true)
    }

    override fun onViewCreated() {
        super.onViewCreated()
        binding.add.setBackgroundTint(colors.theme().theme)
        binding.add.setTint(colors.theme().textPrimary)
        adapter.emptyView = binding.empty
        binding.numbers.adapter = adapter
        binding.numbers.applyNavBarBottomPadding()
        binding.add.applyNavBarBottomMargin()
    }

    override fun render(state: BlockedNumbersState) {
        adapter.updateData(state.numbers)
    }

    override fun unblockAddress(): Observable<Long> = adapter.unblockAddress
    override fun addAddress(): Observable<*> = binding.add.clicks()
    override fun saveAddress(): Observable<String> = saveAddressSubject

    override fun showAddDialog() {
        val layout = LayoutInflater.from(activity).inflate(R.layout.blocked_numbers_add_dialog, null)
        val textWatcher = BlockedNumberTextWatcher(layout.findViewById<QkEditText>(R.id.input), phoneNumberUtils)
        val dialog = AlertDialog.Builder(activity!!)
                .setView(layout)
                .setPositiveButton(R.string.blocked_numbers_dialog_block) { _, _ ->
                    saveAddressSubject.onNext(layout.findViewById<QkEditText>(R.id.input).text.toString())
                }
                .setNegativeButton(R.string.button_cancel) { _, _ -> }
                .setOnDismissListener { textWatcher.dispose() }
        dialog.show()
    }

}
