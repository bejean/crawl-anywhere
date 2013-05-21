#!/bin/bash
export COPY_EXTENDED_ATTRIBUTES_DISABLE=true
export COPYFILE_DISABLE=true
export DEV=$(pwd)
export VERSION=4.0.0
export DISTRIB=/tmp/CA
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

#=============================================
# crawler
#=============================================

#=============================================
# simplepipeline
#=============================================

#=============================================
# indexer
#=============================================

#=============================================
# jar
#=============================================
mkdir $DISTRIB/bin
mkdir $DISTRIB/lib

# utils
cd $DEV/java/utils
mvn clean
mvn package -Dmaven.test.skip=true
rm -rf $DEV/java/utils/target/dependency
mvn dependency:copy-dependencies
cp target/utils-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-utils-$VERSION.jar

# crawler
cd $DEV/java/crawler
mvn clean
mvn package -Dmaven.test.skip=true
rm -rf $DEV/java/crawler/target/dependency
mvn dependency:copy-dependencies
cp target/crawler-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-crawler-$VERSION.jar

# simplepipeline
cd $DEV/java/simplepipeline
mvn clean
mvn package -Dmaven.test.skip=true
rm -rf $DEV/java/simplepipeline/dependency
mvn dependency:copy-dependencies
cp target/pipeline-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-pipeline-$VERSION.jar
 
# indexer
cd $DEV/java/indexer
mvn clean
mvn package -Dmaven.test.skip=true
rm -rf $DEV/java/indexer/dependency
mvn dependency:copy-dependencies
cp target/indexer-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-indexer-$VERSION.jar

## solr
cd $DEV/java/solr
mvn clean
mvn package -Dmaven.test.skip=true
rm -rf $DEV/java/solr/target/dependency
mvn dependency:copy-dependencies
rm target/dependency/*lucene*
rm target/dependency/*solr*

mkdir $DISTRIB/install/solr/solr430/lib
cp target/solr-0.0.1-SNAPSHOT.jar $DISTRIB/install/solr/solr430/lib/eolya-solr4.jar
cp target/dependency/*.jar $DISTRIB/install/solr/solr430/lib/.


## solr3
#cd $DEV/java/solr
#mvn clean
#mvn package -Dmaven.test.skip=true
#rm -rf $DEV/java/solr/target/dependency
#mvn dependency:copy-dependencies
#rm target/dependency/*lucene*
#rm target/dependency/*solr*
#
##rm $DEV/install/bin/solr3/*
#
#mkdir $DISTRIB/install/solr/solr310/crawler/lib
#cp target/solr-0.0.1-SNAPSHOT.jar $DISTRIB/install/solr/solr310/crawler/lib/eolya-solr3.jar
#cp target/dependency/*.jar $DISTRIB/install/solr/solr310/crawler/lib/.
#
#mkdir $DISTRIB/install/solr/solr350/crawler/lib
#cp target/solr-0.0.1-SNAPSHOT.jar $DISTRIB/install/solr/solr350/crawler/lib/eolya-solr3.jar
#cp target/dependency/*.jar $DISTRIB/install/solr/solr350/crawler/lib/.
#
## solr4
#cd $DEV/java/solr4
#mvn clean
#mvn package -Dmaven.test.skip=true
#rm -rf $DEV/java/solr4/target/dependency
#mvn dependency:copy-dependencies
#rm target/dependency/*lucene*
#rm target/dependency/*solr*
#
##rm $DEV/install/bin/solr4/*
#
#mkdir $DISTRIB/install/solr/solr400/crawler/lib
#cp target/solr4-0.0.1-SNAPSHOT.jar $DISTRIB/install/solr/solr400/crawler/lib/eolya-solr4.jar
#cp target/dependency/*.jar $DISTRIB/install/solr/solr400/crawler/lib/.


#=============================================
# crawler WS
#=============================================
mkdir -p $DISTRIB/install/crawler/tomcat
cd $DEV/java/crawlerws
mvn clean
mvn package -Dmaven.test.skip=true
cp target/crawlerws-0.0.1-SNAPSHOT.war $DISTRIB/install/crawler/tomcat/crawlerws-$VERSION.war
#cp $DEV/install/bin/tomcat/crawlerws-jndi*.xml $DISTRIB/install/crawler/tomcat/.
cp config/crawlerws/crawlerws-default.xml $DISTRIB/install/crawler/tomcat/.
#cp target/crawlerws-0.0.1-SNAPSHOT.war $DEV/install/bin/tomcat/crawlerws-$VERSION.war

#=============================================
# lib
#=============================================
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
cp $DEV/web/crawler/src/config/config-default.ini $DISTRIB/web/crawler/config/.

cp -r $DEV/web/search/src $DISTRIB/web/search
rm -rf $DISTRIB/web/search/config/*
cp $DEV/web/search/src/config/config-default.ini $DISTRIB/web/search/config/.
#perl -pi -e 's/\r\n/\n/g' $DISTRIB/web/search/config/config-default.ini

find $DISTRIB/web -name '.svn' -exec rm -rf {} \;

#=============================================
# bin / config
#=============================================
mkdir -p $DISTRIB/config/crawler
cp $DEV/java/crawler/config/crawler/crawler-default.xml $DISTRIB/config/crawler/.
cp -r $DEV/java/crawler/config/crawler/scripts $DISTRIB/config/crawler/.

mkdir -p $DISTRIB/config/pipeline
cp $DEV/java/simplepipeline/config/pipeline/simplepipeline-default.xml $DISTRIB/config/pipeline/.
cp $DEV/java/simplepipeline/config/pipeline/solrmapping.xml $DISTRIB/config/pipeline/.
cp $DEV/java/simplepipeline/config/pipeline/solrboost.xml $DISTRIB/config/pipeline/.
cp $DEV/java/simplepipeline/config/pipeline/contenttypemapping.txt $DISTRIB/config/pipeline/.
cp $DEV/java/simplepipeline/config/pipeline/countrymapping.txt $DISTRIB/config/pipeline/.
cp -r $DEV/java/simplepipeline/config/pipeline/scripts $DISTRIB/config/pipeline/.

mkdir -p $DISTRIB/config/indexer
cp $DEV/java/indexer/config/indexer/indexer-default.xml $DISTRIB/config/indexer/.

cp -r $DEV/java/utils/config/* $DISTRIB/config/.

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