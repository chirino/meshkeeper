/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  The software in this package is published under the terms of the AGPL license
 *  a copy of which has been included with this distribution in the license.txt file.
 */
package org.fusesource.meshkeeper.cloudmix;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudmix.agent.RestGridClient;
import org.fusesource.cloudmix.common.CloudmixHelper;
import org.fusesource.cloudmix.common.GridClient;
import org.fusesource.cloudmix.common.dto.AgentDetails;
import org.fusesource.cloudmix.common.dto.Dependency;
import org.fusesource.cloudmix.common.dto.DependencyStatus;
import org.fusesource.cloudmix.common.dto.FeatureDetails;
import org.fusesource.cloudmix.common.dto.ProfileDetails;
import org.fusesource.cloudmix.common.dto.ProfileStatus;
import org.fusesource.meshkeeper.distribution.PluginClassLoader;
import org.fusesource.meshkeeper.distribution.provisioner.Provisioner;
import org.fusesource.meshkeeper.distribution.registry.RegistryClient;
import org.fusesource.meshkeeper.distribution.registry.RegistryFactory;

/**
 * CloudMixSupport
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class CloudMixProvisioner implements Provisioner {

    Log LOG = LogFactory.getLog(CloudMixProvisioner.class);

    private static final String MESH_KEEPER_CONTROL_PROFILE_ID = "MeshKeeperControl";
    private static final String MESH_KEEPER_CONTROL_FEATURE_ID = MESH_KEEPER_CONTROL_PROFILE_ID + ":Control-Server";
    private static final String MESH_KEEPER_AGENT_PROFILE_ID = "MeshKeeperAgent";
    private static final String MESH_KEEPER_AGENT_FEATURE_ID = MESH_KEEPER_AGENT_PROFILE_ID + ":Launcher";

    private String controllerUrl;
    private String preferredControlControlHost;
    private String[] requestedAgentHosts;
    private RestGridClient gridClient;
    private String cachedRegistryConnectUri = null;

    private boolean machineOwnerShip;

    private int maxAgents;

    public void dumpStatus() throws MeshProvisioningException {
        StringBuffer buf = new StringBuffer(1024);
        getStatus(buf);
        LOG.info(buf.toString());
    }

    protected boolean isProvisioned(String profileId) throws MeshProvisioningException {

        ProfileStatus profileStatus = getGridClient().getProfileStatus(profileId);
        if (profileStatus != null) {
            List<DependencyStatus> dependencyStatus = profileStatus.getFeatures();
            for (DependencyStatus status : dependencyStatus) {
                if (!status.isProvisioned()) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Asserts that all the requested features have been provisioned properly
     */
    protected void assertProvisioned(String profileId) throws MeshProvisioningException {
        long start = System.currentTimeMillis();

        Set<String> provisionedFeatures = new TreeSet<String>();
        Set<String> failedFeatures = null;
        while (true) {
            failedFeatures = new TreeSet<String>();
            long now = System.currentTimeMillis();

            ProfileStatus profileStatus = getGridClient().getProfileStatus(profileId);
            if (profileStatus != null) {
                List<DependencyStatus> dependencyStatus = profileStatus.getFeatures();
                for (DependencyStatus status : dependencyStatus) {
                    String featureId = status.getFeatureId();
                    if (status.isProvisioned()) {
                        if (provisionedFeatures.add(featureId)) {
                            System.out.println("Provisioned feature: " + featureId);
                        }
                    } else {
                        failedFeatures.add(featureId);
                    }
                }
            } else {
                throw new RuntimeException("Profile status not found!");
            }
            if (failedFeatures.isEmpty()) {
                return;
            }

            long delta = now - start;
            if (delta > 20000) {
                throw new MeshProvisioningException("Provision failure. Not enough instances of features: " + failedFeatures + " after waiting " + (20000 / 1000) + " seconds");
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public RestGridClient getGridClient() throws MeshProvisioningException {
        if (gridClient == null) {
            gridClient = createGridController();

        }
        return gridClient;
    }

    /**
     * Returns a newly created client. Factory method
     */
    protected RestGridClient createGridController() throws MeshProvisioningException {
        System.out.println("About to create RestGridClient for: " + controllerUrl);
        return new RestGridClient(controllerUrl);
    }

    private String getMeshKeeperVersion() {
        return PluginClassLoader.getModuleVersion();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.provisioner.Provisioner#deploy()
     */
    public void deploy() throws MeshProvisioningException {

        GridClient controller = getGridClient();

        if (isDeployed()) {
            return;
        }

        //Set up the controller:
        ProfileDetails controlProfile = new ProfileDetails();
        controlProfile.setId(MESH_KEEPER_CONTROL_PROFILE_ID);
        controlProfile.setDescription("This Profile hosts MeshKeeper control server instances");

        FeatureDetails controlFeature = new FeatureDetails();
        controlFeature.setId(MESH_KEEPER_CONTROL_FEATURE_ID);
        controlFeature.setMaximumInstances("1");
        if (preferredControlControlHost != null) {
            controlFeature.preferredMachine(preferredControlControlHost);
        }
        controlFeature.setResource("mop:update run org.fusesource.meshkeeper:meshkeeper-api:" + getMeshKeeperVersion() + " " + org.fusesource.meshkeeper.control.Main.class.getName()
                + " --jms activemq:tcp://0.0.0.0:4041" + " --registry zk:tcp://0.0.0.0:4040");
        controlFeature.setOwnedByProfileId(controlProfile.getId());
        controlFeature.setOwnsMachine(false);
        controlFeature.validContainerType("mop");
        controller.addFeature(controlFeature);
        controlProfile.getFeatures().add(new Dependency(controlFeature.getId()));
        controller.addProfile(controlProfile);

        //Wait for the control profile to be provisioned:
        assertProvisioned(controlProfile.getId());

        //Get the control host:
        List<String> agents = controller.getAgentsAssignedToFeature(MESH_KEEPER_CONTROL_FEATURE_ID);
        AgentDetails details = controller.getAgentDetails(agents.get(0));
        details.getHostname();
        String controlHost = details.getHostname();

        LOG.info("MeshKeeper controller provisioned to: " + controlHost);
        cachedRegistryConnectUri = "zk:tcp://" + controlHost + ":4040";

        long timeout = 60000;
        LOG.info("Waiting " + timeout / 1000 + "s for MeshKeeper control server to come on line.");

        RegistryClient registry = null;
        try {
            registry = new RegistryFactory().create(cachedRegistryConnectUri + "?connectTimeout=" + timeout);

        } catch (Exception e) {
            unDeploy(true);
            throw new MeshProvisioningException("Unable to connect to deployed MeshKeeper controller", e);
        } finally {
            try {
                if (registry != null) {
                    registry.destroy();
                }
            } catch (Exception e) {
            }
        }

        LOG.info("MeshKeeper controller is online, deploying MeshKeeper agent profile");

        ProfileDetails agentProfile = new ProfileDetails();
        agentProfile.setId(MESH_KEEPER_AGENT_PROFILE_ID);
        agentProfile.setDescription("MeshKeeper launch agent");
        FeatureDetails agentFeature = new FeatureDetails();
        //agentFeature.addProperty(MeshKeeperFactory.MESHKEEPER_REGISTRY_PROPERTY, registyConnect);
        agentFeature.setId(MESH_KEEPER_AGENT_FEATURE_ID);
        agentFeature.depends(controlFeature);
        agentFeature.setResource("mop:update run org.fusesource.meshkeeper:meshkeeper-api:" + getMeshKeeperVersion() + " " + org.fusesource.meshkeeper.launcher.Main.class.getName() + " --registry "
                + cachedRegistryConnectUri);

        if (requestedAgentHosts != null && requestedAgentHosts.length > 0) {
            agentFeature.setPreferredMachines(new HashSet<String>(Arrays.asList(requestedAgentHosts)));
        }

        agentFeature.setOwnsMachine(false);
        agentFeature.setMaximumInstances("100");
        agentFeature.validContainerType("mop");
        agentFeature.setOwnedByProfileId(agentProfile.getId());
        controller.addFeature(agentFeature);
        agentProfile.getFeatures().add(new Dependency(agentFeature.getId()));
        controller.addProfile(agentProfile);

        assertProvisioned(agentProfile.getId());

        //TODO: should perhaps use our Registry created above to watch for launch agents:

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * findMeshRegistryUri()
     */
    public String findMeshRegistryUri() throws MeshProvisioningException {
        if (cachedRegistryConnectUri == null) {
            GridClient controller = getGridClient();
            List<String> agents = controller.getAgentsAssignedToFeature(MESH_KEEPER_CONTROL_FEATURE_ID);
            if (agents != null) {
                AgentDetails details = controller.getAgentDetails(agents.get(0));
                details.getHostname();
                String controlHost = details.getHostname();
                cachedRegistryConnectUri = "zk:tcp://" + controlHost + ":4040";
            } else {
                throw new MeshProvisioningException("MeshKeeper is not deployed");
            }
        }

        return cachedRegistryConnectUri;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * getDeploymentUri()
     */
    public String getDeploymentUri() {
        return controllerUrl;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * getPreferredControlHost()
     */
    public String getPreferredControlHost() {
        return preferredControlControlHost;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * getRequestedAgentHosts()
     */
    public String[] getRequestedAgentHosts() {
        return requestedAgentHosts;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.provisioner.Provisioner#getStatus
     * (java.lang.StringBuffer)
     */
    public StringBuffer getStatus(StringBuffer buffer) throws MeshProvisioningException {
        GridClient controller = getGridClient();

        if (buffer == null) {
            buffer = new StringBuffer(1024);
        }

        boolean foundProfile = false;
        for (String profile : new String[] { MESH_KEEPER_AGENT_PROFILE_ID, MESH_KEEPER_CONTROL_PROFILE_ID }) {
            ProfileStatus status = controller.getProfileStatus(profile);
            if (status != null) {
                foundProfile = true;
                buffer.append("Found profile: " + status.getId() + "\n");
            }
        }

        if (!foundProfile) {
            buffer.append("No MeshKeeper profiles found\n");
        }

        boolean foundFeatures = false;
        for (String feature : new String[] { MESH_KEEPER_CONTROL_FEATURE_ID, MESH_KEEPER_AGENT_FEATURE_ID }) {
            List<String> agents = controller.getAgentsAssignedToFeature(feature);
            if (agents != null && !agents.isEmpty()) {
                foundFeatures = true;
                buffer.append("Found agents running " + feature + ": " + agents + "\n");
            }
        }

        if (!foundFeatures) {
            buffer.append("MeshKeeper not currently deployed\n");
        }

        return buffer;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.provisioner.Provisioner#isDeployed
     * ()
     */
    public boolean isDeployed() throws MeshProvisioningException {

        //Check that mesh profiles are deployed
        for (String profile : new String[] { MESH_KEEPER_AGENT_PROFILE_ID, MESH_KEEPER_CONTROL_PROFILE_ID }) {
            if (!isProvisioned(profile)) {
                return false;
            }

        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.provisioner.Provisioner#reDeploy
     * (boolean)
     */
    public void reDeploy(boolean force) throws MeshProvisioningException {
        unDeploy(force);
        deploy();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * setDeploymentUri()
     */
    public void setDeploymentUri(String uri) {
        controllerUrl = uri;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * setPreferredControlHost()
     */
    public void setPreferredControlHost(String preferredControlServerAgent) {
        this.preferredControlControlHost = preferredControlServerAgent;

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.provisioner.Provisioner#
     * setRequestedAgentHosts(java.lang.String[])
     */
    public void setRequestedAgentHosts(String[] requestedAgentHosts) {
        this.requestedAgentHosts = requestedAgentHosts;
    }


    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.distribution.provisioner.Provisioner#getAgentMachineOwnership()
     */
    public boolean getAgentMachineOwnership() {
        return machineOwnerShip;
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.distribution.provisioner.Provisioner#getMaxAgents()
     */
    public int getMaxAgents() {
        return -1;
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.distribution.provisioner.Provisioner#setAgentMachineOwnership(boolean)
     */
    public void setAgentMachineOwnership(boolean machineOwnerShip) {
        this.machineOwnerShip = machineOwnerShip;
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.distribution.provisioner.Provisioner#setMaxAgents(int)
     */
    public void setMaxAgents(int maxAgents) {
        this.maxAgents = maxAgents;
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.provisioner.Provisioner#unDeploy
     * (boolean)
     */
    public void unDeploy(boolean force) throws MeshProvisioningException {
        GridClient controller = getGridClient();

        boolean removed = false;

        for (String profile : new String[] { MESH_KEEPER_AGENT_PROFILE_ID, MESH_KEEPER_CONTROL_PROFILE_ID }) {
            ProfileDetails existing = controller.getProfile(profile);
            if (existing != null) {
                LOG.info("Removing existing meshkeeper profile: " + profile);
                removed = true;
                controller.removeProfile(existing);
            }
        }

        if (!removed) {
            LOG.info("No existing meshkeeper profiles to remove");
        }
    }

    private static final void printUsage() {
        System.out.println("Usage:");
        System.out.println("[deploy|undeploy|status] [cloudmix-control-url] [preferedMeskKeeperControlAgent]");
    }

    public static final void main(String[] args) {

        String command = "deploy";

        if (args.length > 0) {
            command = args[0];
        }

        CloudMixProvisioner support = new CloudMixProvisioner();
        support.setDeploymentUri(CloudmixHelper.getDefaultRootUrl());

        if (args.length > 1) {
            support.setDeploymentUri(args[1]);
        }

        if (args.length > 2) {
            support.setPreferredControlHost(args[2]);
        }

        try {
            if (command.equalsIgnoreCase("deploy")) {
                support.reDeploy(true);
            } else if (command.equalsIgnoreCase("status")) {
                support.dumpStatus();
            } else if (command.equalsIgnoreCase("undeploy")) {
                support.unDeploy(true);
            } else {
                printUsage();
            }

        } catch (Throwable e) {
            System.err.println("Error running MeshKeeper CloudMix provisionner: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
