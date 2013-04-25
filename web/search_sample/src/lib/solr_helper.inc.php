<?php
require_once("SolrPhpClient/Service.php");


/*
 *
**/
function solrConnect($solr_host, $solr_port, $solr_baseurl, $solr_corename) {
	require_once("lib/SolrPhpClient/HttpTransport/Curl.php");
	$httpTransport = new Apache_Solr_HttpTransport_Curl();
	$solr = new Apache_Solr_Service($solr_host, $solr_port, $solr_baseurl . $solr_corename, $httpTransport);
	if (!solrPing($solr)) return null;
	return $solr;
}

/*
 *
**/
function solrPing($solr) {
	if ($solr!=null && $solr->ping())
		return true;
	else
		return false;
}

/*
 *
**/
function getSolrCoreUrl($solr_host, $solr_port, $solr_baseurl, $solr_corename) {
	$ret = 'http://' . $solr_host;
	if ($solr_port!='' && $solr_port!='80')
		$ret .= ':' . $solr_port;
	$ret .= $solr_baseurl . $solr_corename;
}

/*
 *
**/
function getSolrVersion($solr) {
	if ($solr==null) return '';
	$response = $solr->system();
	$raw_response = $response->getRawResponse();
	$ar = json_decode($raw_response, true);
	return $ar["lucene"]["solr-spec-version"];
}

/*
 *
**/
function getSolrMajorVersion($solr) {
	$version = getSolrVersion($solr);
	if (!empty($version)) return $version{0};
	return '';
}

/*
 *
**/
function escapeSolrQuery($value) {
	//list taken from http://lucene.apache.org/java/docs/queryparsersyntax.html#Escaping%20Special%20Characters
	//$pattern = '/(\+|-|&&|\|\||!|\(|\)|\{|}|\[|]|\^|"|~|\*|\?|:|\\\)/';
	$pattern = '/(\&&|\|\||!|\{|}|\[|]|\^|~|\*|\?|:|\\\)/';
	$replace = '\\\$1';
	return str_replace(' ', '+', preg_replace($pattern, $replace, $value));
}

/*
 *
**/
function getSolrQuery($query, $query_lang, $query_field, $word_variations, $default_ope, $solr_major_version) {
	if ($query!='*:*') {
		if ($solr_major_version!='4') {
			$query_lang = '\[' . $query_lang . '\]';
		}
		else {
			$query_lang = '\{' . $query_lang . '\}';
		}

		$query = trim(escapeSolrQuery($query));

		$ret = $query_field . ':(' . $query_lang . $query . ")";
		if ($word_variations) {
			$ret = '(' . $ret . ') OR (' . $query_field . 's:(' . $query_lang . $query . '))';
		}
	}
	$default_ope = strtoupper($default_ope);
	if ($default_ope!='AND' && $default_ope!='OR') $default_ope='AND';
	$ret = '{!q.op=' . $default_ope . '}' . $ret;

	return $ret;
}

function getSolrFiedsValues($solr, $fields, $maxsize=0) {
	if ($solr==null) return "";
	$response = null;
	$ret="";

	if ($maxsize==0) $maxsize=1000;

	$params = array();

	$params['facet'] = 'true';
	$params['facet.field'] = array_map('trim',explode(',',$fields));
	$params['facet.mincount'] = '1';
	$params['facet.limit'] = $maxsize;

	$params['fl'] = $field;
	$params['hl'] = 'false';
	$params['spellcheck'] = 'false';

	$params['debugQuery'] = 'false';

	$response = $solr->search( "*:*", "0", "1", $params);
	if ( $response->getHttpStatus() == 200 ) {
		//print_r( $response->getRawResponse());
		if($response->response->numFound > 0 &&  $response->facet_counts) {
			foreach ($response->facet_counts->facet_fields as $facetfield => $facet) {
				if ( ! get_object_vars($facet) ) continue;
				$ret[$facetfield] = array();
				$count=0;
				foreach ($facet as $facetval => $facetcnt) {
					if ($facetcnt=="0") break;
					if ($facetval!="") $ret[$facetfield][$facetval] = $facetcnt;
				}
			}
		}
	}
	return $ret;
}


/*
 * Cloud
**/
function facet_item_cmp($a, $b) {
	return ($a[1] < $b[1]);
}

