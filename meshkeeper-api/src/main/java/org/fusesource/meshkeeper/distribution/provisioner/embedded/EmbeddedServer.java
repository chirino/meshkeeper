/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.fusesource.meshkeeper.distribution.provisioner.embedded;

import java.io.File;

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
    File dataDirectory;

    String registryURI;

    public File getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(File dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void start() throws Exception {
        // We need to start up..
        if (registryURI != null) {
            return;
        } else {
            registryURI = "zk:tcp://localhost:2101";
            if (dataDirectory == null) {
                dataDirectory = MeshKeeperFactory.getDefaultServerDirectory();
            }
            controlServer = MeshKeeperFactory.createControlServer(registryURI, dataDirectory);
            //Add shutdown hook:
            controlServer.setPreShutdownHook(new Runnable() {
                public void run() {
                    preServerShutdown(false);
                }
            });
            meshKeeper = MeshKeeperFactory.createMeshKeeper(registryURI);
            launchAgent = MeshKeeperFactory.createAgent(meshKeeper);
        }
    }

    private void preServerShutdown(boolean internal) {
        Exception first = null;
        if (launchAgent != null) {
            try {
                launchAgent.stop();
            } catch (Exception e) {
                first = e;
            } finally {
                launchAgent = null;
            }
        }
        if (meshKeeper != null) {
            try {
                meshKeeper.destroy();
            } catch (Exception e) {
                first = first == null ? e : first;
            } finally {
                meshKeeper = null;
            }
        }
        if (!internal) {
            controlServer = null;
            registryURI = null;
        }
    }

    public void stop() throws Exception {
        Exception first = null;

        controlServer.setPreShutdownHook(null);

        try {
            preServerShutdown(true);
        } catch (Exception e) {
            first = first == null ? e : first;
        }
        if (controlServer != null) {
            try {
                controlServer.destroy();
            } catch (Exception e) {
                first = first == null ? e : first;
            } finally {
                controlServer = null;
            }
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