package com.tangosol.examples.events;

import com.tangosol.examples.events.EventsExamples.EventsTimingExample;
import com.tangosol.examples.events.EventsExamples.RedistributionEventsExample;
import com.tangosol.examples.events.EventsExamples.VetoedEventsExample;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.tangosol.examples.contacts.ExamplesHelper.log;
import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

/**
 * Driver executes all the Coherence Events examples.
 * <p>
 * Examples are invoked in this order:
 * <ol>
 *     <li><strong>Timed Events Example</strong> - In this example we time
 *     the elapsed time between pre and post events for each of the events
 *     that occur.</li>
 *     <li><strong>Veto Events Example</strong> - In this example we
 *     illustrate the semantic differences between raising exceptions during
 *     pre and post events.</li>
 *     <li><strong>Partition Transfer Events Example</strong> - This example
 *     illustrates the notifications provided upon partition redistribution
 *     events.</li>
 * </ol>
 *
 * @author hr  2011.11.30
 * @since Coherence 12.1.2
 */
public class Driver
    {

    // ----- static methods -------------------------------------------------

    /**
     * Execute Events examples.
     *
     * @param asArgs  command line arguments
     */
    public static void main(String[] asArgs)
        {
        logHeader("events examples begin");

        // Run examples
        for (Map.Entry<String, Callable<Boolean>> example : EVENTS_EXAMPLES.entrySet())
            {
            String sExample = example.getKey();

            try
                {
                logHeader(String.format("%s begin", sExample));
                log("\n\n");
                boolean fSuccess = example.getValue().call();
                logHeader(String.format("%s completed %ssuccessfully", sExample, fSuccess ? "" : "un"));
                }
            catch(Exception e)
                {
                logHeader(String.format("%s completed unsuccessfully with the following exception:", sExample));
                e.printStackTrace();
                }
            }

        logHeader("events examples completed");
        }

    // ----- constants ------------------------------------------------------

    /**
     * All the examples to be executed in insertion order.
     */
    protected static final Map<String, Callable<Boolean>> EVENTS_EXAMPLES = new LinkedHashMap<String, Callable<Boolean>>();

    /**
     * Default examples.
     */
    static
        {
        EVENTS_EXAMPLES.put("timing interceptor", new EventsTimingExample());
        EVENTS_EXAMPLES.put("veto interceptor", new VetoedEventsExample());
        EVENTS_EXAMPLES.put("redistribution interceptor", new RedistributionEventsExample());
        }
    }