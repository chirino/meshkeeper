/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.packaging;

import java.util.Collection;

import org.fusesource.meshkeeper.MavenTestSupport;
import org.fusesource.meshkeeper.MeshKeeper;

import junit.framework.TestCase;

/**
 * RegistryTest
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class RegistryTest extends TestCase {

    public void testRegistryUUID() throws Exception {
        MeshKeeper mk1 = null;
        MeshKeeper mk2 = null;
        try {
            mk1 = MavenTestSupport.createMeshKeeper("RegistryTest");
            mk2 = MavenTestSupport.createMeshKeeper("RegistryTest");

            mk1.setUUID("mk1");
            mk2.setUUID("mk2");

            //Since we use a relative path here both of these should be 
            //created at a path under the uuid:
            mk1.registry().addRegistryObject("id", false, "1");
            mk2.registry().addRegistryObject("id", false, "2");

            //Test that each one make it:
            assertEquals("1", mk1.registry().getRegistryObject("id"));
            assertEquals("2", mk2.registry().getRegistryObject("id"));

            String abs1 = mk1.registry().addRegistryObject("1", false, "1");
            String abs2 = mk2.registry().addRegistryObject("2", false, "2");

            //Shouldn't see the other guy's data:
            assertEquals(null, mk1.registry().getRegistryObject("2"));
            assertEquals(null, mk2.registry().getRegistryObject("1"));

            //Should see the other guys at absolute path:
            assertEquals("2", mk1.registry().getRegistryObject(abs2));
            assertEquals("1", mk2.registry().getRegistryObject(abs1));

            //Should end up with two entries at the absolute location
            mk1.registry().addRegistryObject("/id/", true, "1-");
            mk2.registry().addRegistryObject("/id/", true, "2-");

            Collection<String> objects1 = mk1.registry().waitForRegistrations("/id", 2, 5000);
            Collection<String> objects2 = mk2.registry().waitForRegistrations("/id", 2, 5000);
            assertTrue(objects1.containsAll(objects2));
            assertTrue(objects2.containsAll(objects1));
        } finally {
            mk1.destroy();
            mk2.destroy();
        }

    }

}
