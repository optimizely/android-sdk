/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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
import android.util.Pair
import java.util.*

/**
 * Manages an Espresso like CountingIdlingResource
 */
object CountingIdlingResourceManager {
    private var countingIdlingResource: CountingIdlingResourceInterface? = null
    private val eventList: MutableList<Pair<String, String>> = LinkedList()
    fun getIdlingResource(context: Context?): CountingIdlingResourceInterface? {
        if (countingIdlingResource == null) {
            countingIdlingResource = CachedCounter(context!!)
        }
        return countingIdlingResource
    }

    @JvmStatic
    fun setIdlingResource(countingIdlingResource: CountingIdlingResourceInterface) {
        CountingIdlingResourceManager.countingIdlingResource = countingIdlingResource
    }

    fun increment() {
        if (countingIdlingResource != null) {
            countingIdlingResource!!.increment()
        }
    }

    fun decrement() {
        if (countingIdlingResource != null) {
            countingIdlingResource!!.decrement()
        }
    }

    fun recordEvent(event: Pair<String, String>) {
        if (countingIdlingResource != null) {
            eventList.add(event)
        }
    }

    @JvmStatic
    fun clearEvents() {
        eventList.clear()
    }

    @JvmStatic
    val events: List<Pair<String, String>>
        get() = eventList
}