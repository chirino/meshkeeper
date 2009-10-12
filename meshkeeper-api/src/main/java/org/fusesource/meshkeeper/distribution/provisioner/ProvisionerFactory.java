/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.provisioner;

import org.fusesource.meshkeeper.distribution.AbstractPluginFactory;
import org.fusesource.meshkeeper.distribution.FactoryFinder;

/** 
 * ProvisioningFactory
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class ProvisionerFactory extends AbstractPluginFactory<Provisioner> {

    public static final FactoryFinder FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/meshkeeper/distribution/provisioner/");

    @Override
    protected final FactoryFinder getFactoryFinder() {
        return FACTORY_FINDER;
    }
}
