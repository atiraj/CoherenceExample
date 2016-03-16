package com.tangosol.examples.events;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryEvent.Type;

import com.tangosol.util.BinaryEntry;

import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * A CantankerousInterceptor is an {@link EventInterceptor} implementation
 * that is argumentative in nature, hence the event of inserting certain keys
 * will result in {@link RuntimeException}s at either pre or post commit
 * phases. Throwing a {@link RuntimeException} during the pre-commit phase
 * will result in a rollback and related exception being propagated to the
 * client. A post-commit exception will result in a log event. A pre-commit
 * event is considered an *ING event with a post-commit event being a
 * *ED event.
 * <p>
 * This interceptor assumes it will be working against a cache with strings
 * as keys with the following items that will be considered objectionable.
 * <table>
 *     <tr><td>Key</td><td>Exception Thrown During Event</td></tr>
 *     <tr><td>{@value #VETO}</td><td>{@link Type#INSERTING} ||
 *             {@link Type#UPDATING}</td></tr>
 *     <tr><td>{@value #NON_VETO}</td><td>{@link Type#INSERTED} ||
 *             {@link Type#UPDATED}</td></tr>
 * </table>
 *
 * @author hr  2011.11.30
 *
 * @since Coherence 12.1.2
 */
@Interceptor(identifier = "cantankerous",
        entryEvents = {Type.INSERTING, Type.INSERTED, Type.UPDATING, Type.UPDATED})
public class CantankerousInterceptor
        implements EventInterceptor<EntryEvent<?, ?>>
    {
    // ----- EventInterceptor methods ---------------------------------------

    /**
     * Throws {@link RuntimeException} iff the key used for this event is
     * {@code #VETO} or {@code #NON_VETO}.
     *
     * @param event  the {@link Event} to be processed
     *
     * @throws RuntimeException iff {@code #VETO} || {@code #NON_VETO} are
     *         keys of the event
     */
    public void onEvent(EntryEvent<?, ?> event)
        {
        for (BinaryEntry binEntry : event.getEntrySet())
            {
            if (VETO.equals(binEntry.getKey()))
                {
                throw new RuntimeException("Objection! value = " + binEntry.getValue());
                }
            else if (NON_VETO.equals(binEntry.getKey())
                    && (event.getType() == Type.INSERTED || event.getType() == Type.UPDATED))
                {

                NamedCache<String, String> cacheResults = getResultsCache();

                int    nMemberId = binEntry.getContext().getCacheService().getCluster().getLocalMember().getId();
                String sMessage  = "Objection falls on deaf ears! value = " + binEntry.getValue();

                cacheResults.put(
                        String.format("%d-NON_VETO-%d", nMemberId, ++m_cNonVetoableEvents),
                        sMessage);

                throw new RuntimeException(sMessage);
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * String used to determine whether the event should be VETO'd during the
     * pre-commit phase.
     */
    public static final String VETO     = "VETO";

    /**
     * String used to determine whether the event should be VETO'd during the
     * post-commit phase.
     */
    public static final String NON_VETO = "NON-VETO";

    // ----- accessors ------------------------------------------------------

    public NamedCache<String, String> getResultsCache()
        {
        if (m_cacheResults == null)
            {
            m_cacheResults = CacheFactory.getTypedCache("events-results",
              withTypes(String.class, String.class));
            }
        return m_cacheResults;
        }

    public void setResultsCache(NamedCache<String, String> m_cacheResults)
        {
        this.m_cacheResults = m_cacheResults;
        }

    public int getNonVetoableEventsCount()
        {
        return m_cNonVetoableEvents;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A counter of the number of non-vetoable exceptions raised.
     */
    private int m_cNonVetoableEvents;

    /**
     * The cache that holds intercepted results
     */
    private NamedCache<String, String> m_cacheResults;
    }
