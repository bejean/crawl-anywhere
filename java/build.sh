#!/bin/bash
export COPY_EXTENDED_ATTRIBUTES_DISABLE=true
export COPYFILE_DISABLE=true
export DISTRIB=$DEV/build
export RSYNCFLAGS="-au"

#=============================================
# some clean-up
#=============================================
find . -name .DS_Store -type f -exec rm {} \;

mvn clean
mvn package -Dmaven.test.skip=true
