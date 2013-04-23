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
cp -r $DEV/install/* $DISTRIB/install/.

#=============================================
# externals
#=============================================

#=============================================
# tools
#============================================

#=============================================
# scripts
#============================================

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
mkdir $DISTRIB/install/bin
mkdir $DISTRIB/install/crawler
mkdir $DISTRIB/install/tomcat


# utils
cd $DEV/java/utils
mvn clean
mvn install -Dmaven.test.skip=true
rm -rf $DEV/java/utils/target/dependency
mvn dependency:copy-dependencies
cp target/utils-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-utils-$VERSION.jar
cp target/utils-0.0.1-SNAPSHOT.jar $DEV/install/bin/crawler/eolya-utils-$VERSION.jar

# crawler
cd $DEV/java/crawler
mvn clean
mvn install -Dmaven.test.skip=true
rm -rf $DEV/java/crawler/target/dependency
mvn dependency:copy-dependencies
cp target/crawler-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-crawler-$VERSION.jar
cp target/crawler-0.0.1-SNAPSHOT.jar $DEV/install/bin/crawler/eolya-crawler-$VERSION.jar

# simplepipeline
cd $DEV/java/simplepipeline
mvn clean
mvn install -Dmaven.test.skip=true
rm -rf $DEV/java/simplepipeline/dependency
mvn dependency:copy-dependencies
cp target/pipeline-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-pipeline-$VERSION.jar
cp target/pipeline-0.0.1-SNAPSHOT.jar $DEV/install/bin/crawler/eolya-pipeline-$VERSION.jar
 
# indexer
cd $DEV/java/indexer
mvn clean
mvn install -Dmaven.test.skip=true
rm -rf $DEV/java/indexer/dependency
mvn dependency:copy-dependencies
cp target/indexer-0.0.1-SNAPSHOT.jar $DISTRIB/bin/eolya-indexer-$VERSION.jar
cp target/indexer-0.0.1-SNAPSHOT.jar $DEV/install/bin/crawler/eolya-indexer-$VERSION.jar

# solr3

# solr4

#=============================================
# crawler WS
#=============================================
mkdir -p $DISTRIB/install/crawler/tomcat
cd $DEV/java/crawlerws
mvn clean
mvn install -Dmaven.test.skip=true
cp target/crawlerws-0.0.1-SNAPSHOT.war $DISTRIB/install/crawler/tomcat/crawlerws-$VERSION.war
#cp $DEV/install/bin/tomcat/crawlerws-jndi*.xml $DISTRIB/install/crawler/tomcat/.
#cp config/crawlerws/crawlerws-default.xml $DISTRIB/install/crawler/tomcat/.
#cp target/crawlerws-0.0.1-SNAPSHOT.war $DEV/install/bin/tomcat/crawlerws-$VERSION.war

#=============================================
# lib
#=============================================
cp -r $DEV/java/crawler/target/dependency/* $DISTRIB/lib/.
cp -r $DEV/java/simplepipeline/target/dependency/* $DISTRIB/lib/.
cp -r $DEV/java/indexer/target/dependency/* $DISTRIB/lib/.
cp -r $DEV/java/utils/target/dependency/* $DISTRIB/lib/.


#=============================================
# web
#=============================================
mkdir $DISTRIB/web
cp -r $DISTRIB/crawler/web $DISTRIB/web/crawler
cp -r $DEV/web/search/src $DISTRIB/web/search
rm -rf $DISTRIB/web/search/config/*
cp $DEV/web/search/src/config/config-default.ini $DISTRIB/web/search/config/.
perl -pi -e 's/\r\n/\n/g' $DISTRIB/web/search/config/config-default.ini

#cp -r $DEV/web/search2/src $DISTRIB/web/search2
#rm -rf $DISTRIB/web/search2/config/*
#rm -rf $DISTRIB/web/search2/pub/themes/taligentia
#rm -rf $DISTRIB/web/search2/pub/themes/amnesty
#cp $DEV/web/search2/src/config/config-default.ini $DISTRIB/web/search2/config/.
#perl -pi -e 's/\r\n/\n/g' $DISTRIB/web/search2/config/config-default.ini

find $DISTRIB/web -name '.svn' -exec rm -rf {} \;

#=============================================
# bin / config
#=============================================
mkdir -p $DISTRIB/config/crawler
cp -r $DISTRIB/crawler/java/config/crawler $DISTRIB/config/.

mkdir -p $DISTRIB/config/pipeline
cp -r $DISTRIB/simplepipeline/java/config/pipeline $DISTRIB/config/.

#cp -r $DEV/java/utils/config/* $DISTRIB/config/.

mkdir -p $DISTRIB/config/indexer
cp -r $DISTRIB/indexer/java/config/indexer $DISTRIB/config/.


#=============================================
# tar
#=============================================
cd $DISTRIB
find $DISTRIB -name .DS_Store -type f -exec rm {} \;

#tar cfz crawl-anywhere-external.tar.gz external
tar cfz crawl-anywhere-dependencies-jar-$VERSION.tar.gz lib
tar cfz crawl-anywhere-$VERSION.tar.gz bin config tools scripts web install src *.txt

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