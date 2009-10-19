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
public interface Distributable{

}
