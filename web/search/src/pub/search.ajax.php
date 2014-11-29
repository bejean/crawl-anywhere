<?php
$rootpath = dirname(__FILE__);
require_once("../init.inc.php");
require_once("../lib/cloud_tag.inc.php");

/**
 * Nettoie et normalise la query utilisateur
 * @param String $q la query
 * @return string la query normalisée.
 */
function cleanUpQ($q) {
	$q = preg_replace('/(\s+)/', ' ', $q);
	$q = preg_replace('/"/', '', $q);
	$q = str_replace('*', '', $q);
	return $q;
}
/**
 * Suppression des accents dans une chaine
 */
function accentsReplace($string) {
	return str_replace( array('à','á','â','ã','ä', 'ç', 'è','é','ê','ë', 'ì','í','î','ï', 'ñ', 'ò','ó','ô','õ','ö', 'ù','ú','û','ü', 'ý','ÿ', 'À','Á','Â','Ã','Ä', 'Ç', 'È','É','Ê','Ë', 'Ì','Í','Î','Ï', 'Ñ', 'Ò','Ó','Ô','Õ','Ö', 'Ù','Ú','Û','Ü', 'Ý'), array('a','a','a','a','a', 'c', 'e','e','e','e', 'i','i','i','i', 'n', 'o','o','o','o','o', 'u','u','u','u', 'y','y', 'A','A','A','A','A', 'C', 'E','E','E','E', 'I','I','I','I', 'N', 'O','O','O','O','O', 'U','U','U','U', 'Y'), $string);
}

function getQueryField($search_language_code) {
	return "content_" . $search_language_code;
}



$action = getRequestParam("action");
$mode = getRequestParam("mode"); // mode = tb means "twitter bootstrap"

$res = "";

