package com.tangosol.examples.security;

import com.tangosol.net.security.IdentityTransformer;

import com.tangosol.net.Service;

import java.security.Principal;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;

/**
 * PasswordIdentityTransformer creates a security token that contains the
 * required password and then adds a list of Principal names.
 *
 * @author dag 2010.04.15
 */
public class PasswordIdentityTransformer
        implements IdentityTransformer

    {
    // ----- IdentityTransformer interface ----------------------------------

    /**
     * Transform a Subject to a token that asserts an identity.
     *
     * @param subject  the Subject representing a user.
     * @param service  the Service requesting an identity token
     *
     * @return the token that asserts identity.
     *
     * @throws SecurityException if the identity transformation fails.
     */
    public Object transformIdentity(Subject subject, Service service)
            throws SecurityException
        {
        // The service is not needed so the service argument is being ignored.
        // It could be used, for example, if there were different token types
        // required per service.
        if (subject == null)
            {
            throw new SecurityException("Incomplete Subject");
            }

        Set<Principal> setPrincipals = subject.getPrincipals();

        if (setPrincipals.isEmpty())
            {
            throw new SecurityException("Incomplete Subject");
            }

        String[] asPrincipalName = new String[setPrincipals.size() + 1];
        int      i                = 0;

        asPrincipalName[i++] = System.getProperty("coherence.password",
                "secret-password");

        for (Iterator<Principal> iter = setPrincipals.iterator(); iter.hasNext();)
            {
            asPrincipalName[i++] = iter.next().getName();
            }

        // The token consists of the password plus the principal names as an
        // array of pof-able types, in this case strings.
        return asPrincipalName;
        }
    }
