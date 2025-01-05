# dobbin
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sshtools/dobbin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.sshtools/dobbin)
[![javadoc](https://javadoc.io/badge2/com.sshtools/dobbin/javadoc.svg)](https://javadoc.io/doc/com.sshtools/dobbin)
![JPMS](https://img.shields.io/badge/JPMS-com.sshtools.dobbin-purple) 

Simple library for "System Tray" support in Java 22 and above on Windows, Mac OS X and Linux.

## Features

 * Single tiny Java utility dependency with no further transitive dependencies.
 * Single native dependency, that itself will depend on operating system provided libraries.
 * Supports Menu items, Checkbox Menu items, Separators and Sub-menus in popup menus.
 * Callback may be attached to any supported menu item.
 * Icon, tooltip and menu contents may be changed at any time.
 * Developed for use with  Linux (using QT). Other operating systems including Windows and Mac OS will follow.
 * Integrates with your GUI toolkits thread.
 
### Planned Or In Progress

 * Windows and Mac OS support (and other platforms based on demand)
 * Icons in menu items
 * Accelerators in menu items

## Rationale

Java does have alternatives for system tray support, but none are absolutely perfect. This project is our attempt, initially to satisfy the needs of our own projects, several of which use system tray icons.

 * Java system tray (or "Indicator") support is incredibly fragmented.
 * AWT has an implementation, but it either looks crap, or doesn't work outside of Windows properly.
 * SWT has an implementation that is pretty good and quite consistent. But, SWT doesn't work well with JPMS. It also has image format related oddities.
 * JavaFX has nothing, so you must use AWT (and so include all of AWT even if its not otherwise needed).
 * [DorkBox](https://github.com/dorkbox/SystemTray) exists, with a stated aim of providing a better system tray for Java. It's great, but it has quite a lot of dependencies including requiring the entire kotlin runtime. It also has issues when used with fully JPMS compliant applications.
 
To address this, and making use of FFMAPI, *Dobbin* delegates all system tray functionality to another native library, [tray](https://github.com/zserge/tray).

**Actually a fork-of-a-fork (from https://github.com/dmikushin/tray) of the C library is currently in use. Various forks of this original library have additional features or maintenance,
and additional changes for my own requirements meant my own hopefully temporary fork.**

This approach means *Dobbin* will work with any toolkit include AWT, Swing, SWT, JavaFX or even "headless" background desktop services.  Builds of the C library for common operating systems and architectures will be included with this library and extracted automatically when needed.

The downside is that only relatively simple menus may be added to your applications popup window. You cannot embed any old toolkit specific component. For many applications though, this will be more than sufficient. Requiring such a modern version of Java may also mean you cannot use this project just yet, but this choice allows Dobbin to be very lightweight.

 
## Configuring your project

The library is available in Maven Central, so configure your project according to the
build system you use. For example, for Maven itself :-

```xml
	<dependencies>
		<dependency>
			<groupId>com.sshtools</groupId>
			<artifactId>dobbin</artifactId>
			<version>0.0.1</version>
		</dependency>
	</dependencies>
```

### Snapshots

*Development builds may be available in the snapshots repository*

```xml

<repositories>
	<repository>
		<id>oss-snapshots</id>
		<url>https://oss.sonatype.org/content/repositories/snapshots</url>
		<snapshots>
			<enabled>true</enabled>
		</snapshots>
		<releases>
			<enabled>false</enabled>
		</releases>
	</repository>
</repositories>
	
..

<dependencies>
	<dependency>
		<groupId>com.sshtools</groupId>
		<artifactId>dobbin</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</dependency>
</dependencies>
```

## Usage

The general life cycle of an indicator is ..

 * Create a `IndicatorArea.Builder`. 
 * Configure the builder, for example integrate with your GUI toolkit thread.
 * Call `build()` to obtain the `IndicatorArea` instance. An area in the future may hold multiple indicators.
 * Obtain an `Indicator.Builder` using `IndicatorArea.builder()`. 
 * Configure the indicators icon, text, and menu structure using the builder.
 * Call `build()` to obtain on `Indicator` instance. The indicator becomes visible at this point.
 * Update the attributes of the `Indicator` at any time. Any changes to the indicator must be done on the loop thread. You can submit the task either directly, or using `IndicatorArea.task()`. 
 * Close the `Indicator` using `Indicator.close()`.
 * Close the `IndicatorArea` using `IndicatorArea.close()`.

### Threading

It is important that any changes to indicators are performed on the thread that created them. The recommended method is to integrate with your GUI toolkits main thread (if you are using one at all).

When creating the `IndicatorArea`, you can provide a `Consumer<Runnable>` that will be used to queue tasks. For example, to integrate with the Swing toolkit, you would do the following.

```java

var area = new IndicatorArea.Builder().
            loop(SwingUtilities::invokeLater).
            build()
```

If you do not provided your own queue, an internal `Executor` will be created. This executor is shutdown when the area is closed.
 
### Full Example

The following self-contained example should give you a good idea of how to use it.

```java
import static com.sshtools.dobbin.IndicatorMenuItem.action;
import static com.sshtools.dobbin.IndicatorMenuItem.checkbox;
import static com.sshtools.dobbin.IndicatorMenuItem.label;
import static com.sshtools.dobbin.IndicatorMenuItem.separator;
import static java.lang.Thread.sleep;

public class MyIndicatorTest {
    public static void main(String[] args) throws Exception {
       /* Create the indicator area */
        try(var area = new IndicatorArea.Builder().
                build()) {
        
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
                
                sleep(5000);
    
                area.task(() -> {
                    indicator.tooltip("Changed the tooltip!");
                });
                
                sleep(5000);
    
                area.task(() -> {
                    indicator.icon(MyIndicatorTest.class.getResource("dialog-error-48.png"));
                });
                
                sleep(5000);
            }
        }
    }

}

```
