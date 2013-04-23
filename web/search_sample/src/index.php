<?php

// Settings
require_once("init.inc.php");

// Solr librairy 
require_once("lib/SolrPhpClient/Service.php");

// Solr helper
require_once("lib/solr_helper.inc.php");

// Tag cloud html generator (+ css/tagcloud.css bellow)
require_once("lib/cloud_tag.inc.php");

// Create Solr object and test connexion
$solr = solrConnect($solr_host, $solr_port, $solr_baseurl, $solr_corename);
if ($solr==null) {
	echo 'Solr unreachable at ' . solrConnect($solr_host, $solr_port, $solr_baseurl, $solr_corename);
	die;
}
$solr_major_version = getSolrMajorVersion($solr);;

// Get user query
$q = (isset($_GET['q']) ? $_GET['q'] : '');
$fq = (isset($_GET['fq']) ? $_GET['fq'] : '');
$fq = urldecode($fq);
$page = (isset($_GET['page']) ? $_GET['page'] : '1');
$l = (isset($_GET['l']) ? $_GET['l'] : '');
$query_lang = $l;
$ww = (isset($_GET['ww']) ? $_GET['ww'] : '');
$word_variations = ($ww=='1');
?>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />
<title>Sample search</title>
<meta http-equiv='Cache-Control' content='no-cache' />
<meta http-equiv='Pragma' content='no-cache' />
<meta http-equiv='Cache' content='no store' />
<meta http-equiv='Expires' content='0' />
<link rel="stylesheet" href="css/tagcloud.css" type="text/css" media="screen" />
<script type="text/javascript" src="js/jquery-1.7.1.min.js"></script>

<script type="text/javascript"> 
<!--
	$(function() {  
		$("#search_do").click(function() {  
			var crit = $("input#crit").val();  
			if (crit != "") doSearch(1, "", "", "", false, "", "");
			return false;
		});  
	});

	function setSearchCrit(crit) {
		var current_criteria = crit;
<?php if ($tag_cloud_query_add) { ?>
		current_criteria = $("input#q").val() + ' ' + crit;
<?php } ?>
		$("input#q").val(current_criteria);
		doSearch(1, "", "", "", false);
	}

	function doSearch(page, fq, fq_previous, id, ischeckbox) {		
		$("input#page").val(page);  	

		var checked = false;
		if (id!="") {
			var checked = $("input#"+id).attr('checked');
			if (ischeckbox) checked = !checked;
		}

		if (checked) {			
			// remove fq from fq_previous
			var temp = unescape(fq_previous);
			fq = unescape(fq);
			var aPrevious = temp.split('||');

			f = "";
			for (var i=0; i<aPrevious.length; i++) {
				if (aPrevious[i]!=fq) {
					if (f!="") f += escape("||");
					f += escape(aPrevious[i]);
				}
			}
		}
		else {
			// add fq to fq_previous
			var f = fq_previous;
			if (fq!="") {
				if (f!="") {
					f = fq + escape("||") + fq_previous;
				}
				else {
					f = fq;
				}
			}
		}
		
		$("input#fq").val(f);  		
		$("#search_form").submit();; 
	}
//-->
</script>


</head>
<body>
	<div id='search'>
		<form name="search_form" id="search_form" action="">
			<input type="text" id="q" name="q" value="<?php echo $q; ?>" autocomplete="off" />
			<input type="submit" id="search_do" name="search_do" value="search" />
			<br />

			<label for="l">The language of the query is</label>					
			<select name="l" id="l">
			<option value="">Not specified</option>								
<?php
		$aValues = getSolrFiedsValues($solr, 'language', 0);
		foreach ($aValues['language'] as $key => $count)
		{
			print( "<option value='" . $key . "'");
			if ($key==$l) print (" selected='selected'");
			print( ">" . $key . " (" . $count . ")</option>");
		}
?>
			</select>
			<br />					
			<label for="ww" class="form_text">Use word variations</label>							
			<input id="ww" type="checkbox" value="1" name="ww" <?php if ($ww=='1') echo ' checked="checked"';?> />
			
			<input type="hidden" name="page" id="page" value='<?php echo $page;?>' />
			<br />
			<input type="hidden" name="fq" id="fq" value='<?php echo $fq;?>' />
		</form>
	</div>
	<br /><br />
	
	<hr />
	<div id='cloud'>
	<strong>Not filtered tag cloud (<?php echo $tag_cloud_language?>/<?php echo $tag_cloud_country?>/<?php echo $tag_cloud_nbhours?>)</strong><br />
<?php
// Display tag cloud without filtering by query
$data = getCloud($solr, 'tag_cloud', null, null, $tag_cloud_size, $tag_cloud_language, $tag_cloud_country, $tag_cloud_nbhours, 3, true);
echo displayCloud($data);
?>
	<br /><br />
	</div>
		
<?php

$params = array();

