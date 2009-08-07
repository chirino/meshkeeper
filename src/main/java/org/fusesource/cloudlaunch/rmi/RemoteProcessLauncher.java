package org.fusesource.cloudlaunch.rmi;

import org.fusesource.cloudlaunch.HostProperties;
import org.fusesource.cloudlaunch.LaunchDescription;
import org.fusesource.cloudlaunch.LocalProcess;
import org.fusesource.cloudlaunch.ProcessLauncher;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.registry.Registry;
import org.fusesource.cloudlaunch.registry.zk.ZooKeeperFactory;
import org.fusesource.rmiviajms.internal.ActiveMQRemoteSystem;
import org.apache.activemq.command.ActiveMQQueue;

import java.net.URI;
import java.rmi.RemoteException;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author chirino
 */
public class RemoteProcessLauncher implements IRemoteProcessLauncher {

    private final ProcessLauncher processLauncher = new ProcessLauncher() {
        @Override
        protected LocalProcess createLocalProcess(LaunchDescription launchDescription, ProcessListener handler, int pid) throws RemoteException {
            return new RemotedProcess(this, launchDescription, handler, pid);
        }
    };
    private RemoteListenerMonitor monitor = new RemoteListenerMonitor(processLauncher);
    private Registry registry;
    Exporter exporter;

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    /**
     * Sets the url of common resources accessible to this agent. This can be
     * used to pull down things like jvm images from a central location.
     * 
     * @param url
     */
    public void setCommonResourceRepoUrl(String url) {
        processLauncher.setCommonResourceRepoUrl(url);
    }

    /**
     * Clears the local resource repository.
     */
    public void purgeResourceRepository() throws Exception {
        processLauncher.purgeResourceRepository();
    }

    public void bind(String owner) throws Exception {
        processLauncher.bind(owner);
    }

    public void unbind(String owner) throws Exception {
        processLauncher.unbind(owner);
    }

    public IRemoteProcess launch(LaunchDescription launchDescription, IRemoteProcessListener listener) throws Exception {
        RemotedProcess lp = (RemotedProcess) processLauncher.launch(launchDescription, new RemoteProcessListener(listener));
        return lp.getProxy();
    }

    public HostProperties getHostProperties()
    {
        return processLauncher.getHostProperties();
    }
    
    public void start() throws Exception {
        processLauncher.start();
        monitor.start();

        exporter = new Exporter();
        exporter.setRegistry(registry);
        exporter.setSource(this);
        exporter.setPath(IRemoteProcessLauncher.REGISTRY_PATH + "/" + getAgentId());
        exporter.setDestination(new ActiveMQQueue(processLauncher.getAgentId()));
        exporter.export();

        System.out.println("PROCESS LAUNCHER + " + getAgentId() + " STARTED\n");
    }

    public void stop() throws Exception {

        exporter.destroy();
        processLauncher.stop();
        monitor.stop();
    }

    public String getAgentId() {
        return processLauncher.getAgentId();
    }

    public void setAgentId(String id) {
        processLauncher.setAgentId(id);
    }

    public File getDataDirectory() {
        return processLauncher.getDataDirectory();
    }

    public void setDataDirectory(File dataDirectory) {
        processLauncher.setDataDirectory(dataDirectory);
    }

    public Map<Integer, LocalProcess> getProcesses() {
        return processLauncher.getProcesses();
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
        Registry registry = null;
        LinkedList<String> alist = new LinkedList<String>(Arrays.asList(args));

        while (!alist.isEmpty()) {
            String arg = alist.removeFirst();
            if (arg.equals("-help") || arg.equals("-h")) {
                RemoteProcessLauncher.showUsage();
                return;
            } else if (arg.equals("-url")) {
                System.setProperty(ActiveMQRemoteSystem.CONNECT_URL_PROPNAME, alist.removeFirst());
            } else if (arg.equals("-commonRepoUrl")) {
                commonRepoUrl = alist.removeFirst();
            } else if (arg.equals("-zkUrl")) {

                try {
                    ZooKeeperFactory factory = new ZooKeeperFactory();
                    factory.setConnectUrl(alist.removeFirst());
                    registry = factory.getRegistry();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error connecting zoo-keeper: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }

        RemoteProcessLauncher agent = new RemoteProcessLauncher();
        agent.setCommonResourceRepoUrl(commonRepoUrl);
        agent.setRegistry(registry);

        //        agent.setPropFileName(argv[0]);
        try {
            agent.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}