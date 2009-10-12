/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.meshkeeper.cloudmix;

import java.net.URISyntaxException;
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

/**
 * CloudMixSupport
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class CloudMixSupport {

    Log LOG = LogFactory.getLog(CloudMixSupport.class);

    private static final String MESH_KEEPER_CONTROL_PROFILE_ID = "MeshKeeperControl";
    private static final String MESH_KEEPER_CONTROL_FEATURE_ID = MESH_KEEPER_CONTROL_PROFILE_ID + ":Control-Server";
    private static final String MESH_KEEPER_AGENT_PROFILE_ID = "MeshKeeperAgent";
    private static final String MESH_KEEPER_AGENT_FEATURE_ID = MESH_KEEPER_AGENT_PROFILE_ID + ":Launcher";

    private String controllerUrl;
    private String preferredControlServerAgent;
    private RestGridClient gridClient;

    public static class ProvisioningFailure extends Exception {
        private static final long serialVersionUID = 8167843986234465129L;

        ProvisioningFailure(String message) {
            super(message);
        }

        ProvisioningFailure(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public void setLog(Log log) {
        LOG = log;
    }

    public void killMeshKeeper() throws URISyntaxException {
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

    public void dumpStatus() throws URISyntaxException {
        GridClient controller = getGridClient();

        boolean foundProfile = false;
        for (String profile : new String[] { MESH_KEEPER_AGENT_PROFILE_ID, MESH_KEEPER_CONTROL_PROFILE_ID }) {
            ProfileStatus status = controller.getProfileStatus(profile);
            if (status != null) {
                foundProfile = true;
                LOG.info("Found profile: " + status.getId());
            }
        }

        if (!foundProfile) {
            LOG.info("No MeshKeeper profiles found");
        }

        boolean foundFeatures = false;
        for (String feature : new String[] { MESH_KEEPER_CONTROL_FEATURE_ID, MESH_KEEPER_AGENT_FEATURE_ID }) {
            List<String> agents = controller.getAgentsAssignedToFeature(feature);
            if (agents != null && !agents.isEmpty()) {
                foundFeatures = true;
                LOG.info("Found agents running " + feature + ": " + agents);
            }
        }

        if (!foundFeatures) {
            LOG.info("MeshKeeper not currently deployed");
        }

    }

    public void deployMeshKeeperProfile() throws URISyntaxException, ProvisioningFailure {
        GridClient controller = getGridClient();

        killMeshKeeper();

        //Set up the controller:
        ProfileDetails controlProfile = new ProfileDetails();
        controlProfile.setId(MESH_KEEPER_CONTROL_PROFILE_ID);
        controlProfile.setDescription("This Profile hosts MeshKeeper control server instances");

        FeatureDetails controlFeature = new FeatureDetails();
        controlFeature.setId(MESH_KEEPER_CONTROL_FEATURE_ID);
        controlFeature.setMaximumInstances("1");
        if (preferredControlServerAgent != null) {
            //            AgentDetails agent = controller.getAgentDetails(preferredControlServerAgent);
            //            if (agent.getContainerType().contains("mop")) {
            //                LOG.info("Setting preferred control agent:" + preferredControlServerAgent);
            //                controlFeature.preferredMachine(preferredControlServerAgent);
            //            }
            controlFeature.preferredMachine(preferredControlServerAgent);
        }
        controlFeature.setResource("mop:run org.fusesource.meshkeeper:meshkeeper-api:" + getMeshKeeperVersion() + " " + org.fusesource.meshkeeper.control.Main.class.getName()
                + " --jms activemq:tcp://0.0.0.0:4041" + " --registry zk:tcp://0.0.0.0:4040");
        controlFeature.setOwnedByProfileId(controlProfile.getId());
        controlFeature.setOwnsMachine(false);
        controlFeature.validContainerType("mop");
        controller.addFeature(controlFeature);
        controlProfile.getFeatures().add(new Dependency(controlFeature.getId()));
        controller.addProfile(controlProfile);

        //Wait for the control profile to be provisioned:
        assertProvisioned(controlProfile);

        //Get the control host:
        List<String> agents = controller.getAgentsAssignedToFeature(MESH_KEEPER_CONTROL_FEATURE_ID);
        AgentDetails details = controller.getAgentDetails(agents.get(0));
        details.getHostname();
        String controlHost = details.getHostname();

        LOG.info("MeshKeeper controller provisioned to: " + controlHost);
        String registyConnect = "zk:tcp://" + controlHost + ":4040";

        //TODO it would be better to poll for the agent's online state somehow instead:
        long delay = 30;
        LOG.info("Sleeping " + delay + "s to wait for controller to come on line");
        try {
            Thread.sleep(delay * 1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        //TODO it would be better to test connectivity:        
        //        RegistryFactory factory = new RegistryFactory();
        //        try {
        //            factory.create("zk:tcp://" + controlHost + ":4040");
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }

        ProfileDetails agentProfile = new ProfileDetails();
        agentProfile.setId(MESH_KEEPER_AGENT_PROFILE_ID);
        agentProfile.setDescription("MeshKeeper launch agent");
        FeatureDetails agentFeature = new FeatureDetails();
        //agentFeature.addProperty(MeshKeeperFactory.MESHKEEPER_REGISTRY_PROPERTY, registyConnect);
        agentFeature.setId(MESH_KEEPER_AGENT_FEATURE_ID);
        agentFeature.depends(controlFeature);
        agentFeature.setResource("mop:run org.fusesource.meshkeeper:meshkeeper-api:" + getMeshKeeperVersion() + " " + org.fusesource.meshkeeper.launcher.Main.class.getName() + " --registry "
                + registyConnect);

        agentFeature.setOwnsMachine(false);
        agentFeature.setMaximumInstances("100");
        agentFeature.validContainerType("mop");
        agentFeature.setOwnedByProfileId(agentProfile.getId());
        controller.addFeature(agentFeature);
        agentProfile.getFeatures().add(new Dependency(agentFeature.getId()));
        controller.addProfile(agentProfile);

        assertProvisioned(agentProfile);

        dumpStatus();
    }

    public String findMeshKeeperConnnectUrl() throws URISyntaxException, Exception {
        GridClient controller = getGridClient();
        List<String> agents = controller.getAgentsAssignedToFeature(MESH_KEEPER_CONTROL_FEATURE_ID);
        AgentDetails details = controller.getAgentDetails(agents.get(0));
        details.getHostname();
        String controlHost = details.getHostname();
        return "zk:tcp://" + controlHost + ":4040";
    }

    /**
     * Asserts that all the requested features have been provisioned properly
     */
    protected void assertProvisioned(ProfileDetails profile) throws ProvisioningFailure {
        long start = System.currentTimeMillis();

        Set<String> provisionedFeatures = new TreeSet<String>();
        Set<String> failedFeatures = null;
        while (true) {
            failedFeatures = new TreeSet<String>();
            long now = System.currentTimeMillis();

            try {
                ProfileStatus profileStatus = getGridClient().getProfileStatus(profile.getId());
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
            } catch (URISyntaxException e) {
                System.err.println("Failed to poll profile status: " + e);
                e.printStackTrace();
            }

            long delta = now - start;
            if (delta > 20000) {
                throw new ProvisioningFailure("Provision failure. Not enough instances of features: " + failedFeatures + " after waiting " + (20000 / 1000) + " seconds");
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public RestGridClient getGridClient() throws URISyntaxException {
        if (gridClient == null) {
            gridClient = createGridController();

        }
        return gridClient;
    }

    /**
     * Returns a newly created client. Factory method
     */
    protected RestGridClient createGridController() throws URISyntaxException {
        System.out.println("About to create RestGridClient for: " + controllerUrl);
        return new RestGridClient(controllerUrl);
    }

    private String getMeshKeeperVersion() {
        return PluginClassLoader.getModuleVersion();
    }

    public String getControllerUrl() {
        return controllerUrl;
    }

    public void setControllerUrl(String controllerUrl) {
        this.controllerUrl = controllerUrl;
    }

    public String getPreferredControlServerAgent() {
        return preferredControlServerAgent;
    }

    public void setPreferredControlServerAgent(String preferredControlServerAgent) {
        this.preferredControlServerAgent = preferredControlServerAgent;
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

        CloudMixSupport support = new CloudMixSupport();
        support.setControllerUrl(CloudmixHelper.getDefaultRootUrl());
        support.setControllerUrl("http://vm-fuseubt1:8181");
        support.setPreferredControlServerAgent("vm-fuseubt1.bedford.progress.com");

        if (args.length > 1) {
            support.setControllerUrl(args[1]);
        }

        if (args.length > 2) {
            support.setPreferredControlServerAgent(args[2]);
        }

        try {
            if (command.equalsIgnoreCase("deploy")) {
                support.deployMeshKeeperProfile();
            } else if (command.equalsIgnoreCase("status")) {
                support.dumpStatus();
            } else if (command.equalsIgnoreCase("undeploy")) {
                support.killMeshKeeper();
            } else {
                printUsage();
            }

        } catch (Throwable e) {
            System.err.println("Error running MeshKeeper CloudMix provisionner: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
