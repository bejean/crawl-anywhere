#!/bin/sh

cd ..
export HOME=$(pwd)

export CLASSPATH=`cygpath --path --windows "$CLASSPATH"`:$HOME/config:
for i in $HOME/bin/*.jar;
do
  export CLASSPATH=`cygpath --path --windows "$CLASSPATH"`:$i
done
for i in $HOME/lib/*.jar;
do
  export CLASSPATH=`cygpath --path --windows "$CLASSPATH"`:$i
done
# Setting log directory and create it if necessary
export LOG_=`cygpath --path --windows "$HOME"`/log
mkdir $LOG > /dev/null 2>&1

cd $LOG

export JVMARGS="-Duser.timezone=Europe/Paris -Xmx512m"
java $JVMARGS fr.eolya.extraction.ScriptsWrapper $1 $2 $3 $4 $5 $6 $7 $8 $9
