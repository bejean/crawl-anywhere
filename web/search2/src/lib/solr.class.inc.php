<?php
/*----------------------------------------------------------------
 *
* Solr abstract class
*
*----------------------------------------------------------------*/
abstract class Solr_Abstract {

	protected $_debug = false;

	// Force Extending class to define this method
	abstract public function connect($solr_host, $solr_port, $solr_baseurl, $solr_corename);
	abstract public function ping();
	abstract public function system();
	abstract public function getFiedValues($field);
	abstract function query(
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
			$mode,
			$rss,
			$debug=false);

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