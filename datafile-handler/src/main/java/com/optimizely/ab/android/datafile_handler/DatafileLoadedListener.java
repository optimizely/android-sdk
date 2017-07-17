/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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

package com.optimizely.ab.android.datafile_handler;

import android.support.annotation.Nullable;
import android.content.Context;

/**
 * Listens for new Optimizely datafiles
 *
 * Datafiles can come from a local file or the CDN
 *
 *
 */
public interface DatafileLoadedListener {

    /**
     * Called with new datafile
     *
     * @param dataFile the datafile json, can be null if datafile loading failed.
     *
     */
     void onDatafileLoaded(@Nullable String dataFile);

    /**
     * datafile download stopped for some reason.
     *
     * @param context application context
     */
     void onStop(Context context);
}
