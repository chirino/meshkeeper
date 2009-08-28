/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.launcher;

import org.fusesource.cloudlaunch.LaunchDescription;
import static org.fusesource.cloudlaunch.Expression.file;

import java.util.LinkedList;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;


/**
 * Java main class that be be used to bootstrap the classpath
 * via remote classpath downloading of another main class.
 *
 * @author chirino
 */
public class RemoteBootstrap {

    private File classLoaderLibs;
    private File cache;
    private String classLoader;
    private String mainClass;
    private String[] args;
    private String distributor;

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
                } else if (arg.equals("--classloader")) {
                    try {
                        main.setClassLoader(alist.removeFirst());
                    } catch (Throwable e) {
                        throw new SyntaxException("Expected a url after the --classloader option.");
                    }
                } else if (arg.equals("--classloader-libs")) {
                    try {
                        main.setClassLoaderLibs(new File(alist.removeFirst()).getCanonicalFile());
                    } catch (Throwable e) {
                        throw new SyntaxException("Expected a directoy after the --classloader-libs option.");
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
            if (main.getMainClass() == null) {
                throw new SyntaxException("Main class not specified.");
            }
            if (main.getCache() == null) {
                throw new SyntaxException("--cache not specified.");
            }
            if (main.getClassLoader() == null) {
                throw new SyntaxException("--classloader not specified.");
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

    private void execute() throws Throwable {
        ClassLoader mainCl = loadMainClassLoader();
        Class<?> clazz = mainCl.loadClass(mainClass);

        // Store our options in the System properties.. they might be usefull
        // to the booted application.
        System.setProperty("cloudlaunch.bootstrap.distributor", distributor);
        System.setProperty("cloudlaunch.bootstrap.classloader", classLoader);
        System.setProperty("cloudlaunch.bootstrap.cache", cache.getPath());
        if( classLoaderLibs!=null ) {
            System.setProperty("cloudlaunch.bootstrap.classloader-libs", classLoaderLibs.getPath());
        }
        
        // Invoke the main.
        Method mainMethod = clazz.getMethod("main", new Class[] { String[].class });
        try {
            mainMethod.invoke(null, new Object[] { args });
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private ClassLoader loadMainClassLoader() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        if (classLoaderLibs != null) {
            // Doing this allows us to keep the classes used in remote class loading
            // seperate from the classes loaded by those classloaders.
            ArrayList<URL> urls = new ArrayList<URL>();
            for (File file : classLoaderLibs.listFiles()) {
                if (file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))) {
                    urls.add(file.getCanonicalFile().toURI().toURL());
                }
            }
            URL u[] = new URL[urls.size()];
            urls.toArray(u);
            cl = new URLClassLoader(u);
        }

        // Create the remote classloader
        Class<?> remoteCLClass = cl.loadClass("org.fusesource.cloudlaunch.classloader.ClassLoaderServerFactory");
        Method method = remoteCLClass.getMethod("createClassLoader", new Class[] { String.class, String.class, File.class, ClassLoader.class });

        // Use the new remote classloader to load the main class
        System.out.println("Looking up remote class loader...");
        return (ClassLoader) method.invoke(null, new Object[] {distributor, classLoader, cache, null});
    }

    private static void showUsage() {
        System.out.println();
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

    public File getClassLoaderLibs() {
        return classLoaderLibs;
    }

    public void setClassLoaderLibs(File classLoaderLibs) {
        this.classLoaderLibs = classLoaderLibs;
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

}