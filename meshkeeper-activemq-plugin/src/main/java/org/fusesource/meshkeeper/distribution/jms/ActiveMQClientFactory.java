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
package org.fusesource.meshkeeper.distribution.jms;

import org.fusesource.meshkeeper.distribution.jms.JMSClientFactory;
import org.fusesource.meshkeeper.distribution.jms.JMSProvider;

/**
 * ActiveMQClientFactory
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class ActiveMQClientFactory extends JMSClientFactory{

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.distribution.jms.JMSClientFactory#createConnectionFactory(java.lang.String)
     */
    @Override
    protected JMSProvider createPlugin(String uri) throws Exception {
        return new ActiveMQProvider();
    }

}
