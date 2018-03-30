package com.optimizely.ab.notification;

import ch.qos.logback.classic.Level;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.internal.LogbackVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Map;

import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class NotificationCenterTest {
    private NotificationCenter notificationCenter;
    private ActivateNotificationListener activateNotification;
    private TrackNotificationListener trackNotification;

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Before
    public void initialize() {
        notificationCenter = new NotificationCenter();
        activateNotification = mock(ActivateNotificationListener.class);
        trackNotification = mock(TrackNotificationListener.class);
    }

    @Test
    public void testAddWrongTrackNotificationListener() {
        int notificationId = notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, trackNotification);
        logbackVerifier.expectMessage(Level.WARN,"Notification listener was the wrong type. It was not added to the notification center.");
        assertEquals(notificationId, -1);
        assertFalse(notificationCenter.removeNotificationListener(notificationId));

    }

    @Test
    public void testAddWrongActivateNotificationListener() {
        int notificationId = notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Track, activateNotification);
        logbackVerifier.expectMessage(Level.WARN,"Notification listener was the wrong type. It was not added to the notification center.");
        assertEquals(notificationId, -1);
        assertFalse(notificationCenter.removeNotificationListener(notificationId));
    }

    @Test
    public void testAddActivateNotificationTwice() {
        ActivateNotificationListener listener = new ActivateNotificationListener() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, String> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {

            }
        };
        int notificationId = notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, listener);
        int notificationId2 = notificationCenter.addNotificationListener(NotificationCenter.NotificationType.Activate, listener);
        logbackVerifier.expectMessage(Level.WARN,"Notificication listener was already added");
        assertEquals(notificationId2, -1);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
        notificationCenter.clearAllNotificationListeners();
    }

    @Test
    public void testAddActivateNotification() {
        int notificationId = notificationCenter.addActivateNotificationListener(new ActivateNotificationListenerInterface() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, String> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {

            }
        });
        assertNotSame(notificationId, -1);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
        notificationCenter.clearAllNotificationListeners();
    }

    @Test
    public void testAddTrackNotification() {
        int notificationId = notificationCenter.addTrackNotificationListener(new TrackNotificationListenerInterface() {
            @Override
            public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, String> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {

            }
        });
        assertNotSame(notificationId, -1);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
        notificationCenter.clearAllNotificationListeners();
    }

    @Test
    public void testNotificationTypeClasses() {
        assertEquals(NotificationCenter.NotificationType.Activate.getNotificationTypeClass(),
                ActivateNotificationListener.class);
        assertEquals(NotificationCenter.NotificationType.Track.getNotificationTypeClass(), TrackNotificationListener.class);
    }

    @Test
    public void testAddTrackNotificationInterface() {
        int notificationId = notificationCenter.addTrackNotificationListener(new TrackNotificationListenerInterface() {
            @Override
            public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, String> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {

            }
        });
        assertNotSame(notificationId, -1);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
        notificationCenter.clearAllNotificationListeners();
    }

    @Test
    public void testAddActivateNotificationInterface() {
        int notificationId = notificationCenter.addActivateNotificationListener(new ActivateNotificationListenerInterface() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, String> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {

            }
        });
        assertNotSame(notificationId, -1);
        assertTrue(notificationCenter.removeNotificationListener(notificationId));
        notificationCenter.clearAllNotificationListeners();
    }

}
