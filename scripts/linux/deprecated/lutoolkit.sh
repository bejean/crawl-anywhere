#!/bin/sh

export HOME=/opt/hurisearch/indexer
export CLASSPATH=$CLASSPATH:$HOME/java/classes

for i in $HOME/java/lib/*.jar; 
do
  export CLASSPATH=$CLASSPATH:$i
done 

export JVMARGS="-Duser.timezone=Europe/Paris -Xmx512m -Dcom.sun.management.jmxremote"
java $JVMARGS fr.eolya.lucene.toolkit.LuToolkit $1 $2 $3 $4 $5 $6 $7 $8 $9 
