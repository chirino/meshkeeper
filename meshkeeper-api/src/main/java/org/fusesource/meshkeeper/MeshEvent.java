/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.fusesource.meshkeeper;

import java.io.Serializable;

/**
 * Event
 * <p>
 * Defines a generic event.
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class MeshEvent implements Serializable {

    private static final long serialVersionUID = 1;

    public MeshEvent() {

    }

    public MeshEvent(int type, String source, Object attachment) {
        this.type = type;
        this.source = source;
        this.attachment = attachment;
    }

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
    
    public String toString()
    {
        return "Event: [" + type + "] from " + source + " attachment: " + attachment;
    }
}
