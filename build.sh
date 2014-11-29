#!/bin/bash
export COPY_EXTENDED_ATTRIBUTES_DISABLE=true
export COPYFILE_DISABLE=true
export DEV=$(pwd)
export VERSION=4.0.0
export DISTRIB=$DEV/build
export RSYNCFLAGS="-au"

#=============================================
# some clean-up
#=============================================
find . -name .DS_Store -type f -exec rm {} \;

mkdir $DISTRIB
export DISTRIB=$DISTRIB/$VERSION
mkdir $DISTRIB
#=============================================
# readme
#=============================================
cp -r $DEV/*.txt $DISTRIB/.

#=============================================
# install
#=============================================
mkdir $DISTRIB/install
mkdir $DISTRIB/install/crawler
cp -r $DEV/install/* $DISTRIB/install/.

#=============================================
# externals
#=============================================
cp -r $DEV/external $DISTRIB/.

#=============================================
# scripts
#============================================
cp -r $DEV/scripts $DISTRIB/.
rm -rf $DISTRIB/scripts/mysolrserver
rm -rf $DISTRIB/scripts/linux/deprecated
rm -rf $DISTRIB/scripts/win/deprecated

#=============================================
# jar
#=============================================
mkdir $DISTRIB/bin
mkdir $DISTRIB/lib

# all
cd $DEV/java
mvn clean
mvn package -Dmaven.test.skip=true

# utils
cd $DEV/java/utils
#mvn clean
#mvn package -Dmaven.test.skip=true
rm -rf $DEV/java/utils/target/dependency
#mvn dependency:copy-dependencies
cp target/utils-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-utils-$VERSION.jar

# crawler
cd $DEV/java/crawler
#mvn clean
#mvn package -Dmaven.test.skip=true
rm -rf $DEV/java/crawler/target/dependency
#mvn dependency:copy-dependencies
cp target/crawler-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-crawler-$VERSION.jar

# simplepipeline
cd $DEV/java/simplepipeline
#mvn clean
#mvn package -Dmaven.test.skip=true
rm -rf $DEV/java/simplepipeline/dependency
#mvn dependency:copy-dependencies
cp target/pipeline-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-pipeline-$VERSION.jar
 
# indexer
cd $DEV/java/indexer
#mvn clean
#mvn package -Dmaven.test.skip=true
rm -rf $DEV/java/indexer/dependency
#mvn dependency:copy-dependencies
cp target/indexer-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-indexer-$VERSION.jar

#=============================================
# crawler WS
#=============================================
#mkdir -p $DISTRIB/install/crawler/tomcat
#cd $DEV/java/crawlerws
##mvn clean
##mvn package -Dmaven.test.skip=true
#cp target/crawlerws-0.0.1-SNAPSHOT.war $DISTRIB/install/crawler/tomcat/crawlerws-$VERSION.war
##cp $DEV/install/bin/tomcat/crawlerws-jndi*.xml $DISTRIB/install/crawler/tomcat/.
#cp config/crawlerws/crawlerws-default.xml $DISTRIB/install/crawler/tomcat/.
##cp target/crawlerws-0.0.1-SNAPSHOT.war $DEV/install/bin/tomcat/crawlerws-$VERSION.war

mkdir $DISTRIB/bin/ws
cd $DEV/java/crawlerws2
cp target/crawlerws2-0.0.1-SNAPSHOT.jar $DISTRIB/bin/ws/eolya-crawlerws-$VERSION.jar


#=============================================
# lib
#=============================================
cd $DEV/java
mvn install:install-file -DgroupId=fr.eolya -DartifactId=utils -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar -Dfile=utils/target/utils-0.0.1-SNAPSHOT.jar
mvn dependency:copy-dependencies

cp -r $DEV/java/crawler/target/dependency/* $DISTRIB/lib/.
cp -r $DEV/java/simplepipeline/target/dependency/* $DISTRIB/lib/.
cp -r $DEV/java/indexer/target/dependency/* $DISTRIB/lib/.
cp -r $DEV/java/utils/target/dependency/* $DISTRIB/lib/.
rm $DISTRIB/lib/utils-0.0.1-SNAPSHOT.jar

#=============================================
# web
#=============================================
mkdir $DISTRIB/web

cp -r $DEV/web/crawler/src $DISTRIB/web/crawler
rm -rf $DISTRIB/web/crawler/config/*
cp $DEV/web/crawler/src/config/config-default.ini $DISTRIB/web/crawler/config/config.ini

sed -e "s/#version#/$VERSION/" $DISTRIB/web/crawler/pub/ressources/mongodb/init_db-infos.json > $DISTRIB/web/crawler/pub/ressources/mongodb/init_db-infos.json.2
rm $DISTRIB/web/crawler/pub/ressources/mongodb/init_db-infos.json
mv $DISTRIB/web/crawler/pub/ressources/mongodb/init_db-infos.json.2 $DISTRIB/web/crawler/pub/ressources/mongodb/init_db-infos.json

cp -r $DEV/web/search/src $DISTRIB/web/search
rm -rf $DISTRIB/web/search/config/*
cp $DEV/web/search/src/config/config-default.ini $DISTRIB/web/search/config/config.ini
#perl -pi -e 's/\r\n/\n/g' $DISTRIB/web/search/config/config-default.ini

find $DISTRIB/web -name '.svn' -exec rm -rf {} \;

#=============================================
# bin / config
#=============================================
mkdir -p $DISTRIB/config/crawler
cp $DEV/java/crawler/config/crawler/crawler-default.xml $DISTRIB/config/crawler/crawler.xml
cp -r $DEV/java/crawler/config/crawler/scripts $DISTRIB/config/crawler/.

mkdir -p $DISTRIB/config/pipeline
cp $DEV/java/simplepipeline/config/pipeline/simplepipeline-default.xml $DISTRIB/config/pipeline/simplepipeline.xml
cp $DEV/java/simplepipeline/config/pipeline/solrmapping.xml $DISTRIB/config/pipeline/.
cp $DEV/java/simplepipeline/config/pipeline/solrboost.xml $DISTRIB/config/pipeline/.
cp $DEV/java/simplepipeline/config/pipeline/contenttypemapping.txt $DISTRIB/config/pipeline/.
cp $DEV/java/simplepipeline/config/pipeline/countrymapping.txt $DISTRIB/config/pipeline/.
cp -r $DEV/java/simplepipeline/config/pipeline/scripts $DISTRIB/config/pipeline/.

mkdir -p $DISTRIB/config/indexer
cp $DEV/java/indexer/config/indexer/indexer-default.xml $DISTRIB/config/indexer/indexer.xml

cp -r $DEV/java/utils/config/* $DISTRIB/config/.

mkdir -p $DISTRIB/config/crawlerws
cp -r $DEV/java/crawlerws2/settings-default.yml $DISTRIB/config/crawlerws/settings.yml

#=============================================
# tar
#=============================================
cd $DISTRIB
find $DISTRIB -name .DS_Store -type f -exec rm {} \;

tar cfz crawl-anywhere-external.tar.gz external
tar cfz crawl-anywhere-dependencies-jar-$VERSION.tar.gz lib
tar cfz crawl-anywhere-$VERSION.tar.gz bin config scripts web install *.txt

#=============================================
# clean up
#=============================================
#rm -rf $DISTRIB/lib
#rm -rf $DISTRIB/external
#rm -rf $DISTRIB/utils
#rm -rf $DISTRIB/crawler
#rm -rf $DISTRIB/simplepipeline
#rm -rf $DISTRIB/indexer
#rm -rf $DISTRIB/solr