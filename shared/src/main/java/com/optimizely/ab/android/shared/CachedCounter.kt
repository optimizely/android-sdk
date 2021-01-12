/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                   *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.shared

import android.content.Context
import org.slf4j.LoggerFactory

class CachedCounter(private val context: Context) : CountingIdlingResourceInterface {
    private val cache: Cache
    private val logger = LoggerFactory.getLogger("CachedCounter")
    @Synchronized
    override fun increment() {
        val value = cache.load(fileName)
        var `val` = Integer.valueOf(value!!)
        `val` += 1
        cache.save(fileName, `val`.toString())
    }

    @Synchronized
    override fun decrement() {
        val value = cache.load(fileName)
        var `val` = Integer.valueOf(value!!)
        if (`val` == 0) {
            return
        }
        `val` -= 1
        cache.save(fileName, `val`.toString())
    }

    companion object {
        private const val fileName = "OptlyCachedCounterKey"
    }

    init {
        cache = Cache(context, logger)
        if (!cache.exists(fileName)) {
            cache.save(fileName, "0")
        }
    }
}