// Search in Solr to get cloud items
function getCloud($solr, $field, $query, $fq, $maxsize=0, $lang='', $country='', $maxhour=0, $multi_term_boost=0, $multi_term_keep=false, $debug=false) {

	$response = null;

	$ret="";

	$maxsizequery=$maxsize;
	if ($maxsize==0) $maxsizequery=1000;
	else if ($multi_term_boost>1 || $multi_term_keep) $maxsizequery= 3 * $maxsize;

	$params = array();

	$params['facet'] = 'true';
	$params['facet.field'] = $field;
	$params['facet.mincount'] = '1';
	$params['facet.limit'] = $maxsizequery;

	$params['fl'] = $field;
	$params['hl'] = 'false';
	$params['spellcheck'] = 'false';

	if ($debug) {
		$params['debugQuery'] = 'true';
	}
	else {
		$params['debugQuery'] = 'false';
	}

	if (is_null($fq)) $fq = array();
	if (!empty($lang)) array_push($fq, "language:" . urlencode($lang));
	if (!empty($country)) array_push($fq, "country:" . urlencode($country));
	$params['fq'] = $fq;

	$crit = '';
	if (!is_null($query)) $crit = $query;
	if ($maxhour>0) {
		if ($crit!='') $crit .= " AND ";
		$crit .= "(createtime:[NOW-". $maxhour . "HOUR TO *])";
	}
	if ($crit=='') $crit = "*:*";

	$response = $solr->search( $crit, "0", "1", $params);

	if ( $response->getHttpStatus() == 200 ) {
		//print_r( $response->getRawResponse());
		$q = $response->getRawResponse();

		$ret = filterCloud($response, $field, $maxsize, $multi_term_boost, $multi_term_keep, $debug);
	}
	return $ret;
}

// Filter cloud items
function filterCloud($response, $field, $maxsize=0, $multi_term_boost=0, $multi_term_keep=false, $debug=false) {

	$ret="";

	if($response->response->numFound > 0 &&  $response->facet_counts) {

		$aFacet = Array();

		foreach ($response->facet_counts->facet_fields as $facetfield => $facet) {

			if ($facetfield!=$field) continue;
			if (!get_object_vars($facet)) continue;

			$count=0;
			foreach ($facet as $facetval => $facetcnt) {
				if ($facetval=="" || $facetcnt=="0")
					break;

				$aItemFacet = Array();
				$aItemFacet[] = $facetval;

				if (!(strpos($facetval, ' ')===FALSE) && $multi_term_boost>1) {
					$facetcnt = $facetcnt * $multi_term_boost;
				}
				$aItemFacet[] = $facetcnt;

				$aFacet[] = $aItemFacet;
			}
		}

		if ($multi_term_boost>1 || $multi_term_keep) {

			// multi term keep
			if ($multi_term_keep) {
				for ($i=0; $i<count($aFacet); $i++) {
					$term1 = $aFacet[$i][0];
					for ($j=0; $j<count($aFacet); $j++) {
						if ($i!=$j) {
							$term2 = $aFacet[$j][0];
							if (preg_match ( "/\\b" . $term1 . "\\b/" , $term2 )) {
								$aFacet[$i][1] = 0;
								$j=count($aFacet);
							}
						}
					}
				}
			}

			// tri
			usort ( $aFacet , "facet_item_cmp" );
		}

		$count = 0;
		for ($i=0; $i<count($aFacet); $i++) {
			if ($aFacet[$i][1]==0) break;
			if ($ret!="") $ret .= "|";
			$ret .= $aFacet[$i][0] . ":" . $aFacet[$i][1];
			$count++;
			if ($maxsize>0 && $count>=$maxsize) break;
		}

	}
	return $ret;
}

// Display cloud
function displayCloud($data) {

	$tags = array();
	$aData = explode ("|", $data);

	foreach ($aData as $value) {
		$aItem = explode (":", $value);
		//$tempTag[tag_name] = utf8_decode($aItem[0]);
		$tempTag["tag_name"] = $aItem[0];
		$tempTag["tag_count"] = $aItem[1];
		$tags[] = $tempTag;
	}

	// instantiate a cloud
	$cloud = new cloud_tag();

	// the class already has defaults, but let's override them for fun
	$cloud->set_label('tag'); // css classes will be cloud1, cloud2, etc.
	$cloud->set_tagsizes(7); // 7 font sizes will be used

	$newtags = $cloud->make_cloud($tags); // make a tag cloud

	$res = "";
	foreach ($newtags as $atag) {
		$escapedName = str_replace ("'", "&acute;", $atag["tag_name"]);
		$res .= "<a href='javascript:void(0);' class='" . $atag["tag_class"] . "' title='" . $escapedName . "' onClick='setSearchCrit(\"" . $escapedName . "\");'>" . str_replace(" ", "&nbsp;", $atag["tag_name"]) . "</a>&nbsp;&nbsp;&nbsp; ";
	}
	return $res;
}


?>
