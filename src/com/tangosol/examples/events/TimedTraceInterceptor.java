package com.tangosol.examples.events;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.annotation.Interceptor.Order;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryEvent.Type;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;
import com.tangosol.net.events.partition.cache.Event;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * TimedTraceInterceptor provides timings between pre and post commit events
 * for each type of event i.e. inserts, updates, removes, and entry processor
 * execution.
 * <p>
 * These timings are collected and averaged at a sample rate defined by
 * parameter <tt>cSample</tt>. Additionally they are output to the log
 * at the same time. This implementation does maintain a strong reference to
 * the each binary key however this is removed upon receiving the post event
 * for the same key.
 *
 * @author hr  2011.11.30
 * @since Coherence 12.1.2
 */
@Interceptor(identifier = "trace", order = Order.HIGH)
public class TimedTraceInterceptor
        implements EventInterceptor<Event<? extends Enum<?>>>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default no-arg constructor.
     */
    public TimedTraceInterceptor()
        {
        this(DEFAULT_SAMPLE_RATE);
        }

    /**
     * Construct an TimedTraceInterceptor with specified {@link Event}
     * types and chain position.
     *
     * @param cSample   sample size to calculate mean and output statistics
     */
    public TimedTraceInterceptor(int cSample)
        {
        Map<Enum<?>, EventTimer> mapTimedEvents = m_mapTimedEvents = new HashMap<Enum<?>, EventTimer>(3);
        EventTimer insertTimer = new EventTimer(Type.INSERTED, cSample);
        EventTimer updateTimer = new EventTimer(Type.UPDATED, cSample);
        EventTimer removeTimer = new EventTimer(Type.REMOVED, cSample);
        EventTimer invocationTimer = new EventTimer(EntryProcessorEvent.Type.EXECUTED, cSample);

        mapTimedEvents.put(Type.INSERTED,  insertTimer);
        mapTimedEvents.put(Type.INSERTING, insertTimer);
        mapTimedEvents.put(Type.UPDATED,   updateTimer);
        mapTimedEvents.put(Type.UPDATING,  updateTimer);
        mapTimedEvents.put(Type.REMOVED,   removeTimer);
        mapTimedEvents.put(Type.REMOVING,  removeTimer);
        mapTimedEvents.put(EntryProcessorEvent.Type.EXECUTED,  invocationTimer);
        mapTimedEvents.put(EntryProcessorEvent.Type.EXECUTING, invocationTimer);
        }

    // ----- EventInterceptor methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void onEvent(Event<?> event)
        {
        if (event instanceof EntryEvent)
            {
            process((EntryEvent) event);
            }
        else if (event instanceof EntryProcessorEvent)
            {
            process((EntryProcessorEvent) event);
            }
        }

    /**
     * This method will be invoked upon execution of an entry processor and
     * will time its execution from prior to post execution, including any
     * backup requests that need to be made as a result.
     *
     * @param event  the {@link EntryProcessorEvent} that encompasses the
     *               requested event
     */
    protected void process(EntryProcessorEvent event)
        {
        EventTimer mapTimedEvents = m_mapTimedEvents.get(event.getType());

        for (BinaryEntry binEntry : event.getEntrySet())
            {
            if (event.getType() == EntryProcessorEvent.Type.EXECUTING)
                {
                mapTimedEvents.starting(binEntry);
                }
            else if (event.getType() == EntryProcessorEvent.Type.EXECUTED)
                {
                mapTimedEvents.started(binEntry);
                }
            }
        }

    /**
     * This method will be invoked upon execution of a data mutating request
     * and will time its execution from prior to post execution, including
     * any backup requests that need to be made as a result.
     *
     * @param event  the {@link EntryEvent} that encompasses the
     *               requested event
     */
    protected void process(EntryEvent<?, ?> event)
        {
        EventTimer mapTimedEvents = m_mapTimedEvents.get(event.getType());

        switch ((Type) event.getType())
            {
            case INSERTING:
            case UPDATING:
            case REMOVING:
                for (BinaryEntry binEntry : event.getEntrySet())
                    {
                    mapTimedEvents.starting(binEntry);
                    }
                break;
            case INSERTED:
            case UPDATED:
            case REMOVED:
                for (BinaryEntry binEntry : event.getEntrySet())
                    {
                    mapTimedEvents.started(binEntry);
                    }
                break;
            }
        }

    // ----- inner class: EventTimer ----------------------------------------

    /**
     * The EventTimer times the elapsed time for each event it is notified
     * of. It correlates the completion event based on equality comparisons
     * of the Binary provided. Additionally it calculates the mean based on a
     * sample set of <tt>cSample</tt> size. When reaching this sample set
     * a log will be made of the current sample set mean and the cumulative
     * mean.
     */
    protected class EventTimer
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct an EventTimer with the event type provided.
         *
         * @param eventType  the type of event this timer will be timing
         */
        protected EventTimer(Enum<?> eventType, int cSample)
            {
            m_eventType = eventType;
            m_cSampleSize = cSample;
            }

        /**
         * Notifies the timer of the execution of the provided key will
         * imminently commence.
         *
         * @param binEntry  the event will commence for this <tt>binEntry</tt>
         */
        public void starting(BinaryEntry binEntry)
            {
            m_mapElapsedTimes.put(binEntry.getBinaryKey(), System.nanoTime());
            }

        /**
         * Notifies the timer of the completion of the event for the provided
         * key.
         *
         * @param binEntry  the event has completed for this <tt>binEntry</tt>
         */
        public void started(BinaryEntry binEntry)
            {
            Long lStart = m_mapElapsedTimes.remove(binEntry.getBinaryKey());
            if (lStart == null)
                {
                return;
                }

            add(System.nanoTime() - lStart);
            }

        /**
         * Regardless of the specific data item add the elapsed time taken to
         * process the data item. Upon reaching the sample set size of events
         * calculate the mean, reset timings and continue.
         *
         * @param lElapsed  the number of nanos taken for a data item to
         *                  process
         */
        protected void add(long lElapsed)
            {
            AtomicInteger nEvents           = m_nEvents;
            AtomicLong    lTotalElapsed     = m_lTotalElapsed;
            int           nCurrEvents       = nEvents.incrementAndGet();
            long          lCurrTotalElapsed = lTotalElapsed.addAndGet(lElapsed);

            if (nCurrEvents % m_cSampleSize == 0)
                {
                nEvents.set(0);
                lTotalElapsed.set(0L);
                ++m_cSamples;

                long lMean   = lCurrTotalElapsed / nCurrEvents;
                     m_lMean = m_lMean == 0 ? lMean : lMean + m_lMean / 2;

                String sStats = String.format("EventStats[name = %s, sampleMean = %fms, mean = %fms]",
                        m_eventType, (double) lMean / 1000000, (double) m_lMean / 1000000);

                CacheFactory.log(sStats, CacheFactory.LOG_INFO);

                NamedCache<String, String> cacheResults = CacheFactory.getTypedCache("events-results", withoutTypeChecking());
                int                        nMemberId    = CacheFactory.getCluster().getLocalMember().getId();

                cacheResults.put(
                        String.format("%d-%s-%d", nMemberId, m_eventType.name(), m_cSamples),
                        sStats);
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * Sample size to calculate mean and output statistics.
         */
        private int               m_cSampleSize;

        /**
         * The start times for a number of Binary keys.
         */
        private Map<Binary, Long> m_mapElapsedTimes = new ConcurrentHashMap<Binary, Long>();

        /**
         * A counter of the total elapsed time.
         */
        private AtomicLong        m_lTotalElapsed = new AtomicLong();

        /**
         * A counter of the number of events processed
         */
        private AtomicInteger     m_nEvents = new AtomicInteger();

        /**
         * An average over time.
         */
        private long              m_lMean;

        /**
         * The number of samples taken.
         */
        private int               m_cSamples;

        /**
         * The type of event being timed.
         */
        private Enum<?>           m_eventType;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The sample size for elapsed times.
     */
    protected static final int DEFAULT_SAMPLE_RATE = 100;

    // ----- data members ---------------------------------------------------

    /**
     * A map of event types to their timers.
     */
    private Map<Enum<?>, EventTimer> m_mapTimedEvents;
    }