if ($action=="autocomplete" || $action=="fiedvalues" || $action=="gettext" || $action=="search" || $action=="gettagcloud") {

	$solr = new Solr();	
	if ($solr->connect($theme->getSolrHost(), $theme->getSolrPort(), $theme->getSolrBaseUrl(), $theme->getSolrCore())) 
	{
		if ($action=="autocomplete")
		{
			$q = $_GET['q'];
			$q = cleanUpQ($q);
			$q = accentsReplace($q);
			$q = strtolower($q);
							
			$res = '';
			
			$values=$solr->getTerms($q, 'content_ntoken', 0, 5);
			if (!empty($values)) {
				$pattern = "/(_|\(|\.|\/|\-|\[|'|,)/";
				$values =  array_values(preg_grep ($pattern , $values, PREG_GREP_INVERT));
		
				$q_terms_count = count(explode(' ', $q));
				$pattern1 = '[^\s]*';
				for ($i=0; $i<$q_terms_count-1; $i++) {
					$pattern1 .= '\s[^\s]*';
				}
				$pattern2 = $pattern1 . '\s[^\s]*';
		
				$pattern = '/^(' . $pattern1 . '|' . $pattern2 . ')$/';
				$values =  array_values(preg_grep ($pattern , $values));
		
				$arr = array();
				$arr["options"] = $values;
				$res = json_encode($arr);
			}
		}
		
		if ($action=="fiedvalues")
		{
			$field = getRequestParam("field");
			$values = $solr->getFiedvalues($field);

			$res='';
			if (!empty($values)) {
				foreach ($values[$field] as $key => $value) {
					if ($res!="") $res .= "|";
					$res .= $key . ":" . $value;
				}
			}
		}

		if ($action=="gettagcloud")
		{
			$field = getRequestParam("field");
			$data = $solr->getCloud($field, 20, '', '', 0, 3, true, false);

			$tags = array();
			$aData = explode ("|", $data);

			//$count=0;
			foreach ($aData as $value) {

				//if ($count < $maxsize)
				//{
				//if ($value != "")
				//{
				$aItem = explode (":", $value);
				//$tempTag[tag_name] = utf8_decode($aItem[0]);
				$tempTag["tag_name"] = $aItem[0];
				$tempTag["tag_count"] = $aItem[1];
				$tags[] = $tempTag;
				//}
				//}
				//$count++;
			}

			// instantiate a cloud
			$cloud = new cloud_tag();

			// the class already has defaults, but let's override them for fun

			$cloud->set_label('tag'); // css classes will be cloud1, cloud2, etc.
			$cloud->set_tagsizes(7); // 7 font sizes will be used

			$newtags = $cloud->make_cloud($tags); // make a tag cloud

			$res = "";
			foreach ($newtags as $atag) {
				$escapedName = str_replace ("'", "&#39;", $atag["tag_name"]);
				$res .= "<a href='javascript:void(0);' class='" . $atag["tag_class"] . "' title='" . $escapedName . "' onClick='setSearchCrit(\"" . $escapedName . "\");'>" . str_replace(" ", "&nbsp;", $atag["tag_name"]) . "</a>&nbsp;&nbsp;&nbsp; ";
			}
		}

		if ($action=="gettext")
		{
			$id = getRequestParam("id");
			$crit = getRequestParam("search_crit");
			//$word_variations = (getRequestParam("search_word_variations") == "1");
			$word_variations = true;
			$query_lang = getRequestParam("search_querylanguage");

			$queryField = getQueryField($search_language_code);
			$debug=false;
			$solr->setDebug($debug);

			$response = $solr->getText($id,$crit,$queryField,$query_lang,$word_variations,$debug);
			if ( $response->getHttpStatus() == 200 ) {

				$numFound = $response->response->numFound;
				$teasers = get_object_vars($response->highlighting);
				if ( $numFound > 0 ) {
					foreach ( $response->response->docs as $doc ) {
						$res .= buildOneDocDisplay($doc, $teasers, $queryField);
					}
					if ($debug) $res .= $response->getRawResponse();
				}
				else {
					if ($debug) $res .= $response->getRawResponse();
				}
			}
			else
			{
				$res .= $response->getHttpStatusMessage();
			}
		}

		if ($action=="search")
		{
			$crit = getRequestParam("search_crit");			
			$sort = getRequestParam("search_sort");
			
			if (!empty($search_default) && empty($crit)) {
				$crit = $search_default;
				$sort = $search_default_sort;
			}

			//$crit = utf8_encode($crit);
			$querylang = getRequestParam("search_querylanguage");
			$word_variations = (getRequestParam("search_word_variations") == "1");
			$debug = (getRequestParam("search_debug") == "1");
			$page = getRequestParam("page");
			if ($page=='' || $page=='0')
			{
				$page = '1';
				$initialSearch = true;
			}
			else
			{
				$initialSearch = false;
			}

			$fq = getRequestParam("fq");
			$filter_lang=getRequestParam("search_language");
			$filter_country=getRequestParam("search_country");
			$filter_mimetype=getRequestParam("search_mimetype");
			$filter_source=getRequestParam("search_org");
			$mode=getRequestParam("mode");

			$filter_tag = getRequestParam("search_tag");
			if (empty($filter_tag)) {
				$bookmark_tag = getRequestParam("bookmark_tag");
				if ($bookmark_tag!="") {
					$filter_tag = explode(",", $bookmark_tag);
				}
			}

			$filter_collection = getRequestParam("search_collection");
			if (empty($filter_collection)) {
				$bookmark_collection = getRequestParam("bookmark_collection");
				if ($bookmark_collection!="") {
					$filter_collection = explode(",", $bookmark_collection);
				}
			}

			$item_per_page = getRequestParam("search_itemperpage");

			$filter_location_lat="";
			$filter_location_lng="";
			$filter_location_radius="";

			if ($uselocation) {
				$filter_location_lat=getRequestParam("search_location_lat");
				$filter_location_lng=getRequestParam("search_location_lng");
				$filter_location_radius=getRequestParam("search_location_radius");
			}

			//if ($page=="") $page="1";
			//if ($item_per_page=="") $item_per_page = "20";

			$search_bookmark = "?q=".$crit."&ql=".$querylang;
			if (!empty($filter_collection))
			$search_bookmark .= "&c=".implode ( $filter_collection , ",");
			if (!empty($filter_tag))
			$search_bookmark .= "&t=".implode ( $filter_tag , ",");
			if ($word_variations)
			$search_bookmark .= "&wv=1";
			if ($filter_lang!="")
			$search_bookmark .= "&lan=".$filter_lang;
			if ($filter_country!="")
			$search_bookmark .= "&country=".$filter_country;
			if ($filter_source!="")
			$search_bookmark .= "&org=".$filter_source;
			if ($filter_mimetype!="")
			$search_bookmark .= "&mime=".$filter_mimetype;

			if (!$fq) $fq = '';
			$fq = urldecode($fq);

			$item_per_page_option = $item_per_page;
			$groupsize = $config->get("results.groupsize", "0");
			$groupdisplaysize = $config->get("results.groupdisplaysize", "3");

			$aFacetFields = array();
			$fqstr = '';
			$fqitms = explode('||', stripslashes($fq));
			sort($fqitms);
			$fqsearch = array();
			$fqitem_field_prev = '';
			$fqcount = 0;
			foreach ($fqitms as $fqitem) {
				if ($fqitem) {
					if ($fqstr!="") $fqstr .= urlencode('||');

					if ($facet_union) {
						$fqitem_parts = explode(':', $fqitem);
							if ($fqitem_field_prev != $fqitem_parts[0]) {
								if (!empty($fqitem_field_prev)) {
									$fqsearch[$fqcount] = '(' . $fqsearch[$fqcount] . ')';
									$fqcount++;
								}
							} 
							if (!empty($fqsearch[$fqcount])) $fqsearch[$fqcount] .= " OR ";
								$fqsearch[$fqcount] .= $fqitem;
						$fqitem_field_prev = $fqitem_parts[0];
					}
					$splititm = explode(':', $fqitem, 2);
					$fqstr .= $splititm[0] . ':' . urlencode($splititm[1]);
					array_push($aFacetFields, $splititm[0]);
					if ($splititm[0]=="source_str") $groupsize = 0;
				}
			}
			if (count($fqsearch) > 0) {
				$fqsearch[$fqcount] = '(' . $fqsearch[$fqcount] . ')';
			}

			if (!$facet_union) $fqsearch = $fqitms;

			$queryField = getQueryField($search_language_code);

			if ($groupsize>0)
			$item_per_page = intval($item_per_page / $groupdisplaysize);

			$debug=false;
			$solr->setDebug($debug);
			$response = $solr->query($crit, $queryField, $querylang, $sort, $groupsize, ($page-1)*$item_per_page, $item_per_page, $fqsearch, $word_variations, $filter_lang, $filter_country, $filter_mimetype, $filter_source, $filter_collection, $filter_tag, $filter_location_lat, $filter_location_lng, $filter_location_radius, $theme->getSolrFields(), $theme->getParamExtra(), $mode, false, $debug);
			if ( $response->getHttpStatus() == 200 ) {
				$res .= $theme->generateResults();
			}
			else
			{
				$res .= $response->getHttpStatusMessage();
			}
		}
	}
	else
	{
		$res = "not pinging";
		//$res .= " (http://" . $solr_host . ":" . $solr_port . $solr_baseurl . $solr_corename . ")";
	}

} else {

	if ($action=="preferences_display")
	{
		$config_facet_union_check = "";
		if ($facet_union) $config_facet_union_check = 'checked="checked"';

		$res = <<<EOD
				<form name="pref_form" id="pref_form" action="">
					Facet UNION <input type="checkbox" name="config_facet_union" id="config_facet_union" value="1" $config_facet_union_check>
				</form>
EOD;
			
	}

	if ($action=="preferences_save")
	{
		$value = getRequestParam("facet_union");
		$config->setCookie( "facet.mode_union", $value );
	}

}

//$res = $_SERVER["QUERY_STRING"] . "<br/>" . $res;
print ($res);




?>
