/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;

/** 
 * CloudLaunchClassLoader
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class CloudLaunchClassLoader extends ClassLoader {

    private final Hashtable<String, Class<?>> classes = new Hashtable<String, Class<?>>();
    
    public CloudLaunchClassLoader(){
        super(Thread.currentThread().getContextClassLoader());
    }
  
    public Class<?> loadClass(String className) throws ClassNotFoundException {
         return findClass(className);
    }
 
    public Class<?> findClass(String className){
        byte classByte[];
        Class<?> result=null;
        result = (Class<?>)classes.get(className);
        if(result != null){
            return result;
        }
        
        try{
            return findSystemClass(className);
        }catch(ClassNotFoundException e){
        }
        try{
           String classPath =    ((String)ClassLoader.getSystemResource(className.replace('.',File.separatorChar)+".class").getFile()).substring(1);
           classByte = loadClassData(classPath);
            result = defineClass(className,classByte,0,classByte.length,null);
            classes.put(className,result);
            return result;
        }catch(Exception e){
            return null;
        } 
    }
 
    private byte[] loadClassData(String className) throws IOException{
 
        File f ;
        f = new File(className);
        int size = (int)f.length();
        byte buff[] = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        dis.readFully(buff);
        dis.close();
        return buff;
    }
}
