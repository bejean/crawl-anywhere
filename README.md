<center><strong>
Starting versin 4.0 Crawl-Anywhere become an open-source project. Current version is 4.0.0-alpha

Stable version 3.x are still available at http://www.crawl-anywhere.com/
</strong></center>


Introduction
------------

Crawl Anywhere is mainly a web crawler. However, Crawl-Anywhere includes all components in order to build a vertical search engine. 

Crawl Anywhere includes :   

* a Web Crawler with a Web administration interface (http://www.crawl-anywhere.com/crawl-anywhere/)
* a document processing pipeline (http://www.crawl-anywhere.com/simple-pipeline/)
* a Solr indexer
* a full featured and customizable Web search application (some search engines using Crawl-anywhere : http://www.hurisearch.org/ or http://www.searchamnesty.org/)

Project home page : http://www.crawl-anywhere.com/

A web crawler is a program that discovers and read all HTML pages or documents (HTML, PDF, Office, ...) on a web site in order for example to index these data and build a search engine (like google). Wikipedia provides a great description of what is a Web crawler : http://en.wikipedia.org/wiki/Web_crawler.


Build distribution
------------------

Pre-requisites : 

* Maven 2.0 or > 
* Oracle Java 6 or >

Steps :

* Clone the this Github project or download the ZIP file (https://github.com/bejean/crawl-anywhere/archive/master.zip)
* Open a console in the root directory of the project
* Edit the build.sh file in order to define target directory

       export DISTRIB=/tmp/CA
       
* ./build.sh > build.log


Installation
------------

Pre-requisites : 

* Oracle Java 6 or >
* Tomcat 5.5 or >
* Apache 2.0 or >
* PHP 5.2.x or 5.3.x or 5.4.x
* MongoDB 64 bits 2.2 or >
* Solr 3.x or >


Steps :

* Either build (see above) or download a pre-built version (http://www.crawl-anywhere.com/get-crawl-anywhere/)
* Copy the build result or extract the downloaded archives into the installation directory (for instance "/opt/crawler")
* Follow instructions here : http://www.crawl-anywhere.com/installation-v400/


Getting Started
---------------

See the User Manual at http://www.crawl-anywhere.com/getting-started/

