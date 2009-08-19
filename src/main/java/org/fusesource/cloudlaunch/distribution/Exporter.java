/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.distribution.Distributor.DistributionRef;

/**
 * Exporter
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class Exporter {

    Log log = LogFactory.getLog(this.getClass());

    Distributor distributor;
    private Distributable source;
    private String path;
    private DistributionRef<Distributable> ref;

    private boolean sequential = true;

    public void export() throws Exception {
        if (ref == null) {
            if (this.path == null) {
                ref = distributor.export(source);
                if (log.isTraceEnabled())
                    log.trace("Exported:" + source);

            } else {
                ref = distributor.register(source, path, true);
                if (log.isTraceEnabled())
                    log.trace("Registered as: " + ref.getPath() + " implementing: " + Arrays.asList(ref.getStub().getClass().getInterfaces()));
            }
        }
    }

    public void destroy() throws Exception {
        if (ref != null) {
            distributor.unregister(source);
        }
    }

    public void setDistributor(Distributor distributor) {
        this.distributor = distributor;
    }

    public Distributor getRegistry() {
        return distributor;
    }

    public Distributable getSource() {
        return source;
    }

    public void setSource(Distributable source) {
        this.source = source;
    }

    public Distributable getStub() {
        if (ref != null) {
            return ref.getStub();
        }
        return null;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSequential(boolean sequential) {
        this.sequential = sequential;
    }

    public boolean getSequential() {
        return this.sequential;
    }

}
