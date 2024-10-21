// Copyright 2022, Optimizely, Inc. and contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.optimizely.ab.android.odp

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.optimizely.ab.android.shared.OptlyStorage
import java.util.UUID

class VuidManager constructor(context: Context, isEnabled: Boolean = false) {
    private var _vuid = ""
    private val keyForVuid = "vuid" // stored in the private "optly" storage
    private var isEnabled: Boolean = isEnabled

    init {
        if (isEnabled) {
            this._vuid = load(context)
        } else {
            this.remove(context)
        }

    }

    fun getVuid(): String? {
        return if (isEnabled) this._vuid else null
    }

    fun isEnabled(): Boolean {
        return isEnabled
    }

    companion object {
        @JvmStatic
        fun isVuid(visitorId: String): Boolean {
            return visitorId.startsWith("vuid_", ignoreCase = true)
        }
    }

    @VisibleForTesting
    fun makeVuid(): String {
        val maxLength = 32 // required by ODP server

        // make sure UUIDv4 is used (not UUIDv1 or UUIDv6) since the trailing 5 chars will be truncated. See TDD for details.
        val vuidFull = "vuid_" + UUID.randomUUID().toString().replace("-", "").lowercase()
        return if (vuidFull.length <= maxLength) vuidFull else vuidFull.substring(0, maxLength)
    }

    @VisibleForTesting
    fun load(context: Context): String {
        val storage = OptlyStorage(context)
        val oldVuid = storage.getString(keyForVuid, null)
        if (oldVuid != null) return oldVuid

        val vuid = makeVuid()
        save(context, vuid)
        return vuid
    }

    @VisibleForTesting
    fun save(context: Context, vuid: String) {
        val storage = OptlyStorage(context)
        storage.saveString(keyForVuid, vuid)
    }

    @VisibleForTesting
    fun remove(context: Context) {
        val storage = OptlyStorage(context)
        storage.removeString(keyForVuid)
    }
}
