package com.tangosol.examples.security;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.WrapperCacheService;

/**
 * Example WrapperCacheService that demonstrates how entitlements can be
 * applied to a wrapped CacheService using the Subject passed from the
 * client via Coherence*Extend. This implementation delegates access control
 * for cache operations to the EntitledNamedCache.
 *
 * @author dag  2010.04.16
 */
public class EntitledCacheService
        extends WrapperCacheService
    {
    /**
     * Create a new EntitledCacheService.
     *
     * @param service     the wrapped CacheService
     */
    public EntitledCacheService(CacheService service)
        {
        super(service);
        }

    // ----- CacheService interface -----------------------------------------

    /**
     * {@inheritDoc}
     */
    public NamedCache ensureCache(String sName, ClassLoader loader)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
        return new EntitledNamedCache(super.ensureCache(sName, loader));
        }

    /**
     * {@inheritDoc}
     */
    public void releaseCache(NamedCache map)
        {
        if (map instanceof EntitledNamedCache)
            {
            EntitledNamedCache cache =  (EntitledNamedCache) map;
            SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_READER);
            map = cache.getNamedCache();
            }
        super.releaseCache(map);
        }

    /**
     * {@inheritDoc}
     */
    public void destroyCache(NamedCache map)
        {
        if (map instanceof EntitledNamedCache)
            {
            EntitledNamedCache cache =  (EntitledNamedCache) map;
            SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_ADMIN);
            map = cache.getNamedCache();
            }
        super.destroyCache(map);
        }
    }
