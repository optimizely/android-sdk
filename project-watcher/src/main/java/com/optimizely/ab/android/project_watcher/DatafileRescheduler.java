package com.optimizely.ab.android.project_watcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DatafileRescheduler extends BroadcastReceiver {
    Logger logger = LoggerFactory.getLogger(DatafileRescheduler.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((context != null && intent != null) && (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED))) {
            BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(context, LoggerFactory.getLogger(BackgroundWatchersCache.class));
            List<String> projectIds = backgroundWatchersCache.getWatchingProjectIds();
            for (String projectId : projectIds) {
                intent = new Intent(context, DatafileRescheduler.class);
                intent.putExtra(DataFileService.EXTRA_PROJECT_ID, projectId);
                context.startService(intent);

                logger.info("Rescheduled data file watching for project {}", projectId);
            }
        } else {
            logger.warn("Received invalid broadcast to data file rescheduler");
        }
    }
}
