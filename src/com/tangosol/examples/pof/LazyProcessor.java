package com.tangosol.examples.pof;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.IOException;

/**
 * LazyProcessor will sleep for a specified period of time.
 *
 * @author hr  2011.11.30
 *
 * @since 12.1.2
 */
public class LazyProcessor
        extends AbstractProcessor
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default no-arg constructor.
     */
    public LazyProcessor()
        {
        }

    /**
     * Constructs a LazyProcessor with a specified time to sleep.
     *
     * @param lSleepTime  the number of milliseconds this processor should
     *                    sleep
     */
    public LazyProcessor(long lSleepTime)
        {
        m_lSleepTime = lSleepTime;
        }

    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
        {
        try
            {
            Thread.sleep(m_lSleepTime);
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        return null;
        }

    // ----- PortableObject members -----------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in) throws IOException
        {
        m_lSleepTime = in.readLong(0);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_lSleepTime);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The number of milliseconds this processor should sleep.
     */
    protected long m_lSleepTime;
    }
