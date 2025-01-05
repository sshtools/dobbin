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
import com.sshtools.dobbin.impl.ctray.tray_h;
import com.sshtools.dobbin.impl.ctray.tray_menu_item;
import com.sshtools.dobbin.impl.ctray.tray.cb;

/**
 * Represents a single instance of an <em>Indicator<em>, as configured and
 * created by an {@link Builder}.
 */
public interface Indicator extends Closeable {
	
	/**
	 * Build a new indicator. 
	 */
	public final class Builder {

		private final static class CTrayIndicator extends AbstractIndicator {
			
			private final MemorySegment trayMem;
			private final Arena arena;
			private IndicatorMenuItem[] root = new IndicatorMenuItem[0];
			private boolean closed;

			private CTrayIndicator(Builder builder) {
				super(builder.indicatorArea);
				this.tooltip = builder.tooltip.orElse("Dobbin");
				
				if(builder.icon.isPresent())
					this.icon = builder.icon.get();
				else {
					if(builder.iconURL.isPresent()) {
						this.icon = builder.indicatorArea.resourceToPath(builder.iconURL.get());
					}
					else {
						this.icon = builder.indicatorArea.resourceToPath(IndicatorArea.class.getResource("idle-48.png"));
					}
				}

				this.indicatorArea.add(this);
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
				
				if(builder.indicatorArea.isTaskThread())
					completeInit();
				else
					builder.indicatorArea.task(this::completeInit);
			}

			@Override
			public void close() {
				if(!closed) {
					if(indicatorArea.isTaskThread()) {
						doClose();
					}
					else {
						indicatorArea.task(this::doClose);
						try {
							while(!closed) {
								Thread.sleep(1);
							}
						} catch (InterruptedException e) {
						}
					}
				}
			}

			@Override
			public void icon(URL icon) {
				icon(indicatorArea.resourceToPath(icon));
			}

			@Override
			public void update(IndicatorMenuItem... root) {
				this.root = root;
				rebuild();
			}
			
			@Override
			protected void rebuild() {
				configure();
				tray_h.tray_update(trayMem);
			}

			private void completeInit() {

				configure();
				if(tray_h.tray_init(trayMem) < 0) {
					throw new IllegalStateException("Failed to initialise tray.");
				}
				
				queueLoop();
				
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
			
			private void doClose() {
				try {
					indicatorArea.remove(this);
					tray_h.tray_exit();
				}
				finally {
					closed = true;
				}
			}

			private boolean loop() {
				return !closed && tray_h.tray_loop(0) == 0;
			}
			
			private void queueLoop() {
				indicatorArea.task(() -> {
					if(loop())
						queueLoop();
				});
			}
			
		}
		private Optional<String> tooltip = Optional.empty();
		private Optional<Path> icon = Optional.empty();
		private Optional<URL> iconURL = Optional.empty();
		private Optional<IndicatorEvent> onAction = Optional.empty();
		private final List<IndicatorMenuItem> menu = new ArrayList<>();
		
		private final IndicatorArea indicatorArea;
		
		Builder(IndicatorArea indicatorArea) {
			this.indicatorArea = indicatorArea;
		}
		
		/**
		 * Create a new indicator based on the configuration in this builder.
		 */
		public Indicator build() {
			return new CTrayIndicator(this);
		}
		

		/**
		 * Set the icon for this indicator. 
		 * 
		 * @param icon icon
		 * @return this for chaining
		 */
		public Builder icon(File icon) {
			return icon(icon.toPath());
		}

		/**
		 * Set the icon for this indicator. 
		 * 
		 * @param icon icon
		 * @return this for chaining
		 */
		public Builder icon(Path icon) {
			this.icon = Optional.of(icon);
			return this;
		}

		/**
		 * Set the icon for this indicator. 
		 * 
		 * @param icon icon
		 * @return this for chaining
		 */
		public Builder icon(String icon) {
			return icon(Paths.get(icon));
		}

		/**
		 * Convenience method to set the icon for this indicator using a URL. This can
		 * be used for example with class path resource URLs (when supported), internet
		 * URLs (when supported) or file URLs.
		 * 
		 * @param icon icon name or path
		 * @return this for chaining
		 */
		public Builder icon(URL iconURL) {
			this.iconURL = Optional.of(iconURL);
			return this;
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
		 * Set the items the menu initially contains
		 * 
		 * @param items items
		 * @return this for chaining
		 */
		public Builder menu(IndicatorMenuItem... items) {
			return menu(Arrays.asList(items));
		}

		/**
		 * Set the callback that will be invoked when the indicator is clicked, i.e. its
		 * primary action, e.g. open the app window.
		 * 
		 * @param onAction callback
		 * @return this for chaining
		 */
		public Builder onAction(IndicatorEvent onAction) {
			this.onAction = Optional.of(onAction);
			return this;
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
		public Builder tooltip(String tooltip) {
			this.tooltip = Optional.of(tooltip);
			return this;
		}
	}

	@Override
	void close();

	/**
	 * Set the icon for this indicator. 
	 * 
	 * @param icon icon path
	 */
	void icon(Path icon);

	/**
	 * Set the icon for this indicator using a URL. This can
	 * be used for example with class path resource URLs (when supported), internet
	 * URLs (when supported) or file URLs.
	 * 
	 * @param icon icon URL
	 */
	void icon(URL icon);

	/**
	 * Set this tooltip for this indicator.
	 * 
	 * @param tooltip tooltip
	 * @return this for chaining
	 */
	void tooltip(String tooltip);

	default void update(Collection<IndicatorMenuItem> root) {
		update(root.toArray(new IndicatorMenuItem[0]));
	}

	void update(IndicatorMenuItem... root);
}
