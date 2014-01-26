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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @version 1.0
 * @author Sandipan Das
 */
public class Boot {

	private static final String JAR_RESOURCE_PROTOCOL_NAME = "jar";
	private static final String DEFAULT_CLASSPATH = "./";
	private static final String MAIN_CLASS_NAME = "org.reactionlabs.wishconnect.core.Main";

	private static void loadJars() {
		try {
			Thread currentThread = Thread.currentThread();
			ClassLoader classLoader = currentThread.getContextClassLoader();
			URLStreamHandlerFactory streamHandlerFactory = new BootResourceURLStreamHandlerFactory(classLoader);
			URL.setURLStreamHandlerFactory(streamHandlerFactory);

			List<URL> classpathUrlList = new ArrayList();
			List<String> classpathItemList = new ArrayList();

			Class bootClass = Boot.class;
			ProtectionDomain protectionDomain = bootClass.getProtectionDomain();
			CodeSource codeSource = protectionDomain.getCodeSource();
			URL currentJarUrl = codeSource.getLocation();
			JarFile currentJarFile = new JarFile(currentJarUrl.getFile());
			ArrayList<JarEntry> jarEntryList = Collections.list(currentJarFile.entries());
			classpathItemList.add(DEFAULT_CLASSPATH);

			for (JarEntry jarEntry : jarEntryList) {
				String jarEntryName = jarEntry.getName();
				if (jarEntryName.endsWith(".jar")) {
					classpathItemList.add(jarEntryName);
				}
			}

			for (String classpathItem : classpathItemList) {
				if (classpathItem.endsWith("/")) {
					classpathUrlList.add(new URL(BootResourceURLStreamHandler.BOOT_RESOURCE_PROTOCOL_NAME + ":" + classpathItem));
				} else {
					classpathUrlList.add(new URL(JAR_RESOURCE_PROTOCOL_NAME + ":" + BootResourceURLStreamHandler.BOOT_RESOURCE_PROTOCOL_NAME + ":" + classpathItem + "!/"));
				}
			}

			ClassLoader jarClassLoader = new URLClassLoader(classpathUrlList.toArray(new URL[0]), null);
			currentThread.setContextClassLoader(jarClassLoader);

			Class mainClass = Class.forName(MAIN_CLASS_NAME, true, jarClassLoader);
			Method main = mainClass.getMethod("main", new Class[]{String[].class});
			main.invoke((Object) null, new Object[]{new String[]{}});

		} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | IOException | NoSuchMethodException | SecurityException ex) {
			Logger.getLogger(Boot.class.getName()).log(Level.SEVERE, null, ex);
			System.exit(1);
		}
	}

	public static void main(String args[]) throws Exception {
		ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.schedule(
				new Runnable() {
					@Override
					public void run() {
						Boot.loadJars();
					}
				}, 0, TimeUnit.SECONDS
		);
	}
}
