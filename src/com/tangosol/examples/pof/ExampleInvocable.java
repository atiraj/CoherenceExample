package com.tangosol.examples.pof;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;

import java.io.IOException;

/**
 * Invocable implementation that increments and returns a given integer.
 */
public class ExampleInvocable
        implements Invocable, PortableObject
    {
    // ----- constructors ---------------------------------------------

    /**
     * Default constructor.
     */
    public ExampleInvocable()
        {
        }

    public ExampleInvocable(int m_nValue)
        {
        this.m_nValue = m_nValue;
        }

    // ----- Invocable interface --------------------------------------

    /**
     * {@inheritDoc}
     */
    public void init(InvocationService service)
        {
        m_service = service;
        }

    /**
     * {@inheritDoc}
     */
    public void run()
        {
        if (m_service != null)
            {
            m_nValue++;
            }
        }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
        {
        return new Integer(m_nValue);
        }

    // ----- PortableObject interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nValue = in.readInt(0);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_nValue);
        }

    // ----- data members ---------------------------------------------

    /**
     * The integer value to increment.
     */
    protected int m_nValue;

    /**
     * The InvocationService that is executing this Invocable.
     */
    private transient InvocationService m_service;
    }