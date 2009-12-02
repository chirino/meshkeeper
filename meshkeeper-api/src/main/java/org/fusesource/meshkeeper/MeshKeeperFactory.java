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
package org.fusesource.meshkeeper;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fusesource.meshkeeper.MeshKeeper.Launcher;
import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.distribution.DistributorFactory;
import org.fusesource.meshkeeper.distribution.provisioner.Provisioner;
import org.fusesource.meshkeeper.distribution.provisioner.ProvisionerFactory;
import org.fusesource.meshkeeper.launcher.LaunchAgent;
import org.fusesource.meshkeeper.launcher.MeshContainer;
import org.fusesource.meshkeeper.util.internal.MeshKeeperWrapper;

/**
 * Factory class that can creates the MeshKeeper clients, agents or control
 * sever instances.
 * 
 * @author chirino
 */
public class MeshKeeperFactory {

    /**
     * When set this property indicates the uri of the MeshKeeper registry
     * service used to connect the MeshKeeper {@link ControlServer} when
     * {@link #createMeshKeeper()} is called.
     * 
     * If set to "embedded" or "provision" this will cause the factory to auto
     * start a MeshKeeper {@link ControlServer} and {@link LaunchAgent}. When
     * set to "provision" #MESHKEEPER_PROVISIONER_PROPERTY should be set to
     * indicate the provisioner uri that is used to locate a provisioner for
     * deploying meshkeeper.
     * 
     * If set to "provisioned" this assumes that the controller and agents were
     * pre-provisioned and the factory will attempt to load the provisioning
     * helper specified by {@link #MESHKEEPER_PROVISIONER_PROPERTY} to locate
     * the registry service.
     * 
     * Otherwise this property is interpreted as a registry connect uri.
     * 
     * @see #MESHKEEPER_PROVISIONER_PROPERTY
     * @see #createMeshKeeper()
     */
    public static final String MESHKEEPER_REGISTRY_PROPERTY = "meshkeeper.registry.uri";

    /**
     * When {@link #MESHKEEPER_REGISTRY_PROPERTY} is set to "provisioned" or
     * "provision" this property is used to locate a provision to either deploy
     * MeshKeeper or locate the registry service uri.
     * 
     * MeshKeeper provides two provisioners out of the box:
     * <ul>
     * <li>"embedded:<path-to-meshkeeper-base> finds a control server running
     * locally with the given <path-to-meshkeeper-base> directory</li>
     * <li>
     * cloudmix:<cloudmix-connect-url> Looks for a MeshKeeper profile
     * provisioned via cloudmix at the cloudmix controller url.</li>
     * <ul>
     * 
     * @see #createMeshKeeper()
     */
    public static final String MESHKEEPER_PROVISIONER_PROPERTY = "meshkeeper.provisioner.uri";

    /**
     * This property specifies the base directory which MeshKeeper components
     * should use to store their data. When not specified the default value will
     * be "./data" relative to the current working directory. 
     */
    public static final String MESHKEEPER_BASE_PROPERTY = "meshkeeper.base";
    
    /**
     * When this property is set, created meshkeepers' UUID is set to the specified 
     * value. This is useful in ensuring that launched processes' meshkeeper instance
     * share the same UUID as their launcher. {@link Launcher}s will set this automatically
     * for launched java processes.
     */
    public static final String MESHKEEPER_UUID_PROPERTY = "meshkeeper.uuid";
    
    private static final ProvisioningTracker PROVISIONING_TRACKER = new ProvisioningTracker();

    /**
     * Gets the default based directory for storing data for MeshKeeper
     * components.
     */
    public static File getDefaultBaseDirectory() {
        return new File(System.getProperty(MESHKEEPER_BASE_PROPERTY, "./meshkeeper"));
    }

    /**
     * Gets the default directory for storing {@link MeshKeeper} data.
     */
    public static File getDefaultClientDirectory() {
        return new File(getDefaultBaseDirectory(), "client");
    }

    /**
     * Gets the default directory for storing {@link LaunchAgent} data.
     */
    public static File getDefaultAgentDirectory() {
        return new File(getDefaultBaseDirectory(), "agent");
    }

    /**
     * Gets the default directory for storing {@link ControlServer} data.
     */
    public static File getDefaultServerDirectory() {
        return new File(getDefaultBaseDirectory(), "server");
    }

    /**
     * Tests if the application is running in a {@link org.fusesource.meshkeeper.MeshContainer}
     * @return True if running in a MeshContainer
     */
    public static final boolean isInMeshContainer() {
        return MeshContainer.isInMeshContainer();
    }
    
    /**
     * When the application is running in {@link org.fusesource.meshkeeper.MeshContainer} this
     * method can be used to get the container's MeshKeeper 
     * 
     * @return The containers MeshKeeper if running in a {@link MeshContainer}
     */
    public static final MeshKeeper getContainerMeshKeeper() {
        return MeshContainer.getMeshKeeper();
    }

    /**
     * Creates a MeshKeeper object using the current value of the
     * {@link #MESHKEEPER_REGISTRY_PROPERTY}. See the comments there for it's
     * usage. If the property is set to "embedded" or "provision" then this
     * method will automatically start and stop a {@link ControlServer} and
     * {@link LaunchAgent}(s) on demand.
     * 
     * @return A new {@link MeshKeeper} connected to the specified control
     *         server
     * @see #MESHKEEPER_REGISTRY_PROPERTY
     * @see #MESHKEEPER_PROVISIONER_PROPERTY
     */
    public static MeshKeeper createMeshKeeper() throws Exception {

        String url = System.getProperty(MESHKEEPER_REGISTRY_PROPERTY, "embedded");

        if ("provisioned".equals(url)) {
            url = PROVISIONING_TRACKER.getProvisioner().findMeshRegistryUri();
        } else if ("provision".equals(url) || "embedded".equals(url)) {
            // We wrap it so we know when we can stop the embedded registiry.
            return new MeshKeeperWrapper(createMeshKeeper(PROVISIONING_TRACKER.acquireProvisioned())) {
                AtomicBoolean destroyed = new AtomicBoolean(false);

                public void destroy() throws Exception {
                    next.destroy();
                    if (destroyed.compareAndSet(false, true)) {
                        PROVISIONING_TRACKER.releaseProvisioned();
                    }
                }
            };
        }
        return createMeshKeeper(url);
    }

