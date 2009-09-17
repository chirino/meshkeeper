/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.launcher;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeper.DistributionRef;

/**
 * MeshContainer
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class MeshContainer implements MeshContainerService {

    public static MeshKeeper mesh;
    public static final Log LOG = LogFactory.getLog(LaunchAgent.class);

    private HashMap<String, Distributable> hosted = new HashMap<String, Distributable>();

    private CountDownLatch closeLatch = new CountDownLatch(1);
    
    public MeshContainer() {
        mesh = RemoteBootstrap.getMeshKeeper();
        try {
            mesh.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public synchronized <T extends Distributable> T host(String name, Distributable object) throws Exception {

        T proxy = null;
        if (!hosted.containsKey(name)) {
            proxy = (T) mesh.remoting().export(object);
            hosted.put(name, object);
        } else {
            throw new Exception("Already hosting an object with name " + name);
        }

        return proxy;
    }

    public synchronized void unhost(String name) throws Exception {

        Distributable d = hosted.get(name);
        if (d != null) {
            mesh.remoting().unexport(d);
        }
    }

    public void run(Runnable r) {
        //TODO a thread pool perhaps:
        Thread thread = new Thread(r);
        thread.start();
    }

    public void close() {
        closeLatch.countDown();
    }

    public static MeshKeeper getMeshKeeper() {
        return mesh;
    }

    public static final void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Expected registry path");
        }
        String path = args[0];

        MeshContainer container = new MeshContainer();
        try {
            DistributionRef ref = container.mesh.distribute(path, false, container);
            System.out.println("Started MeshContainer: " + ref.getRegistryPath());
            container.closeLatch.await();
        } catch (Exception e) {
            LOG.error("MeshContainer error: ", e);
        } finally {
            try {
                mesh.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
