/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/** 
 * RegistryHelper
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class RegistryHelper {

    /**
     * Waits for count objects to register at the specified node. 
     * 
     * @param <T>
     * @param reg The registry
     * @param path The path
     * @param min The minimum number of objects to wait for.
     * @param timeout The maximum amount of time to wait.
     * @return
     * @throws Exception
     */
    public static <T> Collection<T> waitForRegistrations(Registry reg, String path, int min, long timeout) throws TimeoutException, Exception
    {
        return new RegistrationWatcher<T>(path, reg).waitForRegistrations(min, timeout).values();
    }
 
    
    /**
     * 
     */
    private static class RegistrationWatcher<T> implements RegistryWatcher {

        final HashMap<String, T> map = new HashMap<String, T>();
        Registry registry;
        String path;

        RegistrationWatcher(String path, Registry registry) throws Exception {
            this.path = path;
            this.registry = registry;
            registry.addRegistryWatcher(path, this);
        }

        public synchronized void onChildrenChanged(String path, List<String> nodes) {
            for (String node : nodes) {
                if (!map.containsKey(node)) {
                    try {
                        System.out.println("Loading: " + node);
                        Object o = registry.getObject(path + "/" + node);
                        map.put(node, (T) o);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            // Removes agents that go away.
            map.keySet().retainAll(nodes);
            notifyAll();
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
                registry.removeRegistryWatcher(path, this);
            }

            return map;
        }

    }
    
}
