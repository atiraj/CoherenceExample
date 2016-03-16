
package com.tangosol.examples.contacts;


import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.net.NamedCache;
import com.tangosol.util.listener.SimpleMapListener;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;


/**
 * ObserverExample demonstrates how to use a MapListener to monitor cache events.
 *
 * @author dag/tam  2015.05.19
 */
public class ObserverExample
    {
    // ----- ObserverExample methods -------------------------------------

    /**
     * Observe changes to the contacts cache.
     *
     * @param cache  target cache
     */
    public void observe(NamedCache<ContactId, Contact> cache)
        {
        logHeader("ObserverExample begins");

        m_listener = new SimpleMapListener<ContactId, Contact>()
            .addInsertHandler((event) -> System.out.println("entry inserted:\n" + event.getNewValue()))
            .addDeleteHandler((event) -> System.out.println("entry deleted:\n"  + event.getOldValue()))
            .addUpdateHandler((event)->
                {
                System.out.println("entry updated");
                System.out.println("old value:\n" + event.getOldValue());
                System.out.println("new value:\n" + event.getNewValue());
                }
            );

        cache.addMapListener(m_listener);

        logHeader("SimpleMapListener added");
        }

    /**
     * Stop observing changes to the contacts cache.
     *
     * @param cache  target cache
     */
    public void remove(NamedCache<ContactId, Contact> cache)
        {
        cache.removeMapListener(m_listener);
        logHeader("SimpleMapListener removed");
        logHeader("ObserverExample completed");
        }

    // ----- data members --------------------------------------------------

    /**
     * The SimpleMapListener observing changes.
     */
    protected SimpleMapListener<ContactId, Contact> m_listener;
    }
