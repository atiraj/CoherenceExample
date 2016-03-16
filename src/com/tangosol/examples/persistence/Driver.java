package com.tangosol.examples.persistence;

import com.tangosol.coherence.dslquery.QueryPlus;

import com.tangosol.examples.contacts.ObserverExample;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * Driver to execute the Coherence Persistence examples.
 *
 * @author tam  2015.03.20
 * @since  12.2.1
 */
public class Driver
    {
    // ----- static methods -------------------------------------------------

    /**
     * Execute Persistence examples.
     *
     * @param asArgs  command line arguments
     */
    public static void main(String[] asArgs)
            throws Exception
        {
        String   sExample   = EXAMPLE_DEFAULT;
        String[] asExtraArg = new String[] {};

        if (asArgs.length > 3)
            {
            sExample = asArgs[3];
            }
        if (asArgs.length > 4)
            {
            // collect extra arguments
            asExtraArg = new String[asArgs.length - 4];
            System.arraycopy(asArgs, 4, asExtraArg, 0, asExtraArg.length);
            }

        // obtain a new helper to run persistence operations
        PersistenceHelper helper = new PersistenceHelper();

        switch (sExample)
            {
            case EXAMPLE_DEFAULT:
                new BasicSnapshotOperations().runExample(helper, getContactsCache("contacts"));
                break;

            case EXAMPLE_NOTIFICATIONS:
                new NotificationWatcher().runExample(
                        new String[] {"PartitionedPofCache", "PartitionedPofCache2"});
                break;

            case EXAMPLE_PARALLEL:
                new ParallelSnapshotOperations().runExample(helper,
                        getContactsCache("contacts"), getContactsCache("contacts2"));
                break;

            case EXAMPLE_COHQL:
                QueryPlus.main(asExtraArg);
                break;

            case EXAMPLE_POPULATE:
                int nSize = Integer.valueOf(asExtraArg.length != 0 ? asExtraArg[0] : "10000");

                NamedCache<ContactId, Contact> nc = getContactsCache("contacts");
                PersistenceHelper.populateData(nc, nSize);
                System.out.println("Contacts cache now has " + nc.size() + " entries");
                break;

            case EXAMPLE_ADD_LISTENER:
                NamedCache<ContactId, Contact> ncContacts = getContactsCache("contacts");

                ObserverExample observerExample = new ObserverExample();
                observerExample.observe(ncContacts);
                waitForReturn();
                observerExample.remove(ncContacts);
                break;

            case EXAMPLE_CONSOLE:
                CacheFactory.main(asExtraArg);
                break;

            default:
                System.out.println("Example " + sExample + " not found");
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a NamedCache reference using type checking.<p>
     * Note that the persistence/example-cache-config.xml contains the
     * key-type and key value to ensure strong type-checking.
     *
     * @param sCacheName  the cache to retrieve
     *
     * @return a NamedCache reference using type checking
     */
    private static NamedCache<ContactId, Contact> getContactsCache(String sCacheName)
        {
        return CacheFactory.getTypedCache(sCacheName, withTypes(ContactId.class, Contact.class));
        }

    /**
     * Wait for the user to press RETURN.
     */
    private static void waitForReturn()
        {
        System.out.println("Press RETURN to continue");
        System.console().readLine();
        }

    // ----- constants ------------------------------------------------------

    private static final String EXAMPLE_DEFAULT       = "default";
    private static final String EXAMPLE_NOTIFICATIONS = "notifications";
    private static final String EXAMPLE_PARALLEL      = "parallel";
    private static final String EXAMPLE_COHQL         = "cohql";
    private static final String EXAMPLE_POPULATE      = "populate";
    private static final String EXAMPLE_ADD_LISTENER  = "add-listener";
    private static final String EXAMPLE_CONSOLE       = "console";
    }

