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
package com.moez.qksms.injection

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.lifecycle.ViewModelProvider
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.moez.qksms.blocking.BlockingClient
import com.moez.qksms.blocking.BlockingManager
import com.moez.qksms.common.ViewModelFactory
import com.moez.qksms.common.util.BillingManagerImpl
import com.moez.qksms.common.util.NotificationManagerImpl
import com.moez.qksms.common.util.ShortcutManagerImpl
import com.moez.qksms.feature.conversationinfo.injection.ConversationInfoComponent
import com.moez.qksms.feature.themepicker.injection.ThemePickerComponent
import com.moez.qksms.listener.ContactAddedListener
import com.moez.qksms.listener.ContactAddedListenerImpl
import com.moez.qksms.manager.ActiveConversationManager
import com.moez.qksms.manager.ActiveConversationManagerImpl
import com.moez.qksms.manager.AlarmManager
import com.moez.qksms.manager.AlarmManagerImpl
import com.moez.qksms.manager.AnalyticsManager
import com.moez.qksms.manager.AnalyticsManagerImpl
import com.moez.qksms.manager.BillingManager
import com.moez.qksms.manager.ChangelogManager
import com.moez.qksms.manager.ChangelogManagerImpl
import com.moez.qksms.manager.KeyManager
import com.moez.qksms.manager.KeyManagerImpl
import com.moez.qksms.manager.NotificationManager
import com.moez.qksms.manager.PermissionManager
import com.moez.qksms.manager.PermissionManagerImpl
import com.moez.qksms.manager.RatingManager
import com.moez.qksms.manager.ReferralManager
import com.moez.qksms.manager.ReferralManagerImpl
import com.moez.qksms.manager.ShortcutManager
import com.moez.qksms.manager.WidgetManager
import com.moez.qksms.manager.WidgetManagerImpl
import com.moez.qksms.mapper.CursorToContact
import com.moez.qksms.mapper.CursorToContactGroup
import com.moez.qksms.mapper.CursorToContactGroupImpl
import com.moez.qksms.mapper.CursorToContactGroupMember
import com.moez.qksms.mapper.CursorToContactGroupMemberImpl
import com.moez.qksms.mapper.CursorToContactImpl
import com.moez.qksms.mapper.CursorToConversation
import com.moez.qksms.mapper.CursorToConversationImpl
import com.moez.qksms.mapper.CursorToMessage
import com.moez.qksms.mapper.CursorToMessageImpl
import com.moez.qksms.mapper.CursorToPart
import com.moez.qksms.mapper.CursorToPartImpl
import com.moez.qksms.mapper.CursorToRecipient
import com.moez.qksms.mapper.CursorToRecipientImpl
import com.moez.qksms.mapper.RatingManagerImpl
import com.moez.qksms.repository.BackupRepository
import com.moez.qksms.repository.BackupRepositoryImpl
import com.moez.qksms.repository.BlockingRepository
import com.moez.qksms.repository.BlockingRepositoryImpl
import com.moez.qksms.repository.ContactRepository
import com.moez.qksms.repository.ContactRepositoryImpl
import com.moez.qksms.repository.ConversationRepository
import com.moez.qksms.repository.ConversationRepositoryImpl
import com.moez.qksms.repository.MessageRepository
import com.moez.qksms.repository.MessageRepositoryImpl
import com.moez.qksms.repository.ScheduledMessageRepository
import com.moez.qksms.repository.ScheduledMessageRepositoryImpl
import com.moez.qksms.repository.SyncRepository
import com.moez.qksms.repository.SyncRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(subcomponents = [
    ConversationInfoComponent::class,
    ThemePickerComponent::class])
class AppModule(private var application: Application) {

    @Provides
    @Singleton
    fun provideContext(): Context = application

    @Provides
    fun provideContentResolver(context: Context): ContentResolver = context.contentResolver

    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideRxPreferences(preferences: SharedPreferences): RxSharedPreferences {
        return RxSharedPreferences.create(preferences)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
    }

    @Provides
    fun provideViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory = factory

    // Listener

    @Provides
    fun provideContactAddedListener(listener: ContactAddedListenerImpl): ContactAddedListener = listener

    // Manager

    @Provides
    fun provideBillingManager(manager: BillingManagerImpl): BillingManager = manager

    @Provides
    fun provideActiveConversationManager(manager: ActiveConversationManagerImpl): ActiveConversationManager = manager

    @Provides
    fun provideAlarmManager(manager: AlarmManagerImpl): AlarmManager = manager

    @Provides
    fun provideAnalyticsManager(manager: AnalyticsManagerImpl): AnalyticsManager = manager

    @Provides
    fun blockingClient(manager: BlockingManager): BlockingClient = manager

    @Provides
    fun changelogManager(manager: ChangelogManagerImpl): ChangelogManager = manager

    @Provides
    fun provideKeyManager(manager: KeyManagerImpl): KeyManager = manager

    @Provides
    fun provideNotificationsManager(manager: NotificationManagerImpl): NotificationManager = manager

    @Provides
    fun providePermissionsManager(manager: PermissionManagerImpl): PermissionManager = manager

    @Provides
    fun provideRatingManager(manager: RatingManagerImpl): RatingManager = manager

    @Provides
    fun provideShortcutManager(manager: ShortcutManagerImpl): ShortcutManager = manager

    @Provides
    fun provideReferralManager(manager: ReferralManagerImpl): ReferralManager = manager

    @Provides
    fun provideWidgetManager(manager: WidgetManagerImpl): WidgetManager = manager

    // Mapper

    @Provides
    fun provideCursorToContact(mapper: CursorToContactImpl): CursorToContact = mapper

    @Provides
    fun provideCursorToContactGroup(mapper: CursorToContactGroupImpl): CursorToContactGroup = mapper

    @Provides
    fun provideCursorToContactGroupMember(mapper: CursorToContactGroupMemberImpl): CursorToContactGroupMember = mapper

    @Provides
    fun provideCursorToConversation(mapper: CursorToConversationImpl): CursorToConversation = mapper

    @Provides
    fun provideCursorToMessage(mapper: CursorToMessageImpl): CursorToMessage = mapper

    @Provides
    fun provideCursorToPart(mapper: CursorToPartImpl): CursorToPart = mapper

    @Provides
    fun provideCursorToRecipient(mapper: CursorToRecipientImpl): CursorToRecipient = mapper

    // Repository

    @Provides
    fun provideBackupRepository(repository: BackupRepositoryImpl): BackupRepository = repository

    @Provides
    fun provideBlockingRepository(repository: BlockingRepositoryImpl): BlockingRepository = repository

    @Provides
    fun provideContactRepository(repository: ContactRepositoryImpl): ContactRepository = repository

    @Provides
    fun provideConversationRepository(repository: ConversationRepositoryImpl): ConversationRepository = repository

    @Provides
    fun provideMessageRepository(repository: MessageRepositoryImpl): MessageRepository = repository

    @Provides
    fun provideScheduledMessagesRepository(repository: ScheduledMessageRepositoryImpl): ScheduledMessageRepository = repository

    @Provides
    fun provideSyncRepository(repository: SyncRepositoryImpl): SyncRepository = repository

}