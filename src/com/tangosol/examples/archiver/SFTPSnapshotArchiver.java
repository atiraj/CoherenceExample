package com.tangosol.examples.archiver;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import com.oracle.common.base.Blocking;

import com.oracle.datagrid.persistence.PersistenceManager;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.GuardSupport;

import com.tangosol.persistence.AbstractSnapshotArchiver;
import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.GUIDHelper;
import com.tangosol.persistence.Snapshot;
import com.tangosol.persistence.SnapshotArchiver;

import com.tangosol.util.Base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * An implementation of a {@link SnapshotArchiver} that uses SFTP
 * to store and retrieve archived snapshots.<br>
 * To utilize this, refer to the example tangosol-coherence-override.xml
 * file in the test source tree.<br>
 * Replace the third argument with your URI similar to the following:<br>
 * sftp://user:passwd@localhost/path<br>
 * You may omit the password if you have setup SSH equivalence.<br>
 * You should
 *
 * @author tam  2015.03.19
 * @since 12.2.1
 */
public class SFTPSnapshotArchiver
        extends AbstractSnapshotArchiver
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new SFTPSnapshotArchiver which uses the URI SFTP URI.
     *
     * @param sClusterName   the name of the cluster
     * @param sServiceName   the service name
     * @param sURI           a URI for the SFTP server
     */
    public SFTPSnapshotArchiver(String sClusterName, String sServiceName, String sURI)
        {
        super(sClusterName, sServiceName);

        if (sURI == null)
            {
            throw new IllegalArgumentException("URI must be specified");
            }

        try
            {
            URI      uri            = new URI(sURI);
            String   sBaseDirectory = uri.getPath();
            String[] asUserInfo     = uri.getUserInfo().split(":");
            int      nPort          = uri.getPort();

            f_sUsername    = asUserInfo[0];
            f_sPassword    = asUserInfo.length == 2 ? asUserInfo[1] : null;
            f_sHostname    = uri.getHost();

            f_nPort        = nPort == -1 ? 22 : nPort;

            // ensure that the base directory exists  and then create the cluster and
            // service directories off that
            ensureRemoteDirectory(sBaseDirectory, true);
            sBaseDirectory = sBaseDirectory + FTP_DIR_SEP + FileHelper.toFilename(sClusterName);
            ensureRemoteDirectory(sBaseDirectory, false);
            sBaseDirectory = sBaseDirectory + FTP_DIR_SEP + FileHelper.toFilename(sServiceName);
            ensureRemoteDirectory(sBaseDirectory, false);

            f_sBaseDirectory = sBaseDirectory;

            if (!"sftp".equals(uri.getScheme()))
                {
                throw new RuntimeException("Invalid protocol: " + uri.getScheme());
                }
            }
        catch (Exception e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to instantiate SFTP Archiver");
            }
        }

    // ----- AbstractSnapshotArchiver methods -------------------------------

    /**
     * Internal implementation to return the identifiers of the archived
     * snapshots known to this archiver.
     *
     * @return a list of the known archived snapshot identifiers
     */
    @Override
    protected String[] listInternal()
        {
        try
            {
            // Change to remote directory and fail if it does not exist
            ensureRemoteDirectory(f_sBaseDirectory, true);

            Vector vecDirs = m_sftpChannel.ls(f_sBaseDirectory);

            return convertToFileNames(vecDirs, /* fExcludeDirs */ false);
            }
        catch (Exception e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to execute listInternal()");
            }
        }

    /**
     * List the stores for a given snapshot.
     *
     * @param sSnapshot the snapshot name to list stores for
     *
     * @return a {@link String}[] of store names
     */
    @Override
    protected String[] listStoresInternal(String sSnapshot)
        {
        String sDir = getSnapshotDirectory(sSnapshot);

        try
            {
            ensureConnection();

            // Change to remote directory and snapshot and fail if it does not exist
            ensureRemoteDirectory(sDir, /* fExcludeDirs */ true);

            Vector<String> vecStores = m_sftpChannel.ls(sDir);

            return convertToFileNames(vecStores, false);

            }
        catch (Exception e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to execute listStoresInternal()");
            }
        }

    /**
     * Internal implementation to Archive the specified snapshot using SFTP.
     *
     * @param snapshot  the snapshot to archive
     * @param mgr       the PersistenceManager used to read the stores from
     */
    @Override
    protected void archiveInternal(Snapshot snapshot, PersistenceManager<ReadBuffer> mgr)
        {
        String       sSnapshot       = snapshot.getName();
        OutputStream os              = null;
        File         fileMetaTempDir = null;

        // change to the remote directory, which should be created it doesn't exist
        ensureRemoteDirectory(getSnapshotDirectory(sSnapshot), false);

        for (String sStore : snapshot.listStores())
            {
            try
                {
                CacheFactory.log("Archiving store " + sStore + " for snapshot " + sSnapshot, CacheFactory.LOG_QUIET);
                recordStartTime();

                if (CachePersistenceHelper.isGlobalPartitioningSchemePID(GUIDHelper.getPartition(sStore)))
                    {
                    // Create a temporary directory to write archived snapshot metadata properties
                    fileMetaTempDir = FileHelper.createTempDir();

                    writeMetadata(fileMetaTempDir, mgr, sStore);

                    // store the file via SFTP
                    try (InputStream is = new FileInputStream(new File(fileMetaTempDir,
                        CachePersistenceHelper.META_FILENAME));)
                        {
                        m_sftpChannel.put(is, CachePersistenceHelper.META_FILENAME);
                        }
                    }

                os = m_sftpChannel.put(sStore);

                mgr.write(sStore, os);
                os.close();

                // issue heartbeat as operations could take a relatively long time
                GuardSupport.heartbeat();
                }
            catch (IOException | SftpException e)
                {
                throw CachePersistenceHelper.ensurePersistenceException(e, "Error in archiveInternal()");
                }
            finally
                {
                if (os != null)
                    {
                    try
                        {
                        os.close();
                        }
                    catch (IOException ioe)
                        {
                        throw CachePersistenceHelper.ensurePersistenceException(ioe,
                            "Unable to close output stream for store " + sStore);
                        }
                    }

                if (fileMetaTempDir != null)
                    {
                    FileHelper.deleteDirSilent(fileMetaTempDir);
                    }
                }

            recordEndTime();
            }
        }

    /**
     * Internal implementation to retrieve the specified snapshot.
     *
     * @param snapshot  the snapshot to retrieve
     * @param mgr       the PersistenceManager used to write the stores to
     */
    @Override
    protected void retrieveInternal(Snapshot snapshot, PersistenceManager<ReadBuffer> mgr)
        {
        String      sSnapshot = snapshot.getName();
        InputStream is        = null;
        File        fileTemp  = null;

        // change to the remote directory which should exist
        ensureRemoteDirectory(getSnapshotDirectory(sSnapshot), true);

        for (String sStore : snapshot.listStores())
            {
            try
                {
                CacheFactory.log("Retrieving store " + sStore + " for snapshot " + sSnapshot, CacheFactory.LOG_QUIET);
                recordStartTime();

                if (CachePersistenceHelper.isGlobalPartitioningSchemePID(GUIDHelper.getPartition(sStore)))
                    {
                    // validate that the metadata file exists for partition 0
                    if (getMetadata(sSnapshot) == null)
                        {
                        throw new IllegalArgumentException("Cannot load properties file "
                                                           + CachePersistenceHelper.META_FILENAME + " for snapshot "
                                                           + sSnapshot);
                        }
                    }

                // retrieve the remote file to a temporary file and then get the manager
                // to read the store from the temporary file.
                // This could be done with is = m_sftpChannel.get(sStore), but I had issues
                // (which i'm not yet able to resolve) with JSCH not reading all bytes
                // from some sftp servers.

                File fileStore = new File(fileTemp, sStore);

                m_sftpChannel.get(sStore, fileStore.getAbsolutePath());

                is = new FileInputStream(fileStore);

                mgr.read(sStore, is);    // instruct the mgr to read the store from the stream
                is.close();

                is = null;
                fileStore.delete();

                // issue heartbeat as operations could take a relatively long time
                GuardSupport.heartbeat();
                }
            catch (Exception e)
                {
                throw CachePersistenceHelper.ensurePersistenceException(e, "Error reading store " + sStore);
                }
            finally
                {
                if (is != null)
                    {
                    try
                        {
                        is.close();
                        }
                    catch (IOException ioe)
                        {
                        throw CachePersistenceHelper.ensurePersistenceException(ioe,
                            "Unable to close input stream for store " + sStore);
                        }
                    }
                }

            if (fileTemp != null)
                {
                FileHelper.deleteDirSilent(fileTemp);
                }

            recordEndTime();
            }
        }

    /**
     * Internal implementation to remove the specified archived snapshot.
     *
     * @param sSnapshot  the snapshot name to remove
     *
     * @return true if the snapshot was removed
     */
    @Override
    protected boolean removeInternal(String sSnapshot)
        {
        // change to the remote directory which should exist
        ensureRemoteDirectory(getSnapshotDirectory(sSnapshot), true);

        String[] asStores = listStoresInternal(sSnapshot);

        try
            {
            for (String sStore : asStores)
                {
                m_sftpChannel.rm(sStore);

                // issue heartbeat as operations could take a relatively long time
                GuardSupport.heartbeat();
                }

            // remove meta.properties
            m_sftpChannel.rm(CachePersistenceHelper.META_FILENAME);

            // now remove the parent directory
            ensureRemoteDirectory(f_sBaseDirectory, true);
            m_sftpChannel.rmdir(sSnapshot);
            }
        catch (Exception e)
            {
            CacheFactory.log("Unable to remove stores for snapshot " + sSnapshot + ", " + e.getMessage(),
                             CacheFactory.LOG_WARN);

            return false;
            }

        return true;
        }

    /**
     * Internal implementation to retrieve the metadata stored for the archived
     * snapshot.
     *
     * @param sSnapshot  the snapshot name to retrieve metadata
     *
     * @return the metadata for the archived snapshot
     *
     * @throws IOException if any I/O related problems
     */
    @Override
    protected Properties getMetadata(String sSnapshot)
            throws IOException
        {
        ensureRemoteDirectory(getSnapshotDirectory(sSnapshot), true);

        File fileTemp = null;

        try
            {
            fileTemp = FileHelper.createTempDir();
            m_sftpChannel.get(CachePersistenceHelper.META_FILENAME, fileTemp.getAbsolutePath());

            return CachePersistenceHelper.readMetadata(fileTemp);
            }
        catch (SftpException e)
            {
            throw new IOException("Unable to read metadata", e);
            }
        finally
            {
            if (fileTemp != null)
                {
                FileHelper.deleteDirSilent(fileTemp);
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a snapshot directory fully qualified path.
     *
     * @param sSnapshot  the snapshot name to get path for
     *
     * @return a snapshot directory fully qualified path
     */
    private String getSnapshotDirectory(String sSnapshot)
        {
        return new String(f_sBaseDirectory + FTP_DIR_SEP + FileHelper.toFilename(sSnapshot));

        }

    /**
     * Convert a Vector of to an array of file name entries to a list of file names.
     *
     * @param vector        vector of {@link ChannelSftp.LsEntry}
     * @param fExcludeDirs  if true directories are excluded
     *
     * @return a String array of file names
     */
    private String[] convertToFileNames(Vector vector, boolean fExcludeDirs)
        {
        if (vector == null)
            {
            throw new IllegalArgumentException("Vector must not be null");
            }

        List<String> arrayList = new ArrayList<String>();

        for (int i = 0; i < vector.size(); i++)
            {
            ChannelSftp.LsEntry entry  = (ChannelSftp.LsEntry) vector.get(i);
            boolean             fIsDir = entry.getAttrs().isDir();

            if (!fIsDir || (fIsDir && !fExcludeDirs))
                {
                String sFileName = entry.getFilename();

                if (!".".equals(sFileName) && !"..".equals(sFileName)
                    && !CachePersistenceHelper.META_FILENAME.equals(sFileName))
                    {
                    arrayList.add(entry.getFilename());
                    }
                }
            }

        return arrayList.toArray(new String[arrayList.size()]);
        }

    /**
     * Ensures that we have a connection to the SFTP endpoint.
     *
     * @throws IOException if a connection cannot be established or there is an error
     */
    private void ensureConnection()
            throws IOException
        {
        try
            {
            if (m_sftpChannel != null)
                {
                // The connection could have timed out, so issue a pwd() command
                // to ensure it is active
                try
                    {
                    m_sftpChannel.pwd();
                    }
                catch (SftpException e)
                    {
                    CacheFactory.log("SFTP Connection not active, re-connecting", Base.LOG_INFO);

                    // force reconnect on fall through
                    m_sftpChannel = null;
                    }
                }

            if (m_sftpChannel == null)
                {
                if (f_sPassword == null)
                    {
                    f_jsch.addIdentity(System.getProperty("user.home") + File.separatorChar + ".ssh"
                                       + File.separatorChar + "id_rsa");
                    }

                Session session = f_jsch.getSession(f_sUsername, f_sHostname, f_nPort);

                // the session should not stop the JVM from exiting
                session.setDaemonThread(true);

                Properties config = new Properties();

                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                if (f_sPassword != null && !f_sPassword.isEmpty())
                    {
                    session.setPassword(f_sPassword);
                    }

                session.connect();

                m_sftpChannel = (ChannelSftp) session.openChannel("sftp");
                m_sftpChannel.connect();
                }
            }
        catch (JSchException e)
            {
            throw new IOException("Unable to connect", e);
            }

        }

    /**
     * Ensure the remote directory exists. If the directory exists then change
     * the current working directory to the directory.<p>
     * If the directory does not exist and fFailIfNotExists is true then raise
     * an exception otherwise create the directory and change to it.
     *
     * @param sDirectory       the directory to create or change to
     * @param fFailIfNotExists fail if the directory does not exist
     *
     */
    private void ensureRemoteDirectory(String sDirectory, boolean fFailIfNotExists)
        {
        boolean fExists = true;

        try
            {
            ensureConnection();

            try
                {
                m_sftpChannel.cd(sDirectory);
                }
            catch (SftpException e)
                {
                fExists = false;

                if (fFailIfNotExists)
                    {
                    throw new IOException("Unable to change to directory " + sDirectory, e);
                    }
                }

            if (!fExists)
                {
                // directory must not exist, so lets create it and change to it
                try
                    {
                    CacheFactory.log("Creating SFTP Directory " + sDirectory, CacheFactory.LOG_QUIET);
                    m_sftpChannel.mkdir(sDirectory);
                    }
                catch (Exception e)
                    {
                    // multiple SFTP archivers could have registered the directory did not
                    // exist, but it could have been created by one of them
                    try
                        {
                        // wait a bit
                        Blocking.sleep(1000L);

                        // attempt to cd to again as someone else must have created it
                        m_sftpChannel.cd(sDirectory);
                        }
                    catch (Exception e1)
                        {
                        // if we get to this point then it could nto be created
                        // and there was one member who will have the error
                        throw e1;
                        }
                    }

                m_sftpChannel.cd(sDirectory);
                }
            }
        catch (Exception e)
            {
            throw CachePersistenceHelper.ensurePersistenceException(e, "Unable to ensureDirectory " + sDirectory);
            }
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return (this.getClass().getName() + " (host=" + f_sHostname + ", port=" + f_nPort + ", username=" + f_sUsername
                + ", path=" + f_sBaseDirectory + ")");
        }

    // ----- constants -------------------------------------------------------

    /**
     * FTP URI format.
     */
    public static final String URI_FORMAT = "sftp://user:password@host:port/full/path";

    /**
     * Directory separator for FTP commands.
     */
    public static final String FTP_DIR_SEP = "/";

    // ----- data members ---------------------------------------------------

    /**
     * Username to connect to SFTP server.
     */
    private String f_sUsername;

    /**
     * Password to connect to SFTP server. Could be null if SSH equiv is being used.
     */
    private String f_sPassword;

    /**
     * Hostname of the SFTP server.
     */
    private String f_sHostname;

    /**
     * SFTP port to connect to.
     */
    private int f_nPort;

    /**
     * Base directory of the SFTP server.
     */
    private String f_sBaseDirectory;

    /**
     * JSch instance.
     */
    private final JSch f_jsch = new JSch();

    /**
     * An opened channel to an sftp endpoint.
     */
    private ChannelSftp m_sftpChannel = null;
    }
