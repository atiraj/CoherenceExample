package com.tangosol.examples.contacts;

import com.tangosol.examples.pof.FilterFetcher;

import com.tangosol.net.InvocationService;

import com.tangosol.util.Filter;
import com.tangosol.util.FilterBuildingException;
import com.tangosol.util.QueryHelper;
import com.tangosol.util.ValueExtractor;

import java.util.Iterator;
import java.util.Map;

/**
 * FilterFactory is a utility class that provides a set of factory methods
 * that is used for building instances of {@link Filter} or
 * {@link ValueExtractor}.  This example uses an InvocationService to build
 * the Filters and ValueExtractors which requires a network round trip.
 * This class exists for compatibility with C++ and .Net and as a java
 * example of using the InvocationService.
 * <p/>
 * The FilterFactory api accepts a String that specifies the creation of rich
 * Filters in a format that should be familiar to anyone that understands
 * SQL WHERE clauses.  For example the Sring "street = 'Main' and state = 'TX'"
 * would create a tree of Filters that is the same as the following Java code:
 *
 *  new AndFilter(
 *        new EqualsFilter("getStreet","Main"),
 *        new EqualsFilter("getState","TX"));
 *
 * @see QueryHelper
 * <p/>
 * The factory methods catch a number of Exceptions from the implementation
 * stages and subsequently may throw an unchecked FilterBuildingException.
 *
 * @author djl  2010.02.18
 */
public class FilterFactory
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new FilterFactory
     *
     */
    public FilterFactory()
        {
        }

    /**
     * Construct a new FilterFactory and use the given InvocationService name
     * if the implementation needs it. C++ and .Net platforms need to use an
     * InvocationService to go over the network to create Filters and
     * ValueExtractors
     *
     * @param sServiceName   The name of the InvocationService to use
     */
    public FilterFactory(String sServiceName)
        {
        m_sServiceName = sServiceName;
        m_service = null;
        /* It isn't necessary to use the InvocationService under
         * java as QueryHelper is supported.
         * Un-comment the section below if you would like to see
         * the InvocationService in action.
         */

        /*
        try
            {
            m_service =
            (InvocationService) CacheFactory.getService(sServiceName);
            }
        catch (Exception e)
            {
            System.out.println("\n Exception hooking up to service: " +
                    sServiceName);
            System.out.println(e);
            m_service = null;
            }
        */

        }


    // ----- FilterBuilder API ----------------------------------------------

    /**
     * Make a new Filter from the given String.
     *
     * @param s  the Coherence Query Language string representing
     *           a Filter
     *
     * @return the constructed Filter
     *
     * @throws FilterBuildingException may be thrown
     */
    public Filter createFilter(String s)
        {
        return m_service == null ? QueryHelper.createFilter(s)
                : fetchFilter(s,null);
        }

    /**
     * Make a new Filter from the given String.
     *
     * @param s          a String in the Coherence Query Language representing
     *                   a Filter
     * @param aBindings  the array of Objects to use for Bind variables
     *
     * @return the constructed Filter
     *
     * @throws FilterBuildingException may be thrown
     */
    public Filter createFilter(String s, Object[] aBindings)
        {
        return m_service == null ? QueryHelper.createFilter(s, aBindings)
                : fetchFilter(s, aBindings);
        }

    /**
     * Make a new Filter from the given String.
     *
     * @param s            a String in the Coherence Query Language representing
     *                     a Filter
     * @param mapBindings  the Map of Objects to use for Bind variables
     *
     * @return the constructed Filter
     *
     * @throws FilterBuildingException may be thrown
     */
    public Filter createFilter(String s, Map mapBindings)
        {
        return QueryHelper.createFilter(s, mapBindings);
        }

    /**
     * Make a new Filter from the given String.
     *
     * @param s            a String in the Coherence Query Language representing
     *                     a Filter
     * @param aBindings    the array of Objects to use for Bind variables
     * @param mapBindings  the Map of Objects to use for Bind variables
     *
     * @return the constructed Filter
     *
     * @throws FilterBuildingException may be thrown
     */
    public Filter createFilter(String s, Object[] aBindings, Map mapBindings)
        {
        return QueryHelper.createFilter(s, aBindings, mapBindings);
        }

    /**
     * Make a new ValueExtracter from the given String.
     *
     * @param s  a String in the Coherence Query Language representing
     *           a ValueExtractor
     *
     * @return the constructed ValueExtractor
     *
     * @throws FilterBuildingException may be thrown
     */
    public ValueExtractor createExtractor(String s)
        {
        return m_service == null ? QueryHelper.createExtractor(s)
                : fetchExtractor(s);
        }

    // ----- Helper Methods -------------------------------------------------

    /**
     * Make a new Filter from the given String using InvocationService.
     *
     * @param s          a String in the Coherence Query Language representing
     *                   a Filter
     * @param aBindings  the array of Objects to use for Bind variables
     *
     * @return the constructed Filter or null depending on member status
     *         (@see InvocationService)
     */
    public Filter fetchFilter(String s, Object[] aBindings)
        {
        Map      result = m_service.query(new FilterFetcher(s, aBindings),null );
        Iterator iter   = result.values().iterator();

        if (iter.hasNext())
            {
            return (Filter) iter.next();
            }
        else
            {
            return null;
            }
        }

    /**
     * Make a new ValueExtracter from the given String.
     *
     * @param s  a String in the Coherence Query Language representing
     *           a ValueExtractor
     *
     * @return the constructed Filter or null depending on member status
     *         (@see InvocationService)
     *
     * @throws FilterBuildingException may be thrown
     */
    public ValueExtractor fetchExtractor(String s)
        {
        Map result    = m_service.query(new FilterFetcher(s,true), null);
        Iterator iter = result.values().iterator();

        if (iter.hasNext())
            {
            return (ValueExtractor) iter.next();
            }
        else
            {
            return null;
            }
        }


    // ----- data members ---------------------------------------------------

    /**
     * The service name to use
     */
    private String m_sServiceName;

    /**
     * An Object that is the result of each classified dispatch as the
     * tree of Objects is built
     */
    protected InvocationService m_service = null;
    }