package com.tangosol.examples.async;

import com.tangosol.examples.contacts.ExamplesHelper;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;


import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

import static com.tangosol.util.Filters.greater;

/**
 * Demonstrates Entry Processor examples using the {@link AsyncNamedCache} API.
 *
 * @author tam  2015.05.25
 * @since 12.2.1
 */
public class ProcessorExample
    {
    // ----- ProcessorExample methods ---------------------------------------

    /**
     * Run the async Entry Processor examples.
     *
     * @param cache the cache to use
     */
    public void runExample (NamedCache<ContactId, Contact> cache)
            throws ExecutionException, InterruptedException
        {
        logHeader("ProcessorExample begin");

        // Retrieve handle to perform async cache operations
        AsyncNamedCache<ContactId, Contact> asyncCache = cache.async();

        // change all last names to uppercase for contacts > 35 years old
        CompletableFuture<Map<ContactId, Integer>> cf = asyncCache.invokeAll(
                greater(Contact::getAge, 35),
                (entry) ->
                    {
                    Contact c = entry.getValue();
                    c.setLastName(c.getLastName().toUpperCase());
                    entry.setValue(c);
                    return null;
                    });

        System.out.println("Total entries modified is " + cf.get().size());
        ExamplesHelper.displayContactNames(cache, "Contacts");

        // change all names to uppercase with a EntryProcessor instance for all contacts
        CompletableFuture<Map<ContactId, Void>> cf2 = asyncCache.invokeAll(
                ProcessorExample::uppercaseInvocable);

        System.out.println("Total entries modified is " + cf2.get().size());
        ExamplesHelper.displayContactNames(cache, "Uppercase");

        // change all names back to lowercase using two entry processors (EP) in sequence.
        // this is better done in one EP but shows how we can chain EP
        CompletableFuture<Map<ContactId, Void>> cf3 =
                asyncCache.invokeAll(ProcessorExample::firstNameLowercase)
                          .whenComplete((result, exception) ->
                                  asyncCache.invokeAll(ProcessorExample::lastNameLowercase));

        System.out.println("Total entries modified is " + cf3.get().size());
        ExamplesHelper.displayContactNames(cache, "Lowercase");

        logHeader("ProcessorExample completed");
        }

    // ----- helpers --------------------------------------------------------

    /**
     * An entry processor called by method reference to update first name
     * and last name to uppercase.
     *
     * @param entry  the {@link InvocableMap.Entry} to update
     *
     * @return null
     */
    public static Void uppercaseInvocable(InvocableMap.Entry<ContactId, Contact> entry)
        {
        Contact contact = entry.getValue();
        contact.setFirstName(contact.getFirstName().toUpperCase());
        contact.setLastName(contact.getLastName().toUpperCase());
        entry.setValue(contact);
        return null;
        }

    /**
     * An entry processor called by method reference to update first name
     * to lowercase.
     *
     * @param entry  the {@link InvocableMap.Entry} to update
     *
     * @return null
     */
    public static Void firstNameLowercase(InvocableMap.Entry<ContactId, Contact> entry)
        {
        Contact contact = entry.getValue();
        contact.setFirstName(contact.getFirstName().toLowerCase());
        entry.setValue(contact);
        return null;
        }

    /**
     * An entry processor called by method reference to update last name
     * to lowercase.
     *
     * @param entry  the {@link InvocableMap.Entry} to update
     *
     * @return null
     */
    public static Void lastNameLowercase(InvocableMap.Entry<ContactId, Contact> entry)
        {
        Contact contact = entry.getValue();
        contact.setLastName(contact.getLastName().toLowerCase());
        entry.setValue(contact);
        return null;
        }
    }
