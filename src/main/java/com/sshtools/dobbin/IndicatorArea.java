/**
 * Copyright © 2025 SSHTOOLS Limited (support@sshtools.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.dobbin;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IndicatorArea implements Closeable {
	
	private final static class Defaults {
		private final static IndicatorArea DEFAULT = new IndicatorArea();
	}
	
	private final Map<URL, Path> resourceFiles = new HashMap<>();
	private final Set<Path> tmpfiles = new LinkedHashSet<>();
	
	final List<Indicator> indicators = new ArrayList<>();

	private IndicatorArea() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			tmpfiles.forEach(tf -> {
				try {
					Files.deleteIfExists(tf);
				} catch (IOException e) {
				}
			});
		}, "DobbinShutdown"));
	}
	
	@Override
	public void close() {
		while(!indicators.isEmpty()) {
			indicators.get(0).close();
		}
	}

	public IndicatorBuilder builder() {
		return new IndicatorBuilder(this);
	}
	
	public static IndicatorArea get() {
		return Defaults.DEFAULT;
	}
	
	Path resourceToPath(URL resource) {
		if(resourceFiles.containsKey(resource)) {
			return resourceFiles.get(resource);
		}
		else {
			var path = resource.getPath();
			if(resource.getProtocol().equals("file")) {
				return Path.of(path);
			}
			else {
				try {
					var idx = path.lastIndexOf('.');
					var ext = "img";
					if(idx > -1) {
						ext = path.substring(idx + 1);
					}
					var tf = Files.createTempFile("dobbin", "." + ext);
					try(var out = Files.newOutputStream(tf)) {
						try(var in = resource.openStream()) {
							in.transferTo(out);
						}
					}
					tmpfiles.add(tf);
					resourceFiles.put(resource, tf);
					return tf;
				}
				catch(IOException ioe) {
					throw new UncheckedIOException(ioe);
				}
			}
		}
	}
}
