package com.tangosol.examples.async;

import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.GroupAggregator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

import static com.tangosol.util.Filters.greater;
import static com.tangosol.util.Filters.less;
import static com.tangosol.util.Filters.like;

/**
 * Demonstrates aggregator examples using the {@link AsyncNamedCache} API.
 *
 * @author tam  2015.05.25
 * @since 12.2.1
 */
public class AggregatorExample
    {
    // ----- AggregatorExample methods --------------------------------------
    /**
     * Run the async aggregation examples.
     *
     * @param cache the cache to use
     */
    public void runExample (NamedCache<ContactId, Contact> cache)
            throws ExecutionException, InterruptedException
        {
        logHeader("AggregatorExample begin");

        // Retrieve handle to perform async cache operations
        AsyncNamedCache<ContactId, Contact> asyncCache = cache.async();

        // get the count of entries in the cache where age > 35
        CompletableFuture<Integer> cf = asyncCache.aggregate(
                greater(Contact::getAge, 35), new Count<>());
        System.out.println("Count of contacts > 35 is: " + cf.get());

        // get the count of entries in the cache where age > 35 and if the
        // result is > 20 then display a message if there are more than
        // 10 contacts where age is > 60
        cf = asyncCache.aggregate(greater(Contact::getAge, 35), new Count<>())
                .whenComplete((result, exception) ->
                {
                if (result > 20)
                    {
                    asyncCache.aggregate(greater(Contact::getAge, 60), new Count<>())
                            .whenComplete((result60, exception60) ->
                            {
                            if (result60 > 10)
                                {
                                System.out.println("More that 10 contacts over 60. Count is " + result60);
                                }
                            });
                    }
                });

        while (!cf.isDone())
            {
            System.out.println("Waiting for completion");
            Thread.sleep(10L);
            }
        System.out.println("Result of aggregation is: " + cf.get());

        logHeader("AggregatorExample completed");
        }
    }
