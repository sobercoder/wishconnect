/*
 * Copyright 2014 Sandipan Das.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.reactionlabs.wishconnect.boot;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * @author Sandipan Das
 * @version 1.0
 */
public class BootResourceURLStreamHandlerFactory implements URLStreamHandlerFactory {
		private ClassLoader classLoader;
		private URLStreamHandlerFactory chainFactory;

		public BootResourceURLStreamHandlerFactory(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Override
		public URLStreamHandler createURLStreamHandler(String protocolName) {
			if (protocolName.equals(BootResourceURLStreamHandler.BOOT_RESOURCE_PROTOCOL_NAME)) {
				return new BootResourceURLStreamHandler(classLoader);
			}
			if (chainFactory != null) {
				return chainFactory.createURLStreamHandler(protocolName);
			}
			return null;
		}

		public void setURLStreamHandlerFactory(URLStreamHandlerFactory factory) {
			chainFactory = factory;
		}
	}
