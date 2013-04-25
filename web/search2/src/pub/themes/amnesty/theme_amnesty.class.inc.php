<?php
class Theme extends ThemeBase implements iTheme {

	private $aCountries;

	function __construct($config, $locale, $name) {
		parent::__construct($config, $locale, $name);

		$handle = fopen(dirname(__FILE__) . "/ressources/code_countries.txt", "rb");
		$this->aCountries = array();
		while ($handle && !feof($handle) ) {
			$line = trim(fgets($handle));
			if ($line!="") {
				$parts = explode(';', $line);
				$key = trim(strtolower($parts[0]));
				//if (empty($aLexicons) || empty($field) || array_key_exists ( strtoupper($key) , $aLexicons[$field] )  || array_key_exists ( strtolower($key) , $aLexicons[$field] )) {
				$this->aCountries[trim(strtolower($parts[1]))] = strtolower($parts[0]);
				//if ($ucw) $aMapping[trim(strtolower($parts[0]))] = ucwords($aMapping[trim(strtolower($parts[0]))]);
				//} else {
				//	$i = 1;
				//}
			}
		}
		fclose($handle);
	}

	function getBootStrapPath() {
		return 'themes/' . $this->name . '/bootstrap/';
	}

	function getScope() {
		if (isset($_GET['scope']) && $_GET['scope']=='sections') return 'sections';
		$core = str_replace('/', '', $this->config->get("solr.corename.sections"));
		$i = strstr($_GET['solr_url'], $core);
		if (isset($_GET['solr_url']) && strstr($_GET['solr_url'], $core)!==false) return 'sections';
		return 'amnesty';
	}

	function getSolrFields() {
		return "publishtime,item_meta_ai_type_str,item_meta_ai_category_issue_str,item_meta_ai_category_region_str";
	}

	public function getSolrCore(){
		if ($this->getScope()=='amnesty') {
			$solr_corename = $this->config->get("solr.corename");
		} else {
			$solr_corename = $this->config->get("solr.corename.sections");
		}
		if ($solr_corename=="undefined") $solr_corename = "";
		return $solr_corename;
	}

	function generateHtmlStart() {

		$res =  '<!DOCTYPE html>' . "\n";
		$res .= '<html>' . "\n";
		$res .= '<head>' . "\n";
		$res .= '<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>' . "\n";
		$res .= '<meta http-equiv="Cache-Control" content="no-cache" />' . "\n";
		$res .= '<meta http-equiv="Pragma" content="no-cache" />' . "\n";
		$res .= '<meta http-equiv="Cache" content="no store" />' . "\n";
		$res .= '<meta http-equiv="Expires" content="0" />' . "\n";
		$res .= '<meta name="robots" content="index, nofollow" />' . "\n";

		$res .= '<script type="text/javascript" src="' . $this->getJsPath() . 'jquery-1.9.1.min.js"></script>' . "\n";
		$res .= '<script type="text/javascript" src="https://www.google.com/jsapi"></script>' . "\n";
		$res .= '<script type="text/javascript" src="js/jquery.ae.image.resize.min.js"></script>';
				
		/*
		 $res .= '<script type="text/javascript" src="js/jquery-1.7.1.min.js"></script>';
		$res .= '<script type="text/javascript" src="js/autocomplete.js"></script>';
		$res .= '<script type="text/javascript" src="js/jquery.blockUI.js"></script>';
		$res .= '<script type="text/javascript" src="js/jquery.ae.image.resize.min.js"></script>';

		$res .= '<link rel="stylesheet" type="text/css" href="css/reset.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'autocomplete.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'styles.css" media="screen" />';
		*/

		$res .= '<link rel="stylesheet" type="text/css" href="flags/flags.css" media="screen" />';

		$res .= '<!-- Bootstrap -->' . "\n";

		$res .= '<meta name="viewport" content="width=device-width, initial-scale=1.0">' . "\n";

		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getBootStrapPath() . 'css/bootstrap.css" media="screen" />' . "\n";
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getBootStrapPath() . 'css/bootstrap-responsive.css" media="screen" />' . "\n";

		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'amnesty.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'tagcloud.css" media="screen" />';

		$res .= <<<EOD
		<script type='text/javascript' src='https://www.google.com/jsapi'></script>
		<script type='text/javascript'>
  			google.load('visualization', '1', {'packages': ['geomap']});
		</script>
EOD;

		return $res;
	}

