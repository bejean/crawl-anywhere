<?php
//require_once("solr.class.inc.php");
require_once("Solr/Service.php");

function facet_item_cmp($a, $b) {
	return ($a[1] < $b[1]);
}

/*----------------------------------------------------------------
 *
* Solr class with API Php For Solr
*
*----------------------------------------------------------------*/
class Solr {

	private $_debug = false;
	private $_solr = null;
	private $_response = null;

	public function Solr_Php_For_Solr () {
	}

	public function connect($solr_host, $solr_port, $solr_baseurl, $solr_corename) {
		require_once("Solr/HttpTransport/Curl.php");			
		$httpTransport = new Apache_Solr_HttpTransport_Curl();
		$this->_solr = new Apache_Solr_Service($solr_host, $solr_port, $solr_baseurl . $solr_corename, $httpTransport);
		return $this->ping();
	}

	public function ping() {
		if ($this->_solr!=null && $this->_solr->ping())
		return true;
		else
		return false;
	}

	public function system() {
		if ($this->_solr==null) return null;
		$response = $this->_solr->system();
		return $response->getRawResponse();
	}

	public function setDebug($debug) {
		$this->_debug = $debug;
	}

	protected function escapeQuery($value)
	{
		//list taken from http://lucene.apache.org/java/docs/queryparsersyntax.html#Escaping%20Special%20Characters
		//$pattern = '/(\+|-|&&|\|\||!|\(|\)|\{|}|\[|]|\^|"|~|\*|\?|:|\\\)/';
		$pattern = '/(\&&|\|\||!|\{|}|\[|]|\^|~|\*|\?|:|\\\)/';
		$replace = '\\\$1';

		return str_replace(' ', '+', preg_replace($pattern, $replace, $value));
	}
	
	public function getVersion() {
		$raw_response = $this->system();
		$ar = json_decode($raw_response, true);
		return $ar["lucene"]["solr-spec-version"];
	}

	public function getSuggestion($q, $limit) {

		if ($this->_solr==null) return "";

		global $search_multilingual, $search_language_code;

		if ($search_multilingual) {
			$field_sufix = 'ml';
		}
		else {
			$field_sufix = $search_language_code;
		}
		$queryField = "content_" . $field_sufix;

		$params = array();
		$params['terms'] = 'true';
		$params['terms.fl'] = $queryField;
		$params['terms.lower'] = $q;
		$params['terms.prefix'] = $q;
		$params['terms.lower.incl'] = 'true';
		$params['terms.limit'] = $limit;
		$params['qt'] = '/terms';

		$response = $this->_solr->search($q, 0, $limit, $params);
		if ( ! $response->getHttpStatus() == 200 ) {
			return "";
		}

		$q = $response->getRawResponse();

		$ret = "";
		if (is_object ( $response->terms )) {
			if ($search_multilingual) {
				$terms = get_object_vars($response->terms->content_ml);
			}
			else {
				$terms = get_object_vars($response->terms->content_fr);
			}
			foreach($terms as $term => $count) {
				$ret .= sprintf("%s\n", $term);
			}
		}
		return $ret;
	}

	public function getFiedValues($field, $maxsize=0, $debug=false) {

		if ($this->_solr==null) return "";

		$response = null;

		$ret="";

		if ($maxsize==0) $maxsize=1000;
		
		$params = array();

		$params['facet'] = 'true';
		$params['facet.field'] = array_map('trim',explode(',',$field));
		$params['facet.mincount'] = '1';
		$params['facet.limit'] = $maxsize;

		$params['fl'] = $field;
		$params['hl'] = 'false';
		$params['spellcheck'] = 'false';
			
		if ($debug)
		$params['debugQuery'] = 'true';
		else
		$params['debugQuery'] = 'false';
		
			
		$response = $this->_solr->search( "*:*", "0", "1", $params);
		if ( $response->getHttpStatus() == 200 ) {
			//print_r( $response->getRawResponse());
			if($response->response->numFound > 0 &&  $response->facet_counts) {
				foreach ($response->facet_counts->facet_fields as $facetfield => $facet) {
					if ( ! get_object_vars($facet) ) {
						continue;
					}

					$ret[$facetfield] = array();
						
					$count=0;
					foreach ($facet as $facetval => $facetcnt) {
						if ($facetcnt=="0") break;
						
						if ($facetval!="") $ret[$facetfield][$facetval] = $facetcnt;

// 						if ($facetval!="") {
// 							if ($ret!="") $ret .= "|";
// 							$ret .= $facetval . ":" . $facetcnt;
// 						}
					}
				}
			}
		}
		return $ret;
	}


