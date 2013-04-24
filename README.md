crawl-anywhere
--------------

Crawl Anywhere allows you to build vertical search engines. Crawl Anywhere includes :   

* a Web Crawler with a powerful Web user interface
* a document processing pipeline
* a Solr indexer
* a full featured and customizable search application

Project home page : http://www.crawl-anywhere.com/


Build distribution
------------------

Maven 2.0 or > and Oracle Java 6 or > are requiered.

* clone the this Github project or download the ZIP file
* open a console in the root directory of the project
* edit the build.sh file in order to define target directory

       export DISTRIB=/tmp/CA
       
* ./build.sh > build.log


Installation
------------

* Download the project as zip file : https://github.com/bejean/crawl-anywhere/archive/master.zip
* Extract zip file content into a directory (for instance "/tmp/crawler")
* Build the project (see bellow)
* Copy the build result into the final installation directory (for instance "/opt/crawler")
* Follow instructions here : http://www.crawl-anywhere.com/installation-v400/

