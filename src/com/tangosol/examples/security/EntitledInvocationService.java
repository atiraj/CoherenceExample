package com.tangosol.examples.security;

import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationObserver;
import com.tangosol.net.InvocationService;
import com.tangosol.net.WrapperInvocationService;

import java.util.Map;
import java.util.Set;

/**
 * Example WrapperInvocationService that demonstrates how entitlements can be
 * applied to a wrapped InvocationService using the Subject passed from the
 * client via Coherence*Extend. This implementation only allows clients with a
 * specified role to access the wrapped InvocationService.
 *
 *  @author dag  2010.04.19
 */
public class EntitledInvocationService
        extends WrapperInvocationService
    {
    /**
     * Create a new EntitledInvocationService.
     *
     * @param service  the wrapped InvocationService
     */
    public EntitledInvocationService(InvocationService service)
        {
        super(service);
        }

    // ----- InvocationService interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    public void execute(Invocable task, Set setMembers, InvocationObserver observer)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        super.execute(task, setMembers, observer);
        }

    /**
     * {@inheritDoc}
     */
    public Map query(Invocable task, Set setMembers)
        {
        SecurityExampleHelper.checkAccess(SecurityExampleHelper.ROLE_WRITER);
        return super.query(task, setMembers);
        }
    }
