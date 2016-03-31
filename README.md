# Devoxx Digital Signage
Digital signage used at the Devoxx conference with Raspberry PIs. 


# How to install on a PI?

1. Install JDK 8 using 'sudo apt-get install oracle-java8-jdk'

2. Install OpenJFX. Download JavaFX Embedded SDK from http://gluonhq.com/open-source/javafxports/downloads/ and install it in the same directory as the Java Runtime (jfxrt.jar should be in jre/lib/ext). Optionally, see http://docs.gluonhq.com/javafxports/#_embedded_3

3. Run app 'ant -f /home/pi/devoxx-signage-master -Djavafx.main.class=devoxx.Devoxx jfxsa-run' 
