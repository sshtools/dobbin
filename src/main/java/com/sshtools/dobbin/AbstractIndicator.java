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

import java.nio.file.Path;
import java.util.Objects;

public abstract class AbstractIndicator implements Indicator {

	protected Path icon;
	protected String tooltip;
	protected final IndicatorArea indicatorArea;

	AbstractIndicator(IndicatorArea indicatorArea) {
		this.indicatorArea = indicatorArea;
	}

	@Override
	public final void icon(Path icon) {
		indicatorArea.task(() -> {
			if (!Objects.equals(icon, this.icon)) {
				this.icon = icon;
				rebuild();
			}
		});
	}

	@Override
	public final void tooltip(String tooltip) {
		indicatorArea.task(() -> {
			if (!Objects.equals(tooltip, this.tooltip)) {
				this.tooltip = tooltip;
				rebuild();
			}
		});
	}

	protected void rebuild() {
	}
}
