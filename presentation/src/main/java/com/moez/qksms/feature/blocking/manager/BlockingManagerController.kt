package com.moez.qksms.feature.blocking.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isInvisible
import com.jakewharton.rxbinding2.view.clicks
import com.moez.qksms.R
import com.moez.qksms.common.base.QkController
import com.moez.qksms.common.util.Colors
import com.moez.qksms.common.util.extensions.resolveThemeColor
import com.moez.qksms.databinding.BlockingManagerControllerBinding
import com.moez.qksms.injection.appComponent
import com.moez.qksms.util.Preferences
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class BlockingManagerController : QkController<BlockingManagerView, BlockingManagerState, BlockingManagerPresenter>(),
    BlockingManagerView {

    @Inject lateinit var colors: Colors
    @Inject override lateinit var presenter: BlockingManagerPresenter

    private val activityResumedSubject: PublishSubject<Unit> = PublishSubject.create()
    private lateinit var binding: BlockingManagerControllerBinding

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = BlockingManagerControllerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.blocking_manager_title)
        showBackButton(true)

        val states = arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(-android.R.attr.state_activated))

        val textTertiary = view.context.resolveThemeColor(android.R.attr.textColorTertiary)
        val imageTintList = ColorStateList(states, intArrayOf(colors.theme().theme, textTertiary))

        binding.qksms.findViewById<ImageView>(R.id.action).imageTintList = imageTintList
        binding.callBlocker.findViewById<ImageView>(R.id.action).imageTintList = imageTintList
        binding.callControl.findViewById<ImageView>(R.id.action).imageTintList = imageTintList
        binding.shouldIAnswer.findViewById<ImageView>(R.id.action).imageTintList = imageTintList
    }

    override fun onActivityResumed(activity: Activity) {
        activityResumedSubject.onNext(Unit)
    }

    override fun render(state: BlockingManagerState) {
        binding.qksms.findViewById<ImageView>(R.id.action).setImageResource(getActionIcon(true))
        binding.qksms.findViewById<ImageView>(R.id.action).isActivated = true
        binding.qksms.findViewById<ImageView>(R.id.action).isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_QKSMS

        binding.callBlocker.findViewById<ImageView>(R.id.action).setImageResource(getActionIcon(state.callBlockerInstalled))
        binding.callBlocker.findViewById<ImageView>(R.id.action).isActivated = state.callBlockerInstalled
        binding.callBlocker.findViewById<ImageView>(R.id.action).isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_CB
                && state.callBlockerInstalled

        binding.callControl.findViewById<ImageView>(R.id.action).setImageResource(getActionIcon(state.callControlInstalled))
        binding.callControl.findViewById<ImageView>(R.id.action).isActivated = state.callControlInstalled
        binding.callControl.findViewById<ImageView>(R.id.action).isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_CC
                && state.callControlInstalled

        binding.shouldIAnswer.findViewById<ImageView>(R.id.action).setImageResource(getActionIcon(state.siaInstalled))
        binding.shouldIAnswer.findViewById<ImageView>(R.id.action).isActivated = state.siaInstalled
        binding.shouldIAnswer.findViewById<ImageView>(R.id.action).isInvisible = state.blockingManager != Preferences.BLOCKING_MANAGER_SIA
                && state.siaInstalled
    }

    private fun getActionIcon(installed: Boolean): Int = when {
        !installed -> R.drawable.ic_chevron_right_black_24dp
        else -> R.drawable.ic_check_white_24dp
    }

    override fun activityResumed(): Observable<*> = activityResumedSubject
    override fun qksmsClicked(): Observable<*> = binding.qksms.clicks()
    override fun callBlockerClicked(): Observable<*> = binding.callBlocker.clicks()
    override fun callControlClicked(): Observable<*> = binding.callControl.clicks()
    override fun siaClicked(): Observable<*> = binding.shouldIAnswer.clicks()

    override fun showCopyDialog(manager: String): Single<Boolean> = Single.create { emitter ->
        AlertDialog.Builder(activity!!)
                .setTitle(R.string.blocking_manager_copy_title)
                .setMessage(resources?.getString(R.string.blocking_manager_copy_summary, manager))
                .setPositiveButton(R.string.button_continue) { _, _ -> emitter.onSuccess(true) }
                .setNegativeButton(R.string.button_cancel) { _, _ -> emitter.onSuccess(false) }
                .setCancelable(false)
                .show()
    }

}
