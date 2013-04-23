#!/bin/sh

cd /opt/crawler/log
FILE=solr.log
EMAILS="your@email.here"
SUBJECT="OK: crawler"
STATUS=OK

#CNT=`wget --http-user=user --http-password=password  -T 10 -q "http://your-host.com/solr/select?q=&rows=1&wt=json" -O - | tr ',' '\n' |grep numFound|tr ':' ' '|awk '{print $3}'`
CNT=`wget -T 10 -q "http://your-host.com/solr/select?q=&rows=1&wt=json" -O - | tr ',' '\n' |grep numFound|tr ':' ' '|awk '{print $3}'`
if [ "x$CNT" == x ] || [ "$CNT" -lt 500000 ]; then
  SUBJECT="CRITICAL: check http://your-host.com/solr"
  STATUS=CRITICAL
fi

PREV_STAT=`cat .status`

if [ "$STATUS" == "CRITICAL" ]; then
if [ "$PREV_STAT" == "OK" ]; then
cat $FILE | mail $EMAILS -a $FILE -s "$SUBJECT. doc count was only $CNT"
fi
echo CRITICAL > .status
else
if [ "$PREV_STAT" == "CRITICAL" ]; then
cat $FILE | mail $EMAILS -a $FILE -s "SOLVED: http://your-host.com/solr"
fi
echo OK > .status
fi

echo $STATUS > .status
