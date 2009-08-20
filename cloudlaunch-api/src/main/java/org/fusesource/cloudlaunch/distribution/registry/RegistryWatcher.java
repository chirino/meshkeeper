/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.registry;

import java.util.List;

/** 
 * RegsitryWatcher
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface RegistryWatcher {

    public void onChildrenChanged(String path, List<String> children);
}
