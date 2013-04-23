#!/bin/sh

cd ..
export HOME=$(pwd)
export CLASSPATH=$CLASSPATH:$HOME/config

for i in $HOME/bin/*.jar; 
do
  export CLASSPATH=$CLASSPATH:$i
done 

for i in $HOME/lib/*.jar; 
do
  export CLASSPATH=$CLASSPATH:$i
done 

export LOG=$HOME/log
mkdir $LOG > /dev/null 2>&1
cd $LOG

if [ -f indexer.pid ];
then
	echo "indexer.pid already exists !"
else
	#export JVMARGS="-Duser.timezone=Europe/Paris -Xmx1024m -Dcom.sun.management.jmxremote"
	export JVMARGS="-Duser.timezone=Europe/Paris -Xmx512m"
	java $JVMARGS fr.eolya.indexer.Indexer -p $HOME/config/indexer.xml -o >> $LOG/indexer.output 2>&1  &
fi