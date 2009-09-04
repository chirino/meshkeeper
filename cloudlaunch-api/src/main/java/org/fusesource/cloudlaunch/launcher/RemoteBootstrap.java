/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.launcher;

import org.fusesource.cloudlaunch.classloader.ClassLoaderFactory;
import org.fusesource.cloudlaunch.classloader.Marshalled;
import org.fusesource.cloudlaunch.distribution.DistributorFactory;
import org.fusesource.cloudlaunch.Distributor;

import java.util.LinkedList;
import java.util.Arrays;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;


/**
 * Java main class that be be used to bootstrap the classpath
 * via remote classpath downloading of another main class.
 *
 * @author chirino
 */
public class RemoteBootstrap {

    private File cache;
    private String classLoader;
    private String mainClass;
    private String[] args;
    private String distributor;
    private String runnable;

    static class SyntaxException extends Exception {
        private static final long serialVersionUID = 4997524790367555614L;

        SyntaxException(String message) {
            super(message);
        }
    }
    
    public static void main(String args[]) throws Throwable {

        RemoteBootstrap main = new RemoteBootstrap();
        LinkedList<String> alist = new LinkedList<String>(Arrays.asList(args));

        try {
            // Process the options.
            while (!alist.isEmpty()) {
                String arg = alist.removeFirst();
                if (arg.equals("--help")) {
                    showUsage();
                    return;
                } else if (arg.equals("--cache")) {
                    try {
                        main.setCache(new File(alist.removeFirst()).getCanonicalFile());
                    } catch (Exception e) {
                        throw new SyntaxException("Expected a directoy after the --cache option.");
                    }
                } else if (arg.equals("--distributor")) {
                    try {
                        main.setDistributor(alist.removeFirst());
                    } catch (Exception e) {
                        throw new SyntaxException("Expected a url after the --distributor option.");
                    }
                } else if (arg.equals("--runnable")) {
                    try {
                        main.setRunnable(alist.removeFirst());
                    } catch (Throwable e) {
                        throw new SyntaxException("Expected a url after the --runnable option.");
                    }
                } else if (arg.equals("--classloader")) {
                    try {
                        main.setClassLoader(alist.removeFirst());
                    } catch (Throwable e) {
                        throw new SyntaxException("Expected a url after the --classloader option.");
                    }
                } else {
                    // Not an option.. then it must be the main class name and args..
                    main.setMainClass(arg);
                    String a[] = new String[alist.size()];
                    alist.toArray(a);
                    main.setArgs(a);
                    break;
                }
            }

            // Validate required arguments/options.
            if( main.getRunnable() == null ) {
                if (main.getMainClass() == null) {
                    throw new SyntaxException("Main class not specified.");
                }
                if (main.getClassLoader() == null) {
                    throw new SyntaxException("--classloader not specified.");
                }
            }
            if (main.getCache() == null) {
                throw new SyntaxException("--cache not specified.");
            }
            if (main.getDistributor() == null) {
                throw new SyntaxException("--distributor not specified.");
            }

        } catch (SyntaxException e) {
            System.out.println("Invalid Syntax: " + e.getMessage());
            System.out.println();
            showUsage();
            System.exit(2);
        }
        main.execute();
    }

    private static void showUsage() {
        System.out.println();
    }

    private void execute() throws Throwable {

        // Store our options in the System properties.. they might be usefull
        // to the booted application.
        System.setProperty("cloudlaunch.bootstrap.distributor", this.distributor);
        System.setProperty("cloudlaunch.bootstrap.cache", cache.getPath());
        if( runnable !=null ) {
            System.setProperty("cloudlaunch.bootstrap.runnable", runnable);
        } else {
            System.setProperty("cloudlaunch.bootstrap.classloader", classLoader);
            System.setProperty("cloudlaunch.bootstrap.mainclass", mainClass);
        }

        DistributorFactory.setDefaultRegistryUri(this.distributor);
        Distributor distributor = DistributorFactory.createDefaultDistributor();


        System.out.println("bootstrap started...");
        if( runnable !=null ) {
            Runnable r = null;
            try {
                Marshalled<Runnable> marshalled = distributor.getRegistryObject(runnable);
                if( marshalled == null ) {
                    throw new Exception("The runnable not found at: "+ runnable);
                }
//              distributor.getRegistry().remove(runnable, false);
                ClassLoaderFactory clf = marshalled.getClassLoaderFactory();

                System.out.println("Setting up classloader...");
                ClassLoader cl = clf.createClassLoader(getClass().getClassLoader(), cache);

                System.out.println("Executing runnable.");
                r = marshalled.get(cl);
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(100);
            }

            r.run();

            try {
                // The runnable can set the exit code via a system prop.
                System.exit(Integer.parseInt(System.getProperty("cloudlaunch.bootstrap.exit", "0")));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                System.exit(101);
            }

        } else {
            Method mainMethod = null;
            try {
                ClassLoaderFactory clf = distributor.getRegistryObject(this.classLoader);

                System.out.println("Setting up classloader...");
                ClassLoader cl = clf.createClassLoader(getClass().getClassLoader(), cache);

                System.out.println("Executing main.");
                Class<?> clazz = cl.loadClass(mainClass);
                // Invoke the main.
                mainMethod = clazz.getMethod("main", new Class[] { String[].class });
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(100);
            }
            try {
                mainMethod.invoke(null, new Object[] { args });
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }


    ///////////////////////////////////////////////////////////////////
    // Property Accessors
    ///////////////////////////////////////////////////////////////////

    public void setDistributor(String uri) {
        distributor = uri;
    }
    
    public String getDistributor()
    {
        return distributor;
    }
    
    public void setClassLoader(String classLoader) {
        this.classLoader = classLoader;
    }

    public void setCache(File cache) {
        this.cache = cache;
    }

    public File getCache() {
        return cache;
    }

    public String getClassLoader() {
        return classLoader;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getRunnable() {
        return runnable;
    }

    public void setRunnable(String runnable) {
        this.runnable = runnable;
    }
}