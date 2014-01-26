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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author Sandipan Das
 * @version 1.0
 */
class BootResourceURLStreamHandler extends URLStreamHandler {

	private ClassLoader classLoader;

	static final String BOOT_RESOURCE_PROTOCOL_NAME = "boot";

	public BootResourceURLStreamHandler(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new BootResourceURLConnection(url, classLoader);
	}

	@Override
	protected void parseURL(URL url, String spec, int start, int limit) {
		String file;
		if (spec.startsWith(BOOT_RESOURCE_PROTOCOL_NAME + ":")) {
			file = spec.substring(5);
		} else if (url.getFile().equals("./")) {
			file = spec;
		} else if (url.getFile().endsWith("/")) {
			file = url.getFile() + spec;
		} else {
			file = spec;
		}
		setURL(url, BOOT_RESOURCE_PROTOCOL_NAME, "", -1, null, null, file, null, null);
	}
}
