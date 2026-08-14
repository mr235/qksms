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

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.multibindings.Multibinds

/**
 * Declares the ViewModel multibound map at [AppComponent] scope so that Dagger can
 * synthesise an (possibly empty) parent map. The concrete entries are contributed by
 * each Activity's `@ContributesAndroidInjector` subcomponent via `@IntoMap @ViewModelKey`
 * providers in the per-feature modules.
 *
 * Prior to Dagger 2.19 the parent map was auto-generated; newer versions require this
 * explicit declaration.
 */
@Module
abstract class ViewModelModule {

    @Multibinds
    abstract fun bindViewModels(): Map<Class<out ViewModel>, ViewModel>

}
