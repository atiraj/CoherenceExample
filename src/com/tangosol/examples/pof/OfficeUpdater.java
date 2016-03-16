package com.tangosol.examples.pof;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.InvocableMap;

import java.io.IOException;

/**
 * OfficeUpdater updates a contact's office address.<br>
 * Note: This is an example Entry Processor which only updates one attribute.
 * An alternate way to do this would be using an UpdaterProcessor:
 * <pre>
 *     cache.invokeAll(filter, new UpdaterProcessor<>(new PofUpdater(Contact.WORK_ADDRESS), addrWork));
 * </pre>
 *
 * @author dag  2009.02.26
 */
public class OfficeUpdater
        implements InvocableMap.EntryProcessor<ContactId, Contact, Void>, PortableObject
    {
    // ----- constructors -------------------------------------------

    /**
     * Default constructor (necessary for PortableObject implementation).
     */
    public OfficeUpdater()
        {
        }

    /**
     * Construct an OfficeUpdater with a new work Address.
     *
     * @param addrWork the new work address.
     */
    public OfficeUpdater(Address addrWork)
        {
        m_addrWork = addrWork;
        }

    // ----- InvocableMap.EntryProcessor interface ------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Void process(InvocableMap.Entry<ContactId, Contact> entry)
        {
        Contact contact = entry.getValue();

        contact.setWorkAddress(m_addrWork);
        entry.setValue(contact);
        return null;
        }

    // ----- PortableObject interface -------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_addrWork = (Address) reader.readObject(WORK_ADDRESS);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeObject(WORK_ADDRESS, m_addrWork);
        }

    // ----- constants ----------------------------------------------

    /**
     * The POF index for the WorkAddress property
     */
    public static final int WORK_ADDRESS = 0;


    // ----- data members -------------------------------------------

    /**
     * New work address.
     */
    protected Address m_addrWork;
    }