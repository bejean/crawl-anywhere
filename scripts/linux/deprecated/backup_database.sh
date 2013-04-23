#!/bin/sh

DATE=$(date +%Y-%m-%d-%H-%M)
mkdir /home/bejean/backups
mysqldump -ucrawler -pcrawler crawler > /home/bejean/backups/${DATE}-crawler.sql
gzip /home/bejean/backups/${DATE}-crawler.sql
lftp -c "open ftpback14.ovh.net; user ns306226.ovh.net xtfGhIIE7; mput /home/bejean/backups/${DATE}-crawler.sql.gz ; bye"






