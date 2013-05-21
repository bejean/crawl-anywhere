<?php
interface iTheme {
	public function generateBody();
	public function generateHtmlStart();
	public function generateTop();
	public function generateFooter();
	public function generateBodyEnd();
	public function drawSearchForm();

	function getThemePath();
	function getImagePath();
	function getCssPath();
	function getJsPath();

	public function useTwitterBootstrap();
	public function getSolrFields();
	public function getSolrHost();
	public function getSolrPort();
	public function getSolrBaseUrl();
	public function getSolrCore();
	public function getSolrUrl();
	public function getFacet($default_facets);
	public function getFacetExtra($default_facets);
	public function getSolrQuery($default_facets);
	public function getParamExtra();
}

abstract class ThemeBase {
	protected $config;
	protected $locale;
	protected $name;
	protected $debug;

	function __construct($config, $locale, $name) {
		$this->config = $config;
		$this->locale = $locale;
		$this->name = $name;
		$this->debug = false;
	}

	function generateBodyEnd() {
		return "";
	}

	function getThemePath() {
		return 'themes/' . $this->name . '/';
	}

	function getImagePath() {
		return 'themes/' . $this->name . '/images/';
	}

	function getCssPath() {
		return 'themes/' . $this->name . '/css/';
	}

	function getJsPath() {
		return 'themes/' . $this->name . '/js/';
	}

	function useTwitterBootstrap() {
		return false;
	}
	
	function getSolrFields() {
		return "";
	}
	public function getSolrHost(){
		$solr_host = $this->config->get("solr.host");
		if ($solr_host=="undefined") $solr_host = "";
		return $solr_host;
	}
	public function getSolrPort(){
		$solr_port = $this->config->get("solr.port");
		if ($solr_port=="undefined") $solr_port = "";
		return $solr_port;
	}
	public function getSolrBaseUrl(){
		$solr_baseurl = $this->config->get("solr.baseurl");
		if ($solr_baseurl=="undefined") $solr_baseurl = "";
		return $solr_baseurl;
	}
	public function getSolrCore(){
		$solr_corename = $this->config->get("solr.corename");
		if ($solr_corename=="undefined") $solr_corename = "";
		return $solr_corename;
	}
	public function getSolrUrl() {
		$url = $this->getSolrHost();
		if ($this->getSolrPort()!='') {
			$url .= ':' . $this->getSolrPort();
		}
		if ($this->getSolrBaseUrl()!='') {
			$url .= '/' . $this->getSolrBaseUrl();
		}
		if ($this->getSolrCore()!='') {
			$url .= '/' . $this->getSolrCore();
		}
		$url = str_replace('//', '/', $url);
		return 'http://' . $url;
	}
	
	function setDebug($debug) {
		$this->debug = debug;
	}

