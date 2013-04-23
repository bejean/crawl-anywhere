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

if [ -f pipeline.pid ];
then
	echo "pipeline.pid already exists !"
else
	#export JVMARGS="-Duser.timezone=Europe/Paris -Xmx1024m -Dcom.sun.management.jmxremote"
	export JVMARGS="-Duser.timezone=Europe/Paris -Xmx1024m"
	java $JVMARGS fr.eolya.simplepipeline.SimplePipeline -p $HOME//config/simplepipeline.xml -o -v >> $LOG/pipeline.output 2>&1  &
fi

