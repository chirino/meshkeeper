/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.launcher;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.distribution.DistributorFactory;

/**
 * Main
 * <p>
 * For Launching a {@link LaunchAgent}
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class Main {

    private static final void showUsage() {
        System.out.println("Usage:");
        System.out.println("Args:");
        System.out.println(" -(h)elp -- this message");
        System.out.println(" -uri <registry uri> -- specifies the uri of a control server registry.");
        System.out.println(" [-dataDir <directory>] -- specifies data directory used by the Launcher.");
    }

    /*
     * public static void main()
     * 
     * Defines the entry point into this app.
     */
    public static void main(String[] args) {
        String jv = System.getProperty("java.version").substring(0, 3);
        if (jv.compareTo("1.5") < 0) {
            System.err.println("The Launch Agent requires jdk 1.5 or higher to run, the current java version is " + System.getProperty("java.version"));
            System.exit(-1);
            return;
        }

        MeshKeeper meshKeeper = null;
        String distributorUri = null;
        String dataDir = ".";
        LinkedList<String> alist = new LinkedList<String>(Arrays.asList(args));

        try {
            while (!alist.isEmpty()) {
                String arg = alist.removeFirst();
                if (arg.equals("-help") || arg.equals("-h")) {
                    showUsage();
                    return;
                } else if (arg.equals("-uri")) {
                    if (alist.isEmpty()) {
                        throw new Exception("Expected url after -url");
                    }
                    distributorUri = alist.removeFirst();

                } else if (arg.equals("-dataDir")) {
                    if (alist.isEmpty()) {
                        throw new Exception("Directory expected after -dataDir");
                    }
                    dataDir = alist.removeFirst();
                } 
            }

            DistributorFactory.setDefaultDataDirectory(dataDir);
            DistributorFactory.setDefaultRegistryUri(distributorUri);
            meshKeeper = DistributorFactory.createDefaultDistributor();
            
            LaunchAgent agent = new LaunchAgent();
            agent.setDataDirectory(new File(dataDir));
            meshKeeper.start();
            agent.setMeshKeeper(meshKeeper);

            agent.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            try {
                if (meshKeeper != null) {
                    meshKeeper.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
