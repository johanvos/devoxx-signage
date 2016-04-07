# Devoxx Digital Signage
Digital signage used at the Devoxx conference with Raspberry PIs. 


# How to install on a PI?

1. In case you use a standard Raspian Jessie distribution, you don't need to install Java yourself, as Jessie ships with Java. Otherwise, install JDK 8 using 'sudo apt-get install oracle-java8-jdk'

2. Install OpenJFX. Download JavaFX Embedded SDK from http://gluonhq.com/open-source/javafxports/downloads/ and install it in the same directory as the Java Runtime (jfxrt.jar should be in jre/lib/ext). Optionally, see http://docs.gluonhq.com/javafxports/#_embedded_3

3. Build the application on a desktop system, using ant 

4. Transfer the jar file (dist/Devoxx.jar to the pi) 

3. Run app on the pi, using 
java -Djava.ext.dirs=/opt/armv6hf-sdk/rt/lib/ext -jar Devoxx.jar
