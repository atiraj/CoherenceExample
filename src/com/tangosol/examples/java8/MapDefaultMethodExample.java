package com.tangosol.examples.java8;

import com.tangosol.examples.contacts.DataGenerator;
import com.tangosol.examples.contacts.ExamplesHelper;
import com.tangosol.examples.pof.Address;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;

import com.tangosol.net.NamedCache;

import com.tangosol.util.ValueExtractor;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

import static com.tangosol.util.Filters.equal;
import static com.tangosol.util.Filters.lessEqual;

/**
 * Examples of how to use Map Default Methods which have been overridden in
 * Coherence 12.2.1.<p>
 * This example shows a subset of these overall methods and is listed below:
 * <ul>
 *     <li>forEach</li>
 *     <li>replace</li>
 *     <li>replaceAll</li>
 *     <li>putIfAbsent</li>
 *     <li>computeIfPresent</li>
 * </ul>
 * <p>
 * The full list can be found at https://docs.oracle.com/javase/8/docs/api/java/util/Map.html
 * or JavaDoc for {@link NamedCache} and the interfaces it implements.
 *
 * @author tam  2015.05.19
 * @since  12.2.1
 */
public class MapDefaultMethodExample
    {
    // ----- DefaultMethodExample methods -----------------------------------

    /**
     * Run the Default method examples.
     *
     * @param cache the cache to use
     */
    public void runExample (NamedCache<ContactId, Contact> cache)
        {
        logHeader("DefaultMethodExamples begin");
        System.out.println("Cache size is " + cache.size());

        // display all entries where the state is MA
        cache.forEach(equal(ValueExtractor
                                    .of(Contact::getHomeAddress)
                                    .andThen(Address::getState), "MA"),
                (key, value) -> System.out.println("Contact: " + value));

        // display a list of contacts who are <= 30 years old
        cache.forEach(lessEqual(Contact::getAge, 30),
                (key, value) ->
                        System.out.println(value.getFirstName() + " " + value.getLastName() +
                                           ", age=" + value.getAge()));

        // change all last names to uppercase using replaceAll
        cache.replaceAll((key, value) ->
            {
            value.setLastName(value.getLastName().toUpperCase());
            return value;
            });

        ExamplesHelper.displayContactNames(cache, "with last name uppercase");

        // generate a new contact we know does not exist
        Contact   newContact = DataGenerator.generateContact();
        ContactId contactId = new ContactId(newContact.getFirstName(), newContact.getLastName());

        System.out.println("\nNew Contact should be null. Value is: " +
                           cache.get(contactId));

        // putIfAbsent will put newContact if contactId does not exist
        cache.putIfAbsent(contactId, newContact);
        System.out.println("\nNew Contact should now exist. Value is: " +
                           cache.get(contactId));

        // if the value is present then run the function against the entry
        cache.computeIfPresent(contactId, LambdaExample::lowercase);
        System.out.println("\nContact should now have all lowercase names: " +
                           cache.get(contactId));

        // update the home address zipcode in newContact
        Address homeAddress = newContact.getHomeAddress();
        homeAddress.setZipCode("99999");

        // replace the contact if the key exists
        cache.replace(contactId, newContact);
        System.out.println("\nContact should now have 99999 zipcode: " +
                           cache.get(contactId));

        logHeader("DefaultMethodExamples completed");
        }
    }
