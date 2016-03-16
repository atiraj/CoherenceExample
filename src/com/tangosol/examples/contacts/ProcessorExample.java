
package com.tangosol.examples.contacts;


import com.tangosol.examples.pof.Address;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.examples.pof.OfficeUpdater;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.PofUpdater;
import com.tangosol.util.processor.UpdaterProcessor;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;
import static com.tangosol.util.Filters.equal;


/**
 * ProcessorExample demonstrates how to use a processor to modify data in the
 * cache. All Contacts who live in MA will have their work address updated.<p>
 * This example shows two ways to achieve this<br>
 * <ol>
 *  <li>Traditional entry processor class implementing InvocableMap.EntryProcessor</li>
 *  <li>Using UpdaterProcessor and PofUpdater to update POF value directly</li>
 *  <li>Using Java 8 Lambdas</li>
 * </ol>
 * <p>
 * Note: See Java8 examples for more processor examples.
 *
 * @author dag  2009.02.26
 */
public class ProcessorExample
    {
    // ----- ProcessorExample methods -----------------------------------

    /**
     * Perform the example updates to contacts.
     *
     * @param cache  Cache
     */
    public void execute(NamedCache<ContactId, Contact> cache)
        {
        logHeader("ProcessorExample begins");

        // people who live in Massachusetts moved to an in-state office
        Address addrWork = new Address("201 Newbury St.", "Yoyodyne, Ltd.",
                "Boston", "MA", "02116", "US");

        // Filter defines all contacts who live in MA
        ValueExtractor extractor = ValueExtractor.of(Contact::getHomeAddress).andThen(Address::getState);
        Filter filter = equal(extractor, "MA");

        logHeader("Update using traditional Entry Processor class");
        // example using class implementing InvocableMap.EntryProcessor
        cache.invokeAll(filter, new OfficeUpdater(addrWork));

        // Note: Since we are only updating one attribute its better to use an UpdaterProcessor
        logHeader("Update using UpdaterProcessor to modify POF value directly");
        cache.invokeAll(filter, new UpdaterProcessor<>(new PofUpdater(Contact.WORK_ADDRESS), addrWork));

        // update "incorrect" address using lambas
        addrWork.setStreet1("200 Newbury St.");

        logHeader("Update using Java 8 Lambdas");
        cache.invokeAll(filter, entry ->
            {
            Contact contact = entry.getValue();
            contact.setWorkAddress(addrWork);
            entry.setValue(contact);
            return null;
            });

        logHeader("Recalcualte the age attribute");
        cache.invokeAll(entry ->
            {
            Contact contact = entry.getValue();
            contact.calculateAge();
            entry.setValue(contact);
            return null;
            });

        logHeader("ProcessorExample completed");
        }
    }