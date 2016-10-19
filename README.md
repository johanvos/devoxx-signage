# Devoxx Digital Signage
Digital signage used at the Devoxx conference with Raspberry PIs. 


# How to install on a PI?

1. In case you use a standard Raspian Jessie distribution, you don't need to install Java yourself, as Jessie ships with Java. Otherwise, install JDK 8 using 'sudo apt-get install oracle-java8-jdk'

2. Install OpenJFX. Download JavaFX Embedded SDK from http://gluonhq.com/open-source/javafxports/downloads/ and install it in the home directory (for example /home/pi/armv6hf-sdk)

3. Build the application on a desktop system, using ant 

4. Transfer the jar file (dist/Devoxx.jar to the pi) 

3. Run app on the pi, using 
java -Djava.ext.dirs=/home/pi/armv6hf-sdk/rt/lib/ext/ -jar Devoxx.jar room8 signage.properties


# Start Digital Signage automatic on PI

You can create an /etc/rc.local file to start the signage app automatically.

```
#!/bin/sh -e
#
# /etc/rc.local
#
# This script is executed at the end of each multiuser runlevel.
# Make sure that the script will "exit 0" on success or any other
# value on error.
#
# In order to enable or disable this script just change the execution
# bits.
#
# By default this script does nothing.

ROOM=room9

#
# Start the Devoxx display screen app with the right room
#
echo Starting Devoxx room display for $ROOM
cd /home/devoxx
/opt/jdk8/bin/java -Xmx256m -Dfile.encoding=UTF-8 -Djava.util.logging.SimpleFormatter.format='%4$s: %5$s%6$s%n' -jar Devoxx.jar $ROOM /home/devoxx/signage.properties > /tmp/signage.log 2>&1

exit 0
```

# UI Controls

We've added some keyboard controls when the digital signage application is running.

+ Q = Quit application, gives you login prompt
+ U = Update display
+ D = Update schedule data
+ R = Refresh speaker cache
+ -> = Increment test time
+ <- = Decrement test time