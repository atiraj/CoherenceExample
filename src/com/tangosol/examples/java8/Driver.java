package com.tangosol.examples.java8;

import com.tangosol.examples.contacts.DataGenerator;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;
import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * Driver executes all the Coherence Java8 examples.
 *
 * @author tam  2015.05.18
 * @since  12.2.1
 */
public class Driver
    {
    // ----- static methods -------------------------------------------------

    public static void main(String[] asArgs)
        {
        NamedCache<ContactId, Contact> cache =
          CacheFactory.getTypedCache("contacts", withTypes(ContactId.class, Contact.class));

        new Driver().runExamples(cache);
        }

    // ----- Driver methods -----------------------------------------------

    /**
     * Run the Java8 examples.
     *
     * @param cache  the cache to use
     */
    public void runExamples(NamedCache<ContactId, Contact> cache)
        {
        logHeader("Java 8 examples begin");

        repopulate(cache);
        new StreamsExample().runExample(cache);

        repopulate(cache);
        new LambdaExample().runExample(cache);

        repopulate(cache);
        new MapDefaultMethodExample().runExample(cache);

        logHeader("Java 8 examples completed");
        }

    // ----- helpers ------------------------------------------------------

    /**
     * Repopulate the cache with a number of generated entries.
     *
     * @param cache  the cache to populate
     */
    private static void repopulate(NamedCache<ContactId, Contact> cache)
        {
        cache.clear();
        cache.putAll(DataGenerator.generateContacts(100));
        }
    }
