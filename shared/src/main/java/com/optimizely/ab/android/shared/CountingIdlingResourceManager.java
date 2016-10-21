/*
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.optimizely.ab.android.shared;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Manages an Espresso {@link CountingIdlingResource}
 */
@VisibleForTesting
public class CountingIdlingResourceManager {

    @Nullable private static CountingIdlingResource countingIdlingResource;
    @NonNull private static List<Pair<String, String>> eventList = new LinkedList<>();

    @VisibleForTesting
    @Nullable
    public static CountingIdlingResource getIdlingResource() {
        if (countingIdlingResource == null) {
            countingIdlingResource = new CountingIdlingResource("optimizely", true);
        }
        return countingIdlingResource;
    }

    @VisibleForTesting
    public static void setIdlingResource(@NonNull CountingIdlingResource countingIdlingResource) {
        CountingIdlingResourceManager.countingIdlingResource = countingIdlingResource;
    }

    @VisibleForTesting
    public static void increment() {
        if (countingIdlingResource != null) {
            countingIdlingResource.increment();
        }
    }

    @VisibleForTesting
    public static void decrement() {
        if (countingIdlingResource != null) {
            countingIdlingResource.decrement();
        }
    }

    @VisibleForTesting
    public static void recordEvent(Pair<String, String> event) {
        if (countingIdlingResource != null) {
            eventList.add(event);
        }
    }

    @VisibleForTesting
    public static void clearEvents() {
        eventList.clear();
    }

    @VisibleForTesting
    public static List<Pair<String, String>> getEvents() {
        return eventList;
    }
}
