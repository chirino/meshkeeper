/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.cloudlaunch.distribution.rmi;

import java.util.LinkedHashSet;
import java.util.Set;

import org.fusesource.cloudlaunch.distribution.Distributable;

/**
 * AbstractExporter
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class AbstractExporter implements IExporter {

    protected abstract <T> T export(Object obj, Class<?>[] interfaces) throws Exception;

    public <T extends Distributable> T export(Distributable obj) throws Exception {
        LinkedHashSet<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
        collectDistributableInterfaces(obj.getClass(), interfaces);
        Class<?> [] distributable = new Class<?>[interfaces.size()];
        interfaces.toArray(distributable);
        return (T) export(obj, distributable);
    }
    
    protected static void validateInterface(Class<?> i) {
        if (!i.isInterface()) {
            throw new IllegalArgumentException("Not an interface: " + i);
        }
    }

    private static void collectDistributableInterfaces(Class<?> clazz, Set<Class<?>> rc) throws Exception {
        for (Class<?> interf : clazz.getInterfaces()) {
            if (Distributable.class.isAssignableFrom(interf)) {
                validateInterface(interf);
                rc.add(interf);
            }
        }
        
        // Also slowOnewayOperations interfaces in the super classes...
        if (clazz.getSuperclass() != null) {
            collectDistributableInterfaces(clazz.getSuperclass(), rc);
        }
    }
}