	function buildDocBloc($doc, $query, $querylang, $teasers, $queryField) {

		global $aContentTypeImage, $resultshowsource, $resultshowmeta, $results_img_height, $results_img_width, $config, $aCountries, $aLanguages;
			
		$res2 = "<dl>";

		if ($doc->type_str=='dropbox') {
			//$id=urlencode($doc->id);
			$id=$doc->id;
			if (isset($doc->title_dis))
				$title = $doc->title_dis;
			if (empty($title)) {
				$title = $doc->id;
				if (strLastIndexOf("/", $title)!=-1) $title = substr($title, strLastIndexOf("/", $title));
			}

			$key = base64_encode ($doc->sourceid . "|" . $id);
			$res2 .= '<dt><a href="getdropbox.php?id=' . $key .'"  target="_blank">' . $title;

			//$res2 .= '<dt><a href="getdropbox.php?idsrc=' . $doc->sourceid . '&id=' . $id .'"  target="_blank">' . $title;
			//$res2 .= '<dt><a href="' . $doc->id . '"  target="_blank">' . $title;
		} else {
			$res2 .= "<dt><a href='$doc->id' target='_blank'>" . $doc->title_dis;
		}
		$t = $doc->contenttyperoot;
		$img = $this->getImageNameForContentType($aContentTypeImage, $t);
		if ($img!="")
			$res2 .= "&nbsp;<img src='images/" . $img . "' border='0'>";
		$res2 .= "</a>";

		if ($t=='text/html' && $solr_host = $config->getDefault("results.showfastread", "0")) {
			$res2 .= '<a class="ovalbutton" href="javascript:void(0);" onclick="doReader(\'' . $doc->uid . '\', \'' . $query . '\', \'' . $querylang . '\');"><span>' . _('Reader') . '</span></a>';
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

		if ($doc->type_str=='dropbox') {
			$res2 .= "<dd><span class='mnemo'>" . _("Path") . "&nbsp;:</span>&nbsp;" . $doc->id;
		}

		if ($resultshowsource) {
			if (startsWith($doc->id,'http://',false) || startsWith($doc->id,'https://',false)) {
				$parseUrl = parse_url($doc->id);
				$homeUrl = $parseUrl["scheme"] . "://" . $parseUrl["host"];
				$res2 .= "<dd><span class='mnemo'>Source&nbsp;:</span>&nbsp;<span class='value'><a href='$homeUrl' target='_blank'>$doc->source_str</a></span></dd>";
			}
			$res2 .= "<dd><span class='mnemo'>Date&nbsp;:</span>&nbsp;<span class='value'>" . $this->getHumanDate($doc->createtime) . "</span></dd>";
		}

		if ($resultshowmeta) {
			$res2 .= "<dd><table width='90%'><tr><td width='30%'>";
			if (count($doc->tag) > 1)
				$temp = implode(', ', $doc->tag);
			else
				$temp = $doc->tag;
			$res2 .= "<span class='mnemo'>Tags&nbsp;:</span>&nbsp;<span class='value'>" . $temp . "</span></td><td width='38%'>";

			$t = $doc->country;
			$t = $this->getLabelFromCode($aCountries, $t);
			$res2 .= "<span class='mnemo'>Country&nbsp;:</span>&nbsp;<span class='value'>" . $t . "</span></td><td width='38%'>";
			$t = $doc->language;
			$t = $this->getLabelFromCode($aLanguages, $t);
			$res2 .= "<span class='mnemo'>Language&nbsp;:</span>&nbsp;<span class='value'>" . $t;
			$res2 .= "</span></td></tr></table></dd>";
		}
		$res2 .= "</dl>";

		return $res2;
	}


	function buildOneDocDisplay($doc, $teasers, $queryField) {

		global $aContentTypeImage, $resultshowsource, $resultshowmeta;

		$res2 = "<dl>";

		$res2 .= "<dt><a href='$doc->id' target='_blank'>" . $doc->title_dis . "</a>";

		$summary = "";
		$content = $doc->content_ml;
		$content = preg_replace ('/^\[[a-z]{2}\]/', ' ', $content);
		$aContent = explode("\n", $content);
		foreach ($aContent as $line)  $summary .= "<p>" . $line . "</p>";

		//if (false) {

		$aMatches = Array();

		if (isset($teasers)) {
			$docid = strval($doc->id);
			$docteaser = get_object_vars($teasers[$docid]);
			if (isset($docteaser[$queryField]))
			{
				foreach($docteaser[$queryField] as $value)
				{
					$value = preg_replace("/^\\[[a-zA-Z]{2}\\]/", "", $value);
					$value = preg_replace("/^\\[\\<em\\>[a-zA-Z]{2}\\<\\/em\\>\\]/", "", $value);

					preg_match_all ( "/\\<em\\>([^<]*)\\<\\/em\\>/" , $value , $matches );
					foreach ($matches[1] as $match)
					if (!in_array(match,$aMatches)) $aMatches[] = $match;
				}
			}
			foreach ($aMatches as $match) {
				$pattern = "/\\b(" . $match .")\\b/i";
				$summary = preg_replace($pattern, "<b>$1</b>", $summary);
			}
		}

		$res2 .= "<dd>" . $summary . "</dd>";

		// 	if ($resultshowsource) {
		if (startsWith($doc->id,'http://',false) || startsWith($doc->id,'https://',false)) {
			$parseUrl = parse_url($doc->id);
			$homeUrl = $parseUrl["scheme"] . "://" . $parseUrl["host"];
			$res2 .= "<dd><span class='mnemo'>Source&nbsp;:</span>&nbsp;<a href='$homeUrl' target='_blank'>$doc->source_str</a></dd>";
		}
		$res2 .= "<dd><span class='mnemo'>Date&nbsp;:</span>&nbsp;" . $this->getHumanDate($doc->publishtime) . "</dd>";

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

	function getFacetLabel($facetfield, $facet) {
		if ($facet=='') return '';
		$aTemp = explode(',',$facet);
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

	function getLabelFromCode($search, $key) {
		if ($search=="") return "";
		if (array_key_exists  ( strtolower($key)  , $search ))
			return $search[strtolower($key)];
		return $key;
	}

	function getImageNameForContentType($search, $key) {
		if ($search=="") return "";
		if ($key=='text/html' || $key=='text/plain') return '';
		if (array_key_exists  ( $key  , $search ))
			return $search[$key];
		return "";
	}

	function getHumanDate($date, $format = 'd/m/Y H:i:s') {
		return date_format(date_create($date), $format);
		//2012-03-28T13:21:48Z
	}
	public function getFacet($default_facets) {
		return $default_facets;
	}
	public function getFacetExtra($default_facets) {
		return $default_facets;
	}
	public function getSolrQuery($default_facets) {
		return $default_facets;
	}
	public function getParamExtra() {
		return null;
	}
}
?>