package org.fusesource.cloudlaunch.local;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.fusesource.cloudlaunch.HostProperties;
import org.fusesource.cloudlaunch.LaunchDescription;
import org.fusesource.cloudlaunch.Process;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.ResourceManager;
import org.fusesource.cloudlaunch.registry.zk.ZooKeeperFactory;
import org.fusesource.cloudlaunch.rmi.Distributor;
import org.fusesource.cloudlaunch.rmi.rmiviajms.RmiViaJmsExporter;

/**
 * @author chirino
 */
public class ProcessLauncher implements org.fusesource.cloudlaunch.ProcessLauncher {
    public static final long CLEANUP_TIMEOUT = 60000;
    public static final String LOCAL_REPO_PROP = "org.fusesource.testrunner.localRepoDir";

    private String exclusiveOwner;

    private String agentId; //The unique identifier for this agent (specified in ini file);
    private boolean started = false;
    private File dataDirectory = new File(".");
    private File localRepoDirectory;

    //ProcessHandlers:
    private final Map<Integer, LocalProcess> processes = new HashMap<Integer, LocalProcess>();
    int pidCounter = 0;
    private Thread shutdownHook;

    private ResourceManager resourceManager;

    private String commonResourceRepoUrl;

    private HostPropertiesImpl properties = new HostPropertiesImpl();

    private ProcessLauncherMonitor monitor = new ProcessLauncherMonitor(this);
    private Distributor distributor;

    synchronized public void bind(String owner) throws Exception {
        if (exclusiveOwner == null) {
            exclusiveOwner = owner;
            System.out.println("Now bound to: " + exclusiveOwner);
        } else if (!exclusiveOwner.equals(owner)) {
            throw new Exception("Bind failure, already bound: " + exclusiveOwner);
        }
    }

    synchronized public void unbind(String owner) throws Exception {
        if (exclusiveOwner == null) {
        } else if (exclusiveOwner.equals(owner)) {
            System.out.println("Bind to " + exclusiveOwner + " released");
            exclusiveOwner = null;
        } else {
            throw new Exception("Release failure, different owner: " + exclusiveOwner);
        }
    }

    synchronized public Process launch(LaunchDescription launchDescription, ProcessListener handler) throws Exception {
        int pid = pidCounter++;
        LocalProcess rc = createLocalProcess(launchDescription, handler, pid);
        processes.put(pid, rc);
        try {
            rc.start();
        } catch (Exception e) {
            processes.remove(pid);
            throw e;
        }
        
        return (Process) distributor.export(rc).getStub();
    }

    protected LocalProcess createLocalProcess(LaunchDescription launchDescription, ProcessListener handler, int pid) throws Exception {
        return new LocalProcess(this, launchDescription, handler, pid);
    }

