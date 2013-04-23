#!/bin/sh

export INDEXER=/opt/hurisearch/indexer
export CLASSPATH=$CLASSPATH:$INDEXER/java/config
export CLASSPATH=$CLASSPATH:$INDEXER/java/classes

for i in $INDEXER/java/lib/*.jar; 
do
  export CLASSPATH=$CLASSPATH:$i
done 
for i in $INDEXER/java/config/sample-texts-utf8/*.txt ; 
do 
	$(cat $i | java net.olivo.lc4j.LanguageCategorization -c > $INDEXER/java/config/models/`basename $i | sed s/.txt//g`.lm) ; 
done ;
