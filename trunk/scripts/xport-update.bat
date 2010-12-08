REM You need to adjust path to CumulusJC.jar!

set "JAVA_HOME=C:\Program Files\Java\jdk1.6.0_06"
set PATH=%PATH%;C:\Program Files\Java\jdk1.6.0_06\jre\bin"

set "CLASSPATH=lib/axis.jar;lib/commons-discovery-0.2.jar;lib/commons-logging-1.0.4.jar;lib/jaxen-core.jar;lib/jaxen-jdom.jar;lib/jaxrpc.jar;lib/jdom.jar;lib/log4j-1.2.13.jar;lib/saaj.jar;lib/saxpath.jar;bin/xport.jar;lib;c:/program files/canto/cumulus 7 java sdk/CumulusJC.jar;C:\Program Files\Java\jdk1.6.0_06\jre\lib\ext\jai_core.jar;C:\Program Files\Java\jdk1.6.0_06\jre\lib\ext\jai_codec.jar;C:\Program Files\Java\jdk1.6.0_06\jre\bin;C:\Program Files\Java\jdk1.6.0_06\jre\lib\ext\mlibwrapper_jai.jar;"

java -Duser.region=CH -Duser.language=de -Xmx1200m ch.ethz.epics.export.XPort export-ba/export.xml update


REM 4Google: Split sitemap and create sitemap index
php bin\sitemap-splitter.php
