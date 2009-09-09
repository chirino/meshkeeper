/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.util;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeper.DistributionRef;

/**
 * Exporter
 * <p>
 * This is a helper class that is useful for exporting a single object. A common
 * use for this class is within a spring definition; defining an Exporter bean and
 * setting the source to another bean will cause the bean to be exported when this
 * bean is initialized.
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class Exporter {

    Log log = LogFactory.getLog(this.getClass());

    MeshKeeper mesh;
    private Distributable source;
    private String path;
    private Distributable stub;

    private boolean sequential = true;

    public void export() throws Exception {
        if (stub == null) {
            if (this.path == null) {
                stub = mesh.remoting().export(source);
                if (log.isTraceEnabled())
                    log.trace("Exported:" + source);

            } else {
                DistributionRef<Distributable> ref = mesh.distribute(path, true, source);
                path = ref.getRegistryPath();
                stub = ref.getProxy();
                if (log.isTraceEnabled())
                    log.trace("Registered as: " + ref.getRegistryPath() + " implementing: " + Arrays.asList(ref.getProxy().getClass().getInterfaces()));
            }
        }
    }

    public void destroy() throws Exception {
        if (stub != null) {
            mesh.undistribute(source);
        }
    }

    public void setMeshKeeper(MeshKeeper mesh) {
        this.mesh = mesh;
    }

    public MeshKeeper getMeshKeeper() {
        return mesh;
    }

    public Distributable getSource() {
        return source;
    }

    public void setSource(Distributable source) {
        this.source = source;
    }

    public Distributable getStub() {
        return stub;
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
