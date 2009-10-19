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
package org.fusesource.meshkeeper.classloader;

import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.distribution.FactoryFinder;
import org.fusesource.meshkeeper.util.internal.URISupport;


/**
 * ClassLoaderServerFactory provies an extenisble way to create custom ClassLoaderServer implementations.
 *
 * @author chirino
 * @version 1.0
 */
public abstract class ClassLoaderServerFactory {

//    private static final Log LOG = LogFactory.getLog(ClassLoaderServerFactory.class);
    private static final FactoryFinder FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/meshkeeper/classloader/");

    public static final ClassLoaderServer create(String uri, MeshKeeper meshKeeper) throws Exception {
        String parts[] = URISupport.extractScheme(uri);
        ClassLoaderServerFactory factory = FACTORY_FINDER.create(parts[0]);
        return factory.createClassLoaderManager(parts[1], meshKeeper);
    }

    protected abstract ClassLoaderServer createClassLoaderManager(String uri, MeshKeeper meshKeeper) throws Exception;

}