package com.tangosol.examples.java8;

import com.tangosol.examples.pof.Address;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;

import com.tangosol.net.NamedCache;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.stream.RemoteCollectors;
import com.tangosol.util.stream.RemoteStream;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

import static com.tangosol.util.Filters.equal;
import static com.tangosol.util.Filters.greater;

/**
 * Examples of how to use streams to access Coherence caches.
 *
 * @author tam  2015.05.19
 * @since  12.2.1
 */
public class StreamsExample
    {
    // ----- StreamsExample methods -----------------------------------------

    /**
     * Run the Streams examples.
     *
     * @param cache the cache to use
     */
    public void runExample(NamedCache<ContactId, Contact> cache)
        {
        logHeader("StreamsExamples begin");
        System.out.println("Cache size is " + cache.size());

        // get the distinct years that the contacts were born in
        Set<Integer> setYears = cache.stream(Contact::getBirthDate)
                     .map(LocalDate::getYear)
                     .distinct()
                     .collect(RemoteCollectors.toSet());
        System.out.println("Distinct years the contacts were born in:\n" + setYears);

        // get a set of contact names where the age is > 40
        Set<String> setNames = cache.stream(greater(Contact::getAge, 40))
                     .map(entry -> entry.extract(Contact::getLastName) + " " +
                                   entry.extract(Contact::getFirstName) + " age=" +
                                   entry.extract(Contact::getAge))
                     .collect(RemoteCollectors.toSet());
        System.out.println("\nSet of contact names where age > 40:\n" + setNames);

        // get the distinct set of states for home addresses
        Set<String> setStates = cache.stream(Contact::getHomeAddress)
                     .map(Address::getState)
                     .distinct()
                     .collect(RemoteCollectors.toSet());
        System.out.println("\nDistinct set of states for home addresses:\n" + setStates);

        // get the average ages of all contacts
        double avgAge = cache.stream(Contact::getAge)
                      .mapToInt(Number::intValue)
                      .average()
                      .getAsDouble();
        System.out.println("\nThe average age of all contacts is: " + avgAge);

        // get average age using collectors
        avgAge = cache.stream()
                      .collect(RemoteCollectors.averagingInt(Contact::getAge));
        System.out.println("\nThe average age of all contacts using collect() is: " + avgAge);

        // get the maximum age of all contacts
        int maxAge = cache.stream(Contact::getAge)
                      .mapToInt(Number::intValue)
                      .max()
                      .orElse(0);  // in-case of no values
        System.out.println("\nThe maximum age of all contacts is: " + maxAge);

        // get average age of contacts working in MA
        // Note: The filter should be applied as early as possible, e.g as an argument
        // to the stream() call in order to take advantage of indexes
        avgAge = RemoteStream.toIntStream(cache.stream(equal(homeState(), "MA"), Contact::getAge))
                      .average()
                      .orElse(0);
        System.out.println("\nThe average age of contacts who work in MA is: " + avgAge);

        // get a map of birth months and the contact names for that month
        Map<String, List<Contact>> mapContacts =
                cache.stream()
                     .map(Map.Entry::getValue)
                     .collect(RemoteCollectors.groupingBy(birthMonth()));
        System.out.println("\nContacts born in each month:");
        mapContacts.forEach(
                (key, value) -> System.out.println("\nMonth: " + key + ", Contacts:\n" +
                                                   displayNames(value)));

        // get a map of states and the contacts living in each state
        Map<String, List<Contact>> mapStateContacts =
                cache.stream()
                     .map(Map.Entry::getValue)
                     .collect(RemoteCollectors.groupingBy(homeState()));
        System.out.println("\nContacts with home addresses in each state:");
        mapStateContacts.forEach(
                (key, value) -> System.out.println("State " + key + " has " + value.size() +
                                             " Contacts\n" + displayNames(value)));

        logHeader("StreamsExamples completed");
        }

    protected static ValueExtractor<Contact, String> birthMonth()
        {
        return contact -> contact.getBirthDate().getMonth().toString();
        }

    protected static ValueExtractor<Contact, String> homeState()
        {
        return contact -> contact.getHomeAddress().getState();
        }

    /**
     * Display a list of names from the supplied contacts.
     *
     * @param listContacts the List of contacts to display names from
     */
    private String displayNames(List<Contact> listContacts)
        {
        StringBuilder sb = new StringBuilder();
        for (Contact contact : listContacts)
            {
            sb.append("    ")
              .append(contact.getFirstName())
              .append(" ")
              .append(contact.getLastName())
              .append("\n");
            }

        return sb.toString();
        }
    }
