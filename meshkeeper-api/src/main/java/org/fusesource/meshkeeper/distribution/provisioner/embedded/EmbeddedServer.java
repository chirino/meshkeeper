package org.fusesource.meshkeeper.distribution.provisioner.embedded;

import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeperFactory;
import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.launcher.LaunchAgent;

/**
 * @author chirino
 */
public class EmbeddedServer {

    MeshKeeper meshKeeper;
    LaunchAgent launchAgent;
    ControlServer controlServer;
    String registryURI;

    public void start() throws Exception {
        // We need to start up..
        if (registryURI != null) {
            return;
        } else {
            registryURI = "zk:tcp://localhost:2101";
            controlServer = MeshKeeperFactory.createControlServer(registryURI);
            meshKeeper = MeshKeeperFactory.createMeshKeeper(registryURI);
            launchAgent = MeshKeeperFactory.createAgent(meshKeeper);
        }
    }

    public void stop() throws Exception {
        Exception first = null;
        try {
            launchAgent.stop();
        } catch (Exception e) {
            first = e;
        }
        try {
            meshKeeper.destroy();
        } catch (Exception e) {
            first = first == null ? e : first;
        }
        try {
            controlServer.destroy();
        } catch (Exception e) {
            first = first == null ? e : first;
        }
        registryURI = null;
        if (first != null) {
            throw first;
        }
    }

    public String getRegistryUri() {
        if (controlServer != null) {
            return controlServer.getRegistryConnectUri();
        } else {
            return registryURI;
        }
    }
}