<?php
$rootpath = dirname(__FILE__);
require_once("../init.inc.php");
require_once("../lib/cloud_tag.inc.php");

$action = getRequestParam("action");

$res = "";

if ($action=="autocomplete" || $action=="fiedvalues" || $action=="gettext" || $action=="search" || $action=="gettagcloud") {

	$solr_host = $config->get("solr.host");
	$solr_port = $config->get("solr.port");
	$solr_baseurl = $config->get("solr.baseurl");
	if ($solr_baseurl=="undefined") $solr_baseurl = "";
	$solr_corename = $config->get("solr.corename");
	if ($solr_corename=="undefined") $solr_corename = "";

	$solr = new Solr();
	if ($solr->connect($solr_host, $solr_port, $solr_baseurl, $solr_corename))
	{
		if ($action=="autocomplete")
		{
			$q = $_GET['q'];
			$limit = '5';
			$res=$solr->getSuggestion($q, $limit);
		}

		if ($action=="fiedvalues")
		{
			$field = getRequestParam("field");
			$values = $solr->getFiedvalues($field);

			$res='';
			foreach ($values[$field] as $key => $value) {
				if ($res!="") $res .= "|";
				$res .= $key . ":" . $value;
			}
		}

		if ($action=="gettagcloud")
		{
			$field = getRequestParam("field");
				
			$maxhours = $config->get("search.tagcloud.maxhours", "0");
			$data = $solr->getCloud($field, 20, '', '', $maxhours, 3, true, false);

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

			$queryField = getQueryField($search_multilingual, $search_language_code);
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
			$filter_collection = getRequestParam("search_collection");
			$item_per_page = getRequestParam("search_itemperpage");

			$filter_location_lat="";
			$filter_location_lng="";
			$filter_location_radius="";

			if ($uselocation)
			{
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

			/*
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
			*/
				
			$aFacetFields = array();
			$fqstr = '';
			//$fqitms = explode('\|\|', stripslashes($fq));
			$fqitms = explode('||', stripslashes($fq));
			sort($fqitms);
			$fqsearch = array();
			$fqitem_field_prev = '';
			foreach ($fqitms as $fqitem) {
				if ($fqitem) {
					if ($fqstr!="") $fqstr .= urlencode('||');

					if ($facet_union) {
						$fqitem_parts = explode(':', $fqitem);
						if ($fqstr=="") {
							array_push($fqsearch, "(" . $fqitem);
						} else {
							if ($fqitem_field_prev != $fqitem_parts[0])
								$fqsearch[0] .= ") AND (" . $fqitem;
							else
								$fqsearch[0] .= " OR " . $fqitem;
						}
						$fqitem_field_prev = $fqitem_parts[0];
					}
					$splititm = explode(':', $fqitem, 2);
					$fqstr .= $splititm[0] . ':' . urlencode($splititm[1]);
					array_push($aFacetFields, $splititm[0]);
					if ($splititm[0]=="source_str") $groupsize = 0;
				}
			}
			if ($fqsearch[0]!="") $fqsearch[0] .= ")";

			if (!$facet_union) $fqsearch = $fqitms;
				

			$queryField = getQueryField($search_multilingual, $search_language_code);

			if ($groupsize>0)
				$item_per_page = intval($item_per_page / $groupdisplaysize);

			$debug=false;
			$solr->setDebug($debug);
			$response = $solr->query($crit, $queryField, $querylang, $sort, $groupsize, ($page-1)*$item_per_page, $item_per_page, $fqsearch, $word_variations, $filter_lang, $filter_country, $filter_mimetype, $filter_source, $filter_collection, $filter_tag, $filter_location_lat, $filter_location_lng, $filter_location_radius, $mode, false, $debug);
			if ( $response->getHttpStatus() == 200 ) {

				if ($groupsize>0) {
					$numFound = $response->grouped->sourceid->matches;
					$numFoundPaginate = $response->grouped->sourceid->ngroups;
				}
				else {
					$numFound = $response->response->numFound;
					$numFoundPaginate = $response->response->numFound;
				}

				$teasers = get_object_vars($response->highlighting);

				//
				// results summary
				//
				$res .=	"<div id='result_metadata'>";
				$res .=	"<p class='count'><b>";
				$res .= $numFound . "</b> " . 'document(s) match your query' . "</span>";
				$res .=	"</p><p class='subscribe'>";
				//$res .= "<a href='" . $search_bookmark . "' target='_blank'>Permalink <img src='images/bookmark.png' title='Bookmark link to this search' height='20' width='20'></a>";
				//$res .= "&nbsp;&nbsp;&nbsp;-&nbsp;&nbsp;&nbsp;";
				//$res .= "<a href='rss.php" . $search_bookmark . "' target='_blank'>RSS <img src='images/rss.png' title='Bookmark link to this search as a RSS feed' height='20' width='20'></a>";
				$res .= "</p>";

				if($numFound == 0 && $response->spellcheck) {
					$res2 = "";
					foreach ($response->spellcheck->suggestions as $queryterm => $suggestion) {
						if ( ! get_object_vars($suggestion)) {
							continue;
						}
						if ( $queryterm == $querylang ) {
							continue;
						}
						$res2 .= $suggestion->suggestion[0]->word . " ";
					}
					$res2 = trim($res2);
					if ($res2!="") {
						$res .= "<br /><p class='didyoumean'>" . 'Did you mean' . " : <i><a href='javascript:void(0);' onClick='doDidYouMeanSearch(\"" . $res2 . "\");'>";
						$res .= $res2 . "</i></a></p>";
					}
				}

				$res .= "</div>";

				//
				// results data
				//
				$res .= '<div id="result_container">';
				$res .= '<div id="sidebar"><br/><br/>';

				if ($facet_union) {
					if ($initialSearch) {
						$_SESSION["facet_counts"] = $response->facet_counts;
						$facet_counts = $response->facet_counts;
					}
					else {
						if (isset($_SESSION["facet_counts"])) {
							$facet_counts = $_SESSION["facet_counts"];
						}
						else {
							$facet_counts = $response->facet_counts;
						}
					}
				}
				else {
					$facet_counts = $response->facet_counts;
				}

				if ($facet_counts) {

					foreach ($facet_counts->facet_fields as $facetfield => $facet) {

						if ( ! get_object_vars($facet) ) {
							continue;
						}

						$facetfield = strtolower($facetfield);
						$label = $facetfield;
						$label2 = "";
						switch($facetfield)
						{
							case 'collection':
								$label2 = "Collection";
								break;
							case 'tag':
								$label2 = "Tag";
								break;
							case "country":
								$label2 = "Country";
								break;
							case 'language':
								$label2 = "Language";
								break;
							case 'contenttyperoot':
								$label2 = "Format";
								break;
							case 'source_str':
								$label2 = "Source";
								break;
						}

						if ($label2=='') {
							// if $facetField in extra
							$label2 = getExtraFacetLabel($facetfield, $facetextra);
						}


						$res .= '<div id="facet_'. $facetfield . '"><h4 class="hand" onClick="$(\'#facet_'. $facetfield . '_data\').toggle(400);">' . $label2. '</h4>';
						$res .= '<div id="facet_'. $facetfield . '_data">';
						$count=0;
						foreach ($facet as $facetval => $facetcnt)
						{
							$count += 1;

							if ($facetcnt=="0")
								break;

							$crit_fq = $facetfield . ":" . urlencode('"'.$facetval.'"');

							$checked = "";
							if (in_array($facetfield.":\"".$facetval."\"",$fqitms))
								$checked = " checked ";

							if ($facetfield=="language")
							{
								$facetval = getLabelFromCode($aLanguages, $facetval);
							}
							if ($facetfield=="country")
							{
								$facetval = getLabelFromCode($aCountries, $facetval);
							}
							if ($facetfield=="contenttyperoot")
							{
								if ($facetval=="text/plain") $facetval = "";  // bug to be fix in crawler / indexer ???
								$facetval = getLabelFromCode($aContentType, $facetval);
							}
							if ($facetfield=="collection")
							{
								$facetval = strtolower($facetval);
							}
							if ($facetfield=="tag")
							{
								$facetval = strtolower($facetval);
							}
							$chk_id = $facetfield . $count;

							if ($facetval!="")
							{
								$res .= '<table><tr><td valign="top"><input type="checkbox" id="' . $chk_id . '" ' . $checked . ' onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', true, \'\', \'\');">&nbsp;</td>';
								$res .= '<td><a href="#" onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', false, \'\', \'\');">' . $facetval . '</a> (' . $facetcnt . ')</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table>';
							}
						}
						$res .= '</div></div>';
					}
				}
				$res .= "</div>"; // end facet

				$res .= "<div id='result_content'>";

				if ( $numFound > 0 ) {

					$res .= '<div class="pagination">';

					$res .= "
							<script language='javascript'>
							function changeItemPerPage()
							{
							$('#search_itemperpage').val($('#results_itemperpage').val());
				}
							</script>
							";
					$res .= displayPagination ($numFoundPaginate, $item_per_page, $page, $fqstr );
					$res .= "<select id='results_itemperpage' name='results_itemperpage' onChange='changeItemPerPage();'>";
					$item_per_page_values="10,20,50,100";
					$aValues = explode(",",$item_per_page_values);
					foreach($aValues as $value){
						$res .= "<option value='" . $value . "'";
						if ($item_per_page_option==$value)
							$res .= " selected";
						$res .= ">" . $value . "</option>";
					}
					$res .= "</select><label for='results_itemperpage'>" . 'Items per page' . "</label>";
					$res .= '</div>';

					$res2 = "";
					if ($groupsize>0) {
						//$res .= "groupes : " .  $response->grouped->sourceid->matches;
						foreach ($response->grouped->sourceid->groups as $group) {
							$doclist = $group->doclist;
							$groupid = $group->groupValue;
							$countdisplay=0;
							$res2 .= "<div>";
							foreach ($doclist->docs as $doc) {
								if ($countdisplay==$groupdisplaysize) {
									$parseUrl = parse_url($doc->id);
									$homeUrl = $parseUrl["host"];

									$res2 .= "<div id='more_" . $groupid . "'><strong>[+]</strong>&nbsp;<a href='javascript:void(0);' onClick='$(\"#" . $groupid . "\").show();$(\"#more_" . $groupid . "\").hide();'>More results from " . $homeUrl . "</a></div>";
									$res2 .= "<div id='" . $groupid . "' style='display:none'>";
									$res2 .= "<strong>[-]</strong>&nbsp;<a href='javascript:void(0);' onClick='$(\"#" . $groupid . "\").hide();$(\"#more_" . $groupid . "\").show();'>Less results from " . $homeUrl . "</a>";
								}
								$res2 .= buildDocBloc($doc, $crit, $teasers, $queryField);
								$countdisplay++;
							}
							if ($countdisplay>$groupdisplaysize) {
								$parseUrl = parse_url($doc->id);
								$homeUrl = $parseUrl["host"];
								$tmp = "<div id='all_" . $groupid . "'><strong>[>>>]</strong>&nbsp;<a href='#' onclick='doSearch(1, \"" . "source_str:%22" . $doc->source_str . "%22\", \"\", \"\", false, \"\", \"\");'>All results from " . $homeUrl . "</a></div>";
								$res2 .= $tmp;
								$res2 .= "</div>";
							}
							$res2 .= "</div>";
						}
					}
					else {
						foreach ( $response->response->docs as $doc ) {
							$res2 .= buildDocBloc($doc, $crit, $teasers, $queryField);
						}
					}
					$res .= $res2;
					$res .= '<br/><div class="pagination">';
					$res .= displayPagination ($numFoundPaginate, $item_per_page, $page, $fqstr );
					$res .= '</div>';
				}
				$res .= '</div>';

				// 				if ($groupsize>0) {
				// 					$res .= '<div>';
				// 					foreach ($response->clusters as $cluster) {
				// 						$res .= $cluster->labels[0] . '<br/>';
				// 					}
				// 					$res .= '</div>';
				// 				}

				if ($debug)
					$res .= $response->getRawResponse();
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
}

//$res = $_SERVER["QUERY_STRING"] . "<br/>" . $res;
print ($res);

function getExtraFacetLabel($facetfield, $facetextra) {
	if ($facetextra=='') return '';
	$aTemp = explode(',',$facetextra);
	for ($i=0;$i<count($aTemp);$i++) {
		$val=trim($aTemp[$i]);
		$pos = strpos($val,'(');
		if ($pos!==false) {
			if ($facetfield == substr($val,0,$pos)) {
				return substr($val,$pos+1, -1);
			}
		}
	}
	return '';
}

function buildDocBloc($doc, $query, $teasers, $queryField) {

	global $aContentTypeImage, $resultshowsource, $resultshowmeta, $results_img_height, $results_img_width, $aCountries, $aLanguages;

	$res2 = "<dl>";

	$res2 .= "<dt><a href='$doc->id' target='_blank'>" . $doc->title_dis;

	$t = $doc->contenttyperoot;
	$img = getImageNameForContentType($aContentTypeImage, $t);
	if ($img!="")
		$res2 .= "&nbsp;<img src='images/" . $img . "' border='0'>";
	$res2 .= "</a>";

	if ($t=='text/html') {
		$res2 .= '<a class="ovalbutton" href="javascript:void(0);" onclick="doReader(\'' . $doc->uid . '\', \'' . $query . '\');"><span>Reader</span></a>';
	}

	$res2 .= "</dt>";

	//$res .= $tmp;

	$summary = '';
	if (isset($teasers)) {
		$docid = strval($doc->id);
		$docteaser = get_object_vars($teasers[$docid]);
		//if (isset($docteaser->content_ml))
		if (isset($docteaser[$queryField]))
		{
			$summary = "";
			//foreach($docteaser->content_ml as $value)
			foreach($docteaser[$queryField] as $value)
			{
				if ($summary!="") $summary .= "...";
				$value = preg_replace("/^\\[[a-zA-Z]{2}\\]/", "", $value);
				$summary .= $value;
			}
		}
	}

	if ($summary=='')
		$summary = $doc->summary;

	$res2 .= "<dd>" . $summary . "</dd>";

	if ($results_img_height>0 && $results_img_width>0 && !empty($doc->urlimage_str)) {
		$res2 .= "<dd><a href='" . $doc->urlimage_str . "' target='image'><img class='resizeme' src='" . $doc->urlimage_str . "'></a></dd>";
	}

	if ($resultshowsource) {
		$parseUrl = parse_url($doc->id);
		$homeUrl = $parseUrl["scheme"] . "://" . $parseUrl["host"];
		$res2 .= "<dd><span class='mnemo'>Source&nbsp;:</span>&nbsp;<a href='$homeUrl' target='_blank'>$doc->source_str</a></dd>";
		$res2 .= "<dd><span class='mnemo'>Date&nbsp;:</span>&nbsp;" . getHumanDate($doc->createtime) . "</dd>";
	}

	if ($resultshowmeta) {
		$res2 .= "<dd><table width='90%'><tr><td width='30%'>";
		//if (count($doc->tag) > 1)
			$temp = implode(', ', $doc->tag);
		//else
		//	$temp = $doc->tag;
		$res2 .= "<span class='mnemo'>Tags&nbsp;:</span>&nbsp;" . $temp . "</td><td width='38%'>";

		$t = $doc->country;
		$t = getLabelFromCode($aCountries, $t);
		$res2 .= "<span class='mnemo'>Country&nbsp;:</span>&nbsp;" . $t . "</td><td width='38%'>";
		$t = $doc->language;
		$t = getLabelFromCode($aLanguages, $t);
		$res2 .= "<span class='mnemo'>Language&nbsp;:</span>&nbsp;" . $t;
		$res2 .= "</td></tr></table></dd>";
	}
	$res2 .= "</dl>";

	return $res2;
}


function buildOneDocDisplay($doc, $teasers, $queryField) {

	global $aContentTypeImage, $resultshowsource, $resultshowmeta;

	$res2 = "<dl>";

	$res2 .= "<dt><a href='$doc->id' target='_blank'>" . $doc->title_dis . "</a>";

	$summary = '';

	if (false) {

		if (isset($teasers)) {
			$docid = strval($doc->id);
			$docteaser = get_object_vars($teasers[$docid]);
			//if (isset($docteaser->content_ml))
			if (isset($docteaser[$queryField]))
			{
				$summary = "";
				//foreach($docteaser->content_ml as $value)
				foreach($docteaser[$queryField] as $value)
				{
					if ($summary=="")
					{
						$summary .= "...";
						$value = preg_replace("/^\\[[a-zA-Z]{2}\\]/", "", $value);
					}
					$summary .= $value . "...";
				}
			}
		}

	}

	if ($summary=="") {
		$content = $doc->content_ml;
		$content = preg_replace ('/^\[[a-z]{2}\]/', ' ', $content);
		$aContent = explode("\n", $content);
		foreach ($aContent as $line)  $summary .= "<p>" . $line . "</p>";
	}

	$res2 .= "<dd>" . $summary . "</dd>";

	// 	if ($resultshowsource) {
	$parseUrl = parse_url($doc->id);
	$homeUrl = $parseUrl["scheme"] . "://" . $parseUrl["host"];
	$res2 .= "<dd><span class='mnemo'>Source&nbsp;:</span>&nbsp;<a href='$homeUrl' target='_blank'>$doc->source_str</a></dd>";
	$res2 .= "<dd><span class='mnemo'>Date&nbsp;:</span>&nbsp;" . getHumanDate($doc->createtime) . "</dd>";

	// 	}

	// 	if ($resultshowmeta) {
	if (count($doc->tag) > 0) {
		$res2 .= "<dd>";
		$temp = implode(', ', $doc->tag);
		$res2 .= "<span class='mnemo'>Tags&nbsp;:</span>&nbsp;" . $temp;
		$res2 .= "</dd>";
	}
	// 	}
	$res2 .= "</dl>";

	return $res2;
}


function displayPagination ($totalhits, $pagesize, $page, $fqstr ) {

	$itemarround = 2;

	if ($totalhits<=$pagesize)
		return "";

	$res="<span>Pages</span>";

	$firstpage = 1;
	$lastpage = intval(abs((($totalhits-1) / $pagesize) + 1));

	$ndxstart = (($page-1) * $pagesize) + 1;
	$ndxstop = $page * $pagesize;
	if ($ndxstop > $totalhits)
		$ndxstop = $totalhits;

	if ($lastpage > 1) {
		if ($page != 1)
			$res .= '<a href="#" onclick="doSearch(' . ($page-1) . ', \'\', \'' . $fqstr . '\', \'\', false, \'\', \'\');"><b><<</b></a>';

		for ($i=$firstpage; $i<=$lastpage; $i++) {
			if ($i!=$firstpage)
				$res .= ' ';

			if ($i==$page)
				$res .= '<a class="current" href="#" onclick="doSearch(' . $i . ', \'\', \'' . $fqstr . '\', \'\', false, \'\', \'\');">' . $i . '</a>';
			else
			if ($i <= 1 || ($i > ($page - $itemarround) && $i < ($page + $itemarround)) || $i > ($lastpage - 1))
				$res .= '<a href="#" onclick="doSearch(' . $i . ', \'\', \'' . $fqstr . '\', \'\', false, \'\', \'\');"><b>' . $i . '</b></a>';
			else
			if ($i == ($page - $itemarround) || $i == ($page + $itemarround))
				$res .= '&nbsp;...&nbsp;';
		}

		if ($page != $lastpage)
			$res .= '&nbsp;<a href="#" onclick="doSearch(' . ($page+1) . ', \'\', \'' . $fqstr . '\', \'\', false, \'\', \'\');"><b>>></b></a>';

	} else {
		$res .= '&nbsp; ';
	}

	return $res;
}

function getLabelFromCode($search, $key) {
	if ($search=="") return "";
	if (array_key_exists  ( strtolower($key)  , $search ))
		return $search[strtolower($key)];
	return $key;
}

function getImageNameForContentType($search, $key) {
	if ($search=="") return "";
	if (array_key_exists  ( $key  , $search ))
		return $search[$key];
	return "";
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

function getHumanDate($date) {
	return date_format(date_create($date), 'd/m/Y H:i:s');
	//2012-03-28T13:21:48Z
}

?>
