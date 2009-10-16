/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import java.io.File;

import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.distribution.provisioner.Provisioner;
import org.fusesource.meshkeeper.distribution.provisioner.ProvisionerFactory;
import org.fusesource.meshkeeper.distribution.provisioner.Provisioner.MeshProvisioningException;

import junit.framework.TestCase;

/**
 * MeshKeeperProvisionerTest
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class MeshKeeperProvisionerTest extends TestCase {

    public void testEmbeddedProvisionerInstantiation() throws Exception {
        Provisioner provisioner = new ProvisionerFactory().create("embedded");
        assertNotNull(provisioner);
    }

    public void testEmbeddedControlServerDiscoverable() throws Exception {
        File dataDir = MavenTestSupport.getDataDirectory(this.getClass().getSimpleName());
        ControlServer server = MeshKeeperFactory.createControlServer(ControlServer.DEFAULT_REGISTRY_URI, dataDir.getCanonicalFile());
        
        Provisioner provisioner = new ProvisionerFactory().create("embedded:" + dataDir.toURI());
        try {
            assertEquals(server.getRegistryUri(), provisioner.findMeshRegistryUri());
        } finally {
            //Destroy Server we should no longer be able to find it:
            server.destroy();
        }

        MeshProvisioningException expected = null;
        try {
            provisioner.findMeshRegistryUri();
        } catch (MeshProvisioningException e) {
            expected = e;
        }

        assertNotNull(expected);
    }

    public void testCloudmixProvisionerInstantiation() throws Exception {
        Provisioner provisioner = new ProvisionerFactory().create("cloudmix:http://localhost:8181");
        assertNotNull(provisioner);
        assertEquals("http://localhost:8181", provisioner.getDeploymentUri());
    }
}
