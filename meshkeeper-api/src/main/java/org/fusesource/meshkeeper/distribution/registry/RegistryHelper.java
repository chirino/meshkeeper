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
package org.fusesource.meshkeeper.distribution.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.RegistryWatcher;

/**
 * RegistryHelper
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class RegistryHelper {

    private static final Log LOG = LogFactory.getLog(RegistryHelper.class);
    
    /**
     * Waits for count objects to register at the specified node.
     * 
     * @param <T>
     * @param reg
     *            The registry
     * @param path
     *            The path
     * @param min
     *            The minimum number of objects to wait for.
     * @param timeout
     *            The maximum amount of time to wait.
     * @return
     * @throws Exception
     */
    public static <T> Collection<T> waitForRegistrations(RegistryClient reg, String path, int min, long timeout) throws TimeoutException, Exception {
        return new RegistrationWatcher<T>(path, null, reg).waitForRegistrations(min, timeout).values();
    }

    /**
     * Waits for count objects to register at the specified node.
     * 
     * @param <T>
     * @param reg
     *            The registry
     * @param path
     *            The path
     * @param min
     *            The minimum number of objects to wait for.
     * @param timeout
     *            The maximum amount of time to wait.
     * @return
     * @throws Exception
     */
    public static <T> T waitForRegistration(RegistryClient reg, String path, long timeout) throws TimeoutException, Exception {
        String parentPath = path.substring(0, path.lastIndexOf("/"));
        String node = path.substring(path.lastIndexOf("/") + 1);
        HashSet<String> filters = new HashSet<String>();
        filters.add(node);
        return new RegistrationWatcher<T>(parentPath, filters, reg).waitForRegistrations(1, timeout).get(node);
    }

    /**
     * 
     */
    private static class RegistrationWatcher<T> implements RegistryWatcher {

        final HashMap<String, T> map = new HashMap<String, T>();
        RegistryClient registry;
        String path;
        Set<String> filters;

        RegistrationWatcher(String path, Set<String> filters, RegistryClient registry) throws Exception {
            this.path = path;
            this.registry = registry;
            this.filters = filters;
            registry.addRegistryWatcher(path, this);
        }

        @SuppressWarnings("unchecked")
        public synchronized void onChildrenChanged(String path, List<String> nodes) {
            //System.out.println("Nodes changed for " + path + ": " + nodes);
            for (String node : nodes) {
                
                if (filter(node) && !map.containsKey(node)) {
                    try {
                        //System.out.println("Loading: " + node);
                        Object o = registry.getRegistryObject(path + "/" + node);
                        map.put(node, (T) o);
                    } catch (Throwable e) {
                        LOG.error("Error retrieving registry object at " + path + "/" + node, e);
                    }
                }
            }
            // Removes agents that go away.
            map.keySet().retainAll(nodes);
            notifyAll();
        }

        private boolean filter(String node) {
            if (filters == null) {
                return true;
            } else {
                if(filters.contains(node))
                {
                    return true;
                }
                else
                {
                    //System.out.println("Node didn't match filter: " + node);
                    return false;
                }
            }
        }

        public Map<String, T> waitForRegistrations(final int min, long timeout) throws InterruptedException, TimeoutException {

            try {
                synchronized (this) {
                    long started = System.currentTimeMillis();

                    while (timeout > 0 && map.size() < min) {
                        wait(timeout);
                        timeout -= System.currentTimeMillis() - started;
                    }

                    if (map.size() < min) {
                        throw new TimeoutException();
                    }
                }
            } finally {
                try {
                    registry.removeRegistryWatcher(path, this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return map;
        }

    }

}
