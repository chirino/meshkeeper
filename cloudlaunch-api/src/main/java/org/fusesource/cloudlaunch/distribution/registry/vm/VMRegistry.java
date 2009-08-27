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

import org.fusesource.cloudlaunch.distribution.registry.Registry;
import org.fusesource.cloudlaunch.distribution.registry.RegistryWatcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VMRegistry
 * <p>
 * Description: An VM Registry implementation
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class VMRegistry implements Registry {

    VMRNode root = new VMRNode();
    AtomicBoolean started = new AtomicBoolean(false);

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.distribution.registry.Registry#start()
     */
    public void start() throws Exception {
        started.compareAndSet(false, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.distribution.registry.Registry#destroy()
     */
    public void destroy() throws Exception {
        started.set(false);
        root = new VMRNode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.registry.Registry#addData(java
     * .lang.String, boolean, byte[])
     */
    public String addData(String path, boolean sequential, byte[] data) throws Exception {
        checkStarted();

        VMRNode parent = createParentPath(path);
        String name = path.substring(path.lastIndexOf("/"));
        if (!sequential) {
            if (parent.children.containsKey(name)) {
                throw new Exception("Node Already Exists: " + path);
            }
        }
        VMRNode node = parent.createChild(path.substring(path.lastIndexOf("/")), sequential, data);
        return node.path + "/" + node.name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.registry.Registry#addObject(java
     * .lang.String, boolean, java.io.Serializable)
     */
    public String addObject(String path, boolean sequential, Serializable o) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(o);
        os.close();
        return addData(path, sequential, baos.toByteArray());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.registry.Registry#getObject(java
     * .lang.String)
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String path) throws Exception {
        byte[] data = getData(path);
        if (data == null) {
            return null;
        }
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        return (T) in.readObject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.registry.Registry#getData(java
     * .lang.String)
     */
    public byte[] getData(String path) throws Exception {
        VMRNode node = findNode(path);
        if (node == null) {
            return null;
        } else {
            return node.data;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.registry.Registry#remove(java
     * .lang.String, boolean)
     */
    public void remove(String path, boolean recursive) throws Exception {
        VMRNode node = findNode(path);
        if (node != null) {
            node.delete();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.registry.Registry#addRegistryWatcher
     * (java.lang.String,
     * org.fusesource.cloudlaunch.distribution.registry.RegistryWatcher)
     */
    public void addRegistryWatcher(String path, RegistryWatcher watcher) throws Exception {
        checkStarted();
        VMRNode node = createParentPath(path + "/");
        node.addRegistryWatcher(watcher);

    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.cloudlaunch.distribution.registry.Registry#
     * removeRegistryWatcher(java.lang.String,
     * org.fusesource.cloudlaunch.distribution.registry.RegistryWatcher)
     */
    public void removeRegistryWatcher(String path, RegistryWatcher watcher) {
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

    private class VMRNode {
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

        /**
         * 
         */
        public void delete() {
         // TODO Auto-generated method stub
        }

        public VMRNode getChild(String name) {
            if (children == null) {
                return null;
            }
            return children.get(name);
        }

        public void addRegistryWatcher(RegistryWatcher watcher) {
            // TODO Auto-generated method stub
        }

        public void removeRegistryWatcher(RegistryWatcher watcher) {
            // TODO Auto-generated method stub
        }

        private void fireRegistryWatcher() {
            // TODO Auto-generated method stub
        }

    }

}
