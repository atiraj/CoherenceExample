package com.tangosol.examples.archiver;

import com.tangosol.examples.persistence.PersistenceHelper;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;

import static com.tangosol.examples.contacts.ExamplesHelper.log;
import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

import static com.tangosol.examples.persistence.PersistenceHelper.populateData;
import static com.tangosol.net.cache.TypeAssertion.withTypes;

import java.util.Arrays;

/**
 * Example showing how to use SFTP Snapshot Arcvhier.
 *
 * @author tam  2015.03.20
 * @since  12.2.1
 */
public class Driver
    {
    // ----- static methods -------------------------------------------------

    /**
     * Execute Persistence SFTP Snapshot Archiver example.
     *
     * @param asArgs  command line arguments
     */
    public static void main(String[] asArgs)
        {
        // obtain the helper class to issue snapshot operations
        PersistenceHelper helper = new PersistenceHelper();

        new Driver().runExample(helper,
          CacheFactory.getTypedCache("contacts", withTypes(ContactId.class, Contact.class)));
        }

    // ----- Driver methods -------------------------------------------------

    /**
     * Run the example.
     *
     * @param helper the PersistenceHelper for issuing operations
     * @param nc     the NamedCache to use
     */
    public void runExample(PersistenceHelper helper, NamedCache<ContactId, Contact> nc)
        {
        final int DATA_SIZE     = 10000;
        String    sServiceName  = "PartitionedPofCache";
        String    sDataSnapshot = "snapshot-10000";

        log("");
        logHeader("archive example begin");

        try
            {
            populateData(nc, DATA_SIZE);
            logHeader("Populated data size is " + nc.size());

            // check for existing snapshots from previous runs
            String[] asSnapshots = helper.listSnapshots(sServiceName);

            // check to see if there is a snapshot left behind form previous run
            if (asSnapshots.length != 0 && Arrays.asList(asSnapshots).contains(sDataSnapshot))
                {
                logHeader("removing existing snapshot " + sDataSnapshot);
                helper.invokeOperationWithWait(PersistenceHelper.REMOVE_SNAPSHOT, sDataSnapshot, sServiceName);
                }

            asSnapshots = helper.listArchivedSnapshots(sServiceName);

            // check to see if there is an archived snapshot left behind form previous run
            if (asSnapshots.length != 0 && Arrays.asList(asSnapshots).contains(sDataSnapshot))
                {
                logHeader("removing existing archived snapshot " + sDataSnapshot);

                log("Removing snapshot " + sDataSnapshot);
                helper.invokeOperationWithWait(PersistenceHelper.REMOVE_ARCHIVED_SNAPSHOT, sDataSnapshot,
                                               sServiceName);
                }

            logHeader("create new snapshot " + sDataSnapshot);

            helper.invokeOperationWithWait(PersistenceHelper.CREATE_SNAPSHOT, sDataSnapshot, sServiceName);

            log("snapshots = " + Arrays.toString(helper.listSnapshots(sServiceName)));

            logHeader("archive snapshot " + sDataSnapshot);
            helper.invokeOperationWithWait(PersistenceHelper.ARCHIVE_SNAPSHOT, sDataSnapshot, sServiceName);
            log("archived snapshots = " + Arrays.toString(helper.listArchivedSnapshots(sServiceName)));

            logHeader("remove snapshot " + sDataSnapshot);
            helper.invokeOperationWithWait(PersistenceHelper.REMOVE_SNAPSHOT, sDataSnapshot, sServiceName);

            logHeader("clear cache");
            nc.clear();
            log("cache size is now " + nc.size());

            log("look at archived snapshot on remote SFTP machine then press RETURN to continue");
            System.console().readLine();

            logHeader("retrieve archived snapshot " + sDataSnapshot);
            helper.invokeOperationWithWait(PersistenceHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sDataSnapshot,
                                           sServiceName);
            log("snapshots = " + Arrays.toString(helper.listSnapshots(sServiceName)));

            log("Recover snapshot " + sDataSnapshot + " containing 10000 entries");

            helper.invokeOperationWithWait(PersistenceHelper.RECOVER_SNAPSHOT, sDataSnapshot, sServiceName);

            log("cache size is now " + nc.size());

            logHeader("removing archived and local snapshots");

            helper.invokeOperationWithWait(PersistenceHelper.REMOVE_SNAPSHOT, sDataSnapshot, sServiceName);
            helper.invokeOperationWithWait(PersistenceHelper.REMOVE_ARCHIVED_SNAPSHOT, sDataSnapshot,
                                           sServiceName);

            logHeader("archiver example completed");
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error running archiver example");
            }
        }
    }