// query
$full_query = $hidden_query . ' ' . $q;
if (trim($full_query)!='') {
	$query = getSolrQuery($full_query, $query_lang, $query_field, $word_variations, 'AND', $solr_major_version);
	// sort
	$params['sort'] = $sort_with_query;
}
else {
	$query = getSolrQuery('*:*', $query_lang, $query_field, $word_variations, 'AND', $solr_major_version);
	// sort
	$params['sort'] = $sort_without_query;
}


// hidden query
if (trim($hidden_query)!='') {
	$hidden_query_tagcloud = getSolrQuery($full_query, $query_lang, $query_field, $word_variations, 'AND', $solr_major_version);
}

// filtres (facets)
$fqstr = '';
$fqitms = explode('||', stripslashes($fq));
sort($fqitms);

// filtres selectionnes dans l'interface
$fqsearch = array();
$fqsearch_item = '';
$fqitem_field_prev = '';
foreach ($fqitms as $fqitem) {
	if ($fqitem) {
		
		$fqitem_parts = explode(':', $fqitem);
		
		if ($fqitem_field_prev != $fqitem_parts[0]) {
			if (!empty($fqsearch_item)) {
				array_push($fqsearch, $fqsearch_item);
			}
			$fqsearch_item = $fqitem;
		} else {
			$fqsearch_item .= ' ' . $facets_conf[$fqitem_parts[0]]['mode'] . ' ' . $fqitem;
		}
		$fqitem_field_prev = $fqitem_parts[0];

		// reconstruction de la string des filtres
		$splititm = explode(':', $fqitem, 2);
		if ($fqstr!="") $fqstr .= urlencode('||');
		$fqstr .= $splititm[0] . ':' . urlencode($splititm[1]);
	}
}
if (!empty($fqsearch_item)) {
	$fqsearch_item = $fqsearch_item;
	array_push($fqsearch, $fqsearch_item);
}

$fqsearch_tagcloud = array();

// filtres caches
if (count($hidden_filters)!=0) {
	foreach ($hidden_filters as $fq_field => $fq_options) {
		$fqsearch_item = '';
		foreach ($fq_options['values'] as $fq_value) {
			if ($fqsearch_item!='') $fqsearch_item .= ' ' . $fq_options['mode'] . ' ';
			$fqsearch_item .= $fq_field . ':' . $fq_value;
		}
		array_push($fqsearch, $fqsearch_item);
		array_push($fqsearch_tagcloud, $fqsearch_item);
	}
}	

// facets
$facets = Array();
foreach ($facets_conf as $facetfield => $facetoptions) {
	array_push($facets, $facetfield);
	$params['facet.' . $facetfield . '.limit'] = $facetoptions['limit'];
}

$params['facet'] = 'true';
$params['facet.field'] = $facets;
$params['facet.mincount'] = '1';

// fields
$params['fl'] = '*,score';

// highlighting
$params['hl'] = 'true';
$params['hl.fl'] = $query_field;
$params['hl.snippets'] = '5';
$params['hl.fragsize'] = '100';
$params['hl.simple.pre'] = '<b>';
$params['hl.simple.post'] = '</b>';

// spellchecker
// $params['spellcheck.build'] = 'true';
// $params['spellcheck'] = 'true';
// $params['spellcheck.q'] = $query;
// $params['spellcheck.count'] = '3';
// $params['spellcheck.onlyMorePopular'] = 'true';
// $params['spellcheck.extendedResults'] = 'true';
// $params['spellcheck.collate'] = 'false';

// filters
$params['fq'] = $fqsearch;

?>
	<hr />
<div id='cloud'>
	<strong>Filtered tag cloud by hidden query and filter (<?php echo $tag_cloud_language?>/<?php echo $tag_cloud_country?>/<?php echo $tag_cloud_nbhours?>)</strong><br />
<?php
// Display tag cloud filtered by hidden query and filter
$data = getCloud($solr, 'tag_cloud', $hidden_query_tagcloud, $fqsearch_tagcloud, $tag_cloud_size, $tag_cloud_language, $tag_cloud_country, $tag_cloud_nbhours, 3, true);
echo displayCloud($data);
?>
	<br /><br />
	</div>
<?php 

if ($debug) {
	$params['debugQuery'] = 'true';
	$params['debug'] = $debug_solr;
}
else {
	$params['debugQuery'] = 'false';
}

/*
 * Perform search 
**/
$response_elevate=$solr->elevate(preg_replace('/\s+/', ' ',$q), $params); // use the original user query not updated query

/*
if ($response_elevate->getHttpStatus()==200) {
	$hits_elevate = $response_elevate->response->numFound;
}
*/
	
