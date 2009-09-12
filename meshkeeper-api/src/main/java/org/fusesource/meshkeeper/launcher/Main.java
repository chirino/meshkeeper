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
import org.fusesource.meshkeeper.MeshKeeperFactory;
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
        System.out.println("  -h,--help                   -- this message");
        System.out.println("  --registry <uri>            -- specifies the uri of a control server registry.");
        System.out.println("  [--directory <directory>]   -- specifies data directory used by the Launcher.");
    }

    static class UsageException extends Exception {
        UsageException(String message) {
            super(message);
        }
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

        if( System.getProperty("meshkeeper.application")==null ) {
            System.setProperty("meshkeeper.application", Main.class.getName());
        }
        
        MeshKeeper meshKeeper = null;
        String regisitry = null;
        String directory = MeshKeeperFactory.getDefaultAgentDirectory().getPath();
        LinkedList<String> alist = new LinkedList<String>(Arrays.asList(args));

        try {
            while (!alist.isEmpty()) {
                String arg = alist.removeFirst();
                if (arg.equals("--help") || arg.equals("-h")) {
                    showUsage();
                    return;
                } else if (arg.equals("--registry")) {
                    assertHasAdditionalArg(alist, "Expected uri after --registry");
                    regisitry = alist.removeFirst();

                } else if (arg.equals("--directory")) {
                    assertHasAdditionalArg(alist, "Directory expected after --directory");
                    directory = alist.removeFirst();
                } 
            }

            if( regisitry==null ) {
                throw new UsageException("The --registry option is required.");
            }


            System.out.println("Starting Launch Agent against registry: "+regisitry);
            meshKeeper = MeshKeeperFactory.createMeshKeeper(regisitry, new File(directory));

            LaunchAgent agent = new LaunchAgent();
            agent.setDirectory(new File(directory));
            agent.setMeshKeeper(meshKeeper);
            agent.start();
            
        } catch (UsageException e) {
            System.out.println("Invalid usage: "+e.getMessage());
            System.out.println();
            showUsage();
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-2);
        } finally {
            try {
                if (meshKeeper != null) {
                    meshKeeper.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-3);
            }
        }
    }

    private static void assertHasAdditionalArg(LinkedList<String> alist, String message) throws Exception {
        if (alist.isEmpty()) {
            throw new UsageException(message);
        }
    }
}
