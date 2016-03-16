package com.tangosol.examples.federation;

import com.tangosol.coherence.dslquery.QueryPlus;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.util.UUID;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * Driver to display a GUI to exercise the Coherence Federated Cache examples or
 * to run CohQL to allow manual updating of cache contents.<p>
 *
 * This Driver is called using bin/run command as described in examples readme.txt.
 * It can be use in two ways:
 * <p>
 * 1. To startup a GUI connected to either ClusterA or ClusterB, use the following:<br>
 * <pre>
 *    bin/run federation ClusterA<br>
 *    bin/run federation ClusterB<br>
 * </pre>
 * <br>
 * In either of the above cases, the GUI is started allow the user to insert
 * data or clear the caches in either cluster.<br>
 * 2. To start CohQL (Coherence Query Language) command line prompt (or console) to allow
 * manual updating of cache contents, use the following:<p>
 * <pre>
 *   bin/run federation ClusterA cohql
 * or
 *   bin/run federation ClusterA console
 * </pre>
 *
 * <p>
 * Please refer to the readme.txt for instructions on how to run this examples.
 *
 * @author cl/tam  2015.04.27
 * @since  12.2.1
 */
public class Driver
    {
    // ----- static methods -------------------------------------------------

    /**
     * Execute Federation examples.
     *
     * @param asArgs [3] = cluster name to connect
     *               [4] = optional "cohql" or "console" to start cohql or console
     *               instead of GUI
     */
    public static void main(String[] asArgs)
            throws Exception
        {
        String sCluster = "NONE";
        String sCommand = null;

        if (asArgs.length > 3)
            {
            sCluster = asArgs[3];
            }
        if (asArgs.length > 4)
            {
            sCommand = asArgs[4];
            if (!sCommand.equals(COHQL) &&
                !sCommand.equals(CONSOLE))
                {
                System.err.println("You can only specify either " + COHQL +
                                   " or " + CONSOLE + " as an argument.");
                System.exit(1);
                }
            }

        if (!CLUSTER_A.equals(sCluster) &&
            !CLUSTER_B.equals(sCluster))
            {
            System.err.println("Please specify " + CLUSTER_A + " or " + CLUSTER_B);
            System.exit(1);
            }

        setProperties(sCluster);

        Cluster                  cluster = CacheFactory.ensureCluster();
        NamedCache<UUID, String> cache   =
                CacheFactory.getTypedCache("contacts", withTypes(UUID.class, String.class));

        if (COHQL.equals(sCommand))
            {
            QueryPlus.main(new String[0]);
            }
        else if (CONSOLE.equals(sCommand))
            {
            CacheFactory.main(new String[0]);
            }
        else
            {
            new Driver().showGUI(cluster, cache);
            }
        }

    /**
     * Setup the GUI and run a Timer to update cache size periodically for a
     * given cluster and cache.
     *
     * @param cluster  the Cluster we are connected to
     * @param cache    the cache to work on
     */
    private void showGUI(Cluster cluster, NamedCache<UUID, String> cache)
        {
        JFrame frame = new JFrame("Coherence Federated Caching Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JComponent contentPane = (JComponent) frame.getContentPane();

        JPanel pnlMain = new JPanel(new BorderLayout());

        // create the header panel
        JPanel pnlHeader = new JPanel(new GridLayout(2, 2));
        pnlHeader.add(new JLabel("Cluster name:"));
        txtClusterName = getTextField(15, JTextField.LEFT);
        txtClusterName.setText(cluster.getClusterName());
        pnlHeader.add(txtClusterName);

        pnlHeader.add(new JLabel("Contacts cache size:"));
        txtCacheSize = getTextField(15, JTextField.LEFT);
        pnlHeader.add(txtCacheSize);
        pnlMain.add(pnlHeader, BorderLayout.NORTH);

        pnlMain.add(new JLabel(" "), BorderLayout.CENTER);

        // create action panel
        JPanel pnlAction = new JPanel(new FlowLayout());
        btnInsert = new JButton("Insert");
        pnlAction.add(btnInsert);

        // add handler to insert data into cache
        btnInsert.addActionListener(action -> insertData(cache, getInsertSize()));

        pnlAction.add(new JLabel("Count:"));
        txtInsertSize = getTextField(5, JTextField.LEFT);
        txtInsertSize.setEditable(true);
        txtInsertSize.setText("100");
        pnlAction.add(txtInsertSize);

        btnClear = new JButton("Clear Cache");
        pnlAction.add(btnClear);

        // add handler to clear the cache
        btnClear.addActionListener(action -> cache.clear());

        pnlMain.add(pnlAction, BorderLayout.SOUTH);

        contentPane.add(pnlMain);
        contentPane.setOpaque(true);
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        frame.setContentPane(contentPane);

        frame.pack();
        frame.setVisible(true);

        // refresh cache size every 2 seconds
        Timer timer = new Timer(2000, action ->
                txtCacheSize.setText(String.format("%,-20d", cache.size())));
        timer.start();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the value of the txtInsertSize field or DEFAULT if invalid.
     *
     * @return  the value of the txtInsertSize field or DEFAULT if invalid
     */
    private int getInsertSize()
        {
        final int DEFAULT = 0;

        try
            {
            return Integer.valueOf(txtInsertSize.getText());
            }
        catch (NumberFormatException e)
            {
            txtInsertSize.setText(String.valueOf(DEFAULT));
            return DEFAULT;
            }
        }

    /**
     * Create a {@link JTextField} with the specified width and specified
     * alignment.
     *
     * @param  width  the width for the {@link JTextField}
     * @param  align  either {@link JTextField}.RIGHT or LEFT
     *
     * @return the newly created text field
     */
    private JTextField getTextField(int width, int align)
        {
        JTextField textField = new JTextField();

        textField.setEditable(false);
        textField.setColumns(width);
        textField.setHorizontalAlignment(align);

        textField.setOpaque(true);

        return textField;
        }

    /**
     * Set System Properties to ensure the example connects to the correct
     * cluster.
     *
     * @param sCluster  the cluster to connect to
     */
    private static void setProperties (String sCluster)
        {
        // set the cluster name
        System.setProperty("coherence.cluster", sCluster);

        if (CLUSTER_A.equals(sCluster))
            {
            System.setProperty("coherence.clusterport", "11100");
            }
        else if (CLUSTER_B.equals(sCluster))
            {
            System.setProperty("coherence.clusterport", "11200");
            }
        }

    /**
     * Insert the specified number of entries into the cache.
     *
     * @param cache     the cache to insert data into
     * @param cEntries  the number of entries to insert
     */
    private static void insertData (NamedCache<UUID, String> cache, int cEntries)
        {
        final int BATCH = 1000;

        Map<UUID, String> map = new HashMap<>();

        for (int i = 0; i < cEntries; i++)
            {
            map.put(new UUID(), getRandomData());
            if (i % BATCH == 0)
                {
                cache.putAll(map);
                map.clear();
                }
            }

        if (!map.isEmpty())
            {
            cache.putAll(map);
            }
        }

    /**
     * Return a random length String for inserting into the cache. Maximum
     * length is controlled by MAX_DATA_SIZE.
     *
     * @return a random length String
     */
    private static String getRandomData ()
        {
        int    nSize  = f_random.nextInt(MAX_DATA_SIZE);
        char[] acData = new char[nSize];
        for (int i = 0; i < nSize; i++)
            {
            acData[i] = 'X';
            }

        return new String(acData);
        }

    // ----- constants ------------------------------------------------------

    private static final Random f_random = new Random();

    private static final int MAX_DATA_SIZE = 100;

    private static final String CLUSTER_A = "ClusterA";
    private static final String CLUSTER_B = "ClusterB";

    private static final String COHQL   = "cohql";
    private static final String CONSOLE = "console";

    // ----- data members ---------------------------------------------------

    private JTextField txtClusterName;
    private JTextField txtCacheSize;
    private JTextField txtInsertSize;
    private JButton btnInsert;
    private JButton btnClear;
    }