$response=$solr->search($query, ($page-1)*$item_per_page, $item_per_page, $params);
if ($response->getHttpStatus()==200) {

	$hits = $response->response->numFound;
	
	echo '<hr>';
	echo 'Hits: ' . $hits;
	echo '<hr>';

	if ($hits>0) {
		
		// Display pagination
		echo displayPagination ($hits, $item_per_page, $page, $fqstr ) . '<br /><br />' ;
		
		$res='';
		
		// Display elevated documents
		$elevated_ids = Array();
		if ($page==1) {
			$teasers = get_object_vars($response_elevate->highlighting);
			foreach ( $response_elevate->response->docs as $doc ) {
				$elevated_ids[]=$doc->id;
				$res .= displayResultItem($doc, $teasers);
			}
		}
		
		// Display results list
		$teasers = get_object_vars($response->highlighting);
		foreach ( $response->response->docs as $doc ) {	
			if (count($elevated_ids)>0 && in_array($doc->id, $elevated_ids)) continue; // do not display twice an elevated item in result list
			$res .= displayResultItem($doc, $teasers);
		}
		echo $res;

		// Display tag cloud for the search result set
?>
		<hr>
		<strong>Tag cloud for the current result set</strong><br />
<?php 
		$data = filterCloud($response, 'tag_cloud', $tag_cloud_size, 3, true, true);
		echo displayCloud($data);
		
		// Display facets
		echo '<hr>';
		$facet_counts = $response->facet_counts;
		if ($facet_counts) {
			$res='';
			$count=0;
			foreach ($facet_counts->facet_fields as $facetfield => $facet) {
				if($facetfield=='tag_cloud') continue; // not this one
				if ( ! get_object_vars($facet) ) continue;
				$res .= $facetfield . '<br>';
				foreach ($facet as $facetval => $facetcnt) {
					if (in_array(strtolower($facetval), $facets_conf[$facetfield]['values']) || count($facets_conf[$facetfield]['values'])==0) {
						$chk_id = $facetfield . $count++;
						$crit_fq = $facetfield . ":" . urlencode('"'.$facetval.'"');
						$checked = "";
						if (in_array($facetfield.":\"".$facetval."\"",$fqitms)) $checked = " checked ";
						$res .= '<input type="checkbox" id="' . $chk_id . '" ' . $checked . ' onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', true);">&nbsp;';
						$res .= '<a href="javascript:void(0)" onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', false);">' . $facetval . '</a> (' . $facetcnt . ') <br>';
					}
				}
				$res .= '<br>';
			}
			echo $res;
		}		
	}	
	
	if ($debug) {
		echo '<hr>';
		echo 'Ping : ' . $solr->ping();
		echo '<hr>';
		$res = $response->getRawResponse();
		echo '<div id="raw_response">' . $res . '</div>';
	}
}

?>

</body>
</html>



<?php

/*
 * Result item rendering
*/
function displayResultItem($doc, $teasers) {
	$res = "";

	$summary = '';
	if (isset($teasers)) {
		$docid = strval($doc->id);
		$docteaser = get_object_vars($teasers[$docid]);
		if (isset($docteaser[$query_field]))
		{
			$summary = "";
			foreach($docteaser[$query_field] as $value)
			{
				if ($summary!="") $summary .= "...";
				$value = preg_replace("/^\\[[a-zA-Z]{2}\\]/", "", $value);
				$summary .= $value;
			}
		}
	}
		
	if ($summary=='') $summary = $doc->summary;
	$res .= '<a href="' . $doc->id . '">' . $doc->title_dis . '</a><br>';
	$res .= $summary . '<br>';
	$res .= '    date: ' . $doc->createtime . '<br>';
	$res .= '<br>';
	
	return $res;
}


/*
 * Pagination
 */
function displayPagination ($totalhits, $pagesize, $page, $fqstr ) {

	$itemarround = 3;

	if ($totalhits<=$pagesize)
	return "";

	$res="<span>Pages</span>&nbsp;";

	$firstpage = 1;
	$lastpage = intval(abs((($totalhits-1) / $pagesize) + 1));

	$ndxstart = (($page-1) * $pagesize) + 1;
	$ndxstop = $page * $pagesize;
	if ($ndxstop > $totalhits)
	$ndxstop = $totalhits;

	if ($lastpage > 1) {
		if ($page != 1)
		$res .= '<a href="#" onclick="doSearch(' . ($page-1) . ', \'\', \'' . $fqstr . '\', \'\', false);"><b><<</b></a>';

		for ($i=$firstpage; $i<=$lastpage; $i++) {
			if ($i!=$firstpage)
			$res .= ' ';

			if ($i==$page)
			$res .= '<a class="current" href="#" onclick="doSearch(' . $i . ', \'\', \'' . $fqstr . '\', \'\', false);">' . $i . '</a>';
			else
			if ($i <= 1 || ($i > ($page - $itemarround) && $i < ($page + $itemarround)) || $i > ($lastpage - 1))
			$res .= '<a href="#" onclick="doSearch(' . $i . ', \'\', \'' . $fqstr . '\', \'\', false);"><b>' . $i . '</b></a>';
			else
			if ($i == ($page - $itemarround) || $i == ($page + $itemarround))
			$res .= '&nbsp;...&nbsp;';
		}

		if ($page != $lastpage)
		$res .= '&nbsp;<a href="#" onclick="doSearch(' . ($page+1) . ', \'\', \'' . $fqstr . '\', \'\', false);"><b>>></b></a>';

	} else {
		$res .= '&nbsp; ';
	}

	return $res;
}



?>
