#!/bin/sh

export INDEXER=/opt/hurisearch/indexer
export CLASSPATH=$CLASSPATH:$INDEXER/java/config
export CLASSPATH=$CLASSPATH:$INDEXER/java/classes

for i in $INDEXER/java/lib/*.jar; 
do
  export CLASSPATH=$CLASSPATH:$i
done 

dos2unix $INDEXER/java/config/ngp-sample-texts-utf8/*.txt

for i in $INDEXER/java/config/ngp-sample-texts-utf8/*.txt ; 
do 
	java de.spieleck.app.cngram.RunNGram -create $i $i utf-8 
done ;
