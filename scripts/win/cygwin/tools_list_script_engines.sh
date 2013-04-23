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
export LOG_DIR=`cygpath --path --windows "$HOME"`/log
mkdir $LOG_DIR > /dev/null 2>&1
# Run directory
export RUN_DIR=`cygpath --path --windows "$HOME"`/run
export CONF_DIR=`cygpath --path --windows "$HOME"`/config

java fr.eolya.utils.ListScriptEngines