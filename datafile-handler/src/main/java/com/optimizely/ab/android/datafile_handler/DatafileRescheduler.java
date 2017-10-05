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

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.JobWorkService;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Broadcast Receiver that handles app upgrade and phone restart broadcasts in order
 * to reschedule {@link DatafileService}
 * In order to use this class you must include the declaration in your AndroidManifest.xml.
 * <pre>
 * {@code
 * <receiver
 *  android:name="DatafileRescheduler"
 *  android:enabled="true"
 *  android:exported="false">
 *  <intent-filter>
 *      <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
 *      <action android:name="android.intent.action.BOOT_COMPLETED" />
 *  </intent-filter>
 * </receiver>
 * }
 * </pre>
 *
 * as well as set the download interval for datafile download in the Optimizely builder.
 */
public class DatafileRescheduler extends BroadcastReceiver {
    Logger logger = LoggerFactory.getLogger(DatafileRescheduler.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((context != null && intent != null) && (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED))) {
            logger.info("Received intent with action {}", intent.getAction());

            BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(
                    new Cache(context, LoggerFactory.getLogger(Cache.class)),
                    LoggerFactory.getLogger(BackgroundWatchersCache.class));
            Dispatcher dispatcher = new Dispatcher(context, backgroundWatchersCache, LoggerFactory.getLogger(Dispatcher.class));
            intent = new Intent(context, DatafileService.class);
            dispatcher.dispatch(intent);


        } else {
            logger.warn("Received invalid broadcast to data file rescheduler");
        }
    }

    /**
     * Handles building sending Intents to {@link DatafileService}
     *
     * This abstraction mostly makes unit testing easier
     */
    static class Dispatcher {

        @NonNull private final Context context;
        @NonNull private final BackgroundWatchersCache backgroundWatchersCache;
        @NonNull private final Logger logger;

        Dispatcher(@NonNull Context context, @NonNull BackgroundWatchersCache backgroundWatchersCache, @NonNull Logger logger) {
            this.context = context;
            this.backgroundWatchersCache = backgroundWatchersCache;
            this.logger = logger;
        }

        void dispatch(Intent intent) {
            List<String> projectIds = backgroundWatchersCache.getWatchingProjectIds();

            for (String projectId : projectIds) {
                // for scheduled jobs Android O and above, we use the JobScheduler and persistent periodic jobs
                // so, we don't need to do anything.
               // if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    intent.putExtra(DatafileService.EXTRA_PROJECT_ID, projectId);
                    ServiceScheduler.startService(context, DatafileService.JOB_ID, intent);
                    logger.info("Rescheduled data file watching for project {}", projectId);
                //}
            }

        }
    }
}
