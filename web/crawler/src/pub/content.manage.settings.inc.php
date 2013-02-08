<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================

$config_crawler = new ConfigTool();
$config_crawler->setConfigFromFile( $config->get("crawler.properties") );

$crawler_period = $config_crawler->get("crawler.period") / 24;
$crawler_period_binary = $config_crawler->get("crawler.period_binary_file") / 24;
if ($config_crawler->get("crawler.child_only")=="0")
	$crawler_child_only = "No";
else
	$crawler_child_only = "Yes";

$max_simultaneous_item_per_source = $config_crawler->get("crawler.max_simultaneous_item_per_source");
if ($max_simultaneous_item_per_source== "undefined")
	$max_simultaneous_item_per_source = $config_crawler->get("crawler.max_simultaneous_url_per_server");
	
?>
<div id="contenu">
    <h2>Crawler settings (read only)</h2>
    <div id="settings">
    <center>
        <table>
        <tr><th>Parameter</th><th>Value</th><th>Description</th></tr>
        <tr><td class='head_light'>Site recrawl period</td><td><?php print($crawler_period); ?></td><td>Minimum number of days between two crawls of a same site.</td></tr>
        <tr><td class='head_light'>Binary document recrawl period</td><td><?php print($crawler_period_binary); ?></td><td>Minimum number of days between two crawls of a same binary file (PDF, DOC, XLS, PPT, ...).</td></tr>
        <tr><td class='head_light'>Maximum number of concurrent crawled server</td><td><?php print($config_crawler->get("crawler.max_simultaneous_source")); ?></td><td>Maximum number of crawled servers at the same time. This value will depend of Internet connection capabilities and indexing capabilities of the server.</td></tr>
        <tr><td class='head_light'>Maximum number of concurrent pages crawled per server</td><td><?php print($max_simultaneous_item_per_source); ?></td><td>Maximum number of crawled pages on a server at the same time. This value will depend of Internet connection capabilities and indexing capabilities of the server.<br/>However, this value must not be to high in order not stress to much crawled servers.<br/>This value can be overwrited in the source parameters.</td></tr>
        <tr><td class='head_light'>Crawl max depth</td><td><?php print($config_crawler->get("crawler.max_depth")); ?></td><td>Default maximum depth a source will be crawled from the starting page (which has depth = 0).<br/>This value can be overwrited in the source parameters.</td></tr>
        <tr><td class='head_light'>Crawl child pages only</td><td><?php print($crawler_child_only); ?></td><td>Default crawling strategy for not child pages of the starting page. A child page have an url starting as the starting page<br/>(http://www.blup.com/news/article1.html is a child of the starting page http://www.blup.com/news/list.html).<br/>This value can be overwrited in the source parameters.</td></tr>
        </table>
    </center>
    </div>   
</div>