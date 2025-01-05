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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class IndicatorArea implements Closeable {
	
	private final Map<URL, Path> resourceFiles = new HashMap<>();
	private final Set<Path> tmpfiles = new LinkedHashSet<>();
	private final List<Indicator> indicators = new CopyOnWriteArrayList<>();
	
	public final static class Builder {
		private Optional<Consumer<Runnable>> executor = Optional.empty();
		
		public Builder loop(Consumer<Runnable> executor) {
			this.executor = Optional.of(executor);
			return this;
		}
		
		public IndicatorArea build() {
			return new IndicatorArea(this);
		}
	}

	private final Optional<Consumer<Runnable>> executor;
	private ExecutorService defaultExecutor;

	private IndicatorArea(Builder bldr) {
		this.executor = bldr.executor;
		
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
		
		if(defaultExecutor != null) {
			defaultExecutor.shutdown();
		}
	}

	public Indicator.Builder builder() {
		return new Indicator.Builder(this);
	}
	
	public void task(Runnable task) {
		this.executor.ifPresentOrElse(exec -> {
			exec.accept(task);
		}, () -> {
			if(defaultExecutor == null) {
				defaultExecutor = Executors.newSingleThreadExecutor();
			}
			defaultExecutor.submit(task);
		});
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

	void add(Indicator indicator) {
		if(indicators.isEmpty())
			indicators.add(indicator);
		else
			throw new IllegalStateException("Only a single indicator per runtime is currently supported.");
	}

	void remove(Indicator indicator) {
		indicators.remove(indicator);
	}
}
