package com.optimizely.ab.android.event_handler;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.slf4j.LoggerFactory;

public class EventWorker extends Worker {
    public static final String workerId = "EventWorker";

    EventDispatcher eventDispatcher;

    public EventWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        OptlyStorage optlyStorage = new OptlyStorage(context);
        EventClient eventClient = new EventClient(new Client(optlyStorage,
                LoggerFactory.getLogger(Client.class)), LoggerFactory.getLogger(EventClient.class));
        EventDAO eventDAO = EventDAO.getInstance(context, "1", LoggerFactory.getLogger(EventDAO.class));
        ServiceScheduler serviceScheduler = new ServiceScheduler(
                context,
                new ServiceScheduler.PendingIntentFactory(context),
                LoggerFactory.getLogger(ServiceScheduler.class));
        eventDispatcher = new EventDispatcher(context, optlyStorage, eventDAO, eventClient, serviceScheduler, LoggerFactory.getLogger(EventDispatcher.class));

    }

    @NonNull
    @Override
    public Result doWork() {
        String url = getInputData().getString("url");
        String body = getInputData().getString("body");
        boolean dispatched = true;

        if (url != null && !url.isEmpty() && body != null && !body.isEmpty()) {
            dispatched = eventDispatcher.dispatch(url, body);
        }
        else {
            dispatched = eventDispatcher.dispatch();
        }

        return dispatched ? Result.success() : Result.retry();
    }
}
