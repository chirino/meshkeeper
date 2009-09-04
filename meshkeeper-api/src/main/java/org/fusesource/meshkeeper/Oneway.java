/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

/**
 * Adding this annotation to a method on a {@link Distributable} method indicates
 * that the operation should be invoked asynchronously. 
 * 
 * @author cmacnaug 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target( { java.lang.annotation.ElementType.METHOD })
public @interface Oneway {

}
