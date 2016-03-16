package com.tangosol.examples.pof;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.AbstractInvocable;

import java.io.IOException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RedistributionInvocable has three states in which appropriate action is
 * taken:
 * <ol>
 *     <li><strong>{@link State#DISABLE}</strong> - Disables the logging
 *     performed by {@link com.tangosol.examples.events.RedistributionInterceptor}.</li>
 *     <li><strong>{@link State#ENABLE}</strong> - Enables the logging
 *     performed by {@link com.tangosol.examples.events.RedistributionInterceptor}.</li>
 *     <li><strong>{@link State#KILL}</strong> - Kills the JVM this invocable
 *     is executed on.</li>
 * </ol>
 *
 * @author hr  2011.11.30
 *
 * @since 12.1.2
 */
public class RedistributionInvocable
        extends AbstractInvocable
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default no-arg constructor.
     */
    public RedistributionInvocable()
        {
        this(State.DISABLE);
        }

    /**
     * Constructs a RedistributionInvocable with the specified state.
     *
     * @param state  the state indicating the action to be performed
     */
    public RedistributionInvocable(State state)
        {
        m_state = state;
        }

    // ----- Invocable methods ----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void run()
        {
        switch (m_state)
            {
            case DISABLE:
                ENABLED.set(false);
                break;
            case ENABLE:
                ENABLED.set(true);
                break;
            case KILL:
                System.exit(1);
            }
        }

    // ----- PortableObject methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in) throws IOException
        {
        m_state = State.values()[in.readInt(0)];
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_state.ordinal());
        }

    // ----- inner class: State ---------------------------------------------

    /**
     * Representation of the action to be performed when
     * {@link RedistributionInvocable#run()}.
     */
    public enum State
        {
        /**
         * Disables the logging performed by
         * {@link com.tangosol.examples.events.RedistributionInterceptor}
         */
        DISABLE,
        /**
         * Enables the logging performed by
         * {@link com.tangosol.examples.events.RedistributionInterceptor}
         */
        ENABLE,
        /**
         * Terminates the JVM process in which the
         * {@link RedistributionInvocable} is executed.
         */
        KILL
        }

    // ----- constants ------------------------------------------------------

    /**
     * Flag used to determine whether to log partition events.
     */
    public static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    // ----- data members ---------------------------------------------------

    /**
     * The state used to determine which action to perform.
     */
    protected State m_state;
    }
