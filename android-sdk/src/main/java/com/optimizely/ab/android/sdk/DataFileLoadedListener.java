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

package com.optimizely.ab.android.sdk;

import android.support.annotation.Nullable;

/**
 * Listens for new Optimizely datafiles
 *
 * Datafiles can come from a local file or the CDN
 *
 * @hide
 */
interface DataFileLoadedListener {

    /**
     * Called with new datafile
     *
     * @param dataFile the datafile json, can be null if datafile loading failed.
     *
     * @hide
     */
    void onDataFileLoaded(@Nullable String dataFile);
}
