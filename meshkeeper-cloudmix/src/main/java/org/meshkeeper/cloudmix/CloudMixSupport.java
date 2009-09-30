/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.meshkeeper.cloudmix;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.fusesource.cloudmix.agent.RestGridClient;
import org.fusesource.cloudmix.common.CloudmixHelper;
import org.fusesource.cloudmix.common.GridClient;
import org.fusesource.cloudmix.common.dto.Dependency;
import org.fusesource.cloudmix.common.dto.DependencyStatus;
import org.fusesource.cloudmix.common.dto.FeatureDetails;
import org.fusesource.cloudmix.common.dto.ProfileDetails;
import org.fusesource.cloudmix.common.dto.ProfileStatus;
import org.fusesource.meshkeeper.distribution.PluginClassLoader;
import org.fusesource.meshkeeper.distribution.PluginResolver;

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

    private String controllerUrl;
    private String preferredControlServerAgent;
    private RestGridClient gridClient;
    private ArrayList<FeatureDetails> features = new ArrayList<FeatureDetails>(2);
    private ProfileDetails meshProfile;
    
    public void deployMeshKeeperProfile() throws URISyntaxException {
        GridClient controller = getGridClient();
        
        meshProfile = new ProfileDetails();
        meshProfile.setId("MeshKeeper");
        meshProfile.setDescription("MeshKeeper is a platform for launching, coordinating and controlling arbitrary scripts, executables, java processes and objects within a mesh of computers.");
        
        FeatureDetails controlServer = new FeatureDetails();
        controlServer.setId("MeshKeeper-Control-Server");
        controlServer.setMaximumInstances("1");
        if (preferredControlServerAgent != null) {
            controlServer.preferredMachine(preferredControlServerAgent);
        }
        controlServer.setResource("mop:run org.fusesource.meshkeeper:meshkeeper-api:" + getMeshKeeperVersion() + " " + org.fusesource.meshkeeper.control.Main.class.getName());
        features.add(controlServer);
        
        FeatureDetails agentFeature = new FeatureDetails();
        agentFeature.setId("MeshKeeper-Launch-Agent");
        agentFeature.depends(controlServer.getId());
        agentFeature.setResource("mop:run org.fusesource.meshkeeper:meshkeeper-api:" + getMeshKeeperVersion() + " " + org.fusesource.meshkeeper.launcher.Main.class.getName() + " --registry zk:tcp://localhost:4040");
        features.add(agentFeature);
        
        for(FeatureDetails feature : features)
        {
            feature.setOwnedByProfileId(meshProfile.getId());
            controller.addFeature(feature);
            meshProfile.getFeatures().add(new Dependency(feature.getId()));
        }
        
        System.out.println("MeshProfile Features: " + meshProfile.getFeatures());
        controller.addProfile(meshProfile);
        
        assertProvisioned();
    }
    
    /**
     * Asserts that all the requested features have been provisioned properly
     */
    protected void assertProvisioned() {
        long start = System.currentTimeMillis();

        Set<String> provisionedFeatures = new TreeSet<String>();
        Set<String> failedFeatures = null;
        while (true) {
            failedFeatures = new TreeSet<String>();
            long now = System.currentTimeMillis();

            try {
                ProfileStatus profileStatus = getGridClient().getProfileStatus(meshProfile.getId());
                if (profileStatus != null) {
                    List<DependencyStatus> dependencyStatus = profileStatus.getFeatures();
                    System.out.println("Got dependency status for: " + dependencyStatus);
                    for (DependencyStatus status : dependencyStatus) {
                        String featureId = status.getFeatureId();
                        if (status.isProvisioned()) {
                            if (provisionedFeatures.add(featureId)) {
                                System.out.println("Provisioned feature: " + featureId);
                            }
                        } else {
                            System.out.println("UNProvisioned feature: " + featureId);
                            failedFeatures.add(featureId);
                        }
                    }
                }
                else
                {
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
                throw new RuntimeException("Provision failure. Not enough instances of features: " 
                            + failedFeatures + " after waiting " + (20000 / 1000) + " seconds");
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
    
    public static final void main(String[] args) {

        CloudMixSupport support = new CloudMixSupport();
        support.setControllerUrl("http://vm-fuseubt1:8181");
        try {
            support.deployMeshKeeperProfile();
            Thread.currentThread().sleep(60000);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
    }

}
