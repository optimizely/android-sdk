/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.shared;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Manages an Espresso like CountingIdlingResource
 */
public class CountingIdlingResourceManager {

    @Nullable private static CountingIdlingResourceInterface countingIdlingResource;
    @NonNull private static List<Pair<String, String>> eventList = new LinkedList<>();

    @Nullable public static CountingIdlingResourceInterface getIdlingResource(Context context) {
        if (countingIdlingResource == null) {
            countingIdlingResource = new CachedCounter(context);
        }
        return countingIdlingResource;
    }

    public static void setIdlingResource(@NonNull CountingIdlingResourceInterface countingIdlingResource) {
        CountingIdlingResourceManager.countingIdlingResource = countingIdlingResource;
    }

    public static void increment() {
        if (countingIdlingResource != null) {
            countingIdlingResource.increment();
        }
    }

    public static void decrement() {
        if (countingIdlingResource != null) {
            countingIdlingResource.decrement();
        }
    }

    public static void recordEvent(Pair<String, String> event) {
        if (countingIdlingResource != null) {
            eventList.add(event);
        }
    }

    public static void clearEvents() {
        eventList.clear();
    }
    
    public static List<Pair<String, String>> getEvents() {
        return eventList;
    }
}
