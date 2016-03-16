package com.tangosol.examples.persistence;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

/**
 * A class to listen for Persistence Notifications and record the
 * duration of the operations. Please see {@link com.tangosol.persistence.PersistenceManagerMBean} for
 * descriptions of each of the JMX notifications.<br>
 * The supported notifications are:
 * <ul>
 *   <li>recover.begin</li>
 *   <li>recover.end</li>
 *   <li>create.snapshot.begin</li>
 *   <li>create.snapshot.end</li>
 *   <li>recover.snapshot.begin</li>
 *   <li>recover.snapshot.end</li>
 *   <li>remove.snapshot.begin</li>
 *   <li>remove.snapshot.end</li>
 *   <li>archive.snapshot.begin</li>
 *   <li>archive.snapshot.end</li>
 *   <li>retrieve.archived.snapshot.begin</li>
 *   <li>retrieve.archived.snapshot.end</li>
 *   <li>remove.archived.snapshot.begin</li>
 *   <li>remove.archived.snapshot.end</li>
 * </ul>
 *
 * @author tam  2015.03.18
 * @since  12.2.1
 */
public class NotificationWatcher
    {
    // ----- NotificationWatcher methods ------------------------------------

    /**
     * Run the example.
     *
     * @param asArgs array of services to list to for notifications.
     */
    public void runExample(String[] asArgs)
        {
        if (asArgs.length == 0)
            {
            System.out.println("Please provide a list of services to listen for notifications on");
            System.exit(1);
            }

        Cluster     cluster     = CacheFactory.ensureCluster();
        Set<String> setServices = new HashSet<>();

        for (String sServiceName : asArgs)
            {
            setServices.add(sServiceName);
            }

        logHeader("Getting MBeanServer...");

        MBeanServer server = MBeanHelper.findMBeanServer();

        logHeader("Retrieving Registry...");

        Registry registry = cluster.getManagement();

        if (server == null)
            {
            throw new RuntimeException("Unable to find MBeanServer");
            }

        try
            {
            for (String sServiceName : setServices)
                {
                logHeader("Registering listener for " + sServiceName);

                String sMBeanName = "Coherence:" + PersistenceHelper.getMBeanName(sServiceName);

                PersistenceHelper.waitForRegistration(registry, sMBeanName);

                ObjectName           oBeanName = new ObjectName(sMBeanName);
                NotificationListener listener  = new PersistenceNotificationListener(sServiceName);

                server.addNotificationListener(oBeanName, listener, null, null);
                f_mapListeners.put(oBeanName, listener);
                }

            logHeader("Waiting for notifications. CTRL-C to interrupt.");

            Thread.sleep(Long.MAX_VALUE);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }
        finally
            {
            // unregister all registered notifications
            f_mapListeners.forEach((k, v) ->
                {
                try
                    {
                    server.removeNotificationListener(k, v);
                    }
                catch (Exception eIgnore)
                    {
                    }
                });
            }
        }

    // ----- inner class: PersistenceNotificationListener --------------------

    /**
     * Class to respond to JMX notifications.
     */
    private class PersistenceNotificationListener
            implements NotificationListener
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a new listener for the given service.
         *
         * @param sServiceName  service name to listen for
         */
        public PersistenceNotificationListener(String sServiceName)
            {
            f_sServiceName = sServiceName;
            }

        // ----- NotificationListener interface -----------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void handleNotification(Notification notification, Object oHandback)
            {
            m_notification = notification;

            String sUserData = m_notification.getUserData().toString();
            String sMessage  = m_notification.getMessage() + " " + m_notification.getUserData();    // default

            // determine if its a begin or end notification
            String sType = m_notification.getType();

            if (sType.indexOf(BEGIN) > 0)
                {
                // save the begin time
                f_mapNotify.put(sType, m_notification.getTimeStamp());
                sMessage = m_notification.getMessage();
                }
            else if (sType.indexOf(END) > 0)
                {
                // try and find the begin notification
                String sBegin   = sType.replaceAll(END, BEGIN);
                Long   ldtStart = f_mapNotify.get(sBegin);

                if (ldtStart != null)
                    {
                    String sBaseType = sType.replaceAll(END, "");

                    sMessage = "  " + m_notification.getMessage()
                               + (sUserData == null || sUserData.isEmpty() ? "" : sUserData) + " (Duration="
                               + (m_notification.getTimeStamp() - ldtStart) + "ms)";
                    f_mapNotify.remove(sBegin);
                    }
                }
            else
                {
                sMessage = f_sServiceName + ": " + sType + "";
                }

            System.out.println(new Date(m_notification.getTimeStamp()) + " : " + f_sServiceName + " (" + sType + ") "
                               + sMessage);
            }

        /**
         * Suffix for notification begin.
         */
        private static final String BEGIN = ".begin";

        /**
         * Suffix for notification end.
         */
        private static final String END = ".end";

        /**
         * Service name for the listener.
         */
        private final String f_sServiceName;

        // ----- data members -----------------------------------------------

        /**
         * The last notification received, or null if one hasn't been
         * received yet.
         */
        private Notification m_notification;

        /**
         * Map of notifications and the start time.
         */
        private Map<String, Long> f_mapNotify = new ConcurrentHashMap<>();
        }

    // ----- data members ---------------------------------------------------

    /**
     * map containing the registered listeners and their object name.
     */
    private static final Map<ObjectName, NotificationListener> f_mapListeners = new HashMap<>();
    }
