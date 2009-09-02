package org.fusesource.cloudlaunch.launcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.Process;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.classloader.ClassLoaderFactory;
import org.fusesource.cloudlaunch.classloader.ClassLoaderServer;
import org.fusesource.cloudlaunch.classloader.ClassLoaderServerFactory;
import org.fusesource.cloudlaunch.classloader.Marshalled;
import org.fusesource.cloudlaunch.distribution.Distributor;

import java.util.concurrent.Executor;

/**
 * @author chirino
 */
public class RemoteExecutor implements Executor {
    public static final Log LOG = LogFactory.getLog(RemoteExecutor.class);

    private Distributor distributor;
    private ClassLoaderServer classLoaderServer;
    private LaunchAgentService agent;

    public Process launch(Runnable runnable, ProcessListener handler) throws Exception {
        ClassLoaderFactory factory = classLoaderServer.export(runnable.getClass().getClassLoader(), 100);
        Marshalled<Runnable> marshalled = new Marshalled<Runnable>(factory, runnable);
        return agent.launch(marshalled, handler);
    }

    public void execute(Runnable runnable) {
        try {
            launch(runnable, null);
        } catch (Exception e) {
            LOG.warn("Failed to launch runnable task: "+e, e);
        }
    }

    public void start() throws Exception {
        if( agent == null ) {
            throw new IllegalArgumentException("agent property must be set");
        }
        if( classLoaderServer==null ) {
            if( distributor == null ) {
                throw new IllegalArgumentException("distributor or classLoaderServer property must be set");
            }
            classLoaderServer = ClassLoaderServerFactory.create("basic:", distributor);
        }
        classLoaderServer.start();
    }

    public void stop() throws Exception {
        classLoaderServer.stop();
    }

    public LaunchAgentService getAgent() {
        return agent;
    }

    public void setAgent(LaunchAgentService agent) {
        this.agent = agent;
    }

    public ClassLoaderServer getClassLoaderServer() {
        return classLoaderServer;
    }

    public void setClassLoaderServer(ClassLoaderServer classLoaderServer) {
        this.classLoaderServer = classLoaderServer;
    }

    public Distributor getDistributor() {
        return distributor;
    }

    public void setDistributor(Distributor distributor) {
        this.distributor = distributor;
    }
}