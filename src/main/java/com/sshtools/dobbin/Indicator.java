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
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Represents a single instance of an <em>Indicator<em>, as configured and
 * created by an {@link IndicatorBuilder}.
 */
public interface Indicator extends Closeable {

	void update(IndicatorMenuItem... root);

	default void update(Collection<IndicatorMenuItem> root) {
		update(root.toArray(new IndicatorMenuItem[0]));
	}

	@Override
	void close();

	/**
	 * Set the icon for this indicator using a URL. This can
	 * be used for example with class path resource URLs (when supported), internet
	 * URLs (when supported) or file URLs.
	 * 
	 * @param icon icon URL
	 */
	void icon(URL icon);

	/**
	 * Set the icon for this indicator. 
	 * 
	 * @param icon icon path
	 */
	void icon(Path icon);

	/**
	 * Set this tooltip for this indicator.
	 * 
	 * @param tooltip tooltip
	 * @return this for chaining
	 */
	void tooltip(String tooltip);

	/**
	 * If you are integrating with your toolkits main loop, this method should be called.
	 * 
	 * @return icon name or path
	 */
	boolean loop();
}
