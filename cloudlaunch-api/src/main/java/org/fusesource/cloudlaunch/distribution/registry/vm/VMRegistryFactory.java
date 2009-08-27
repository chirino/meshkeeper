/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.registry.vm;

import org.fusesource.cloudlaunch.distribution.registry.Registry;
import org.fusesource.cloudlaunch.distribution.registry.RegistryFactory;

/** 
 * VMRegistryFactory
 * <p>
 * Description: A Factory for in VM Registries.
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class VMRegistryFactory extends RegistryFactory{

    /* (non-Javadoc)
     * @see org.fusesource.cloudlaunch.distribution.registry.RegistryFactory#createRegistry(java.lang.String)
     */
    @Override
    protected Registry createRegistry(String uri) throws Exception {
        return new VMRegistry();
    }
}
