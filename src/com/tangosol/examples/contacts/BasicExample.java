package com.tangosol.examples.contacts;

import com.tangosol.examples.pof.ContactId;
import com.tangosol.examples.pof.Contact;

import com.tangosol.net.NamedCache;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

/**
 * BasicExample shows basic cache operations like adding, getting and removing
 * data.
 *
 * @author dag  2009.03.04
 */
public class BasicExample
    {
    // ----- BasicExample methods -------------------------------------------

    /**
     * Execute a cycle of basic operations.
     *
     * @param cache  target cache
     */
    public void execute(NamedCache<ContactId, Contact> cache)
        {
        Contact   contact   = DataGenerator.generateContact();
        ContactId contactId = new ContactId(contact.getFirstName(),
                contact.getLastName());

        logHeader("BasicExample begins");
        // associate a ContactId with a Contact in the cache
        cache.put(contactId, contact);

        // retrieve the Contact associated with a ContactId from the cache
        contact = cache.get(contactId);
        System.out.println(contact);

        // remove mapping of ContactId to Contact from the cache.
        cache.remove(contactId);
        logHeader("BasicExample completed");
        }
    }
