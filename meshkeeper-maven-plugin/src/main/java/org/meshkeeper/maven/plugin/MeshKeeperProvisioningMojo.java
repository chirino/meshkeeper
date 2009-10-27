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
package org.meshkeeper.maven.plugin;

import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.fusesource.meshkeeper.distribution.provisioner.Provisioner;
import org.fusesource.meshkeeper.distribution.provisioner.ProvisionerFactory;

/**
 * Goal for provisioning meshkeeper.
 * 
 * @goal provision
 * 
 * @phase process-sources
 */
public class MeshKeeperProvisioningMojo extends AbstractMojo {

    /**
     * Type of provisioning to do deploy or undeploy
     * 
     * @parameter default-value="deploy"
     * 
     */
    private String action;

    /**
     * The uri used to specify the provisioning class. An example would be:
     * cloudmix:http://localhost:8181
     * 
     * @parameter expression="${meshkeeper.provisionerUri}"
     *            default-value="embedded"
     */
    private String provisionerUri;

    /**
     * The control-url for the provisioner. This can be used to set a value used
     * to connect to a provisioner (this may be set in cases where such a url is
     * not encoded in the provisionerUri
     * 
     * @parameter expression="${meshkeeper.provisioner.deploymentUrl}"
     */
    private URL deploymentUrl;

    /**
     * The preferred control host.
     * 
     * @parameter expression="${meshkeeper.provisioner.preferredControlHost}"
     */
    private String controlHost;

    /**
     * The preferred list of agent hosts.
     * 
     * @parameter
     */
    private String[] agentHosts;

    /**
     * Sets the maximum number of launch agents to deploy. In the absence
     * of a specified list of agent hosts this will limit the number of 
     * agents deployed. If agentHosts are specified then the number of
     * agents deployed will be the minimum of the number of agentHots or
     * this limit, with agentHosts dictating which hosts should be used. 
     * 
     * @parameter
     */
    private Integer maxAgents;

    /**
     * The port on which the MeshKeeper provisioned registry server should
     * listen. Specifying 0 causes the control server to choose an appropriate
     * port.
     * 
     * @parameter
     */
    private Integer registryPort;

    /**
     * Instructs the underlying provisioner to grant the provisioned agents
     * exclusive ownership of the machines on which they are provisioned. When
     * supported this should prevent other users from using the machines.
     * 
     * @parameter
     */
    private Boolean agentMachineOwnership;

    public void execute() throws MojoExecutionException {
        Provisioner provisioner = null;
        try {
            provisioner = new ProvisionerFactory().create(provisionerUri);
        } catch (Throwable thrown) {
            thrown.printStackTrace();
            throw new MojoExecutionException("Failure instantiating provisioner", thrown);
        }

        if (deploymentUrl != null) {
            provisioner.setDeploymentUri(deploymentUrl.toString());
        }

        if (agentHosts != null) {
            provisioner.setRequestedAgentHosts(agentHosts);
        }

        if (maxAgents != null) {
            provisioner.setMaxAgents(maxAgents);
        }

        if (controlHost != null) {
            provisioner.setPreferredControlHost(controlHost);
        }

        if (registryPort != null) {
            provisioner.setRegistryPort(registryPort);
        }

        if (agentMachineOwnership != null) {
            provisioner.setAgentMachineOwnership(agentMachineOwnership);
        }

        if (action.equals("deploy")) {
            try {
                provisioner.reDeploy(true);
                getLog().info(provisioner.getStatus(null).toString());
            } catch (Throwable thrown) {
                throw new MojoExecutionException("Failure provisioning meshkeeper", thrown);
            }
        } else if (action.equals("undeploy")) {
            try {
                provisioner.unDeploy(true);
            } catch (Throwable thrown) {
                throw new MojoExecutionException("Failure deprovisioning meshkeeper on cloudmix", thrown);
            }
        } else {
            throw new MojoExecutionException("Invalid provisioning action: " + action);
        }
    }
}
