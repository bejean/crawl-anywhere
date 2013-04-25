<?php
require_once("solr.class.inc.php");

require('Solarium/library/Solarium/Autoloader.php');
Solarium_Autoloader::register();


/*----------------------------------------------------------------
 *
* Solr class with API Solarium
*
*----------------------------------------------------------------*/
class Solr extends Solr_Abstract {

	private $_client = null;

	public function Solr_Solarium () {
	}

	public function connect($solr_host, $solr_port, $solr_baseurl, $solr_corename) {
		$config = array(
			    'adapteroptions' => array(
			        'host' => $solr_host,
			        'port' => $solr_port,
			        'path' => $solr_baseurl . '/' . $solr_corename,
		)
		);
		$this->_client = new Solarium_Client($config);
		return $this->ping();
	}

	public function ping() {
		if ($this->_client==null)
		return false;
		// create a ping query
		$ping = $this->_client->createPing();
		// execute the ping query
		try{
			$this->_client->ping($ping);
			return true;
		}catch(Solarium_Exception $e){
			return false;
		}
	}
	public function system() {
		return null;
	}

	public function getFiedValues($field) {

		if ($this->_client==null)
		return "";

		// get a select query instance
		$query = $this->_client->createSelect();

		// get the facetset component
		$facetSet = $query->getFacetSet();

		// create a facet field instance and set options
		$facet = $facetSet->createFacetField();
		$facet->setKey($field);
		$facet->setField($field);

		// add the facet instance to the facetset
		$facetSet->addFacet($facet);

		// this executes the query and returns the result
		$resultset = $this->_client->select($query);

		// display the total number of documents found by solr
		//echo 'NumFound: '.$resultset->getNumFound();

		// display facet counts
		//echo '<hr/>Facet counts for field "inStock":<br/>';
		$facet = $resultset->getFacetSet()->getFacet($field);
		foreach($facet as $value => $count) {
			if ($value!="") {
				if ($ret!="") $ret .= "|";
				$ret .= $value . ":" . $count;
			}
		}

		return $ret;
	}


	function query(
	$qry,
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
	$mode,
	$rss,
	$debug=false) {
		
		global $facetcollections, $facettags, $facetcountry, $facetlanguage, $facetcontenttype, $facetsourcename;
		
		$query = $this->_client->createSelect();
		
		if ($word_variations && $querylang!="") {
			$queryField = "content_mls";
		}
		else {
			$queryField = "content_ml";
		}
		
		if ($query_lang=="")
		$query_lang = "en";

		
		// Facets		
		if (!$rss) {
// 			$aFacet = Array();
// 			if ($facetcollections) array_push($aFacet, "collection");
// 			if ($facettags) array_push($aFacet, "tag");
// 			if ($facetsourcename) array_push($aFacet, "source_str");
// 			if ($facetcountry) array_push($aFacet, "country");
// 			if ($facetlanguage) array_push($aFacet, "language");
// 			if ($facetcontenttype) array_push($aFacet, "contenttyperoot");
		
// 			$params['facet'] = 'true';
// 			$params['facet.field'] = $aFacet;
// 			$params['facet.mincount'] = '1';
// 			$params['facet.limit'] = '10';
			
			// get the facetset component
			$facetSet = $query->getFacetSet();
			
			if ($facetcollections) {
				$facet = $facetSet->createFacetField();
				$facet->setKey('collection');
				$facet->setField('collection');
				$facetSet->addFacet($facet);
			}
			
			if ($facetcountry) {
				$facet = $facetSet->createFacetField();
				$facet->setKey('country');
				$facet->setField('country');
				$facetSet->addFacet($facet);
			}
			
		}
		
		
		
		

		
		
		$qry = trim($this->escapeQuery($qry));
		$finalqry = '\[' . $query_lang . '\]' . $qry;
		
		$query->setQuery($queryField . ':' . $finalqry);
		
		// this executes the query and returns the result
		$resultset = $this->_client->select($query);
		
		$sorl_response = new Solr_Response($resultset->getResponse()->getBody(), $resultset->getResponse()->getStatusCode());
		return $sorl_response;
	}

}

?>