	function generateBodyEnd() {

		$res = '<script type="text/javascript" src="' . $this->getBootStrapPath() . 'js/bootstrap.min.js"></script>' . "\n";
		return $res;
	}

	function generateTop() {

		global $login, $user;

		$res  = '<div class="header">';
		$res .= '<div class="container">';
		$res .= '<div id="header" class="row">';
		$res .= '<div id="header_text" class="span11"><h1>' . $this->config->get("application.title") . '</h1></div>';
		$res .= '<div id="header_tools" class="span1">';

		switch($this->locale) {
			case 'en';
			$res .= '<a href="?locale=fr">Français</a>';
			break;
			default;
			$res .= '<a href="?locale=en">English</a>';
		}

		$res .= '</div>';
		$res .= '</div>';
		$res .= '</div>';
		$res .= '</div>';
		$res .= '</div>';

		return $res;
	}

	function generateFooter() {

		$res  = '<div id="footer" class="footer">';
		$res .= '<div class="container">';
		$res .= '<div class="row"><div class="span12">';
		$res .= '<p>';
		$res .= 'Copyright 2013 <a href="http://www.huridocs.org/" target="_blank">HURIDOCS</a> - ';
		$res .= 'Search by <a href="http://www.taligentia.com/" target="_blank">Taligentia</a> using <a href="http://lucene.apache.org/solr/" target="_blank">Solr</a> and <a href="http://www.crawl-anywhere.com/" target="_blank">Crawl Anywhere</a>';
		//$res .= ' - User interface by <a href="http://www.ketse.com/" target="_blank">Ketse.com</a>';
		$res .= '</p>';
		$res .= '</div>';
		$res .= '</div>';
		$res .= '</div>';

		return $res;
	}

	function useTwitterBootstrap() {
		return true;
	}

	public function generateBody() {

		$res = '';

		$res .= $this->generateTop();

		$res .= <<<EOD
				<div class="container main_body">
EOD;


		//		<div class='clear'></div>
		$res .= <<<EOD
				<div id="search">
EOD;

		$res .= $this->drawSearchForm();

		//			<div class='clear'></div>
		$res .= <<<EOD
				</div>
				<div id="result"></div>
EOD;
			

		$res .= <<<EOD
					<div id="push"></div>
				</div>
EOD;

		$res .= $this->generateFooter();

		$res .= $this->generateBodyEnd();

		return $res;
	}