    /**
     * Equivalent to calling {@link #createMeshKeeper(String registry, File)}
     * with a null dataDir parameter.
     * 
     * @param registry
     *            The registry connect uri of the controller.
     * @return A MeshKeeper
     * @throws Exception
     *             If there is an error creating the meshkeeper.
     */
    static public MeshKeeper createMeshKeeper(String registry) throws Exception {
        return createMeshKeeper(registry, getDefaultClientDirectory());
    }

    /**
     * Creates a MeshKeeper connected to the controller running at the given
     * registry connect uri.
     * <p>
     * <b>Note that {@link #createMeshKeeper()} should be used instead if you
     * are running an embedded server. <b>
     * 
     * @param registry
     *            The registry connect uri of the controller.
     * @param dataDir
     *            The path at which the {@link MeshKeeper} should store data.
     * @return A MeshKeeper
     * @throws Exception
     *             If there is an error creating the meshkeeper.
     */
    static public MeshKeeper createMeshKeeper(String registry, File dataDir) throws Exception {
        DistributorFactory df = new DistributorFactory();
        df.setRegistryUri(registry);
        df.setDirectory(dataDir.getCanonicalPath());
        MeshKeeper mk = df.create();
        mk.start();
        return mk;
    }

    /**
     * Equivalent to calling {@link #createAgent(MeshKeeper, File)} with a null
     * dataDir parameter.
     * 
     * @param keeper
     *            The MeshKeeper for the agent.
     * @return An embedded launch agent.
     * @throws Exception
     *             If there is an error creating the agent.
     */
    static public LaunchAgent createAgent(MeshKeeper keeper) throws Exception {
        return createAgent(keeper, MeshKeeperFactory.getDefaultAgentDirectory());
    }

    /**
     * Creates an embedded {@link LaunchAgent}. Note, it is not usually
     * necessary for application code to call this directly. This method is here
     * primarily as a helper for the case where a {@link LaunchAgent} is
     * deployed by Spring. Most often a {@link LaunchAgent} is pre provisioned
     * for you.
     * 
     * @param keeper
     *            The MeshKeeper for the agent.
     * @param dataDir
     *            the directoy for storing {@link LaunchAgent} data.
     * @return An embedded launch agent.
     * @throws Exception
     *             If there is an error creating the agent.
     */
    static public LaunchAgent createAgent(MeshKeeper keeper, File dataDir) throws Exception {
        LaunchAgent agent = new LaunchAgent();
        agent.setMeshKeeper(keeper);
        if (dataDir == null) {
            dataDir = getDefaultAgentDirectory();
        }
        agent.setDirectory(dataDir);
        agent.start();
        return agent;
    }

    /**
     * Creates an embedded {@link ControlServer}. Note, it is not usually
     * necessary for application code to call this directly. This method is here
     * primarily as a helper for the case where a {@link ControlServer} is
     * deployed by Spring. Most often a {@link ControlServer} is pre provisioned
     * for you.
     * 
     * @param keeper
     *            The MeshKeeper for the agent.
     * @param dataDir
     *            the directoy for storing {@link LaunchAgent} data.
     * @return An embedded launch agent.
     * @throws Exception
     *             If there is an error creating the agent.
     */
    static public ControlServer createControlServer(String registry) throws Exception {
        return createControlServer(registry, MeshKeeperFactory.getDefaultServerDirectory());
    }

    /**
     * Creates an embedded {@link ControlServer}. Note, it is not usually
     * necessary for application code to call this directly. This method is here
     * primarily as a helper for the case where a {@link ControlServer} is
     * deployed by Spring. Most often a {@link ControlServer} is pre provisioned
     * for you.
     * 
     * @param registry
     *            The registry uri used to create the control server registry.
     * @param dataDir
     *            the directoy for storing {@link ControlServer} data.
     * @return An embedded launch agent.
     * @throws Exception
     *             If there is an error creating the server.
     */
    static public ControlServer createControlServer(String registry, File dataDir) throws Exception {
        ControlServer rc = new ControlServer();
        if (registry != null) {
            rc.setRegistryUri(registry);
        }
        rc.setDirectory(dataDir.getCanonicalPath());
        rc.start();
        return rc;
    }

    private static class ProvisioningTracker {

        private Provisioner provisioner = null;
        private int acquireCounter;

        public synchronized String acquireProvisioned() throws Exception {

            if (acquireCounter == 0) {
                getProvisioner().deploy();
            }

            acquireCounter++;
            return getProvisioner().findMeshRegistryUri();
        }

        public synchronized void releaseProvisioned() throws Exception {

            acquireCounter--;
            if (acquireCounter != 0) {
                return;
            } else {
                getProvisioner().unDeploy(false);
            }
        }

        public synchronized Provisioner getProvisioner() throws Exception {
            if (provisioner == null) {
                String provisionerUri = System.getProperty(MESHKEEPER_PROVISIONER_PROPERTY, "embedded");
                provisioner = new ProvisionerFactory().create(provisionerUri);
            }

            return provisioner;
        }
    }

}