
package com.tangosol.examples.contacts;


import com.tangosol.examples.pof.Address;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.net.NamedCache;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;

import com.tangosol.util.extractor.KeyExtractor;

import com.tangosol.util.filter.AlwaysFilter;

import java.util.Set;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

import static com.tangosol.util.Filters.equal;
import static com.tangosol.util.Filters.greater;
import static com.tangosol.util.Filters.like;
import static com.tangosol.util.Filters.notEqual;

/**
 * QueryExample runs sample queries for contacts.
 * <p/>
 * The purpose of this example is to show how to create Extractors on cache
 * data and how to create a KeyExtractor for the cache keys.
 * <p/>
 * It also illustrates how to use the indexes to filter the dataset in order to
 * efficiently create a matching set.
 * <p/>
 * Finally the example demonstrates how to use some of the built-in cache
 * aggregators to do simple computational tasks on the cache data.
 *
 * @author dag 2009.02.23
 */
public class QueryExample
    {
    // ----- QueryExample methods ---------------------------------------

    /**
     * Create indexes in the cache and query it for data.
     *
     * @param cache cache to query
     */
    public void query (NamedCache<ContactId, Contact> cache)
        {
        logHeader("QueryExample begins");

        // define extractors using method references to re-use for indexes and filters
        // Note: In versions prior to 12.2.1 this would be achieved by ChainedExtractors
        ValueExtractor<Contact, String> veHomeCity  = ValueExtractor.of(Contact::getHomeAddress).andThen(Address::getCity);
        ValueExtractor<Contact, String> veHomeState = ValueExtractor.of(Contact::getHomeAddress).andThen(Address::getState);
        ValueExtractor<Contact, String> veWorkState = ValueExtractor.of(Contact::getWorkAddress).andThen(Address::getState);

        // Add indexes to make queries more efficient
        // Ordered index applied to fields used in range and like filter queries
        cache.addIndex(KeyExtractor.of(ContactId::getLastName), /*fOrdered*/ true, /*comparator*/ null);
        cache.addIndex(Contact::getAge, /*fOrdered*/ true,  /*comparator*/ null);
        cache.addIndex(veHomeCity,      /*fOrdered*/ true,  /*comparator*/ null);
        cache.addIndex(veHomeState,     /*fOrdered*/ false, /*comparator*/ null);
        cache.addIndex(veWorkState,     /*fOrdered*/ false, /*comparator*/ null);

        // Find all contacts who live in Massachusetts
        Set setResults = cache.entrySet(equal(veHomeCity, "MA"));
        printResults("MA Residents", setResults);

        // Find all contacts who live in Massachusetts and work elsewhere
        setResults = cache.entrySet(equal(veHomeState, "MA")
                .and(notEqual(veWorkState, "MA")));
        printResults("MA Residents, Work Elsewhere", setResults);

        // Find all contacts whose city name begins with 'S'
        setResults = cache.entrySet(like(veHomeCity, "S%"));
        printResults("City Begins with S", setResults);

        final int nAge = 58;
        // Find all contacts who are older than nAge
        setResults = cache.entrySet(greater(Contact::getAge, nAge));
        printResults("Age > " + nAge, setResults);

        // Find all contacts with last name beginning with 'S' that live
        // in Massachusetts. Uses both key and value in the query
        setResults = cache.entrySet(like(KeyExtractor.of(ContactId::getLastName), "S%")
                .and(equal(veHomeState, "MA")));
        printResults("Last Name Begins with S and State Is MA", setResults);

        // Count contacts who are older than nAge for the entire cache dataset
        long cCount = cache.stream(greater(Contact::getAge, nAge)).count();
        System.out.println("count > " + nAge + ": " + cCount);

        // Find minimum age for the entire cache dataset.
        System.out.println("min age: " + cache.aggregate(AlwaysFilter.INSTANCE,
                new LongMin<Contact>(Contact::getAge)));

        // Calculate average age for the entire cache dataset.
        System.out.println("avg age: " + cache.aggregate(AlwaysFilter.INSTANCE,
                new DoubleAverage<Contact>(Contact::getAge)));

        // Find maximum age for the entire cache dataset.
        System.out.println("max age: " + cache.aggregate(AlwaysFilter.INSTANCE,
                new LongMax<Contact>(Contact::getAge)));

        logHeader("QueryExample completed");
        }

    /**
     * Print results of the query
     *
     * @param sTitle     the title that describes the results
     *
     * @param setResults a set of query results
     */
    private void printResults(String sTitle, Set setResults)
        {
        System.out.println(sTitle);
        for (Object setResult : setResults)
            {
            System.out.println(setResult);
            }
        }
    }
    