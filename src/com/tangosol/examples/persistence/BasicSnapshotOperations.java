package com.tangosol.examples.persistence;

import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;

import static com.tangosol.examples.contacts.ExamplesHelper.log;
import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

import java.util.Arrays;

/**
 * Example showing how to call basic snapshot operations from Java code.
 *
 * @author tam  2015.03.20
 * @since  12.2.1
 */
public class BasicSnapshotOperations
    {
    // ----- BasicSnapshotOperations methods --------------------------------

    /**
     * Run the example.
     *
     * @param helper the PersistenceHelper for issuing operations
     * @param nc     the NamedCache to use
     */
    public void runExample(PersistenceHelper helper, NamedCache <ContactId, Contact> nc)
        {
        final  int DATA_SIZE  = 10000;
        String sServiceName   = "PartitionedPofCache";
        String sEmptySnapshot = "snapshot-empty";
        String sDataSnapshot  = "snapshot-10000";

        log("");
        logHeader("BasicSnapshotOperations example begin");

        try
            {
            nc.clear();
            logHeader("Initial data size is " + nc.size());

            String[] asSnapshots = helper.listSnapshots(sServiceName);

            // check to see if there any any snapshots left behind
            if (asSnapshots.length != 0 )
                {
                logHeader("removing existing snapshots");

                // remove any existing snapshots
                for (String sSnapshot : asSnapshots)
                    {
                    log("Removing snapshot " + sSnapshot);
                    helper.invokeOperationWithWait(PersistenceHelper.REMOVE_SNAPSHOT, sSnapshot, sServiceName);
                    }
                }

            logHeader("create new snapshots");
            log("create new snapshot " + sEmptySnapshot);

            helper.invokeOperationWithWait(PersistenceHelper.CREATE_SNAPSHOT, sEmptySnapshot, sServiceName);

            log("snapshots = " + Arrays.toString(helper.listSnapshots(sServiceName)));

            PersistenceHelper.populateData(nc, DATA_SIZE);
            logHeader("Populated data size is " + nc.size());

            log("create new snapshot " + sDataSnapshot);

            helper.invokeOperationWithWait(PersistenceHelper.CREATE_SNAPSHOT, sDataSnapshot, sServiceName);

            log("snapshots = " + Arrays.toString(helper.listSnapshots(sServiceName)));

            logHeader("recover snapshot");
            nc.clear();
            log("cleared cache, size = " + nc.size());

            log("Recover snapshot " + sDataSnapshot + " containing " + DATA_SIZE + " entries");

            helper.invokeOperationWithWait(PersistenceHelper.RECOVER_SNAPSHOT, sDataSnapshot, sServiceName);

            log("cache size is now " + nc.size());

            log("Recover snapshot " + sEmptySnapshot + " containing 0 entries");

            helper.invokeOperationWithWait(PersistenceHelper.RECOVER_SNAPSHOT, sEmptySnapshot, sServiceName);

            log("cache size is now " + nc.size());

            logHeader("removing snapshots");

            helper.invokeOperationWithWait(PersistenceHelper.REMOVE_SNAPSHOT, sEmptySnapshot, sServiceName);
            helper.invokeOperationWithWait(PersistenceHelper.REMOVE_SNAPSHOT, sDataSnapshot, sServiceName);

            log("snapshots = " + Arrays.toString(helper.listSnapshots(sServiceName)));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error running Persistence examples");
            }

        logHeader("BasicSnapshotOperations example completed");
        }
    }