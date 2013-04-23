#!/bin/sh

# Get the path of crawl anywhere root directory
export HOME="$( cd "$( dirname "$0" )/.." && pwd )"

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

export JVMARGS="-Duser.timezone=Europe/Paris -Xmx512m"
java $JVMARGS fr.eolya.extraction.ScriptsWrapper $1 $2 $3 $4 $5 $6 $7 $8 $9

