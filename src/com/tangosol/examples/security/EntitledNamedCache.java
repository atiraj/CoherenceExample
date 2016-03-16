package com.tangosol.examples.security;

import com.tangosol.net.NamedCache;
import com.tangosol.net.security.SecurityHelper;

import com.tangosol.net.cache.WrapperNamedCache;


import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

/**
 * Example WrapperNamedCache that demonstrates how entitlements can be applied
 * to a wrapped NamedCache using the Subject passed from the client via
 * Coherence*Extend. This implementation only allows clients with a specified
 * role to access the wrapped NamedCache.
 *
 * @author dag  2010.04.16
 */
public class EntitledNamedCache
        extends WrapperNamedCache
    {
    /**
     * Create a new EntitledNamedCache.
     *
     * @param cache  the wrapped NamedCache
     */
    public EntitledNamedCache(NamedCache cache)
        {
        super(cache, cache.getCacheName());
        }

    // ----- NamedCache interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void release()
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        super.release();
        }

    /**
     * {@inheritDoc}
     */
    public void destroy()
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_ADMIN);
        super.destroy();
        }

    /**
     * {@inheritDoc}
     */
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.put(oKey, oValue, cMillis);
        }

    /**
     * {@inheritDoc}
     */
    public void addMapListener(MapListener listener)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        super.addMapListener(new EntitledMapListener(listener));
        }

    /**
     * {@inheritDoc}
     */
    public void removeMapListener(MapListener listener)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        super.removeMapListener(listener);
        }

    /**
     * {@inheritDoc}
     */
    public void addMapListener(MapListener listener, Object oKey, boolean fLite)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        super.addMapListener(new EntitledMapListener(listener), oKey, fLite);
        }

    /**
     * {@inheritDoc}
     */
    public void removeMapListener(MapListener listener, Object oKey)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        super.removeMapListener(listener, oKey);
        }

    /**
     * {@inheritDoc}
     */
    public void addMapListener(MapListener listener, Filter filter, boolean fLite)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        super.addMapListener(new EntitledMapListener(listener), filter, fLite);
        }

    /**
     * {@inheritDoc}
     */
    public void removeMapListener(MapListener listener, Filter filter)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        super.removeMapListener(listener, filter);
        }

    /**
     * {@inheritDoc}
     */
    public int size()
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.size();
        }

    /**
     * {@inheritDoc}
     */
    public void clear()
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        super.clear();
        }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.isEmpty();
        }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object oKey)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.containsKey(oKey);
        }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object oValue)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.containsValue(oValue);
        }

    /**
     * {@inheritDoc}
     */
    public Collection values()
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.values();
        }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map map)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        super.putAll(map);
        }

    /**
     * {@inheritDoc}
     */
    public Set entrySet()
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.entrySet();
        }

    /**
     * {@inheritDoc}
     */
    public Set keySet()
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.keySet();
        }

    /**
     * {@inheritDoc}
     */
    public Object get(Object oKey)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.get(oKey);
        }

    /**
     * {@inheritDoc}
     */
    public Object remove(Object oKey)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.remove(oKey);
        }

    /**
     * {@inheritDoc}
     */
    public Object put(Object oKey, Object oValue)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.put(oKey, oValue);
        }

    /**
     * {@inheritDoc}
     */
    public Map getAll(Collection colKeys)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.getAll(colKeys);
        }

    /**
     * {@inheritDoc}
     */
    public boolean lock(Object oKey, long cWait)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.lock(oKey, cWait);
        }

    /**
     * {@inheritDoc}
     */
    public boolean lock(Object oKey)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.lock(oKey);
        }

    /**
     * {@inheritDoc}
     */
    public boolean unlock(Object oKey)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.unlock(oKey);
        }

    /**
     * {@inheritDoc}
     */
    public Set keySet(Filter filter)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.keySet(filter);
        }

    /**
     * {@inheritDoc}
     */
    public Set entrySet(Filter filter)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.entrySet(filter);
        }

    /**
     * {@inheritDoc}
     */
    public Set entrySet(Filter filter, Comparator comparator)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.entrySet(filter, comparator);
        }

    /**
     * {@inheritDoc}
     */
    public void addIndex(ValueExtractor extractor, boolean fOrdered, Comparator comparator)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        super.addIndex(extractor, fOrdered, comparator);
        }

    /**
     * {@inheritDoc}
     */
    public void removeIndex(ValueExtractor extractor)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        super.removeIndex(extractor);
        }

    /**
     * {@inheritDoc}
     */
    public Object invoke(Object oKey, EntryProcessor agent)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.invoke(oKey, agent);
        }

    /**
     * {@inheritDoc}
     */
    public Map invokeAll(Collection collKeys, EntryProcessor agent)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.invokeAll(collKeys, agent);
        }

    /**
     * {@inheritDoc}
     */
    public Map invokeAll(Filter filter, EntryProcessor agent)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.invokeAll(filter, agent);
        }

    /**
     * {@inheritDoc}
     */
    public Object aggregate(Collection collKeys, EntryAggregator agent)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.aggregate(collKeys, agent);
        }

    /**
     * {@inheritDoc}
     */
    public Object aggregate(Filter filter, EntryAggregator agent)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return super.aggregate(filter, agent);
        }

    // ----- inner class ----------------------------------------------------

    /**
     * Example MapListener that adds authorization to map events.
     */
    public class EntitledMapListener
            implements MapListener
        {
        // ----- constructors -------------------------------------------

        /**
         * Construct an EntitledMapListener with the current subject.
         * The subject will not be available in the security context
         * when events are received by the proxy at runtime.
         *
         * @param  listener  the MapListener
         */
        public EntitledMapListener(MapListener listener)
            {
            m_listener = listener;
            m_subject  = SecurityHelper.getCurrentSubject();
            }

        // ----- MapListener interface ----------------------------------

        /**
         * {@inheritDoc}
         */
        public void entryInserted(MapEvent mapEvent)
            {
            try
                {
                SecurityExampleHelper.checkAccess(
                    SecurityExampleHelper.ROLE_WRITER, m_subject);

                }
            catch (SecurityException e)
                {
                System.out.println("Access denied for entryInserted");
                return;
                }
            m_listener.entryInserted(mapEvent);
            }

        /**
         * {@inheritDoc}
         */
        public void entryUpdated(MapEvent mapEvent)
            {
            try
                {
                SecurityExampleHelper.checkAccess(
                    SecurityExampleHelper.ROLE_WRITER, m_subject);

                }
            catch (SecurityException e)
                {
                System.out.println("Access denied for entryUpdated");
                return;
                }
            m_listener.entryUpdated(mapEvent);
            }

        /**
         * {@inheritDoc}
         */
        public void entryDeleted(MapEvent mapEvent)
            {
            try
                {
                SecurityExampleHelper.checkAccess(
                    SecurityExampleHelper.ROLE_WRITER, m_subject);

                }
            catch (SecurityException e)
                {
                System.out.println("Access denied for entryDeleted");
                return;
                }
            m_listener.entryDeleted(mapEvent);
            }


        //  ----- data members ------------------------------------------

        /**
         * Subject from security context when the MapListener was registered.
         */
        private Subject m_subject;

        /**
         * Registered listener.
         */
        private MapListener m_listener;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the wrapped NamedCache.
     *
     * @return  the wrapped CacheService
     */
    public NamedCache getNamedCache()
        {
        return (NamedCache) getMap();
        }
    }