package com.tangosol.examples.security;

import com.tangosol.io.pof.PofPrincipal;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ScopeResolver;
import com.tangosol.net.ScopedCacheFactoryBuilder;
import com.tangosol.net.security.SecurityHelper;

import javax.security.auth.Subject;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Map;

import static com.tangosol.util.Base.ensureClassLoader;

/**
 * This class is an instance of a {@link ScopedCacheFactoryBuilder}
 * that will create different {@link ConfigurableCacheFactory} instances
 * scoped to the current security {@link Subject}. This allows code running
 * in a single process to switch cache access back and forth between different
 * {@link Subject}s at runtime.
 *
 * @author jk 2015.01.29
 */
public class SubjectScopedCacheFactoryBuilder
        extends ScopedCacheFactoryBuilder
    {

    // ----- constructors ---------------------------------------------------

    public SubjectScopedCacheFactoryBuilder()
        {
        if (m_scopeResolver == null)
            {
            m_scopeResolver = new SubjectScopeResolver();
            }
        }

    // ----- ScopedCacheFactoryBuilder methods ------------------------------

    @Override
    protected ConfigurableCacheFactory getFactory(final String sConfigURI, final ClassLoader loader)
        {
        return AccessController.doPrivilegedWithCombiner((PrivilegedAction<ConfigurableCacheFactory>) () ->
            getConfigurableCacheFactoryInternal(sConfigURI, loader));
        }

    // ----- helper methods -------------------------------------------------

    private ConfigurableCacheFactory getConfigurableCacheFactoryInternal(String sConfigURI, ClassLoader loader)
        {
        ClassLoader loaderSearch     = loader = ensureClassLoader(loader);
        String      sRole            = getCurrentRole();
        String      sScopedConfigURI = (sRole == null) ? sConfigURI : sConfigURI + "/" + sRole;

        Map<String, ConfigurableCacheFactory> mapCCF;
        do
            {
            mapCCF = m_mapByLoader.get(loaderSearch);
            }
        while ((mapCCF == null || !mapCCF.containsKey(sScopedConfigURI))
              && (loaderSearch = loaderSearch.getParent()) != null);

        ConfigurableCacheFactory ccf = mapCCF == null ? null : mapCCF.get(sScopedConfigURI);
        if (ccf == null)
            {
            synchronized (this)
                {
                mapCCF = ensureConfigCCFMap(loader);
                ccf    = mapCCF.get(sScopedConfigURI);

                if (ccf == null)
                    {
                    ccf = buildFactory(sConfigURI, loader);
                    }
                mapCCF.put(sScopedConfigURI, ccf);
                }
            }
        return ccf;
        }

    protected String getCurrentRole()
        {
        Subject subject = SecurityHelper.getCurrentSubject();

        if (subject == null)
            {
            return null;
            }

        for (PofPrincipal pofPrincipal : subject.getPrincipals(PofPrincipal.class))
            {
            String sName = pofPrincipal.getName();
            if (sName.startsWith("role_"))
                {
                return sName;
                }
            }

        return null;
        }

    // ----- inner class: SubjectScopeResolver ------------------------------

    /**
     * This implementation of a {@link com.tangosol.net.ScopeResolver} sets the scope to the
     * name of the current subject so that {@link com.tangosol.net.ConfigurableCacheFactory}
     * instances may be scoped to a specific {@link javax.security.auth.Subject}.
     */
    public static class SubjectScopeResolver
            implements ScopeResolver
        {

        @Override
        public String resolveScopeName(String sConfigURI, ClassLoader loader, String sScopeName)
            {
            Subject subject = SecurityHelper.getCurrentSubject();

            if (subject == null)
                {
                return null;
                }

            for (PofPrincipal pofPrincipal : subject.getPrincipals(PofPrincipal.class))
                {
                String sName = pofPrincipal.getName();
                if (sName.startsWith("role_"))
                    {
                    return sName;
                    }
                }

            return null;
            }
        }
    }
