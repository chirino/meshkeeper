/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution;


/** 
 * Distributable
 * <p>
 * Description: This class is similar in purpose to the {@link java.rmi.Remote} interface
 * in that it serves as a marker on an interface to indicate that the interface can be 
 * used for remote method invocation. However, unlike {@link java.rmi.Remote}, this interface
 * does not add the constraint that methods throw a {@link java.rmi.RemoteException} which makes
 * it simpler to convert an interface to a remote interface. Objects whose methods remote method 
 * invocation result in a failure of the underlying rmi impementation will instead produce a 
 * {@link RuntimeException}
 * 
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface Distributable {

}
