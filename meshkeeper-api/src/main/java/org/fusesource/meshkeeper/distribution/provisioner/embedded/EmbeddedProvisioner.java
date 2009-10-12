/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.provisioner.embedded;

import org.fusesource.meshkeeper.distribution.provisioner.Provisioner;

/**
 * EmbeddedProvisioner
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class EmbeddedProvisioner implements Provisioner {

    private static EmbeddedServer EMBEDDED_SERVER;
    private static final Object SYNC = new Object();

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
            throw new MeshProvisioningException("Embedded Server not started");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#getDeploymentUri()
     */
    public String getDeploymentUri() {
        // TODO Auto-generated method stub
        return "embedded:";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#getPreferredControlHost()
     */
    public String getPreferredControlHost() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#getRequestedAgentHosts()
     */
    public String[] getRequestedAgentHosts() {
        // TODO Auto-generated method stub
        return null;
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
    public boolean isDeployed() throws MeshProvisioningException {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#reDeploy(boolean)
     */
    public void reDeploy(boolean force) throws MeshProvisioningException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshProvisioner#setDeploymentUri()
     */
    public void setDeploymentUri(String uri) {
        // No-Op 

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
        // TODO Auto-generated method stub

    }

}
