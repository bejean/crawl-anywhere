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

if [ -f crawler_test.pid ];
then
	echo "crawler_test.pid already exists !"
else
	export JVMARGS="-Duser.timezone=Europe/Paris -Xmx512m"
	java $JVMARGS fr.eolya.crawler.crawler.Crawler -p $HOME/config/crawler.properties -t >> $LOG/crawler_test.output 2>&1  &
fi

