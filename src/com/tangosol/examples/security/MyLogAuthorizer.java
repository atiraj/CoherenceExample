package com.tangosol.examples.security;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.security.StorageAccessAuthorizer;
import com.tangosol.util.BinaryEntry;

import javax.security.auth.Subject;

/**
 * Example of authorizing access to server-side operations. When running the
 * security examples, look for the log messages below in the output from the
 * 'bin/run-proxy security' command. The security examples use a proxy server
 * but the implementation of a {@link StorageAccessAuthorizer} does not require
 * one as they run on the storage-enabled nodes.<p>
 * Note: There is an out of the box storage-authorizer called <strong>auditing</strong>
 * which can be added to a backing-map-scheme by using the following:
 * <code>
 *     &lt;storage-authorizer&gt;auditing&lt;/storage-authorizer&gt;
 * </code>
 * See {@link com.tangosol.net.security.AuditingAuthorizer} for more information on
 * the implementation details.
 * <p>
 * See javadoc for {@link StorageAccessAuthorizer} and documentation
 * on 'Securing Oracle Coherence' for more information on this feature.
 *
 * @author oracle  2015.05.25
 * @since  12.2.1
 */
public class MyLogAuthorizer
        implements StorageAccessAuthorizer
    {
    // ----- constructors ---------------------------------------------------

    public MyLogAuthorizer ()
        {
        this(false);
        }

    public MyLogAuthorizer (boolean fStrict)
        {
        f_fStrict = fStrict;
        }

    // ----- StorageAccessAuthorizer methods --------------------------------

    @Override
    public void checkRead (BinaryEntry entry, Subject subject, int nReason)
        {
        logEntryRequest(entry, subject, false, nReason);

        if (subject == null && f_fStrict)
            {
            throw new SecurityException("subject is not provided");
            }
        }

    @Override
    public void checkWrite (BinaryEntry entry, Subject subject, int nReason)
        {
        logEntryRequest(entry, subject, true, nReason);

        if (subject == null && f_fStrict)
            {
            throw new SecurityException("subject is not provided");
            }
        }

    @Override
    public void checkReadAny (BackingMapContext context, Subject subject,
                              int nReason)
        {
        logMapRequest(context, subject, false, nReason);

        if (subject == null && f_fStrict)
            {
            throw new SecurityException("subject is not provided");
            }
        }

    @Override
    public void checkWriteAny (BackingMapContext context, Subject subject,
                               int nReason)
        {
        logMapRequest(context, subject, true, nReason);

        if (subject == null && f_fStrict)
            {
            throw new SecurityException("subject is not provided");
            }
        }

    // ----- helper methods --------------------------------------------------

    /**
     * Log the entry level authorization request.
     *
     * @param entry    the entry to authorize access to
     * @param subject  the Subject
     * @param fWrite   true for write operation; read otherwise
     * @param nReason  the reason for the check
     */
    protected void logEntryRequest (BinaryEntry entry, Subject subject,
                                    boolean fWrite, int nReason)
        {
        CacheFactory.log('"' + (fWrite ? "Write" : "Read")
                         + "\" request for key=\""
                         + entry.getKey()
                         + (subject == null ?
                            "\" from unidentified user" :
                            "\" on behalf of " + subject.getPrincipals())
                         + " caused by \"" + nReason + "\""
                , CacheFactory.LOG_INFO);
        }

    /**
     * Log the backing map level authorization request.
     *
     * @param context  the context of the backing map to authorize access to
     * @param subject  the Subject
     * @param fWrite   true for write operation; read otherwise
     * @param nReason  the reason for the check
     */
    protected void logMapRequest (BackingMapContext context, Subject subject,
                                  boolean fWrite, int nReason)
        {
        CacheFactory.log('"' + (fWrite ? "Write-any" : "Read-any")
                         + "\" request for cache \""
                         + context.getCacheName() + '"'
                         + (subject == null ?
                            " from unidentified user" :
                            " on behalf of " + subject.getPrincipals())
                         + " caused by \"" + nReason + "\""
                , CacheFactory.LOG_INFO);
        }

    // ----- data members ---------------------------------------------------

    private final boolean f_fStrict;
    }