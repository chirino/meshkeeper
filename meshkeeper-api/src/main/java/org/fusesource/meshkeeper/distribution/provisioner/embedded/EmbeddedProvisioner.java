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

import static org.fusesource.meshkeeper.control.ControlServer.ControlEvent.SHUTDOWN;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.fusesource.meshkeeper.MeshEvent;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeperFactory;
import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.distribution.provisioner.Provisioner;

/**
 * EmbeddedProvisioner
 * <p>
 * The embedded provisioner provisions local MeshKeeper controller. It supports
 * in process provisioning and shutdown of a controller running in another
 * process
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class EmbeddedProvisioner implements Provisioner {

    private static EmbeddedServer EMBEDDED_SERVER;
    private static final Object SYNC = new Object();
    private boolean machineOwnerShip;
    private String deploymentUri;
    private int registryPort = 0;
    private int provisioningTimeout = -1;

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#deploy()
     */
    public void deploy() throws MeshProvisioningException {
        synchronized (SYNC) {
            if (EMBEDDED_SERVER == null) {
                EMBEDDED_SERVER = new EmbeddedServer();
                try {
                    EMBEDDED_SERVER.setDataDirectory(getControlServerDirectory());
                    EMBEDDED_SERVER.setRegistryPort(registryPort);
                    EMBEDDED_SERVER.start();
                } catch (Exception e) {
                    throw new MeshProvisioningException("Error starting embedded server", e);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#unDeploy(boolean)
     */
    public void unDeploy(boolean force) throws MeshProvisioningException {
        synchronized (SYNC) {
            if (EMBEDDED_SERVER != null) {
                try {
                    EMBEDDED_SERVER.stop();
                    EMBEDDED_SERVER = null;
                } catch (Exception e) {
                    throw new MeshProvisioningException("Error starting embedded server", e);
                }
            }
            //Might be running elsewhere:
            else {
                MeshKeeper mesh = null;
                try {
                    mesh = MeshKeeperFactory.createMeshKeeper(findMeshRegistryUri());
                    mesh.eventing().sendEvent(new MeshEvent(SHUTDOWN.ordinal(), this.getClass().getSimpleName(), null), ControlServer.CONTROL_TOPIC);

                    File f = new File(MeshKeeperFactory.getDefaultServerDirectory(), ControlServer.CONTROLLER_PROP_FILE_NAME);
                    long timeout = System.currentTimeMillis() + 5000;
                    while (System.currentTimeMillis() < timeout && f.exists()) {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new MeshProvisioningException("interrupted", ie);
                } catch (Exception e) {

                }

            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#findMeshRegistryUri()
     */
    public String findMeshRegistryUri() throws MeshProvisioningException {
        if (EMBEDDED_SERVER != null) {
            return EMBEDDED_SERVER.getRegistryUri();
        } else {
            try {
                Properties p = getFileProps();
                String registryUri = p.getProperty(MeshKeeperFactory.MESHKEEPER_REGISTRY_PROPERTY);
                if (registryUri != null) {
                    return registryUri;
                }

            } catch (Exception e) {
            }
            throw new MeshProvisioningException("Embedded Server not started");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#getDeploymentUri()
     */
    public String getDeploymentUri() {
        return "embedded:";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#getPreferredControlHost()
     */
    public String getPreferredControlHost() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#getRequestedAgentHosts()
     */
    public String[] getRequestedAgentHosts() {
        try {
            return new String[] { InetAddress.getLocalHost().getCanonicalHostName() };
        } catch (UnknownHostException e) {
            return new String[] { "localhost" };
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.MeshProvisioner#getStatus(java.lang.StringBuffer
     * )
     */
    public synchronized StringBuffer getStatus(StringBuffer buffer) throws MeshProvisioningException {
        if (buffer == null) {
            buffer = new StringBuffer(512);
        }

        if (EMBEDDED_SERVER != null) {
            buffer.append("Embedded MeshKeeper is deployed at: " + findMeshRegistryUri());
        } else {
            buffer.append("Embedded MeshKeeper is not deployed\n");
        }

        return buffer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#isDeployed()
     */
    public synchronized boolean isDeployed() throws MeshProvisioningException {
        if (EMBEDDED_SERVER != null) {
            return true;
        }
        //Possible that this is deployed locally in another process.
        else if (deploymentUri != null) {

            MeshKeeper mesh = null;
            try {
                //See if we can connect:
                mesh = MeshKeeperFactory.createMeshKeeper(findMeshRegistryUri());
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                if (mesh != null) {
                    try {
                        mesh.destroy();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    File getControlServerDirectory() throws Exception {
        if (deploymentUri != null) {
            return new File(deploymentUri);
        } else {
            return MeshKeeperFactory.getDefaultServerDirectory();
        }
    }

    private Properties getFileProps() throws Exception {
        File propFile = new File(getControlServerDirectory(), ControlServer.CONTROLLER_PROP_FILE_NAME);

        if (propFile.exists()) {
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(propFile);
                props.load(fis);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
            return props;
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#reDeploy(boolean)
     */
    public void reDeploy(boolean force) throws MeshProvisioningException {
        unDeploy(true);
        deploy();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#setDeploymentUri()
     */
    public void setDeploymentUri(String deploymentUri) {
        this.deploymentUri = deploymentUri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#setPreferredControlHost()
     */
    public void setPreferredControlHost(String preferredControlHost) {
        // No-Op we'll always go local
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.MeshProvisioner#setRequestedAgentHosts(java
     * .lang.String[])
     */
    public void setRequestedAgentHosts(String[] agentHosts) {
        // No-Op we'll always go local
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * getAgentMachineOwnership()
     */
    public boolean getAgentMachineOwnership() {
        return machineOwnerShip;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.provisioner.Provisioner#getMaxAgents
     * ()
     */
    public int getMaxAgents() {
        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * setAgentMachineOwnership(boolean)
     */
    public void setAgentMachineOwnership(boolean machineOwnerShip) {
        this.machineOwnerShip = machineOwnerShip;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.provisioner.Provisioner#setMaxAgents
     * (int)
     */
    public void setMaxAgents(int maxAgents) {
        //No-Op for now only one locally.
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * setRegistryPort(int)
     */
    public void setRegistryPort(int port) {
        this.registryPort = port;
    }

    /**
     * 
     * @return The time allows to wait for each provisioned component to come
     *         online.
     */
    public long getProvisioningTimeout() {
        return provisioningTimeout;
    }

    /**
     * sets the time allows to wait for each provisioned component to come
     * online.
     * 
     * @param provisioningTimeout
     *            the time allows to wait for each provisioned component to come
     *            online.
     */
    public void setProvisioningTimeout(long provisioningTimeout) {

    }

}
