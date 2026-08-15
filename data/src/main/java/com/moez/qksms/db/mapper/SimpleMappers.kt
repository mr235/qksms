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
package com.moez.qksms.db.mapper

import com.moez.qksms.db.entity.BlockedMessageNotificationEntity
import com.moez.qksms.db.entity.BlockedNumberEntity
import com.moez.qksms.db.entity.SyncLogEntity
import com.moez.qksms.model.BlockedMessageNotification
import com.moez.qksms.model.BlockedNumber
import com.moez.qksms.model.SyncLog

/**
 * Straightforward 1:1 mappers for the simplest models: [BlockedNumber],
 * [BlockedMessageNotification], and [SyncLog].
 */

// ─── BlockedNumber ──────────────────────────────────────────────────────────────

fun BlockedNumberEntity.toDomain(): BlockedNumber = BlockedNumber(id = id, address = address)

fun BlockedNumber.toEntity(): BlockedNumberEntity = BlockedNumberEntity(id = id, address = address)

// ─── BlockedMessageNotification ─────────────────────────────────────────────────

fun BlockedMessageNotificationEntity.toDomain(): BlockedMessageNotification =
    BlockedMessageNotification(id = id, content = content)

fun BlockedMessageNotification.toEntity(): BlockedMessageNotificationEntity =
    BlockedMessageNotificationEntity(id = id, content = content)

// ─── SyncLog ────────────────────────────────────────────────────────────────────

fun SyncLogEntity.toDomain(): SyncLog = SyncLog().apply { date = this@toDomain.date }

fun SyncLog.toEntity(): SyncLogEntity = SyncLogEntity(date = date)
