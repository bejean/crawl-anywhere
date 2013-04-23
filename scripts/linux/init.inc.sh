#!/bin/sh

# Initialize classpath
export CLASSPATH=$CLASSPATH:$HOME/config

for i in $HOME/bin/*.jar; 
do
  export CLASSPATH=$CLASSPATH:$i
done 

for i in $HOME/lib/*.jar; 
do
  export CLASSPATH=$CLASSPATH:$i
done 

# Setting log directory and create it if necessary
export LOG_DIR=$HOME/log
mkdir $LOG_DIR > /dev/null 2>&1

# Run directory
export RUN_DIR=$HOME/run
export CONF_DIR=$HOME/config
