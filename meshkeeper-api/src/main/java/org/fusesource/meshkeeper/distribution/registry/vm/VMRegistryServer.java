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
package org.fusesource.meshkeeper.distribution.registry.vm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fusesource.meshkeeper.RegistryWatcher;
import org.fusesource.meshkeeper.control.ControlService;
import org.fusesource.meshkeeper.distribution.DistributorFactory;

/**
 * VMRegistryServer
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class VMRegistryServer implements ControlService {
    private static final ExecutorService EXECUTOR = DistributorFactory.getExecutorService();
    private final AtomicBoolean started = new AtomicBoolean(false);

    VMRNode root = new VMRNode();

    public synchronized String addData(String path, boolean sequential, byte[] data) throws Exception {
        checkStarted();

        VMRNode parent = createParentPath(path);
        String name = path.substring(path.lastIndexOf("/"));
        if (!sequential) {
            if (parent.children.containsKey(name)) {
                throw new Exception("Node Already Exists: " + path);
            }
        }
        VMRNode node = parent.createChild(path.substring(path.lastIndexOf("/")), sequential, data);
        return node.getFullPath();
    }

    public synchronized String addObject(String path, boolean sequential, Serializable o) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(o);
        os.close();
        return addData(path, sequential, baos.toByteArray());
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(String path) throws Exception {
        byte[] data = getData(path);
        if (data == null) {
            return null;
        }
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        return (T) in.readObject();
    }

    public synchronized byte[] getData(String path) throws Exception {
        VMRNode node = findNode(path);
        if (node == null) {
            return null;
        } else {
            return node.data;
        }
    }

    public synchronized void remove(String path, boolean recursive) throws Exception {
        VMRNode node = findNode(path);
        if (node != null) {
            if (node.hasChildren()) {
                throw new Exception("Node Not Empty: " + path);
            }
            if (node.hasData()) {
                node.data = null;
            }
            if (node.isOkToDelete()) {
                node.delete(true);
            }
        }
    }

    public synchronized void addRegistryWatcher(String path, RegistryWatcher watcher) throws Exception {
        checkStarted();
        VMRNode node = createParentPath(path + "/");
        node.addRegistryWatcher(watcher);

    }

    public synchronized void removeRegistryWatcher(String path, RegistryWatcher watcher) {
        VMRNode node = findNode(path);
        if (node != null) {
            node.removeRegistryWatcher(watcher);
        }
    }

    /**
     * @param path
     * @param recursive
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<String> list(String path, boolean recursive, String... filters) {
        if (filters != null) {
            HashSet<String> filterSet = new HashSet<String>();
            filterSet.addAll(Arrays.asList(filters));
            return list(path, recursive, new LinkedList<String>(), filterSet);
        } else {
            return list(path, recursive, new LinkedList<String>(), Collections.EMPTY_SET);
        }
    }

    private Collection<String> list(String path, boolean recursive, Collection<String> results, Set<String> filters) {
        VMRNode node = findNode(path);
        if (node != null) {
            if (node.data != null) {
                results.add(node.getFullPath());
            }

            if (recursive && node.children != null) {
                for (VMRNode child : node.children.values()) {
                    if (!filters.remove(child.getFullPath())) {
                        list(child.getFullPath(), recursive, results, filters);
                    }
                }
            }
        }
        return results;
    }

    private VMRNode createParentPath(String path) throws Exception {
        StringTokenizer tok = new StringTokenizer(path, "/");
        VMRNode parent = root;
        while (tok.countTokens() > 1) {
            parent.createChild(tok.nextToken(), false, null);
        }
        return parent;
    }

    /**
     * @param path
     * @return
     */
    private VMRNode findNode(String path) {
        path = path.trim();
        VMRNode node = root;
        StringTokenizer tok = new StringTokenizer(path, "/");
        while (node != null && tok.hasMoreTokens()) {
            node = node.getChild(tok.nextToken());
        }
        return root;
    }

    private void checkStarted() throws Exception {
        if (!started.get()) {
            throw new Exception("Not Connected");
        }
    }

    //////////////////////////////////////////////////////////////////////
    //Control Service Implementation
    //////////////////////////////////////////////////////////////////////

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.control.ControlService#destroy()
     */
    public void destroy() throws Exception {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.control.ControlService#start()
     */
    public void start() throws Exception {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.control.ControlService#getName()
     */
    public String getName() {
        return "VMRegistryServer";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.control.ControlService#getServiceUri()
     */
    public String getServiceUri() {
        return "vm:" + getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.control.ControlService#setDirectory(java.lang
     * .String)
     */
    public void setDirectory(String directory) {
        //NoOp
    }

    //////////////////////////////////////////////////////////////////////
    //End of Control Service Implementation
    //////////////////////////////////////////////////////////////////////

    public class VMRNode {
        VMRNode parent;
        HashMap<String, VMRNode> children;
        String name = "";
        String path = "/";
        boolean sequential;
        byte[] data;
        HashSet<RegistryWatcher> watchers;

        public VMRNode createChild(String name, boolean sequential, byte[] data) throws Exception {
            if (this.sequential) {
                throw new Exception("Can't create child for sequential node");
            }

            if (children == null) {
                children = new HashMap<String, VMRNode>();
            } else {
                VMRNode existing = children.get(name);
                if (existing != null) {
                    return existing;
                }
            }

            VMRNode rc = new VMRNode();
            rc.parent = this;
            rc.path = this.path + this.name + "/";
            rc.name = name;
            rc.sequential = sequential;
            rc.data = data;
            children.put(name, rc);
            fireRegistryWatcher();
            return rc;
        }

        public void removeChild(String name) {
            if (children == null) {
                return;
            }
            if (children.remove(name) != null && children.isEmpty()) {
                children = null;
            }
        }

        public VMRNode getChild(String name) {
            if (children == null) {
                return null;
            }
            return children.get(name);
        }

        public String getFullPath() {
            return path + "/" + name;
        }

        public boolean hasChildren() {
            return children != null && !children.isEmpty();
        }

        public boolean hasData() {
            return data != null;
        }

        public boolean hasWatchers() {
            return watchers != null && !watchers.isEmpty();
        }

        public boolean isOkToDelete() {
            return !(hasChildren() || hasWatchers());
        }

        public void delete(boolean deleteEmptyAncestors) {
            if (parent != null) {
                parent.removeChild(name);
                parent.fireRegistryWatcher();

                if (deleteEmptyAncestors) {
                    if (parent.isOkToDelete() && !parent.hasData()) {
                        parent.delete(deleteEmptyAncestors);
                    }
                }
            }
        }

        public void addRegistryWatcher(RegistryWatcher watcher) {
            if (watchers == null) {
                watchers = new HashSet<RegistryWatcher>(1);
            }
            watchers.add(watcher);
            fireWatchEvent(new RegistryWatcher[] { watcher });
        }

        public void removeRegistryWatcher(RegistryWatcher watcher) {
            if (watchers != null) {
                if (watchers.remove(watcher)) {
                    if (watchers.isEmpty()) {
                        watchers = null;
                        if (!hasData() && isOkToDelete()) {
                            delete(true);
                        }
                    }
                }
            }
        }

        private void fireRegistryWatcher() {
            if (watchers != null) {
                fireWatchEvent((RegistryWatcher[]) watchers.toArray());
            }
        }

        private void fireWatchEvent(final RegistryWatcher[] targets) {
            EXECUTOR.execute(new Runnable() {

                public void run() {
                    ArrayList<String> childList = new ArrayList<String>();
                    synchronized (VMRegistryServer.this) {
                        if (children != null) {
                            childList.addAll(children.keySet());
                        }
                    }

                    for (RegistryWatcher w : targets) {
                        w.onChildrenChanged(getFullPath(), childList);
                    }
                }
            });
        }
    }

}
