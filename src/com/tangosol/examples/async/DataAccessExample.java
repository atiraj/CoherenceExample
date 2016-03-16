package com.tangosol.examples.async;

import com.tangosol.examples.contacts.DataGenerator;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.tangosol.examples.contacts.ExamplesHelper.log;
import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

/**
 * Demonstrates basic data access examples using the {@link AsyncNamedCache} API.
 *
 * @author tam 2015.05.25
 * @since 12.2.1
 */
public class DataAccessExample
    {
    // ----- DataAccessExample methods --------------------------------------

    /**
     * Run the async data access examples.
     *
     * @param cache the named cache to use
     */
    public void runExample (NamedCache<ContactId, Contact> cache)
            throws ExecutionException, InterruptedException
        {
        logHeader("DataAccessExample begin");

        // Retrieve handle to perform async cache operations
        AsyncNamedCache<ContactId, Contact> asyncCache = cache.async();

        logHeader("Creating new contact");
        // create a new contact object to insert via async
        Contact newContact = DataGenerator.generateContact();
        ContactId contactId = new ContactId(newContact.getFirstName(),
                newContact.getLastName());

        // async returns CompletableFuture which implements Future
        Future future = asyncCache.put(contactId, newContact);

        // ignore result as put will return previous value
        future.get();

        logHeader("Retrieving new contact and applying function");
        // use CompletableFuture and apply a function
        CompletableFuture<Contact> cf = asyncCache.get(contactId).thenApply(
                DataAccessExample::uppercase);

        Contact contact = cf.get();
        log("Contact: \n" + contact);

        CompletableFuture<Contact> cfRemove = asyncCache.remove(contactId)
                            .whenComplete((result, error) ->
                                System.out.print("Contact removed=" + result.getFirstName() +
                                                 " " + result.getLastName() + ", error=" + error));
        // sleep to allow whenComplete() to fire
        Thread.sleep(1000L);

        Contact removedContact = cf.get();
        if (removedContact.equals(contact))
            {
            logHeader("Correct contact was removed");
            }
        else
            {
            logHeader("Contact was not removed");
            }

        logHeader("DataAccessExample completed");
        }

    /**
     * Return a Contact and convert the given contacts name to uppercase.
     * This method does not modify a cache entry, it is called with a
     * key and value returned from the cache.
     *
     * @param contact the contact to modify
     *
     * @return the modified contact
     */
    protected static Contact uppercase (Contact contact)
        {
        contact.setLastName(contact.getLastName().toUpperCase());
        contact.setFirstName(contact.getFirstName().toUpperCase());
        return contact;
        }
    }
