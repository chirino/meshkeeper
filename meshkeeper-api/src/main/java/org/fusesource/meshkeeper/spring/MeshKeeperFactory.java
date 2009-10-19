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
package org.fusesource.meshkeeper.spring;

import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.distribution.DistributorFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;

/**
 * A spring FactoryBean to create MeshKeeper instances.
 * 
 * @author chirino
 */
public class MeshKeeperFactory implements FactoryBean, InitializingBean, DisposableBean {
    
    private MeshKeeper meshKeeper;

    public void afterPropertiesSet() throws Exception {
        DistributorFactory factory = new DistributorFactory();
        factory.setRegistryUri(registry);
        factory.setDirectory(directory.getCanonicalPath());
        meshKeeper = factory.create();
        meshKeeper.start();
    }

    public void destroy() throws Exception {
        meshKeeper.destroy();
    }

    public Class<?> getObjectType() {
        return MeshKeeper.class;
    }

    public boolean isSingleton() {
        return true;
    }
    public Object getObject() throws Exception {
        return meshKeeper;
    }

    private String registry;
    private File directory;

    public void setRegistryUri(String registry) {
        this.registry = registry;
    }

    public String getRegistryUri() {
        return registry;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public File getDirectory() {
        return directory;
    }
}