    public synchronized void start() throws Exception {
        if (started) {
            return;
        }

        if (localRepoDirectory == null) {
            localRepoDirectory = new File(dataDirectory, "local-repo");
            System.getProperties().setProperty(LOCAL_REPO_PROP, localRepoDirectory.getCanonicalPath());
        }

        if (resourceManager == null) {
            resourceManager = new ResourceManager();
            resourceManager.setLocalRepoDir(localRepoDirectory);
            if (commonResourceRepoUrl != null) {
                resourceManager.setCommonRepo(commonResourceRepoUrl, null);
            }
        }

        started = true;
        if (agentId == null) {

            try {
                setAgentId(java.net.InetAddress.getLocalHost().getHostName());
            } catch (java.net.UnknownHostException uhe) {
                System.out.println("Error determining hostname.");
                uhe.printStackTrace();
                setAgentId("UNDEFINED");
            }
        }

        shutdownHook = new Thread(getAgentId() + "-Shutdown") {
            public void run() {
                System.out.println("Executing Shutdown Hook for " + ProcessLauncher.this);
                try {
                    ProcessLauncher.this.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        properties.fillIn(this);

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        monitor.start();

        distributor.register(this, ProcessLauncher.REGISTRY_PATH + "/" + getAgentId(), false);
        
        System.out.println("PROCESS LAUNCHER " + getAgentId() + " STARTED\n");

    }

    public synchronized void stop() throws Exception {
        if (!started) {
            return;
        }

        if (Thread.currentThread() != shutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }

        started = false;

        for (LocalProcess process : processes.values()) {
            try {
                process.kill();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        processes.clear();

        if (resourceManager != null) {
            try {
                resourceManager.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        monitor.requestCleanup();
        monitor.stop();

        distributor.unregister(this);
        distributor.unexport(this);

    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * Clears the launchers local resource cache.
     * 
     * @throws IOException
     *             If there is an error purging the cache.
     */
    public void purgeResourceRepository() throws IOException {
        if (resourceManager != null) {
            resourceManager.purgeLocalRepo();
        }
    }

    /**
     * Sets the url of common resources accessible to this agent. This can be
     * used to pull down things like jvm images from a central location.
     * 
     * @param url
     */
    public void setCommonResourceRepoUrl(String url) {
        commonResourceRepoUrl = url;
    }

    /**
     * Gets the url of common resources accessible to this agent.
     * 
     * @return
     */
    public String getCommonResourceRepoUrl() {
        return commonResourceRepoUrl;
    }

    /**
     * Sets the base directory where the agent puts it's data.
     * 
     * @param dataDirectory
     */
    public void setDataDirectory(File dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    /**
     * @return Gets the data directory where the launcher stores files.
     */
    public File getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Gets properties about the agent and it's host machine.
     * 
     * @return
     */
    public HostProperties getHostProperties() {
        return properties;
    }

    /**
     * Sets the name of the agent id. Once set it cannot be changed.
     * 
     * @param id
     *            the name of the agent id.
     */
    public void setAgentId(String id) {
        if (agentId == null && id != null) {
            agentId = id.trim().toUpperCase();
        }
    }

    /**
     * @return This agent's id.
     */
    public String getAgentId() {
        return agentId;
    }

    public Distributor getDistributor() {
        return distributor;
    }

    public void setDistributor(Distributor distributor) {
        this.distributor = distributor;
    }

    public Map<Integer, LocalProcess> getProcesses() {
        return processes;
    }

    public String toString() {
        return "ProcessLauncer-" + getAgentId();
    }

    /**
     * @param i
     */
    public void checkForRogueProcesses(int timeout) {
        // TODO Auto-generated method stub
        System.err.println("TODO: ProcessLauncher.checkForRogueProcesses PING NOT YET IMPLEMENTED");
    }

    private static final void showUsage() {
        System.out.println("Usage:");
        System.out.println("Args:");
        System.out.println(" -(h)elp -- this message");
        System.out.println(" -url <rmi url> -- specifies address of remote broker to connect to.");
        System.out.println(" -commonRepoUrl <url> -- specifies common resource location.");
        System.out.println(" -zkUrl <url> -- Specifies the zoo-keeper connect url (required).");
    }

    /*
     * public static void main()
     * 
     * Defines the entry point into this app.
     */
    public static void main(String[] args) {
        String jv = System.getProperty("java.version").substring(0, 3);
        if (jv.compareTo("1.5") < 0) {
            System.err.println("The RemoteProcessLauncher requires jdk 1.5 or higher to run, the current java version is " + System.getProperty("java.version"));
            System.exit(-1);
            return;
        }

        String commonRepoUrl = null;
        Distributor distributor = new Distributor();
        LinkedList<String> alist = new LinkedList<String>(Arrays.asList(args));

        while (!alist.isEmpty()) {
            String arg = alist.removeFirst();
            if (arg.equals("-help") || arg.equals("-h")) {
                ProcessLauncher.showUsage();
                return;
            } else if (arg.equals("-url")) {
                RmiViaJmsExporter rmiExporter = new RmiViaJmsExporter();
                rmiExporter.setConnectUrl(alist.removeFirst());
            } else if (arg.equals("-commonRepoUrl")) {
                commonRepoUrl = alist.removeFirst();
            } else if (arg.equals("-zkUrl")) {

                try {
                    ZooKeeperFactory factory = new ZooKeeperFactory();
                    factory.setConnectUrl(alist.removeFirst());
                    distributor.setRegistry(factory.getRegistry());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error connecting zoo-keeper: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }

        ProcessLauncher agent = new ProcessLauncher();
        agent.setCommonResourceRepoUrl(commonRepoUrl);
        distributor.start();
        agent.setDistributor(distributor);

        //        agent.setPropFileName(argv[0]);
        try {
            agent.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            try {
                distributor.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}