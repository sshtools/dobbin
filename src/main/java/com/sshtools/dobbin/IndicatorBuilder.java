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

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.sshtools.dobbin.impl.ctray.tray;
import com.sshtools.dobbin.impl.ctray.tray.cb;
import com.sshtools.dobbin.impl.ctray.tray_h;
import com.sshtools.dobbin.impl.ctray.tray_menu_item;

/**
 * Build a new indicator. 
 */
public final class IndicatorBuilder {

	private Optional<String> tooltip = Optional.empty();
	private Optional<Path> icon = Optional.empty();
	private Optional<IndicatorEvent> onAction = Optional.empty();
	private final List<IndicatorMenuItem> menu = new ArrayList<>();
	private final IndicatorArea indicatorArea;
	
	IndicatorBuilder(IndicatorArea indicatorArea) {
		this.indicatorArea = indicatorArea;
	}
	
	/**
	 * Set the items the menu initially contains
	 * 
	 * @param items items
	 * @return this for chaining
	 */
	public IndicatorBuilder menu(IndicatorMenuItem... items) {
		return menu(Arrays.asList(items));
	}
	
	/**
	 * Set the items the menu initially contains
	 * 
	 * @param items items
	 * @return this for chaining
	 */
	public IndicatorBuilder menu(Collection<IndicatorMenuItem> items) {
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
	public IndicatorBuilder onAction(IndicatorEvent onAction) {
		this.onAction = Optional.of(onAction);
		return this;
	}

	/**
	 * Set the icon for this indicator. 
	 * 
	 * @param icon icon
	 * @return this for chaining
	 */
	public IndicatorBuilder icon(String icon) {
		return icon(Paths.get(icon));
	}

	/**
	 * Set the icon for this indicator. 
	 * 
	 * @param icon icon
	 * @return this for chaining
	 */
	public IndicatorBuilder icon(File icon) {
		return icon(icon.toPath());
	}

	/**
	 * Set the icon for this indicator. 
	 * 
	 * @param icon icon
	 * @return this for chaining
	 */
	public IndicatorBuilder icon(Path icon) {
		this.icon = Optional.of(icon);
		return this;
	}

	/**
	 * Convenience method to set the icon for this indicator using a URL. This can
	 * be used for example with class path resource URLs (when supported), internet
	 * URLs (when supported) or file URLs.
	 * 
	 * @param icon icon name or path
	 * @return this for chaining
	 */
	public IndicatorBuilder icon(URL icon) {
		return icon(indicatorArea.resourceToPath(icon));
	}

	/**
	 * Get this tooltip for this indicator.
	 * 
	 * @return tooltip
	 */
	public Optional<String> tooltip() {
		return tooltip;
	}

	/**
	 * Set this tooltip for this indicator.
	 * 
	 * @param tooltip title
	 * @return this for chaining
	 */
	public IndicatorBuilder tooltip(String tooltip) {
		this.tooltip = Optional.of(tooltip);
		return this;
	}

	/**
	 * Create a new indicator based on the configuration in this builder.
	 */
	public Indicator build() {
		return new CTrayIndicator(this);
	}
	
	private final static class CTrayIndicator extends AbstractIndicator {
		
		private final MemorySegment trayMem;
		private final Arena arena;
		private IndicatorMenuItem[] root = new IndicatorMenuItem[0];
		private Thread thread;
		private final IndicatorArea indicatorArea;

		private CTrayIndicator(IndicatorBuilder builder) {
			this.indicatorArea = builder.indicatorArea;
			this.tooltip = builder.tooltip.orElse("Dobbin");
			this.icon = builder.icon.orElseGet(() -> builder.indicatorArea.resourceToPath(IndicatorArea.class.getResource("idle-48.png")));
			this.root = builder.menu.toArray(new IndicatorMenuItem[0]); 
			
			arena = Arena.ofAuto();
			trayMem = arena.allocate(tray.layout());
			
			builder.onAction.ifPresent(act -> {
				@SuppressWarnings("unused")
				var menucb = cb.allocate(seg -> {
					act.action(this);
				}, arena);
				tray.cb(trayMem, menucb);
			});
			
			configure();

			thread = new Thread(this::mainLoop, "CTrayLoop");
			thread.start();
		}

		@Override
		public void update(IndicatorMenuItem... root) {
			this.root = root;
			rebuild();
		}

		@Override
		public void close() {
			indicatorArea.indicators.remove(this);
		}
		
		@Override
		protected void rebuild() {
			configure();
			tray_h.tray_update(trayMem);
		}

		@SuppressWarnings("unused")
		private void configure() {
			tray.icon_filepath(trayMem, arena.allocateFrom(icon.toString(), Charset.forName("US-ASCII")));
			tray.tooltip(trayMem, arena.allocateFrom(tooltip, Charset.forName("US-ASCII")));
			var items = tray_menu_item.allocateArray(root.length + 1, arena);
			for(var i = 0 ; i< root.length; i++) {
				var itemSeg = tray_menu_item.asSlice(items, i);
				var item = root[i];
				
				tray_menu_item.text(itemSeg, arena.allocateFrom(item.text(), Charset.forName("US-ASCII")));
				
				switch(item.type()) {
				case ACTION:
					item.onAction().ifPresent(act -> {
						tray_menu_item.cb(itemSeg, tray_menu_item.cb.allocate(seg -> {
							act.action(item);
						}, arena));
					});
					break;
				case CHECKBOX:
					tray_menu_item.checkbox(itemSeg, 1);
					tray_menu_item.checked(itemSeg, item.checked() ? 1 : 0);
					tray_menu_item.cb(itemSeg, tray_menu_item.cb.allocate(seg -> {
						item.checked(tray_menu_item.checked(itemSeg) == 1);
						item.onAction().ifPresent(act -> act.action(item));
					}, arena));
					break;
				case SEPARATOR:
					break;
				default:
					break;
				}
			}
			tray.menu(trayMem, items);
		}

		@Override
		public boolean loop() {
			// TODO Auto-generated method stub
			return false;
		}
		
		private void mainLoop() {
			if(tray_h.tray_init(trayMem) < 0) {
				throw new IllegalStateException("Failed to initialise tray.");
			}
			try {
				// TODO what does blocking mean here
				int blocking = 0;
				while (tray_h.tray_loop(blocking) == 0) {
					if (blocking == 0) {
						Thread.sleep(10);
					}
				}
			}
			catch(Exception e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public void icon(URL icon) {
			icon(indicatorArea.resourceToPath(icon));
		}
		
	}
}