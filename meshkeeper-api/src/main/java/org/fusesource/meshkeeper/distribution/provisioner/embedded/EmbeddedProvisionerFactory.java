/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.provisioner.embedded;

import org.fusesource.meshkeeper.distribution.provisioner.Provisioner;
import org.fusesource.meshkeeper.distribution.provisioner.ProvisionerFactory;

/** 
 * EmbeddedProvisionerFactory
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class EmbeddedProvisionerFactory extends ProvisionerFactory {

    
    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.distribution.registry.RegistryFactory#createRegistry(java.lang.String)
     */
    @Override
    protected Provisioner createPlugin(String uri) throws Exception {
        return new EmbeddedProvisioner();
    }
}
