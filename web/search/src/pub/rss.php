<?php
$rootpath = dirname(__FILE__);
require_once("../init.inc.php");
require_once("../lib/rss.class.inc.php");

$solr = new Solr();
if ($solr->connect($theme->getSolrHost(), $theme->getSolrPort(), $theme->getSolrBaseUrl(), $theme->getSolrCore())) {
	
	$crit = getRequestParam("q");	
	$tag = getRequestParam("t");
	$querylang = getRequestParam("ql");
	$fqitms = array();
	$word_variations = (getRequestParam("wv") == "1");	
	
	$filter_lang=getRequestParam("lang");
	$filter_country=getRequestParam("country");
	$filter_mimetype=getRequestParam("mime");
	$filter_source=getRequestParam("org");
	$filter_tag = Array();
	if ($tag!="")
	$filter_tag = explode(",", $tag);
	
	if ($filter_country!="" || $filter_lang!="" || $filter_mimetype!="" || $filter_source!="")
		$mode = "advanced";	
	else
		$mode = "simple";
		
	$queryField = getQueryField($search_multilingual, $search_language_code);
	
	$response = $solr->query($crit, $queryField, $querylang, '', 0, 0, 100, $fqitms, $word_variations, $filter_lang, $filter_country, $filter_mimetype, $filter_source, $filter_collection, $filter_tag, '', '', '', '', true, false);
	if ( $response->getHttpStatus() == 200 ) {
		//print_r( $response->getRawResponse() );
		
		$url = $config->get("application.url");
		$title = $config->get("application.title");
		
		$feed = new RSS();
		$feed->title       = $title;
		$feed->link        = $url;
		$feed->description = "Recent articles matching your criteria : " . $crit;

		if ( $response->response->numFound > 0 ) {
			foreach ( $response->response->docs as $doc ) 
			{
				$item = new RSSItem();
				$item->title = $doc->title_dis;
				$item->link  = $doc->id;
				$item->setPubDate($doc->createtime); 
				$item->description = "<![CDATA[ $doc->summary ]]>";
				$feed->addItem($item);
			}
		}
		echo $feed->serve();
	}	
}

function getQueryField($search_multilingual, $search_language_code) {
	if ($search_multilingual) {
		$field_sufix = 'ml';
	}
	else {
		$field_sufix = $search_language_code;
	}
	return "content_" . $field_sufix;
}
?>