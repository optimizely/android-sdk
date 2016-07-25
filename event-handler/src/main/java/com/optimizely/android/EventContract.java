package com.optimizely.android;

import android.provider.BaseColumns;

/**
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 *
 * Constants for Event SQL table
 */
public final class EventContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public EventContract() {}

    /* Inner class that defines the table contents */
    public static abstract class Event implements BaseColumns {
        public static final String TABLE_NAME = "event";
        public static final String COLUMN_NAME_URL = "url";
    }
}

