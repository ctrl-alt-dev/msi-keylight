keylight
========

Control the lights of the MSI Steel series keyboard on Ubuntu (GE62).

(C) 2016 E.Hooijmeijer, [LGPL v3 licensed](http://www.gnu.org/licenses/lgpl-3.0.en.html)

Usage:
```
 java -jar Keylight.jar MLCLCLC
 
  M: Ledmode : 0..4 (normal,gaming,breathe,demo,wave)
  L: Level :   0..3 (high,med,low,light)
  C: Color :   0..8 (off,red,orange,yellow,green,light blue,blue,purple,white)
```

Example: purple keyboard
```
 java -jar KeyLight.jar 0171717
```

Hardware check
--------------

Execute `lsusb` in the console to see if you have a device with the ID 1770:ff00.

```
Bus 001 Device 002: ID 1770:ff00 
```

If you don't have it, this program won't work.

Building
--------

Prerequisites:
* Java 7+  ([See Stackoverflow](http://stackoverflow.com/questions/14788345/how-to-install-jdk-on-ubuntu-linux))
* Maven 3+ (`sudo apt-get install maven`)

```
mvn clean install
```

The `keylight.jar` file will be in the `./target/` folder.

Non Root Configuration
----------------------

Accessing USB devices on Linux as non root user requires some configuration. 
  
You need to create/edit `/etc/udev/rules.d/99-userusbdevices.rules` and add the following line:
```
SUBSYSTEM=="usb",ATTR{idVendor}=="1770",ATTR{idProduct}=="ff00",MODE="0660",GROUP="**YOUR GROUP HERE**"
```
Insert your users group as the GROUP.  

To apply the new settings you need to invoke: 
```
udevadm trigger
```

Releases
========

0.0.1 First release 
-------------------
* First Release

FAQ
===

There are many similar implementations, why another one?
--------------------------------------------------------

I couldn't get any other implementation to work, so I decided to write my own :)

