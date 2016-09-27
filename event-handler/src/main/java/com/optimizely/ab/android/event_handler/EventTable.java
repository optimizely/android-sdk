package com.optimizely.ab.android.event_handler;

import android.provider.BaseColumns;

/**
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 *
 * Constants for Event SQL table
 */
public final class EventTable implements BaseColumns {
    public static final String NAME = "event";

    class Column {
        public static final String _ID = BaseColumns._ID;
        public static final String URL = "url";
        public static final String REQUEST_BODY = "requestBody";
    }

    private EventTable() {}
}

