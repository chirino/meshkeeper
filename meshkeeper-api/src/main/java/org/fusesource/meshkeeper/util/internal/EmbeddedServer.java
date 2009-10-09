package org.fusesource.meshkeeper.util.internal;

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
    int aquireCounter;

    synchronized public String aquireEmbeddedRegistry() throws Exception {
        aquireCounter++;
        if( registryURI != null ) {
            return registryURI;
        }

        // We need to start up..
        registryURI = "zk:tcp://localhost:2101";
        controlServer = MeshKeeperFactory.createControlServer(registryURI);
        meshKeeper = MeshKeeperFactory.createMeshKeeper(registryURI);
        launchAgent = MeshKeeperFactory.createAgent(meshKeeper);
        return registryURI;
    }

    synchronized public void releaseEmbeddedRegistry() throws Exception {
        aquireCounter--;
        if( aquireCounter!= 0 ) {
            return;
        }

        Exception first=null;
        try {
            launchAgent.stop();
        } catch (Exception e) {
            first = e;
        }
        try {
            meshKeeper.destroy();
        } catch (Exception e) {
            first = first==null ? e : first;
        }
        try {
            controlServer.destroy();
        } catch (Exception e) {
            first = first==null ? e : first;
        }
        registryURI=null;
        if( first!=null ) {
            throw first;
        }

    }
}