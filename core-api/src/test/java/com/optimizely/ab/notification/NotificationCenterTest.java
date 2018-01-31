package com.optimizely.ab.notification;

import ch.qos.logback.classic.Level;
import com.optimizely.ab.internal.LogbackVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Map;

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
