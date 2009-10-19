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
