/**
 *
 *    Copyright 2017, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * This class handles impression and conversion notificationsListeners. It replaces NotificationBroadcaster and is intended to be
 * more flexible.
 */
public class NotificationCenter {
    /**
     * NotificationType is used for the notification types supported.
     */
    public enum NotificationType {

        Activate(ActivateNotification.class), // Activate was called. Track an impression event
        Track(TrackNotification.class); // Track was called.  Track a conversion event

        private Class notificationTypeClass;

        NotificationType(Class notificationClass) {
            this.notificationTypeClass = notificationClass;
        }

        public Class getNotificationTypeClass() {
            return notificationTypeClass;
        }
    };


    // the notification id is incremented and is assigned as the callback id, it can then be used to remove the notification.
    private int notificationListenerID = 1;

    final private static Logger logger = LoggerFactory.getLogger(NotificationCenter.class);

    // notification holder holds the id as well as the notification.
    private static class NotificationHolder
    {
        int notificationId;
        NotificationListener notificationListener;

        NotificationHolder(int id, NotificationListener notificationListener) {
            notificationId = id;
            this.notificationListener = notificationListener;
        }
    }

    /**
     * Instantiate a new NotificationCenter
     */
    public NotificationCenter() {
        notificationsListeners.put(NotificationType.Activate, new ArrayList<NotificationHolder>());
        notificationsListeners.put(NotificationType.Track, new ArrayList<NotificationHolder>());
    }

    // private list of notification by notification type.
    // we used a list so that notification order can mean something.
    private Map<NotificationType, ArrayList<NotificationHolder>> notificationsListeners =new HashMap<NotificationType, ArrayList<NotificationHolder>>();


    /**
     * Add a notification listener to the notification center.
     *
     * @param notificationType - enum NotificationType to add.
     * @param notificationListener - Notification to add.
     * @return the notification id used to remove the notification.  It is greater than 0 on success.
     */
    public int addNotification(NotificationType notificationType, NotificationListener notificationListener) {

        Class clazz = notificationType.notificationTypeClass;
        if (clazz == null || !clazz.isInstance(notificationListener)) {
            logger.warn("Notification listener was the wrong type. It was not added to the notification center.");
            return -1;
        }

        for (NotificationHolder holder : notificationsListeners.get(notificationType)) {
            if (holder.notificationListener == notificationListener) {
                logger.warn("Notificication listener was already added");
                return -1;
            }
        }
        int id = notificationListenerID++;
        notificationsListeners.get(notificationType).add(new NotificationHolder(id, notificationListener ));
        logger.info("Notification listener {} was added with id {}", notificationListener.toString(), id);
        return id;
    }

    /**
     * Remove the notification listener based on the notificationId passed back from addNotification.
     * @param notificationID the id passed back from add notification.
     * @return true if removed otherwise false (if the notification is already registered, it returns false).
     */
   public boolean removeNotification(int notificationID) {
       for (NotificationType type : NotificationType.values()) {
            for (NotificationHolder holder : notificationsListeners.get(type)) {
                if (holder.notificationId == notificationID) {
                    notificationsListeners.get(type).remove(holder);
                    logger.info("Notification listener removed {}", notificationID);
                    return true;
                }
            }
        }

        logger.warn("Notification listener with id {} not found", notificationID);

        return false;
    }

    /**
     * Clear out all the notification listeners.
     */
    public void clearAllNotifications() {
        for (NotificationType type : NotificationType.values()) {
            clearNotifications(type);
        }
    }

    /**
     * Clear notification listeners by notification type.
     * @param notificationType type of notificationsListeners to remove.
     */
    public void clearNotifications(NotificationType notificationType) {
        notificationsListeners.get(notificationType).clear();
    }

    // fire a notificaiton of a certain type.  The arg list changes depending on the type of notification sent.
    public void sendNotifications(NotificationType notificationType, Object ...args) {
        ArrayList<NotificationHolder> holders = notificationsListeners.get(notificationType);
        for (NotificationHolder holder : holders) {
            try {
                holder.notificationListener.notify(args);
            }
            catch (Exception e) {
                logger.error("Unexpected exception calling notification listener {}", holder.notificationId, e);
            }
        }
    }

}
