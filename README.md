Introduction
------------

Crawl Anywhere is a mainly a web crawler. However, Crawl-Anywhere includes all components in order to build a vertical search engine. 

Crawl Anywhere includes :   

* a Web Crawler with a Web administration interface (http://www.crawl-anywhere.com/crawl-anywhere/)
* a document processing pipeline (http://www.crawl-anywhere.com/simple-pipeline/)
* a Solr indexer
* a full featured and customizable Web search application (some search engines using Crawl-anywhere : http://www.hurisearch.org/ or http://www.searchamnesty.org/)

Project home page : http://www.crawl-anywhere.com/

A web crawler is a program that will try to discover and read all HTML pages or documents (HTML, PDF, Office, ...) on a web site in order for example to index these data and build a search engine (like google). Wikipedia provides a great description of what is a Web crawler : http://en.wikipedia.org/wiki/Web_crawler.


Build distribution
------------------

Maven 2.0 or > and Oracle Java 6 or > are requiered.

* Clone the this Github project or download the ZIP file (https://github.com/bejean/crawl-anywhere/archive/master.zip)
* Open a console in the root directory of the project
* Edit the build.sh file in order to define target directory

       export DISTRIB=/tmp/CA
       
* ./build.sh > build.log


Installation
------------

* Either build (see above) or download a pre-built version (http://www.crawl-anywhere.com/get-crawl-anywhere/)
* Copy the build result or extract the downloaded archives into the installation directory (for instance "/opt/crawler")
* Follow instructions here : http://www.crawl-anywhere.com/installation-v400/


Getting Started
---------------

See the User Manual at http://www.crawl-anywhere.com/getting-started/

