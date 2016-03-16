package com.tangosol.examples.events;

import com.tangosol.examples.pof.LazyProcessor;
import com.tangosol.examples.pof.RedistributionInvocable;
import com.tangosol.examples.pof.RedistributionInvocable.State;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * EventsExamples illustrates various features within the Unified Events
 * Model. This includes:
 * <ol>
 *     <li>Providing mean elapsed times split by event type.</li>
 *     <li>Illustrating the different semantics in throwing exceptions in pre
 *         events compared to post events.</li>
 *     <li>Illustrating logging of partition movement when enabled.</li>
 * </ol>
 *
 * @author hr  2011.11.30
 * @since Coherence 12.1.2
 */
@SuppressWarnings("unchecked")
public class EventsExamples
    {

    // ----- inner-class: EventsTimingExample -------------------------------

    /**
     * The EventsTimingExample is a catalyst for action to be performed by
     * {@link TimedTraceInterceptor}. This illustrates how the elapsed time
     * between pre and post events can be measured which are inserted into a
     * results cache. The entries inserted into the results cache are
     * displayed via the stdout of the process executing this class.
     */
    public static class EventsTimingExample
            implements Callable<Boolean>
        {
        // ----- Callable methods -------------------------------------------

        /**
         * {@inheritDoc}
         */
        public Boolean call() throws Exception
            {
            NamedCache<Integer, String> cacheEvents  = CacheFactory.getTypedCache("events", withTypes(Integer.class, String.class));
            NamedCache<String,  String> cacheResults = getCache("events-results");

            int cFrequency = ((PartitionedService) cacheEvents.getCacheService()).getOwnershipEnabledMembers().size();
            int cSet       = 110;

            MapListener ml = new MultiplexingMapListener<String, String>()
                {
                @Override
                protected void onMapEvent(MapEvent evt)
                    {
                    String[] asKey = ((String) evt.getKey()).split("-");

                    System.out.printf("Received stats [memberId=%s, eventType=%s, sample=%s] = %s\n",
                            asKey[0], asKey[1], asKey[2], evt.getNewValue());
                    }
                };

            try
                {
                cacheResults.addMapListener(ml);

                // execute inserts and updates
                for (int i = cFrequency; i > 0; --i)
                    {
                    for (int j = 1, cMax = cSet * cFrequency; j <= cMax; ++j)
                        {
                        cacheEvents.put(j, "value " + j);
                        }
                    }

                // execute processors
                int nTotalTime = 3000;
                int cThreads   = 5;
                int nSleepTime = nTotalTime / (cThreads * cSet * cFrequency);
                for (int i = 1, cMax = cSet * cFrequency; i <= cMax; ++i)
                    {
                    cacheEvents.invoke(i, new LazyProcessor(nSleepTime));
                    }
                }
            finally
                {
                cacheEvents.clear();
                cacheResults.removeMapListener(ml);
                cacheResults.clear();
                }
            return true;
            }
        }

    /**
     * The VetoedEventsExample is a catalyst for action to be performed by
     * {@link CantankerousInterceptor}. This illustrates the semantics of
     * throwing exceptions in pre and post events. The exceptions that are
     * expected to only be logged are inserted into a results cache. The
     * entries inserted into the results cache are
     * displayed via the stdout of the process executing this class.
     */
    public static class VetoedEventsExample
            implements Callable<Boolean>
        {

        // ----- Callable methods -------------------------------------------

        /**
         * {@inheritDoc}
         */
        public Boolean call() throws Exception
            {
            // perform events to cause interceptors to veto said event
            NamedCache<String, String> cacheVetoEvents = getCache("vetod-events");
            NamedCache<String, String> cacheResults    = getCache("events-results");

            MapListener ml = new MultiplexingMapListener<String, String>()
                {
                @Override
                protected void onMapEvent(MapEvent evt)
                    {
                    String[] asKey = ((String) evt.getKey()).split("-");

                    System.out.printf("Received event [memberId=%s, eventType=%s, count=%s] = %s\n",
                            asKey[0], asKey[1], asKey[2], evt.getNewValue());
                    }
                };
            try
                {
                int cSet     = 110;
                int cVeto    = 5;
                int cNonVeto = 10;
                int cVetoed  = 0;

                cacheResults.addMapListener(ml);

                for (int i = 1; i <= cSet; ++i)
                    {
                    boolean fVetoed = false;

                    if (i % (cSet / cVeto) == 0)
                        {
                        try
                            {
                            cacheVetoEvents.put(CantankerousInterceptor.VETO, "value: " + i);
                            }
                        catch(Throwable e)
                            {
                            fVetoed = true;
                            ++cVetoed;
                            }
                        }
                    if (i % (cSet / cNonVeto) == 0)
                        {
                        cacheVetoEvents.put(CantankerousInterceptor.NON_VETO, "value: " + i);
                        fVetoed = true;
                        }

                    if (!fVetoed)
                        {
                        cacheVetoEvents.put(String.valueOf(i), "value: " + i);
                        }
                    }
                System.out.printf("Number of veto'd events: %d\n", cVetoed);
                }
            finally
                {
                cacheVetoEvents.clear();
                cacheResults.removeMapListener(ml);
                cacheResults.clear();
                }
            return true;
            }
        }

    /**
     * The RedistributionEventsExample is a catalyst for action to be
     * performed by {@link RedistributionInterceptor}. This illustrates how
     * partition redistribution events can be logged.
     */
    public static class RedistributionEventsExample
            implements Callable<Boolean>
        {

        // ----- Callable methods -------------------------------------------

        /**
         * {@inheritDoc}
         */
        public Boolean call() throws Exception
            {
            // transfer events
            try
                {
                InvocationService is       = (InvocationService) CacheFactory.getService("InvocationService");
                Random            rnd      = new Random();
                int               cMembers = is.getInfo().getServiceMembers().size();

                if (cMembers < 3)
                    {
                    System.err.println("<Error> At least two members must exist for the RedistributionEvent example");
                    return false;
                    }

                // enable the logging of transfer event
                is.query(new RedistributionInvocable(State.ENABLE), null);

                Set<Member> isMembers = is.getInfo().getServiceMembers();
                isMembers.remove(is.getCluster().getLocalMember());

                Member memChosen = new ArrayList<Member>(isMembers).get(rnd.nextInt(isMembers.size()));

                System.out.printf("Choosing to kill member %s\n", memChosen);
                is.query(new RedistributionInvocable(State.KILL), Collections.singleton(memChosen));
                }
            finally
                {
                }

            return true;
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a cache correctly typed with String, String.
     *
     * @param sCacheName  the name of the cache
     *
     * @return the cache correctly typed.
     */
    private static NamedCache<String, String> getCache(String sCacheName)
        {
        return CacheFactory.getTypedCache(sCacheName, withTypes(String.class, String.class));
        }
    }
