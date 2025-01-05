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

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class IndicatorMenuItem {
	
	public final static class Builder {
		private final MenuItemType type;
		private Optional<String> text = Optional.empty();
		private Optional<Path> icon = Optional.empty();
		private Optional<URL> iconURL = Optional.empty();
		private boolean disabled;
		private boolean checked;
		private Optional<IndicatorActionEvent> onAction = Optional.empty();
		private final List<IndicatorMenuItem> menu = new ArrayList<>();
		
		public Builder(MenuItemType type) {
			this.type = type;
		}
		
		/**
		 * Set the items the menu initially contains
		 * 
		 * @param items items
		 * @return this for chaining
		 */
		public Builder menu(IndicatorMenuItem... items) {
			return menu(Arrays.asList(items));
		}
		
		/**
		 * Set the items the menu initially contains
		 * 
		 * @param items items
		 * @return this for chaining
		 */
		public Builder menu(Collection<IndicatorMenuItem> items) {
			this.menu.clear();
			this.menu.addAll(items);
			return this;
		}

		/**
		 * Set the callback that will be invoked when the indicator is clicked, i.e. its
		 * primary action, e.g. open the app window.
		 * 
		 * @param onAction callback
		 * @return this for chaining
		 */
		public Builder onAction(IndicatorActionEvent onAction) {
			this.onAction = Optional.of(onAction);
			return this;
		}
		
		public Builder text(String text) {
			this.text = Optional.of(text);
			return this;
		}

		public Builder iconURL(URL iconURL) {
			this.iconURL = Optional.of(iconURL);
			return this;
		}
		
		public Builder icon(Path icon) {
			this.icon = Optional.of(icon);
			return this;
		}

		public Builder selected() {
			return checked(true);
		}

		public Builder deselected() {
			return checked(true);
		}

		public Builder checked(boolean checked) {
			this.checked = checked;
			return this;
		}
		
		public Builder enabled() {
			return enabled(true);
		}

		public Builder disabled() {
			return disabled(true);
		}

		public Builder enabled(boolean enabled) {
			return disabled(!enabled);
		}
		
		public Builder disabled(boolean disabled) {
			this.disabled = disabled;
			return this;
		}
		
		public IndicatorMenuItem build() {
			return new IndicatorMenuItem(this);
		}
	}
	
	private final MenuItemType type;
	private String text; 
	private boolean disabled; 
	private boolean checked;
	private Optional<Path> icon = Optional.empty();
	private Optional<URL> iconURL = Optional.empty();
	private Optional<IndicatorActionEvent> onAction = Optional.empty();
	private final List<IndicatorMenuItem> children = new ArrayList<>();

	private IndicatorMenuItem(Builder bldr) {
		this.type = bldr.type;
		this.text = bldr.text.orElse("Dobbin");
		this.icon = bldr.icon;
		this.iconURL = bldr.iconURL;
		this.disabled = bldr.disabled;
		this.checked =  bldr.checked;
		this.onAction = bldr.onAction;		
	}
	
	public String text() {
		return text;
	}

	public Optional<Path> resolveIcon(IndicatorArea area) {
		if(icon.isPresent())
			return icon;
		else {
			return iconURL.map(u -> area.resourceToPath(u));
		}
	}

	/*
	 * public void text(String text) { this.text = text; }
	 */

	public boolean disabled() {
		return disabled;
	}

//	public void disabled(boolean disabled) {
//		this.disabled = disabled;
//	}

	public boolean checked() {
		return checked;
	}

	/* public */void checked(boolean checked) {
		this.checked = checked;
	}

	/*
	 * public void onAction(IndicatorActionEvent onAction) { this.onAction =
	 * Optional.of(onAction); }
	 */

	Optional<IndicatorActionEvent> onAction() {
		return onAction;
	}

	public MenuItemType type() {
		return type;
	}

	public List<IndicatorMenuItem> children() {
		return Collections.unmodifiableList(children);
	}

	public static IndicatorMenuItem label(String text) {
		return new Builder(MenuItemType.LABEL).text(text).build();
	}

	public static IndicatorMenuItem label(String text, Path icon) {
		return new Builder(MenuItemType.LABEL).text(text).icon(icon).build();
	}
	
	public static IndicatorMenuItem separator() {
		return new Builder(MenuItemType.SEPARATOR).build();
	}
	
	public static IndicatorMenuItem action(String text, IndicatorActionEvent  onAction) {
		return new Builder(MenuItemType.ACTION).text(text).onAction(onAction).build();
	}
	
	public static IndicatorMenuItem action(String text, Path icon, IndicatorActionEvent  onAction) {
		return new Builder(MenuItemType.ACTION).text(text).icon(icon).onAction(onAction).build();
	}
	
	public static IndicatorMenuItem action(String text, URL icon, IndicatorActionEvent  onAction) {
		return new Builder(MenuItemType.ACTION).text(text).iconURL(icon).onAction(onAction).build();
	}
	
	public static IndicatorMenuItem action(String text, boolean disabled, IndicatorActionEvent onAction) {
		return new Builder(MenuItemType.ACTION).text(text).onAction(onAction).disabled(disabled).build();
	}
	
	public static IndicatorMenuItem checkbox(String text, IndicatorActionEvent onAction) {
		return new Builder(MenuItemType.CHECKBOX).text(text).onAction(onAction).build();
	}
	
	public static IndicatorMenuItem checkbox(String text, boolean checked, IndicatorActionEvent onAction) {
		return new Builder(MenuItemType.CHECKBOX).text(text).checked(checked).onAction(onAction).build();
	}
	
	public static IndicatorMenuItem checkbox(String text, boolean checked, boolean disabled, IndicatorActionEvent onAction) {
		return new Builder(MenuItemType.CHECKBOX).text(text).checked(checked).disabled(disabled).onAction(onAction).build();
	}

}