	public function getCloud($field, $maxsize=0, $lang='', $country='', $maxhour=0, $multi_term_boost=0, $multi_term_keep=false, $debug=false) {

		if ($this->_solr==null) return "";

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
			
		if ($debug)
		$params['debugQuery'] = 'true';
		else
		$params['debugQuery'] = 'false';
					
		$fq = array();
		if (!empty($lang)) array_push($fq, "language:" . urlencode($lang));
		if (!empty($country)) array_push($fq, "country:" . urlencode($country));
		$params['fq'] = $fq;
				
		$crit = "";
		if ($maxhour>0) $crit .= " createtime:[NOW-". $maxhour . "HOUR TO *]";
		if ($crit=='') $crit = "*:*";
		
		$response = $this->_solr->search( $crit, "0", "1", $params);

		if ( $response->getHttpStatus() == 200 ) {
			//print_r( $response->getRawResponse());
			$q = $response->getRawResponse();

			if($response->response->numFound > 0 &&  $response->facet_counts) {
					
				$aFacet = Array();

				foreach ($response->facet_counts->facet_fields as $facetfield => $facet) {

					if ( ! get_object_vars($facet) ) {
						continue;
					}

					$count=0;
					foreach ($facet as $facetval => $facetcnt) {
						if ($facetval=="" || $facetcnt=="0")
						break;

						$facetval = str_replace("\"", " ", $facetval);
						//$facetval = str_replace("'", " ", $facetval);
						$facetval = trim($facetval);
						
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
		}
		return $ret;
	}

	function buildQuery($dismax, $queryField, $query_lang, $qry, $word_variations, $search_multilingual) {

		if (empty($qry) || $qry=='*' || $qry=='*:*') {
			return '*:*';
		}
		
		if ($dismax) {
			/*
			if ($word_variations) $queryField = $queryField . 's';
			if ($search_multilingual)
			$finalqry = '[' . $query_lang .  ']' . $qry ;
			else
			$finalqry = $qry ;
			*/
			$finalqry = $qry ;
		}
		else {
			//if ($word_variations)
			//	$queryField = $queryField . 's';

			// excape query
			//$qry = trim($solr->escape($qry));
			$qry = trim($this->escapeQuery($qry));
			//$params['df'] = $queryField;

			/*
			 $aCrit = preg_split( "/\s+/", $qry, -1, PREG_SPLIT_NO_EMPTY);
			$finalqry = "";
			foreach ( $aCrit as $crit)
			{
			$finalqry .= $queryField . ':' . '\[' . $query_lang .  '\]' . $crit . " ";
			}
			*/
			if ($search_multilingual) {
				$solr_version = $this->getVersion();

				if ($solr_version{0}!='4') {
					$query_lang = '\[' . $query_lang . '\]';
				}
				else {
					$query_lang = '\{' . $query_lang . '\}';
				}

				$finalqry = $queryField . ':(' . $query_lang . $qry . ")";
				if ($word_variations) {
					$finalqry = '(' . $finalqry . ') OR (' . $queryField . 's:(' . $query_lang . $qry . '))';
					//$finalqry = $queryField . 's:(' . '\[' . $query_lang . '\]' . $qry . ')';
				}
			} else{
				$finalqry = $queryField . ':(' . $qry . ")";
				if ($word_variations)
				$finalqry = '(' . $finalqry . ') OR (' . $queryField . 's:(' . $qry . '))';
			}
		}

		return $finalqry;
	}

	function getText(
	$id,
	$qry,
	$queryField,
	$query_lang,
	$word_variations,
	$debug=false) {

		global $search_multilingual, $search_language_code;

		if ($this->_solr==null) return "";

		$this->_response = null;

		$params = array();

		if ($query_lang=="") $query_lang = "en";

		$dismax=false;
		if ($dismax) $params['defType'] = "dismax";
		$finalqry = $this->buildQuery($dismax, $queryField, $query_lang, $qry, $word_variations, $search_multilingual);
		$finalqry = "(" . $finalqry . ")" . " AND (uid:\"" . $id . "\")";

		$params['facet'] = 'false';

		$params['fl'] = 'id, title_dis, summary, createtime, source_str, tag, ' . $queryField;

		$params['hl'] = 'true';
		$params['hl.fl'] = $queryField;
		$params['hl.snippets'] = '100';
		$params['hl.fragsize'] = '100';
		//$params['hl.maxAnalyzedChars']= $params['hl.fragsize'];
		$params['hl.simple.pre'] = '<em>';
		$params['hl.simple.post'] = '</em>';

		/*
		 $params['hl.useFastVectorHighlighter'] = 'true';
		$params['hl.tag.pre'] = '<b>'; // for FastVectorHighlighter
		$params['hl.tag.post'] = '</b>';
		*/

		//$params['tv'] = 'true';
		//$params['tv.fl'] = $queryField;
		//$params['tv.tf'] = 'true';
		//$params['tv.df'] = 'true';
		//$params['tv.positions'] = 'true';
		//$params['tv.offsets'] = 'true';
		//$params['tv.tf_idf'] = 'true';
		//$params['tv.all'] = 'true';
		//$params['tv.fl'] = '1';
		//$params['tv.docIds'] = '1';


		if ($debug)
		$params['debugQuery'] = 'true';
		else
		$params['debugQuery'] = 'false';

		$response = $this->_solr->search( $finalqry, 0, 1, $params);

		$sorl_response = new Solr_Response($response->getRawResponse(), $response->getHttpStatus());
		return $sorl_response;
	}


	function query(
	$qry,
	$queryField,
	$query_lang,
	$sort,
	$groupsize,
	$offset,
	$count,
	$fq,
	$word_variations,
	$filter_lang,
	$filter_country,
	$filter_mimetype,
	$filter_source,
	$filter_collection,
	$filter_tag,
	$filter_location_lat,
	$filter_location_lng,
	$filter_location_radius,
	$fields,
	$p,
	$mode,
	$rss,
	$debug=false) {

		//global $search_multilingual, $search_language_code, $facetcollections, $facettags, $facetcountry, $facetlanguage, $facetcontenttype, $facetsourcename, $facetextra;
		global $search_multilingual, $search_language_code, $facetuse, $facetextra, $facetqueries, $facetlimit, $search_requesthandler;
		
		if ($this->_solr==null) return "";

		$this->_response = null;

		if (!empty($p)) 
			$params = $p;
		else
			$params = array();

		if ($query_lang=="") $query_lang = "en";

		$dismax=false;
		if (!empty($search_requesthandler)) {
			$dismax=true;
			$params['qt'] = $search_requesthandler;
		}
		//if ($dismax) $params['defType'] = "dismax";
		
		$finalqry = $this->buildQuery($dismax, $queryField, $query_lang, $qry, $word_variations, $search_multilingual);
		if (!$dismax) $finalqry = "(" . $finalqry . ")";

		// params facet
		if ($rss) {
			$params['facet'] = 'false';
		}
		else {
			$aFacet = array();
			if (!empty($facetuse)) {
				$aTemp = array_map('trim',explode(",",$facetuse));
				foreach ($aTemp as $key => $value) {
					if ($value=='source') $aTemp[$key] = 'source_str';
					if ($value=='contenttype') $aTemp[$key] = 'contenttyperoot';

					$ex = '';
					for ($i=0; $i<count($fq); $i++) {
						$aa = $fq[$i];
						$ab = $aTemp[$key] . ':';
						if (startswith($fq[$i], '(' . $aTemp[$key] . ':')) {
							$fq[$i] = '{!tag=' . $aTemp[$key] . '_' . $i . '}' . $fq[$i];
							if (!empty($ex)) $ex = $ex . ',';
							$ex = $ex . $aTemp[$key] . '_' . $i;
						}
					}
					
					if (empty($ex)) {
						array_push($aFacet, $aTemp[$key]);
					} else {
						array_push($aFacet, '{!ex=' . $ex . '}' . $aTemp[$key]);
					}
				}	
			}
			
			if (!empty($facetextra)) {
				$aTemp = array_map('trim',explode(",",$facetextra));
				for ($i=0;$i<count($aTemp);$i++) {
					$val=trim($aTemp[$i]);
					$pos = strpos($val,'(');
					if ($pos!==false) {
						$val = substr($val,0,$pos);
					}
					$ex = '';
					for ($j=0; $j<count($fq); $j++) {
						$aa = $fq[$j];
						$ab = $val . ':';
						if (startswith($fq[$j], '(' . $val . ':')) {
							$fq[$j] = '{!tag=' . $val . '_' . $j . '}' . $fq[$j];
							if (!empty($ex)) $ex = $ex . ',';
							$ex = $ex . $val . '_' . $j;
						}
					}	
					if (empty($ex)) {
						array_push($aFacet,  $val);
					} else {
						array_push($aFacet, '{!ex=' . $ex . '}' .  $val);
					}

				}
			}
			
			if (!empty($facetqueries)) {
				$aFacetQuery = array();
				foreach($facetqueries as $f) {
					foreach($f['conditions'] as $c) {
						array_push($aFacetQuery, $f['field'] . ':[' . $c['condition'] . ']');
					}
				}	
				$params['facet.query'] = $aFacetQuery;
			}
			
			if (count($aFacet)>0) {
				$params['facet'] = 'true';
				$params['facet.field'] = $aFacet;
				$params['facet.mincount'] = '1';
				if ($facetlimit>0) $params['facet.limit'] = $facetlimit;
			} else {
				$params['facet'] = 'false';
			}
		}

		// params fields
		if ($rss) {
			$params['fl'] = 'id, title_dis, summary, createtime, score';
		}
		else {
			$f = 'id, id, uid, sourceid, title_dis, tag, collection, country, language, summary, createtime, source_str, type_str, urlimage_str, contenttype, contenttyperoot, tag_cloud, score';
			$af = array_merge ( array_map('trim',explode(",",$f)), array_map('trim',explode(",",$fields)) );
			$af = array_filter(array_unique($af), 'strlen');
			$params['fl'] = implode(',', $af);
		}

		// Test Carrot2
		$bTestCarrot2 = false;
		if ($bTestCarrot2 && !$rss) {
			$params['clustering'] = 'true';
			$params['clustering.results'] = 'true';
			$params['carrot.title'] = 'title_dis';
			$params['carrot.snippet'] = 'summary';
		}

		// params highlighting
		if ($rss) {
			$params['hl'] = 'false';
		}
		else {
			$params['hl'] = 'true';

			//if ($word_variations)
			//	$params['hl.fl'] = $queryField1 . "s";
			//else
			$params['hl.fl'] = $queryField;
			$params['hl.snippets'] = '5';
			$params['hl.fragsize'] = '100';
			$params['hl.simple.pre'] = '<b>';
			$params['hl.simple.post'] = '</b>';
			/*
			 $params['hl.useFastVectorHighlighter'] = 'true';
			$params['hl.tag.pre'] = '<b>'; // for FastVectorHighlighter
			$params['hl.tag.post'] = '</b>';
			*/

			//$params['hl.mergeContiguous'] = 'true';
		}

		// spellchecker
		if ($rss) {
			$params['spellcheck'] = 'false';
		}
		else {
			//$params['spellcheck.build'] = 'true';
			$params['spellcheck'] = 'true';
			$params['spellcheck.q'] = $qry;
			$params['spellcheck.count'] = '3';
			$params['spellcheck.onlyMorePopular'] = 'true';
			$params['spellcheck.extendedResults'] = 'true';
			$params['spellcheck.collate'] = 'false';

			if ($groupsize>0) {
				$params['group'] = 'true';
				$params['group.field'] = 'sourceid';
				$params['group.limit'] = $groupsize;
				$params['group.ngroups'] = 'true';
			}
		
		}

		// param filter meta
		if ($mode=="advanced") {
			if ($filter_lang!="")
			array_push($fq, "language:" . urlencode($filter_lang));

			if ($filter_country!="")
			array_push($fq, "country:" . urlencode($filter_country));

			if ($filter_mimetype!="")
			array_push($fq, "contenttyperoot:\"".$filter_mimetype."\"");

			if ($filter_source!="")
			$finalqry .= " AND source_text:(" . $filter_source . ")";
		}

		$params['fq'] = $fq;

		$collection = "";
		if (!empty($filter_collection)) {
			for ($i=0; $i<count($filter_collection); $i++) {
				if ($collection!="")
				$collection .= " OR ";
				//$collection .= "collection:". strtolower($filter_collection[$i]) . " OR collection:" . strtoupper($filter_collection[$i]) ;
				$collection .= "collection:\"". strtolower($filter_collection[$i])."\"";
			}
		}

		if ($collection!="" && !$dismax) {
			$finalqry = $finalqry . " AND (" . $collection . ")";
		}

		$tag = "";
		if (!empty($filter_tag)) {
			for ($i=0; $i<count($filter_tag); $i++) {
				if ($tag!="")
				$tag .= " OR ";
				//$tag .= "tag:". strtolower($filter_tag[$i]) . " OR tag:" . strtoupper($filter_tag[$i]);
				$tag .= "tag:\"". strtolower($filter_tag[$i])."\"";

			}
		}

		if ($tag!="" && !$dismax) {
			$finalqry = $finalqry . " AND (" . $tag . ")";
		}

		//$finalqry .= $queryField . ':'
		//$finalqry = 'tagssrch:' . $qry . '^4 title:' . $qry . '^3 categoriessrch:' . $qry . '^1.2 text:' . $qry . '^1';

		if ($rss) {
			$params['sort'] = 'createtime desc';
		}
		else {
			if ($sort!='')
			$params['sort'] = $sort;
		}

		if ($filter_location_lat != "" && $filter_location_lng != "" && $filter_location_radius != "") {
			$finalqry = "{!spatial lat=" . $filter_location_lat . " long=" . $filter_location_lng . " radius=" . $filter_location_radius . " unit=km threadCount=2} " . $finalqry;
		}

		//$debug = true;
		if ($debug) {
			$params['debugQuery'] = 'true';
			$params['debug'] = 'query';
		} else {
			$params['debugQuery'] = 'false';
		}
		
		if (!$dismax) $finalqry = "{!q.op=AND}" . $finalqry;
	
		$response = $this->_solr->search( $finalqry, $offset, $count, $params);

		$sorl_response = new Solr_Response($response->getRawResponse(), $response->getHttpStatus());
		$q = $sorl_response->getRawResponse();
		
		if ($debug) echo $q;

		return $sorl_response;
	}
}

/*----------------------------------------------------------------
*
* Solr response
*
*----------------------------------------------------------------*/
class Solr_Response {

	private $_raw_response = null;
	private $_json_response = null;
	private $_http_status = null;
	private $_debug = false;

	/**
	 * Whether the raw response has been parsed
	 *
	 * @var boolean
	 */
	protected $_isParsed = false;

	/**
	 * Parsed representation of the data
	 *
	 * @var mixed
	 */
	protected $_parsedData;


	// private constructor function
	// to prevent external instantiation
	public function __construct($rawResponse, $httpStatus) {
		$this->_raw_response = $rawResponse;
		$this->_http_status = $httpStatus;
	}

	public function getRawResponse() {
		return $this->_raw_response;
	}

	public function getJsonResponse() {
		if ($this->_raw_response == null)
		return null;
		if ($this->_json_response == null)
		$this->_json_response = json_decode($this->_raw_response);

		return $this->_json_response;
	}

	public function getHttpStatus() {
		return $this->_http_status;
		$this->_response->getHttpStatus();
	}

	/**
	 * Magic get to expose the parsed data and to lazily load it
	 *
	 * @param string $key
	 * @return mixed
	 */
	public function __get($key)
	{
		if (!$this->_isParsed)
		{
			$this->_parseData();
			$this->_isParsed = true;
		}

		if (isset($this->_parsedData->$key))
		{
			return $this->_parsedData->$key;
		}

		return null;
	}

	/**
	 * Magic function for isset function on parsed data
	 *
	 * @param string $key
	 * @return boolean
	 */
	public function __isset($key)
	{
		if (!$this->_isParsed)
		{
			$this->_parseData();
			$this->_isParsed = true;
		}

		return isset($this->_parsedData->$key);
	}

	/**
	 * Parse the raw response into the parsed_data array for access
	 *
	 * @throws Apache_Solr_ParserException If the data could not be parsed
	 */
	protected function _parseData()
	{
		$this->_parsedData = json_decode($this->_raw_response);
	}

}	
?>