	function drawSearchForm() {
		global $config;

		global $usecollections;
		global $usetags;
		global $usetagcloud;
		global $usesourcename;
		global $uselanguage;
		global $usecountry;
		global $usecontenttype;
		global $useadvanced;

		global $aLanguages, $aCountries, $aContentType, $aLanguagesStemmed;

		global $solrMainContentLanguage;

		$defaultQueryLanguage = $config->getDefault("search.default_query_language", "fr");
		$language_stemming_by_default = $config->get("search.language_stemming_by_default");

		$core = isset($_SESSION["core"]) ? $uuid = $_SESSION["core"] : '';
		$config_file = isset($_SESSION["config"]) ? $uuid = $_SESSION["config"] : '';

		$res = <<<EOD

		<script type='text/javascript'>
  			function setLang(id, text) {
  				$('#search_querylanguage').val(id);
  				$('#search_language_button').html(text);
			}
		
			function switchScope(scope) {
				var url = window.location.href;
				hash = url.indexOf(url, '#');
				if (hash!=-1) url = url.substring(0,hash-1);
				url = url.replace(/(scope=).*?(&)/,'$1' + scope + '$2');
				url = url.replace(/(scope=).*?$/,'$1' + scope);
				if (!url.match(/(scope=)/)) {
					if (!url.match(/\?/)) {
						url += '?scope=' + scope;
					} else {
						url += '&scope=' + scope;
					}
				}
				window.location.href = url;
			}
		
			function resetPage() {
				window.location.href = window.location.href;
			}
			
			function showAll(facet) {
				$('table[id^="' + facet + '"]').show();
				$('#showall_facets_' + facet).hide();
			}
		
		</script>

		<form name="search_form" id="search_form" action="">
			<input type="hidden" name="config_save" id="config_save" value="$config_file">
			<input type="hidden" name="core_save" id="core_save" value="$core">
			<input type="hidden" name="action" id="action" value="search">
			<input type="hidden" name="page" id="page">
			<input type="hidden" name="fq" id="fq">
			<input type="hidden" name="mode" id="mode" value="simple">
			<input type="hidden" name="search_itemperpage" id="search_itemperpage" value="20">
			<input type="hidden" name="search_word_variations" id="search_word_variations" value="$language_stemming_by_default">
			<input type="hidden" name="bookmark_tag" id="bookmark_tag" value="">
			<input type="hidden" name="bookmark_collection" id="bookmark_collection" value="">
			<input type="hidden" name="search_querylanguage" id="search_querylanguage" value="$defaultQueryLanguage">
			<input type="hidden" name="search_country" id="search_country" value="">
			<input type="hidden" name="search_mimetype" id="search_mimetype" value="">
			<input type="hidden" name="search_org" id="search_org" value="">

			<div class='row spacer'>
				<div class='span8'>
					<div class='btn-group' data-toggle='buttons-radio'>
EOD;
			
		if ($this->getScope()=='amnesty') {

			$res .= <<<EOD
  						<button type='button' class='btn btn-primary active'>Amnesty International</button>
  						<button type='button' class='btn' onClick='switchScope("sections")'>Sections National</button>
EOD;
		} else {
			$res .= <<<EOD
  						<button type='button' class='btn' onClick='switchScope("amnesty")'>Amnesty International</button>
  						<button type='button' class='btn btn-primary active'>Sections National</button>
EOD;
		}

		$res .= <<<EOD
  						</div>
				</div>
			</div>
			
			<div class='row spacer_small'>
				<div class='span8 input-prepend input-append'>
						<div class='btn-group'>
				
EOD;

		$res .= "<a class='btn btn-custom' href='#'><i class='icon-edit icon-white'></i> <span id='search_language_button'>" . $aLanguages[$defaultQueryLanguage] . "</span></a>";

		$res .= <<<EOD
  						<a class='btn btn-custom dropdown-toggle' data-toggle='dropdown' href='#'><span class='caret'></span></a>
  						<ul class='dropdown-menu'>
EOD;

		foreach ($aLanguages as $lang_id => $lang_text) {
			$res .= "<li><a onclick='setLang(\"" . $lang_id . "\",\"" . $lang_text . "\")'></i> " . $lang_text . "</a></li>";
		}

		$msg1 = dgettext ( "amnesty" , "Type your search");
		$msg2 = dgettext ( "amnesty" , "Reset");
		$search_label = _('Search');
		
		$res .= <<<EOD
    						</ul>
					<input id="search_crit" type="text" value="" name="search_crit" class="input-xxlarge" autocomplete="off"  placeholder="$msg1 ..."/>
					<a id="search_do" type="submit" name="search_do" class="btn btn-primary">$search_label</a>
					<a id="reset" type="button" name="reset" class="btn btn-custom" onClick="resetPage()">$msg2</a>
				</div>
				</div>
				<div class="span4"></div>
			</div>
EOD;


		if ($usetagcloud) {
			$res .= <<<EOD
			<div class='row'>
			<div id="tagcloud" class='span11'></div>
			</div>
EOD;
		}

		$res .= <<<EOD
				</form>
EOD;
		return $res;
	}

