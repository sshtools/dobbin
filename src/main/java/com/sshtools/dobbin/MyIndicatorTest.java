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

import static com.sshtools.dobbin.IndicatorMenuItem.action;
import static com.sshtools.dobbin.IndicatorMenuItem.checkbox;
import static com.sshtools.dobbin.IndicatorMenuItem.label;
import static com.sshtools.dobbin.IndicatorMenuItem.separator;

public class MyIndicatorTest {
	public static void main(String[] args) throws Exception {
		/* Get the indicator area */
		var area = IndicatorArea.get();
		
		/* Use the builder to create an Indicator */
		try(var indicator = area.builder().
				
			/* The initial indicator icon and tooltip */
			icon(MyIndicatorTest.class.getResource("idle-48.png")).
			tooltip("Indicator Test").
			
			/* Invoked when indicator is left-clicked */
			onAction((ind) -> {
				System.out.println("App open! " + ind);
			}).
			
			/* The initial menu. Show when indicator is right-clicked */
			menu(label("Some label"),
				action("Action 1", (itm) -> {
					System.out.println("Action 1 " + itm);
				}),
				checkbox("Always On Top", (itm) -> {}),
				separator(),
				action("Quit", (itm) -> {
					System.exit(0);
				})).
			
			/* Build the indicator. It will remain until close() is called, 
			 * or in this example when the try-with-resource goes out of scope  */
			build()) {
			
			Thread.sleep(5000);
			indicator.tooltip("Change the tooltip!");
			
			Thread.sleep(5000);
			indicator.icon(MyIndicatorTest.class.getResource("dialog-error-48.png"));
		}
	}

}
