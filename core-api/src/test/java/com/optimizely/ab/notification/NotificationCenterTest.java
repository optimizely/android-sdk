package com.optimizely.ab.notification;

import ch.qos.logback.classic.Level;
import com.optimizely.ab.internal.LogbackVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class NotificationCenterTest {
    private NotificationCenter notificationCenter;
    private ActivateNotification activateNotification;
    private TrackNotification trackNotification;

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Before
    public void initialize() {
        notificationCenter = new NotificationCenter();
        activateNotification = mock(ActivateNotification.class);
        trackNotification = mock(TrackNotification.class);
    }

    @Test
    public void testAddWrongTrackNotificationListener() {
        int notificationId = notificationCenter.addNotification(NotificationCenter.NotificationType.Activate, trackNotification);
        logbackVerifier.expectMessage(Level.WARN,"Notification listener was the wrong type. It was not added to the notification center.");
        assertEquals(notificationId, -1);
        assertFalse(notificationCenter.removeNotification(notificationId));
    }

    @Test
    public void testAddWrongActivateNotificationListener() {
        int notificationId = notificationCenter.addNotification(NotificationCenter.NotificationType.Track, activateNotification);
        logbackVerifier.expectMessage(Level.WARN,"Notification listener was the wrong type. It was not added to the notification center.");
        assertEquals(notificationId, -1);
        assertFalse(notificationCenter.removeNotification(notificationId));
    }
}
