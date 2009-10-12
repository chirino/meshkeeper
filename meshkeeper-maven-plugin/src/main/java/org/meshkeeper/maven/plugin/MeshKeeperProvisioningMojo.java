package org.meshkeeper.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.meshkeeper.cloudmix.CloudMixSupport;

/**
 * Goal for provisioning meshkeeper.
 * 
 * @goal provision
 * 
 * @phase process-sources
 */
public class MeshKeeperProvisioningMojo extends AbstractMojo {
    /**
     * Type of provisioning to do cloudmix or embedded
     * 
     * @parameter expression="${provision.provisioningType}"
     *            default-value="embedded"
     * 
     */
    private String type;

    /**
     * Type of provisioning to do deploy or undeploy
     * 
     * @parameter default-value="deploy"
     * 
     */
    private String action;

    /**
     * The control-url for cloudmix
     * 
     * @parameter expression="${cloudmix.url"}
     *            default-value="http://localhost:8181/"
     */
    private URL cloudmixUrl;

    public void execute() throws MojoExecutionException {
        if (type.equals("cloudmix")) {
            CloudMixSupport cloudmixSupport = new CloudMixSupport();
            cloudmixSupport.setControllerUrl(cloudmixUrl.toString());

            if (action.equals("deploy")) {
                try {
                    cloudmixSupport.deployMeshKeeperProfile();
                    cloudmixSupport.dumpStatus();
                } catch (Throwable thrown) {
                    throw new MojoExecutionException("Failured deploying meshkeeper on cloudmix", thrown);
                }
            } else if (action.equals("undeploy")) {
                try {
                    cloudmixSupport.killMeshKeeper();
                } catch (Throwable thrown) {
                    throw new MojoExecutionException("Failured deploying meshkeeper on cloudmix", thrown);
                }
            } else {
                throw new MojoExecutionException("Invalid provisioning action: " + action);
            }
        }
        else
        {
            throw new MojoExecutionException("unsupported provisioning type: " + type);
        }
        
    }
}
