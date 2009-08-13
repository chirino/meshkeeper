/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.launcher;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

import org.fusesource.cloudlaunch.control.ControlServer;
import org.fusesource.cloudlaunch.distribution.Distributor;

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
        System.out.println(" -url <rmi url> -- specifies address of remote broker to connect to.");
        System.out.println(" [-dataDir <directory>] -- specifies data directory for.");
        System.out.println(" -commonRepoUrl <url> -- specifies common resource location.");
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

        Distributor distributor = null;
        String distributorUrl = null;
        String commonRepoUrl = null;
        String dataDir = ".";
        LinkedList<String> alist = new LinkedList<String>(Arrays.asList(args));

        try {
            while (!alist.isEmpty()) {
                String arg = alist.removeFirst();
                if (arg.equals("-help") || arg.equals("-h")) {
                    showUsage();
                    return;
                } else if (arg.equals("-url")) {
                    if (alist.isEmpty()) {
                        throw new Exception("Expected url after -url");
                    }
                    distributorUrl = alist.removeFirst();

                } else if (arg.equals("-dataDir")) {
                    if (alist.isEmpty()) {
                        throw new Exception("Directory expected after -dataDir");
                    }
                    dataDir = alist.removeFirst();
                } else if (arg.equals("-commonRepoUrl")) {
                    commonRepoUrl = alist.removeFirst();
                } else if (arg.equals("-distributorUrl")) {
                }
            }

            if (distributorUrl == null) {
                distributorUrl = "zk:" + ControlServer.DEFAULT_REGISTRY_URL;
            }
            distributor = Distributor.create(distributorUrl);

            LaunchAgent agent = new LaunchAgent();
            agent.setCommonResourceRepoUrl(commonRepoUrl);
            agent.setDataDirectory(new File(dataDir));
            distributor.start();
            agent.setDistributor(distributor);

            agent.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            try {
                if (distributor != null) {
                    distributor.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
