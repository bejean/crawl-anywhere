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

java fr.eolya.utils.ListScriptEngines