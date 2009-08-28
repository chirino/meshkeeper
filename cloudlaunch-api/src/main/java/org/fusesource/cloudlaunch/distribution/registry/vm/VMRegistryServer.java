/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.registry.vm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fusesource.cloudlaunch.control.ControlService;
import org.fusesource.cloudlaunch.distribution.DistributorFactory;
import org.fusesource.cloudlaunch.distribution.registry.RegistryWatcher;

/** 
 * VMRegistryServer
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class VMRegistryServer implements ControlService{
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
    
    
    /* (non-Javadoc)
     * @see org.fusesource.cloudlaunch.control.ControlService#destroy()
     */
    public void destroy() throws Exception {
        // TODO Auto-generated method stub
        
    }
    
    /* (non-Javadoc)
     * @see org.fusesource.cloudlaunch.control.ControlService#start()
     */
    public void start() throws Exception {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.fusesource.cloudlaunch.control.ControlService#getName()
     */
    public String getName() {
        return "VMRegistryServer";
    }

    /* (non-Javadoc)
     * @see org.fusesource.cloudlaunch.control.ControlService#getServiceUri()
     */
    public String getServiceUri() {
        return "vm:" + getName();
    }

    /* (non-Javadoc)
     * @see org.fusesource.cloudlaunch.control.ControlService#setDataDirectory(java.lang.String)
     */
    public void setDataDirectory(String directory) {
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
