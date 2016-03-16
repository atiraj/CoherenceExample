package com.tangosol.examples.persistence;

import com.oracle.common.base.Timeout;

import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.tangosol.examples.contacts.ExamplesHelper.log;
import static com.tangosol.examples.contacts.ExamplesHelper.logHeader;

import static com.tangosol.examples.persistence.PersistenceHelper.populateData;

/**
 * Example showing how to call snapshot operations for multiple services in
 * parallel. This example assumes that you want to suspend all services
 * before taking the snapshot to get a consistent view of the data.
 *
 * @author tam  2015.03.20
 * @since  12.2.1
 */
public class ParallelSnapshotOperations
    {
    // ----- ParallelSnapshotOperations methods -----------------------------

    /**
     * Run the example.
     *
     * @param nc1     the first NamedCache to use
     * @param nc2     the second NamedCache to use
     * @param helper  the PersistenceHelper for issuing operations
     */
    public void runExample(PersistenceHelper helper,
                           NamedCache<ContactId, Contact> nc1, NamedCache<ContactId, Contact> nc2)
        {
        final int DATA_SIZE    = 10000;

        String sServiceName1 = "PartitionedPofCache";
        String sServiceName2 = "PartitionedPofCache2";

        Set<String> setServices = new HashSet<>();
        setServices.add(sServiceName1);
        setServices.add(sServiceName2);
        String sSnapshot    = "snapshot";

        log("");
        logHeader("ParallelSnapshotOperations example begin");

        logHeader("populating caches");
        nc1.clear();
        nc2.clear();

        populateData(nc1, DATA_SIZE);
        populateData(nc2, DATA_SIZE);

        log(nc1.getCacheName() + " size is " + nc1.size());
        log(nc2.getCacheName() + " size is " + nc2.size());

        logHeader("creating snapshot " + sSnapshot);
        executeOperation(helper, PersistenceHelper.CREATE_SNAPSHOT, sSnapshot, setServices);

        logHeader("list snapshots");
        log("snapshots = " + Arrays.toString(helper.listSnapshots(sServiceName1)));
        log("snapshots = " + Arrays.toString(helper.listSnapshots(sServiceName2)));

        logHeader("clearing caches");
        nc1.clear();
        nc2.clear();

        log(nc1.getCacheName() + " size is " + nc1.size());
        log(nc2.getCacheName() + " size is " + nc2.size());

        logHeader("recovering snapshot " + sSnapshot);
        executeOperation(helper, PersistenceHelper.RECOVER_SNAPSHOT, sSnapshot, setServices);

        logHeader("cache size after recovery");
        log(nc1.getCacheName() + " size is " + nc1.size());
        log(nc2.getCacheName() + " size is " + nc2.size());

        logHeader("remove snapshots");
        executeOperation(helper, PersistenceHelper.REMOVE_SNAPSHOT, sSnapshot, setServices);

        logHeader("ParallelSnapshotOperations example completed");
        }

    /**
     * Issue an operation against a number of services and suspend and resume them
     * beforehand.
     *
     * @param helper      the helper to use to issue snapshot operations
     * @param sOperation  the persistence operation to issue against the service
     * @param sSnapshot   the snapshot name
     * @param setServices the array of services
     */
    private void executeOperation(PersistenceHelper helper, String sOperation,
                                  String sSnapshot, Set<String> setServices)
        {
        logHeader("issuing " + sOperation + " against services " + setServices);

        try (Timeout timeout = Timeout.after(180, TimeUnit.SECONDS))
            {
            // suspend the services
            for (String sService : setServices)
                {
                log("Suspending service " + sService);
                helper.suspendService(sService);
                setServices.add(sService);
                }

            invokeOperationWithWait(helper, sOperation, sSnapshot, setServices);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Unable to create snapshot ");
            }
        finally
            {
            // resume all services
            for (String sService : setServices)
                {
                log("Resuming service " + sService);
                helper.resumeService(sService);
                }
            }
        }

    /**
     * Issue operations (in parallel for all services) and wait for the operation
     * to be complete by polling the "Idle" attribute of the PersistenceCoordinator
     * for the services.
     * The services are assumed to be suspended before this operation is called
     * and must be resumed after the operation has completed.
     * <pre>
     *  Set<String> setServices = new HashSet<>();
     * try (Timeout t = Timeout.after(120, TimeUnit.SECONDS))
     *     {
     *     setServices.add("service1");
     *     setServices.add("service2");
     *     setServices.add("service3");
     *
     *     for (String sServiceName : setServices)
     *         {
     *         helper.suspendService(sServiceName);
     *         }
     *     invokeOperationWithWait(helper, "createSnapshot", "snapshot-name", setServices);
     *     }
     * catch (Exception e)
     *     {
     *     // ... handle error
     *     }
     * finally
     *     {
     *     for (String sServiceName : setServices)
     *         {
     *         helper.resumeService(sServiceName);
     *         }
     *     }
     * </pre>
     *
     * @param helper       The {@link PersistenceHelper} to execute the commands
     * @param sOperation   the operation to execute
     * @param sSnapshot    the snapshot name
     * @param setServices  the set of services to execute the operation for
     *
     * @throws javax.management.MBeanException if any MBean related errors
     * @throws RuntimeException if any execution problems
     */
    public static void invokeOperationWithWait(final PersistenceHelper helper, String sOperation,
        String sSnapshot, Set<String> setServices)
        {
        ExecutorService executor = null;

        try
            {
            // build up the list of operations to call against the services
            List<Callable<Object>> listOperations = new ArrayList<>();

            for (String sServiceName : setServices)
                {
                listOperations.add(() ->
                    {
                    helper.invokeOperationWithWait(sOperation, sSnapshot, sServiceName);
                    return null;
                    });
                }

            // create an executor service for each invocation of this method.
            // This may be a little heavyweight, but we only have the executor
            // service running when its needed
            executor = Executors.newFixedThreadPool(Math.min(setServices.size(), POOL_THREAD_COUNT));

            List<Future<Object>> listResults = executor.invokeAll(listOperations);

            // once we get here we know all operations are completed, so issue get
            // on each one. We can ignore result as exceptions thrown will mean
            // the call to the operation failed.
            Set<Throwable> setExceptions = new HashSet<>();

            for (int i = 0; i < listResults.size(); i++)
                {
                try
                    {
                    listResults.get(i).get();
                    }
                catch (Exception e)
                    {
                    setExceptions.add(e);
                    }
                }

            if (setExceptions.size() != 0)
                {
                StringBuilder sb = new StringBuilder("One or more services failed the operation");

                for (Throwable error : setExceptions)
                    {
                    sb.append(error.getMessage() + "\n");
                    }

                throw new RuntimeException(sb.toString());
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Unable to complete operation " + sOperation + " for services "
                                              + setServices);
            }
        finally
            {
            if (executor != null)
                {
                executor.shutdownNow();
                }
            }
        }

    // ----- constants --------------------------------------------------

    private static final int POOL_THREAD_COUNT = 10;
    }
