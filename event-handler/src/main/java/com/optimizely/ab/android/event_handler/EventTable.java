/**
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
package com.optimizely.ab.android.event_handler;

import android.provider.BaseColumns;

/**
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 *
 * Constants for Event SQL table
 */
public final class EventTable implements BaseColumns {
    public static final String NAME = "event";

    private EventTable() {
    }

    class Column {
        public static final String _ID = BaseColumns._ID;
        public static final String URL = "url";
        public static final String REQUEST_BODY = "requestBody";
    }
}

