package com.tangosol.examples.security;

import com.tangosol.examples.pof.ExampleInvocable;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import java.security.PrivilegedExceptionAction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;

import static com.tangosol.examples.contacts.ExamplesHelper.log;
import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * This class demonstrates simplified role based access control.
 * <p>
 * The role policies are defined in SecurityExampleHelper. Enforcement
 * is done by EntitledCacheService, EntitledNamedCache, and EntitledMapListener.
 *
 * @author dag 2010.04.16
 */
@SuppressWarnings(value = "unchecked")
public class AccessControlExample
    {
    // ----- static methods -------------------------------------------------

    /**
     * Demonstrate role based read/write access to the cache.
     */
    public static void shouldBeAbleToReadFromAndWriteToCache()
            throws Exception
        {
        logHeader("cache read/write example begins");

        // Someone with read and write role
        Subject subject = SecurityExampleHelper.login("JohnWhorfin");

        // All cache access should be in the scope of a PrivilegedExceptionAction
        Subject.doAs(subject, (PrivilegedExceptionAction) () ->
            {
            NamedCache<String, String> cache = getSecurityCache();

            cache.put("myKey", "myValue");
            cache.get("myKey");
            log("Success: read and write allowed");

            return null;
            });

        logHeader("cache read/write example complete");
        }

    /**
     * Demonstrate role based read only access to the cache.
     */
    public static void shouldBeAbleToReadFromButNotWriteToCache()
            throws Exception
        {
        logHeader("cache read only example begins");

        // Someone with reader role can read but not write
        Subject subject = SecurityExampleHelper.login("JohnBigboote");

        // All cache access should be in the scope of a PrivilegedExceptionAction
        Subject.doAs(subject, (PrivilegedExceptionAction) () ->
            {
            NamedCache<String, String> cache = getSecurityCache();

            cache.get("myKey");
            log("Success: read allowed");

            try
                {
                cache.put("anotherKey", "anotherValue");

                throw new AssertionError("Expected a security exception - Subject has read-only access");
                }
            catch (Exception e)
                {
                // expected SecurityException as Subject cannot write to cache
                }

            return null;
            });

        logHeader("cache read only example complete");
        }

    /**
     * Demonstrate role based read only access cannot destroy cache
     */
    public static void shouldBeAbleToReadFromButNotDestroyCache()
            throws Exception
        {
        logHeader("cache destroy with read only access example begins");

        // Someone with writer role cannot call destroy
        Subject subject = SecurityExampleHelper.login("JohnWhorfin");

        // All cache access should be in the scope of a PrivilegedExceptionAction
        Subject.doAs(subject, (PrivilegedExceptionAction) () ->
            {
            NamedCache<String, String> cache = getSecurityCache();

            try
                {
                cache.destroy();

                throw new AssertionError("Expected a security exception - Subject has read only access");
                }
            catch (Exception e)
                {
                // expected SecurityException as Subject cannot destroy cache
                }

            return null;
            });

        logHeader("cache destroy with read only access example complete");
        }

    /**
     * Demonstrate admin role can destroy cache
     */
    public static void shouldBeAbleToDestroyCacheWithAdminRights()
            throws Exception
        {
        logHeader("cache destroy example begins");

        // Someone with admin role can call destroy
        Subject subject = SecurityExampleHelper.login("BuckarooBanzai");

        // All cache access should be in the scope of a PrivilegedExceptionAction
        Subject.doAs(subject, (PrivilegedExceptionAction) () ->
            {
            NamedCache<String, String> cache = getSecurityCache();
            cache.destroy();
            log("Success: Correctly allowed to " + "destroy the cache");

            return null;
            });

        logHeader("cache destroy example complete");
        }

    /**
     * Demonstrate that read/write Subject can run Invocables.
     */
    public static void shouldRunInvocablesWithReadWriteAccess()
            throws Exception
        {
        logHeader("InvocationService read/write access control example begins");

        // Someone with writer role can run invocables
        Subject subject = SecurityExampleHelper.login("JohnWhorfin");

        // All cache access should be in the scope of a PrivilegedExceptionAction
        Subject.doAs(subject, (PrivilegedExceptionAction) () ->
            {
            InvocationService service = (InvocationService) CacheFactory
                    .getService(SecurityExampleHelper.INVOCATION_SERVICE_NAME);

            service.query(new ExampleInvocable(), null);
            log("Success: Correctly allowed to use the invocation service");

            return null;
            });

        logHeader("InvocationService read/write access control example complete");
        }

    /**
     * Demonstrate that read-only Subject cannot run Invocables.
     */
    public static void shouldNotRunInvocablesWithReadOnlyAccess()
            throws Exception
        {
        logHeader("InvocationService read-only access control example begins");

        // Someone with reader role cannot cannot run invocables
        Subject subject = SecurityExampleHelper.login("JohnBigboote");

        // All cache access should be in the scope of a PrivilegedExceptionAction
        Subject.doAs(subject, (PrivilegedExceptionAction) () ->
            {
            InvocationService service = (InvocationService)  CacheFactory
                    .getService(SecurityExampleHelper.INVOCATION_SERVICE_NAME);

            try
                {
                service.query(new ExampleInvocable(), null);

                throw new AssertionError("Expected a security exception - Subject has read only access");
                }
            catch (Exception e)
                {
                // expected security exception
                }

            return null;
            });

        logHeader("InvocationService access control example completed");
        }

    public static void accessMapListener()
            throws Exception
        {
        logHeader("MapListener access control example begins");

        String sDefaultConfigName = ExtensibleConfigurableCacheFactory.FILE_CFG_CACHE;
        String sConfigName        = System.getProperty("coherence.cacheconfig", sDefaultConfigName);

        ExtensibleConfigurableCacheFactory.Dependencies deps        =
                ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(sConfigName);

        // Someone with reader role tries to listen for map events
        Subject            subjectReadOnly  = SecurityExampleHelper.login("JohnBigboote");
        ExampleMapListener listenerReadOnly = new ExampleMapListener(1);

        // All cache access should be in the scope of a PrivilegedExceptionAction
        Subject.doAs(subjectReadOnly, (PrivilegedExceptionAction) () ->
            {
            // A ConfigurableCacheFactory must be created per Subject
            ConfigurableCacheFactory   ccf   = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(null);
            NamedCache<String, String> cache = ccf.ensureTypedCache(SecurityExampleHelper.SECURITY_CACHE_B_NAME, null,
                                                                    withTypes(String.class, String.class));

            cache.addMapListener(listenerReadOnly);

            return null;
            });

        // Someone with writer role generates map events
        Subject            subjectWriter    = SecurityExampleHelper.login("JohnWhorfin");
        ExampleMapListener listenerWriter   = new ExampleMapListener(1);

        // All cache access should be in the scope of a PrivilegedExceptionAction
        Subject.doAs(subjectWriter, (PrivilegedExceptionAction) () ->
            {
            // A ConfigurableCacheFactory must be created per Subject
            ConfigurableCacheFactory ccf = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(null);
            NamedCache<String, String> cache = ccf.ensureTypedCache(SecurityExampleHelper.SECURITY_CACHE_B_NAME, null,
                                                                    withTypes(String.class, String.class));

            cache.addMapListener(listenerWriter);

            // generate a map event
            cache.put("yetAnotherKey", "yetAnotherValue");

            return null;
            });

        // The Writer's map listener should receive the event
        boolean fWriterRecievedEvent = listenerWriter.await(5, TimeUnit.SECONDS);
        boolean fReaderRecievedEvent = listenerReadOnly.await(5, TimeUnit.SECONDS);

        if (fReaderRecievedEvent)
            {
            throw new AssertionError("Read-Only Subject should not have received the event");
            }

        if (!fWriterRecievedEvent)
            {
            throw new AssertionError("Writer Subject should have received the event");
            }

        logHeader("MapListener access control example ends");
        }

    // ----- inner class: ExampleMapListener -------------------------

    /**
     * ExampleMapListener listens for map events.
     *
     * @author dag  2010.07.21
     */
    public static class ExampleMapListener
            extends CountDownLatch
            implements MapListener
        {
        // ----- constructor --------------------------------------------

        public ExampleMapListener(int count)
            {
            super(count);
            }

        // ----- MapListener interface ----------------------------------

        @Override
        public void entryInserted(MapEvent event)
            {
            countDown();
            }

        @Override
        public void entryUpdated(MapEvent event)
            {
            countDown();
            }

        @Override
        public void entryDeleted(MapEvent event)
            {
            countDown();
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the security cache correctly typed.
     *
     * @return the security cache correctly typed
     */
    private static NamedCache<String, String> getSecurityCache()
        {
        return CacheFactory.getTypedCache(SecurityExampleHelper.SECURITY_CACHE_NAME,
                                          withTypes(String.class, String.class));
        }
    }
