package com.tangosol.examples.contacts;

import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.net.NamedCache;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;

import com.tangosol.util.Filter;

import java.util.Iterator;
import java.util.Set;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

/**
 * QueryLanguageExample runs sample queries for contacts.
 * <p/>
 * The purpose of this example is to show how to create Extractors and
 * Filters by using the Coherence QueryHelper interface indirectly through
 * a FilterFactory. This example does the same operations as the
 * QueryExample in the contacts example.
 *
 * @author djl 2010.03.01
 */
public class QueryLanguageExample
    {
    // ----- QueryLanguageExample methods ---------------------------------------

    /**
     * Create indexes in the cache and query it for data.
     *
     * @param cache cache to query
     * @param ff    the FilterFactory to use for making Filters
                    and ValueExtractors
     */
     public void query(NamedCache<ContactId, Contact> cache, FilterFactory ff)
         {
         logHeader("QueryLanguageExample begins");

         // Add indexes to make queries more efficient
         // Ordered index applied to fields used in range and like filter queries
         cache.addIndex(ff.createExtractor("age"), /*fOrdered*/ true,
                 /*comparator*/ null);
         cache.addIndex(ff.createExtractor("key().lastName"),
                  /*fOrdered*/ true, /*comparator*/ null);
         cache.addIndex(ff.createExtractor("homeAddress.city"),
                 /*fOrdered*/ true, /*comparator*/ null);
         cache.addIndex(ff.createExtractor("homeAddress.state"),
                 /*fOrdered*/ false, /*comparator*/ null);
         cache.addIndex(ff.createExtractor("workAddress.state"),
                 /*fOrdered*/ false, /*comparator*/ null);

         // Find all contacts who live in Massachusetts
         Set setResults = cache.entrySet(ff.createFilter("homeAddress.state = 'MA'"));
         printResults("MA Residents", setResults);

         // Find all contacts who live in Massachusetts and work elsewhere
         setResults = cache.entrySet(
              ff.createFilter("homeAddress.state is 'MA' and workAddress is not 'MA'"));
         printResults("MA Residents, Work Elsewhere", setResults);

         // Find all contacts whose city name begins with 'S'
         setResults = cache.entrySet(ff.createFilter("homeAddress.city like 'S%'"));
         printResults("City Begins with S", setResults);

         final int nAge = 58;
         Object[] aEnv  =  new Object[] {new Integer(nAge)};
         // Find all contacts who are older than nAge
         setResults = cache.entrySet(ff.createFilter("age > ?1",aEnv));
         printResults("Age > " + nAge, setResults);

         // Find all contacts with last name beginning with 'S' that live
         // in Massachusetts. Uses both key and value in the query
         setResults = cache.entrySet(
                  ff.createFilter("lastName like 'S%' and homeAddress.state = 'MA'"));
         printResults("Last Name Begins with S and State Is MA", setResults);

         // Count contacts who are older than nAge for the entire cache dataset.
         System.out.println("count > " + nAge + ": " + cache.aggregate(
                 ff.createFilter("age > ?1", aEnv), new Count<ContactId, Contact>()));

         // Find minimum age for the entire cache dataset.
         Filter always = ff.createFilter("true");
         System.out.println("min age: " + cache.aggregate(always, new LongMin<Integer>("getAge")));

         // Calculate average age for the entire cache dataset.
         System.out.println("avg age: " + cache.aggregate(always, new DoubleAverage<Integer>("getAge")));

         // Find maximum age for the entire cache dataset.
         System.out.println("max age: " + cache.aggregate(always, new LongMax<Integer>("getAge")));

	     logHeader("QueryLanguageExample completed");
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
        for (Iterator iter = setResults.iterator(); iter.hasNext();)
            {
            System.out.println(iter.next());
            }
        }
    }
