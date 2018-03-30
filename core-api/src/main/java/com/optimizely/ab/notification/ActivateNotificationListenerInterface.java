package com.optimizely.ab.notification;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;

import javax.annotation.Nonnull;
import java.util.Map;

public interface ActivateNotificationListenerInterface {
    /**
     * onActivate called when an activate was triggered
     * @param experiment - The experiment object being activated.
     * @param userId - The userId passed into activate.
     * @param attributes - The filtered attribute list passed into activate
     * @param variation - The variation that was returned from activate.
     * @param event - The impression event that was triggered.
     */
    public void onActivate(@Nonnull Experiment experiment,
                                    @Nonnull String userId,
                                    @Nonnull Map<String, String> attributes,
                                    @Nonnull Variation variation,
                                    @Nonnull LogEvent event) ;

}
