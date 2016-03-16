package com.tangosol.examples.async;

import com.tangosol.examples.contacts.DataGenerator;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;
import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * Driver executes all the Coherence Async examples.
 *
 * @author tam  2015.05.22
 * @since 12.2.1
 */
public class Driver
    {
    // ----- static methods -------------------------------------------------

    public static void main (String[] asArgs)
        {
        NamedCache<ContactId, Contact> cache =
          CacheFactory.getTypedCache("contacts", withTypes(ContactId.class, Contact.class));

        new Driver().runExamples(cache);
        }

    // ----- Driver methods -------------------------------------------------

    /**
     * Run the Java8 examples.
     *
     * @param cache  the cache to use
     */
    public void runExamples(NamedCache<ContactId, Contact> cache)
        {
        logHeader("Async examples begin");

        cache.clear();
        cache.putAll(DataGenerator.generateContacts(100));

        try
            {
            new DataAccessExample().runExample(cache);

            new ProcessorExample().runExample(cache);

            new AggregatorExample().runExample(cache);
            }
        catch (Exception e)
            {
            System.out.println("Example completed with an error. " + e.getMessage());
            e.printStackTrace();
            }

        logHeader("Async examples completed");
        }
    }