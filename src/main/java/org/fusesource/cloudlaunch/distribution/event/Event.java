/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.event;

import java.io.Serializable;


/** 
 * Event
 * <p>
 * Defines a generic event. 
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class Event implements Serializable{

    private static final long serialVersionUID = 1;
    
    private int type;
    private String source;
    private Object attachment;

    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getAttachment() {
        return (T) attachment;
    }
    
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }
}
