/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import org.fusesource.meshkeeper.distribution.provisioner.Provisioner;
import org.fusesource.meshkeeper.distribution.provisioner.ProvisionerFactory;

import junit.framework.TestCase;

/** 
 * MeshKeeperProvisionerTest
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class MeshKeeperProvisionerTest extends TestCase {

    
    public void testEmbeddedProvisionerInstantiation() throws Exception
    {
        Provisioner provisioner = new ProvisionerFactory().create("embedded");
        assertNotNull(provisioner);
    }
    
    public void testCloudmixProvisionerInstantiation() throws Exception
    {
        Provisioner provisioner = new ProvisionerFactory().create("cloudmix:http://localhost:8181");
        assertNotNull(provisioner);
        assertEquals("http://localhost:8181", provisioner.getDeploymentUri());
    }
}
