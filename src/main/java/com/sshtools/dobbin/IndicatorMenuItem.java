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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class IndicatorMenuItem {
	private final MenuItemType type;
	private String text; 
	private boolean disabled; 
	private boolean checked;
	private Optional<IndicatorActionEvent> onAction = Optional.empty();
	private final List<IndicatorMenuItem> children = new ArrayList<>();
	
	private IndicatorMenuItem(MenuItemType type, String text, boolean disabled, boolean checked, IndicatorActionEvent onAction) {
		super();
		this.type = type;
		this.text = text;
		this.disabled = disabled;
		this.checked = checked;
		this.onAction = Optional.ofNullable(onAction);
	}

	public String text() {
		return text;
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
		return new IndicatorMenuItem(MenuItemType.LABEL, text, false, false, null);
	}
	
	public static IndicatorMenuItem separator() {
		return new IndicatorMenuItem(MenuItemType.SEPARATOR, "-", false, false, null);
	}
	
	public static IndicatorMenuItem action(String text, IndicatorActionEvent  onAction) {
		return new IndicatorMenuItem(MenuItemType.ACTION, text, false, false, onAction);
	}
	
	public static IndicatorMenuItem action(String text, boolean disabled, IndicatorActionEvent onAction) {
		return new IndicatorMenuItem(MenuItemType.ACTION, text, disabled, false, onAction);
	}
	
	public static IndicatorMenuItem checkbox(String text, IndicatorActionEvent onAction) {
		return new IndicatorMenuItem(MenuItemType.CHECKBOX, text, false, false, onAction);
	}
	
	public static IndicatorMenuItem checkbox(String text, boolean checked, IndicatorActionEvent onAction) {
		return new IndicatorMenuItem(MenuItemType.CHECKBOX, text, false, checked, onAction);
	}
	
	public static IndicatorMenuItem checkbox(String text, boolean checked, boolean disabled, IndicatorActionEvent onAction) {
		return new IndicatorMenuItem(MenuItemType.CHECKBOX, text, disabled, checked, onAction);
	}

}