	public function generateResults() {

		Global $response, $search_bookmark, $item_per_page, $page, $fqstr, $fqitms, $crit, $querylang, $queryField, $groupsize, $groupdisplaysize, $item_per_page_option, $facetextra, $facetqueries, $facet_union, $resultshowpermalink, $resultshowrss, $initialSearch, $aContentType, $aLanguages, $facetlimit;

		$res = "";

		if ($groupsize>0) {
			$numFound = $response->grouped->sourceid->matches;
			$numFoundPaginate = $response->grouped->sourceid->ngroups;
		}
		else {
			$numFound = $response->response->numFound;
			$numFoundPaginate = $response->response->numFound;
		}

		$teasers = get_object_vars($response->highlighting);

		/*
		 if ($this->getScope()!='amnesty') {

		$res .= <<<EOD

		<script type='text/javascript'>

		google.load('visualization', '1', {'packages': ['geochart']});
		google.setOnLoadCallback(drawRegionsMap);
		var data = google.visualization.arrayToDataTable([['Country', 'Popularity'],['Germany', 200],['United States', 300],['Brazil', 400],['Canada', 500],['France', 600],['RU', 700]]);
		function drawRegionsMap() {
		var options = {};
		var chart = new google.visualization.GeoChart(document.getElementById('chart_div'));
		chart.draw(data, options);
		};
			
		</script>
		<div id="chart_div" style="width: 900px; height: 500px;"></div>
		EOD;

		}
		*/
		//
		// results summary
		//
		$res .=	"<div id='result_summary'>";
		$res .= "<div class='row'><div class='span9'>";
		$res .=	"<p class='count'><span><b>" . $numFound . "</b> " . _('document(s) match your query') . "</span></p></div><div class='span3'>";

		if ($resultshowpermalink || $resultshowrss) {
			$res .= "<p class='subscribe'>";
			/*
			 if ($resultshowpermalink) {
			$res .= "<a onClick='doBookmark(this); return false;' href='" . $search_bookmark . "' target='_blank'>Bookmark&nbsp;<img src='images/bookmark.png' title='" . _('Bookmark link to this search') . "' height='20' width='20'></a>";
			}
			if ($resultshowpermalink && $resultshowrss) {
			$res .= '&nbsp;&nbsp;';
			}
			*/
			if ($resultshowrss)
				$res .= "<a href='rss.php" . $search_bookmark . "' target='_blank'><img src='images/rss.png' title='Bookmark link to this search as a RSS feed' height='20' width='20'></a>";
			$res .= "</p>";
		}
		$res .= '</div>';

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
				$res .= "<br /><p class='didyoumean'>" . _('Did you mean') . " : <i><a href='javascript:void(0);' onClick='doDidYouMeanSearch(\"" . $res2 . "\");'>";
				$res .= $res2 . "</i></a></p>";
			}
		}

		$res .= "</div></div>";

		//
		// results data
		//
		$res .= '<div id="result_container" class="row">';

		$res .= "<div id='result_content' class='span9'>";

		if ( $numFound > 0 ) {

			$res .= '<div class="pagination">';

			$res .= "
					<script language='javascript'>
					function changeItemPerPage() {
					    $('#search_itemperpage').val($('#results_itemperpage').val());
					    doSearch(0, \"\", \"\", \"\", false, \"\", \"\");
		            }
					</script>
					";
			$res .= $this->displayPagination ($numFoundPaginate, $item_per_page, $page, $fqstr );
			$res .= "</div>";
			/*
			 $res .= "<div><select id='results_itemperpage' name='results_itemperpage' onChange='changeItemPerPage();' class='span1'>";
			$item_per_page_values="10,20,50,100";
			$aValues = explode(",",$item_per_page_values);
			foreach($aValues as $value){
			$res .= "<option value='" . $value . "'";
			if ($item_per_page_option==$value)
				$res .= " selected";
			$res .= ">" . $value . "</option>";
			}
			$res .= "</select>&nbsp;". _('Items per page');
			$res .= '</div>';
			*/
			$res .= '<div id="result_items">';

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
						$res2 .= $this->buildDocBloc($doc, $crit, $querylang, $teasers, $queryField);
						$countdisplay++;
					}
					if ($countdisplay>$groupdisplaysize) {
						$parseUrl = parse_url($doc->id);
						$homeUrl = $parseUrl["host"];
						$tmp = "<div id='all_" . $groupid . "'><strong>[>>>]</strong>&nbsp;<a href='#' onclick='doSearch(1, \"" . "source_str:%22" . $doc->source_str . "%22\", \"\", \"\", false);'>All results from " . $homeUrl . "</a></div>";
						$res2 .= $tmp;
						$res2 .= "</div>";
					}
					$res2 .= "</div>";
				}
			}
			else {
				foreach ( $response->response->docs as $doc ) {
					$res2 .= $this->buildDocBloc($doc, $crit, $querylang, $teasers, $queryField);
				}
			}
			$res .= $res2;
			$res .= '</div>';
			$res .= '<div class="pagination">';
			$res .= $this->displayPagination ($numFoundPaginate, $item_per_page, $page, $fqstr );
			$res .= '</div>';
		}
		$res .= '</div>';


		// facet
		$res .= '<div id="result_facet" class="span3"><br/><br/>';

		if ($facet_union) {
			// TODO : http://wiki.apache.org/solr/SimpleFacetParameters#Multi-Select_Faceting_and_LocalParams
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

		if (!empty($fqitms) && count($fqitms)>0 && !empty($fqitms[0])) {
			$res .= '<div id="facet_selected"><h4 class="hand" onClick="$(\'#facet_selected_data\').toggle(400);">Active filters</h4>';
			$res .= '<div id="facet_selected_data">';
				
			$count=0;
			foreach ($fqitms as $fqitm) {
				$count++;
				$fqitm_parts = explode(':', $fqitm);
				$fqitm_parts[1] = substr($fqitm_parts[1], 1, strlen($fqitm_parts[1])-2);

				
				if ($fqitm_parts[0]=='publishtime') {
					$crit_fq = $fqitm_parts[0] . ":[" . urlencode($fqitm_parts[1]) . "]";
				} else {
					$crit_fq = $fqitm_parts[0] . ":" . urlencode('"' . $fqitm_parts[1] . '"');
				}
				
				$chk_id = 'facet_active_' . $fqitm_parts[0] . '_' . $count;
				
				$facetval = '';
				if ($fqitm_parts[0]=="language"){
					$facetval = $this->getLabelFromCode($aLanguages, $fqitm_parts[1]);
				}
				if ($fqitm_parts[0]=="country") {
					$facetval = $this->getLabelFromCode($aCountries, $fqitm_parts[1]);
				}
				if ($fqitm_parts[0]=="contenttyperoot") {
					$facetval = $this->getLabelFromCode($aContentType, $fqitm_parts[1]);
				}

				if (empty($facetval)) {
					$facetval = $fqitm_parts[1];
					
					if ($fqitm_parts[0]=='publishtime') {
						foreach($facetqueries as $facetfield => $query) {
						foreach($query['conditions'] as $c) {
							if ($facetval==$c['condition']) {
								$facetval=$c['mnemo'];
								break;
							}
						}
						}
					}
					
					$facetval2 = dgettext ( "amnesty" , $facetval);
					if ($facetval2 == $facetval) {
						$facetval = ucfirst($facetval);
						$facetval = str_replace("_", " ", $facetval);
					} else {
						$facetval = $facetval2;
					}
				}

				//$crit_fq = $facetfield . ":[" . urlencode($facetcrit) . "]";
						
				$res .= '<table style="width:100%;">';
				$res .= '<tr><td valign="top" style="width:20px;"><input type="checkbox" id="' . $chk_id . '" checked onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', true);">&nbsp;</td>';
				$res .= '<td><a href="#" class="facet_link" onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', false);">' . $facetval . '</a></td></tr></table>';
			}

			$chk_id = 'facet_active_all';
			//$res .= '<table style="width:100%;">';
			//$res .= '<tr><td valign="top" style="width:20px;"><input type="checkbox" id="' . $chk_id . '" checked onclick="doSearch(1, \'\', \'\', \'' . $chk_id . '\', true);">&nbsp;</td>';
			//$res .= '<td><a href="#" class="facet_link" onclick="doSearch(1, \'\', \'\', \'' . $chk_id . '\', false);">All</a></td></tr></table>';
			$res .= '</div>';
			$res .= '<div class="mt10 clrfix" id="facet_active_all"><a data-alias="s" class="more2 gdash showAll_facets" href="#" onclick="doSearch(1, \'\', \'\', \'' . $chk_id . '\', false);"><span>' . dgettext ( "amnesty" , 'Reset all') . '</span></a></div>';
			$res .= '</div>';
		}

		if ($facet_counts) {

			/*
			 * facet_fields
			*/

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
						$label2 = _("Subject");
						break;
					case "country":
						$label2 = _("Country");
						break;
					case 'language':
						$label2 = _("Language");
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
					$label2 = $this->getFacetLabel($facetfield, $facetextra);
					$label2 = dgettext ( "amnesty" , trim($label2));
				}

				$res .= '<div class="spacer_small" id="facet_'. $facetfield . '"><h4 class="hand" onClick="$(\'#facet_'. $facetfield . '_data\').toggle(400);">' . $label2. '</h4>';
				$res .= '<div id="facet_'. $facetfield . '_data" class="facet_scroll">';
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
						$facetval = $this->getLabelFromCode($aLanguages, $facetval);
					}
					if ($facetfield=="country")
					{
						$facetval = $this->getLabelFromCode($aCountries, $facetval);
					}
					if ($facetfield=="contenttyperoot")
					{
						//if ($facetval=="text/plain") $facetval = "";  // bug to be fix in crawler / indexer ???
						$facetval = $this->getLabelFromCode($aContentType, $facetval);
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

					if ($facetval!="") {
						$facetval2 = dgettext ( "amnesty" , $facetval);
						if ($facetval2 == $facetval) {
							$facetval2 = ucfirst($facetval);
							$facetval2 = str_replace("_", " ", $facetval2);
						}

						if ($facetlimit==0 && $count>10) {
							$res .= '<table id="' . $chk_id . '" style="display:none; width:100%;">';
						} else {
							$res .= '<table id="' . $chk_id . '" style="width:100%;">';
						}

						$res .= '<tr class="facet_line"><td valign="top" style="width:20px;"><input type="checkbox" id="' . $chk_id . '" ' . $checked . ' onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', true);">&nbsp;</td>';
						$res .= '<td><a href="#" class="facet_link" onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', false);">' . $facetval2 . '</a> <span class="facet_count">' . $facetcnt . '</span></td></tr></table>';
					}
				}
				$res .= '</div>';

				if ($facetlimit==0 && $count>10) {
					$res .= '<div class="mt10 clrfix" id="showall_facets_' . $facetfield . '"><a data-alias="s" class="more2 gdash showAll_facets" href="#" onClick="showAll(\'' . $facetfield . '\');"><span>' . dgettext ( "amnesty" , 'Show all') . '</span></a></div>';
				}
				$res .= '</div>';
			}
		}

		/*
		 * facet_queries
		*/
		if (!empty($facetqueries) && $this->getScope()=='amnesty') {
			foreach($facetqueries as $facetfield => $query) {

				$label = $facetfield;
				$label2 = dgettext ( "amnesty" , $label);

				$res .= '<div class="spacer_small" id="facet_'. $facetfield . '"><h4 class="hand" onClick="$(\'#facet_'. $facetfield . '_data\').toggle(400);">' . $label2. '</h4>';
				$res .= '<div id="facet_'. $facetfield . '_data">';
				$count=0;
				foreach($query['conditions'] as $c) {

					$count += 1;
					$facetval = '';

					foreach ($facet_counts->facet_queries as $facet_query => $facet_count) {
						if ($facet_query == $facetfield . ':[' . $c['condition'] . ']') {
							$facetval = $c['mnemo'];
							$facetcnt = $facet_count;
							$facetcrit = $c['condition'];
						}
					}

					if (!empty($facetval)) {
						$chk_id = $facetfield . $count;
						$checked = "";
						$crit_fq = $facetfield . ":[" . urlencode($facetcrit) . "]";
						if (in_array($facetfield . ":[" . $facetcrit . "]",$fqitms))
							$checked = " checked ";

						$facetval = dgettext ( "amnesty" , $facetval);

						$res .= '<table style="width:100%;"><tr class="facet_line"><td valign="top" style="width:20px;"><input type="checkbox" id="' . $chk_id . '" ' . $checked . ' onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', true);">&nbsp;</td>';
						$res .= '<td><a href="#" class="facet_link" onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', false);">' . $facetval . '</a>  <span class="facet_count">' . $facetcnt . '</span></td></tr></table>';
					}
				}
				$res .= '</div></div>';
			}
		}

		$res .= "</div>"; // end facet

		$res .= "</div>"; // row

		//$res .= '<div class="clear"></div>';

		// 				if ($groupsize>0) {
		// 					$res .= '<div>';
		// 					foreach ($response->clusters as $cluster) {
		// 						$res .= $cluster->labels[0] . '<br/>';
		// 					}
		// 					$res .= '</div>';
		// 				}

		//if ($this->debug)
		//$res .= '<pre>' . $response->getRawResponse() . '</pre>';

		return $res;
	}

	function displayPagination ($totalhits, $pagesize, $page, $fqstr ) {

		$itemarround = 4;

		if ($totalhits<=$pagesize)
			return "";

		$res = "<div class='pagination pagination-centered'><ul>";

		$firstpage = 1;
		$lastpage = intval(abs((($totalhits-1) / $pagesize) + 1));

		$ndxstart = (($page-1) * $pagesize) + 1;
		$ndxstop = $page * $pagesize;
		if ($ndxstop > $totalhits)
			$ndxstop = $totalhits;

		if ($lastpage > 1) {
			if ($page != 1)
				$res .= '<li><a href="#" onclick="doSearch(' . ($page-1) . ', \'\', \'' . $fqstr . '\', \'\', false);"><b><<</b></a></li>';

			for ($i=$firstpage; $i<=$lastpage; $i++) {
				if ($i!=$firstpage)
					$res .= ' ';

				if ($i==$page) {
					$res .= '<li><a class="active" href="#" onclick="doSearch(' . $i . ', \'\', \'' . $fqstr . '\', \'\', false);">' . $i . '</a></li>';
				} else {
					if ($i <= 1 || ($i > ($page - $itemarround) && $i < ($page + $itemarround)) || $i > ($lastpage - 1))
						$res .= '<li><a href="#" onclick="doSearch(' . $i . ', \'\', \'' . $fqstr . '\', \'\', false);"><b>' . $i . '</b></a></li>';
					else
					if ($i == ($page - $itemarround) || $i == ($page + $itemarround))
						$res .= '<li class="disabled"><a href="#">...</a></li>';
				}
			}
			if ($page != $lastpage)
				$res .= '<li><a href="#" onclick="doSearch(' . ($page+1) . ', \'\', \'' . $fqstr . '\', \'\', false);"><b>>></b></a></li>';

		} else {
			$res .= '&nbsp; ';
		}
		$res .= '</ul></div>';

		return $res;
	}

	function buildDocBloc($doc, $query, $querylang, $teasers, $queryField) {

		global $aContentTypeImage, $resultshowsource, $resultshowmeta, $results_img_height, $results_img_width, $config;
			
		$res2 = "<dl>";

		$flag = '';
		if ($this->getScope()=='amnesty') {
			if (!empty($this->aCountries[strtolower($doc->item_meta_ai_category_region_str)])) {
				$flag = "<img rel='tooltip' class='flag flag-" . $this->aCountries[strtolower($doc->item_meta_ai_category_region_str)] . "' title='" . ucwords(strtolower($doc->item_meta_ai_category_region_str)) . "' src='flags/blank.gif' style='width: 16px; height: 11px;'>";
			} else {
				$flag = "<img rel='tooltip' class='flag flag-xx' title='' src='flags/blank.gif' style='width: 16px; height: 11px;'>";
			}
			$flag .= "&nbsp;";
		}

		$res2 .= "<dt>" . $flag . "<a href='$doc->id' target='_blank'><span class='doc_title'>" . $doc->title_dis . "</span>";
		$t = $doc->contenttyperoot;
		$img = $this->getImageNameForContentType($aContentTypeImage, $t);
		if ($img!="")
			$res2 .= "&nbsp;<img src='images/" . $img . "' border='0'>";
		$res2 .= "</a>";

		if ($t=='text/html' && $solr_host = $config->getDefault("results.showfastread", "0")) {
			$res2 .= '<a class="ovalbutton" href="javascript:void(0);" onclick="doReader(\'' . $doc->uid . '\', \'' . $query . '\', \'' . $querylang . '\');"><span>' . _('Reader') . '</span></a>';
		}
		$res2 .= "</dt>";

		$summary = '';
		if (isset($teasers)) {
			$docid = strval($doc->id);
			$docteaser = get_object_vars($teasers[$docid]);
			if (isset($docteaser[$queryField])) {
				$summary = "";
				foreach($docteaser[$queryField] as $value) {
					if ($summary!="") $summary .= "...";
					$value = preg_replace("/^\\[[a-zA-Z]{2}\\]/", "", $value);
					$summary .= $value;
				}
			}
		}

		if ($summary=='')
			$summary = $doc->summary;

		$res2 .= "<dd class='doc_margin_left'><span class='doc_summary'>" . $summary . "</span></dd>";

		if ($results_img_height>0 && $results_img_width>0 && !empty($doc->urlimage_str) && !endsWith($doc->urlimage_str,'.gif',false)) {
			$res2 .= "<dd><a href='" . $doc->urlimage_str . "' target='image'><img class='resizeme' src='" . $doc->urlimage_str . "'></a></dd>";
		}

		if ($resultshowsource) {
			if (startsWith($doc->id,'http://',false) || startsWith($doc->id,'https://',false)) {
				$parseUrl = parse_url($doc->id);
				$homeUrl = $parseUrl["scheme"] . "://" . $parseUrl["host"];
				$res2 .= "<dd><span class='mnemo'>Source&nbsp;:</span>&nbsp;<a href='$homeUrl' target='_blank'>$doc->source_str</a></dd>";
			}
		}
		if ($resultshowmeta) {
			$res2 .= "<dd><ul class='doc_ul'>";
			if ($this->getScope()=='amnesty') {
				$res2 .= "<li class='doc_ul doc_lb doc_g'>&bull;&nbsp;" . $this->getHumanDate($doc->publishtime, 'd/m/Y') . "</li>&nbsp;&nbsp;";
				$t = $doc->item_meta_ai_type_str;
				if (!empty($t)) {
					$t2 = dgettext ( "amnesty" , $t);
					if ($t2 == $t) {
						$t2 = ucfirst($t);
						$t2 = str_replace("_", " ", $t2);
					}
					$res2 .= "<li class='doc_ul doc_lb doc_g'>&bull;&nbsp;" . $t2 . "</li>&nbsp;&nbsp;";
				}
				$t = $doc->item_meta_ai_category_issue_str;
				if (!empty($t)) {
					$t2 = dgettext ( "amnesty" , $t);
					if ($t2 == $t) {
						$t2 = ucfirst($t);
						$t2 = str_replace("_", " ", $t2);
					}
					$res2 .= "<li class='doc_ul doc_lb doc_g'>&bull;&nbsp;" . $t2 . "</li>&nbsp;&nbsp;";
				}
				$t = $doc->item_meta_ai_category_region_str;
				if (!empty($t)) {
					$res2 .= "<li class='doc_ul doc_lb doc_g'>&bull;&nbsp;" . $t . "</li>&nbsp;&nbsp;";
				}
			} else {
				$res2 .= "<li class='doc_ul doc_lb doc_g'>&bull;&nbsp;" . $doc->source_str . "</li>&nbsp;&nbsp;";
			}
			$res2 .= "</ul></dd>";
		}


		/*
		 if ($resultshowmeta) {
		$res2 .= "<dd>&nbsp;</dd>";
		$res2 .= "<dd>";
		$res2 .= "<span class='mnemo'>Date&nbsp;:</span>&nbsp;" . $this->getHumanDate($doc->publishtime, 'd/m/Y');
		$res2 .= "</dd>";
		$t = $doc->item_meta_ai_type_str;
		if (!empty($t)) {
		$t2 = dgettext ( "amnesty" , $t);
		if ($t2 == $t) {
		$t2 = ucfirst($t);
		$t2 = str_replace("_", " ", $t2);
		}
		$res2 .= "<dd>";
		$res2 .= "<span class='mnemo'>Type&nbsp;:</span>&nbsp;" . $t2 . "</td><td width='38%'>";
		$res2 .= "</dd>";
		}
		$t = $doc->item_meta_ai_category_issue_str;
		if (!empty($t)) {
		$t2 = dgettext ( "amnesty" , $t);
		if ($t2 == $t) {
		$t2 = ucfirst($t);
		$t2 = str_replace("_", " ", $t2);
		}
		$res2 .= "<dd>";
		$res2 .= "<span class='mnemo'>Issue&nbsp;:</span>&nbsp;" . $t2 . "</td><td width='38%'>";
		$res2 .= "</dd>";
		}
		$t = $doc->item_meta_ai_category_region_str;
		if (!empty($t)) {
		$res2 .= "<dd>";
		$res2 .= "<span class='mnemo'>Region&nbsp;:</span>&nbsp;" . $t;
		$res2 .= "</dd>";
		}
		}
		*/
		$res2 .= "</dl>";

		return $res2;
	}

	public function getFacet($default_facets) {
		if ($this->getScope()=='amnesty') return 'language,contenttype';
		return $default_facets;
	}
	public function getFacetExtra($default_facets) {
		if ($this->getScope()=='amnesty') return $default_facets;
		return '';
	}
	public function getSolrQuery($default_facets) {
		return $default_facets;
	}
}
?>
