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
				$this->aCountries[trim(strtolower($parts[1]))] = strtolower($parts[0]);
			}
		}
		fclose($handle);
	}

	function getBootStrapPath() {
		return 'themes/' . $this->name . '/bootstrap/';
	}

	function getSolrFields() {
		return "*.score";
	}

	public function getSolrCore(){
		$solr_corename = $this->config->get("solr.corename");
		if ($solr_corename=="undefined") $solr_corename = "";
		return $solr_corename;
	}

	function generateHtmlStart() {

		$res =  '<!DOCTYPE html>' . "\n";
		$res .= '<html>' . "\n";
		$res .= '<head>' . "\n";
				
		$res .= '<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>' . "\n";
		$res .= '<title>' . $this->config->get("application.title") . '</title>';
		$res .= '<meta http-equiv="Cache-Control" content="no-cache" />' . "\n";
		$res .= '<meta http-equiv="Pragma" content="no-cache" />' . "\n";
		$res .= '<meta http-equiv="Cache" content="no store" />' . "\n";
		$res .= '<meta http-equiv="Expires" content="0" />' . "\n";
		$res .= '<meta name="robots" content="index, nofollow" />' . "\n";

		$res .= '<script type="text/javascript" src="' . $this->getJsPath() . 'jquery-1.9.1.min.js"></script>' . "\n";
		
		// TODO: not working anymore with IE11
		// http://stackoverflow.com/questions/14923301/uncaught-typeerror-cannot-read-property-msie-of-undefined-jquery-tools
		$res .= <<<EOD
		<script type='text/javascript'>
	 		$.browser = {};
			(function () {
				$.browser.msie = false;
				$.browser.version = 0;
				if (navigator.userAgent.match(/MSIE ([0-9]+)\./)) {
					$.browser.msie = true;
					$.browser.version = RegExp.$1;
				}
			})();
		</script>
EOD;

		$res .= '<script type="text/javascript" src="' . $this->getJsPath() . 'jquery.ae.image.resize.min.js"></script>';
		$res .= '<script type="text/javascript" src="' . $this->getJsPath() . 'jquery.blockUI-2.7.0.js"></script>';
		
		$res .= '<link rel="stylesheet" type="text/css" href="flags/flags.css" media="screen" />' . "\n";

		$res .= '<!-- Bootstrap -->' . "\n";
		$res .= '<meta name="viewport" content="width=device-width, initial-scale=1.0">' . "\n";
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getBootStrapPath() . 'css/bootstrap.css" media="screen" />' . "\n";
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getBootStrapPath() . 'css/bootstrap-responsive.css" media="screen" />' . "\n";

		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'styles.css" media="screen" />' . "\n";
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'tagcloud.css" media="screen" />' . "\n";
		
		return $res;
	}

	function generateBodyEnd() {

		$res = '';
		$res .= '<script type="text/javascript" src="' . $this->getBootStrapPath() . 'js/bootstrap.min.js"></script>' . "\n";
		return $res;
	}

	function generateTop() {

		global $login, $user;
		
		$logo = $this->getImagePath() . $this->config->get("logo.file");
		$title = $this->config->get("application.title");
		
		$res = <<<EOD
		
<div class="header">
	<div class="container">
		<div id="header" class="row">
			<div id="header_text" class="span1">
				<a href="/"><img src="$logo"></a>
			</div>
			<div id="header_text" class="span5"><h1>$title</h1></div>
			<div id="header_tools" class="span6">
				<p span class="h_tools">
EOD;

		switch($this->locale) {
			case 'en';
			$res .= '<a href="?locale=fr">Français</a>';
			break;
			default;
			$res .= '<a href="?locale=en">English</a>';
		}
		
		if ($login && $user->getName()!="") {
			$res .= '&nbsp;-&nbsp;<a href="javascript:void(0);" onclick="doLogout();">' . _("Logout") . '</a>';
		}
		
		$res .= <<<EOD
		
				</p>
			</div>
		</div>
	</div>
</div>				
				
EOD;

		return $res;
	}

	function generateFooter() {
		
		$footer = $this->config->get("application.footer");
		
		$res  = '<div id="footer" class="footer">';
		$res .= '<div class="container">';
		$res .= '<div class="row"><div class="span12">';
		$res .= '<p>';
		$res .= $footer;
		$res .= '</p>';
		$res .= '</div>';
		$res .= '</div>';
		$res .= '</div>';

		return $res;
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

		$res .= <<<EOD
		<div id="reader-dialog" style="display: none; cursor: default">
			<div id="reader-dialog-title"
			style="padding: 10px; text-align: left; background-color: #808080;"><?php echo _('Reader'); ?><input style="float: right;" type="button" onClick='$.unblockUI()' value="<?php echo _('Close'); ?>" /></div>
			<div id="reader-dialog-text" style="padding: 10px; text-align: left; overflow: auto; max-height: 400px;"></div>
		</div>
	
		<div id="preferences-dialog" style="display: none; cursor: default">
			<div id="preferences-dialog-title"
			style="padding: 10px; text-align: left; background-color: #808080;"><?php echo _('Preferences'); ?><input style="float: right;" type="button" onClick='preferencesCloseDialog();' value="<?php echo _('Close'); ?>" /></div>
			<div id="preferences-dialog-text" style="padding: 10px; text-align: left; overflow: auto; max-height: 400px;"></div>
		</div>
EOD;
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
			
				<div class='row spacer_small'>
					<div class='span8  input-append'>
						<div class='btn-group'>
				
EOD;

		$msg1 = dgettext ( "amnesty" , "Type your search");
		$msg2 = dgettext ( "amnesty" , "Reset");
		$search_label = _('Search');
		
		$res .= <<<EOD
		
							<input id="search_crit" type="text" value="" name="search_crit" class="search_input input-xxlarge" autocomplete="off"  placeholder="$msg1 ..."/>
							<a id="search_do" type="button" name="search_do" class="btn btn-primary">$search_label</a>
							<a id="reset" type="button" name="reset" class="btn btn-custom2" onClick="resetPage()">$msg2</a>
						</div>
					</div>		
				
					<div class='span4'>
									
EOD;

		if ($useadvanced) {
			$label1 = _('Advanced search');
			$label2 = _('Simple search');
		
			$res .= <<<EOD
		
						<span id="switch_advanced"><a href='javascript:void(0)' onClick='switchMode("advanced");'>$label1</a></span>
						<span id="switch_simple" style='display:none'><a href='javascript:void(0)' onClick='switchMode("simple");'>$label2</a></span>
					</div>
				</div>
EOD;
		}
		
		if ($usecollections) {
			$res .= <<<EOD
				
				<div class='row'>
					<div class='span8'>
						<div id="div_query_collections" class="option">
							<div id="div_query_collections_label" style="width:90px; float:left">
								<span class="label">Collections</span>
							</div>
							<div id="st_option" style="margin-left:100px;">
								<div id="div_query_collections_values"></div>
							</div>
						</div>
					</div>
				</div>
EOD;
		}
		
		if ($usetags) {
			$res .= <<<EOD
				
				<div class='row'>
					<div class='span8'>
						<div id="div_query_tags" class="option">
							<div id="div_query_tags_label" style="width:90px; float:left">
								<span class="label">Tags</span>
							</div>
							<div id="st_option" style="margin-left:100px;">
								<div id="div_query_tags_values"></div>
							</div>
						</div>
					</div>
				</div>	
EOD;
		}	

		if ($useadvanced) {
			$res .= <<<EOD
				
				<div id="search_advanced" style='display:none'>
					<div class='row spacer_tiny'>
						<div class='span8'>
				
EOD;
		
			if ($usesourcename) {
				$label = _('Source name');
		
				$res .= <<<EOD
				
							<div class="option">
								<div style="width:90px; float:left">$label</div>
								<div id="st_option" style="margin-left:100px;">
									<input id="search_org" type="text" value="" name="search_org" />
								</div>
							</div>
EOD;
				}
				else
					$res .= '<input type="hidden" name="search_org" id="search_org" value="">';
		
				if ($uselanguage) {
					$label1 = _('Language');
					$label2 = _('Any languages');
		
					$res .= <<<EOD
					
							<div class="option">
								<div style="width:90px; float:left">$label1</div>
								<div id="st_option" style="margin-left:100px;">
									<select name="search_language" id="search_language">
										<option value="">$label2</option>
		
EOD;
		
					foreach ($aLanguages as $key => $value) {
						$res .= "<option value='" . $key . "'>" . $value . "</option>";
					}
		
					$res .= <<<EOD
					
									</select>
								</div>
							</div>
EOD;
				}
				else
					$res .= '<input type="hidden" name="search_language" id="search_language" value="$defaultQueryLanguage">';
		
				if ($usecountry) {
					$label1 = _('Country');
					$label2 = _('Any countries');
		
					$res .= <<<EOD
					
							<div class="option">
								<div style="width:90px; float:left">$label1</div>
								<div id="st_option" style="margin-left:100px;">
									<select name="search_country" id="search_country">
										<option value="">$label2</option>
		
EOD;
		
					foreach ($aCountries as $key => $value) {
						$res .= "<option value='" . $key . "'>" . $value . "</option>";
					}
					$res .= <<<EOD
					
									</select>
								</div>
							</div>
EOD;
				}
				else
					$res .= '<input type="hidden" name="search_country" id="search_country" value="">';
		
				if ($usecontenttype) {
					$label1 = _('Format');
					$label2 = _('Any formats');
		
					$res .= <<<EOD
					
							<div class="option">
								<div style="width:90px; float:left">$label1</div>
								<div id="st_option" style="margin-left:100px;">
									<select name="search_mimetype" id="search_mimetype">
										<option value="">$label2</option>
		
EOD;
		
					foreach ($aContentType as $key => $value) {
						$res .= "<option value='" . $key . "'>" . $value . "</option>";
					}
		
					$res .= <<<EOD
					
									</select>
								</div>
							</div>
		
EOD;
				}
				else
					$res .= '<input type="hidden" name="search_mimetype" id="search_mimetype" value="">';
		
				$res .= <<<EOD
				
						</div>
					</div>
				</div>
EOD;
		} else {
			$res .= <<<EOD
			
					<input type="hidden" name="search_language" id="search_language" value="$defaultQueryLanguage">
					<input type="hidden" name="search_country" id="search_country" value="">
					<input type="hidden" name="search_mimetype" id="search_mimetype" value="">
					<input type="hidden" name="search_org" id="search_org" value="">	
EOD;
		} // useadvanced			
			
		if ($usetagcloud) {
			$res .= <<<EOD
			
			<div class='row'>
				<div id="tagcloud" class='span12'></div>
			</div>
EOD;
		}

		$res .= <<<EOD
		
				</form>				
EOD;
		
		return $res;
	}

	public function generateResults() {

		Global $response, $search_bookmark, $item_per_page, $page, $fqstr, $fqitms, $crit, $querylang, $queryField, $groupsize, $groupdisplaysize, $item_per_page_option, $facetextra, $facetqueries, $facet_union, $resultshowpermalink, $resultshowrss, $initialSearch, $aContentType, $aLanguages, $aCountries, $facetlimit;

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

		//
		// results summary
		//
		$res .=	"<div id='result_summary' class='row'>";
		$res .= "<div class='span12'>";
		$res .=	"<p class='count'><span><b>" . $numFound . "</b> " . _('document(s) match your query') . "</span>";

		if ($resultshowpermalink || $resultshowrss) {
			//$res .= "<p class='subscribe'>";
			/*
			 if ($resultshowpermalink) {
			$res .= "<a onClick='doBookmark(this); return false;' href='" . $search_bookmark . "' target='_blank'>Bookmark&nbsp;<img src='images/bookmark.png' title='" . _('Bookmark link to this search') . "' height='20' width='20'></a>";
			}
			if ($resultshowpermalink && $resultshowrss) {
			$res .= '&nbsp;&nbsp;';
			}
			*/
			if ($resultshowrss)
				$res .= "<span style='float: right'><a href='rss.php" . $search_bookmark . "' target='_blank'><img src='themes/ca/images/rss.png' title='Bookmark link to this search as a RSS feed' height='20' width='20'></a></span>";
			//$res .= "</p>";
		}
		$res .= '</p></div>';

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

		$res .= "</div>";

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
		$res .= '<div id="result_facet" class="span3">';

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
			$res .= '<div class="mt10 clrfix" id="facet_active_all"><a data-alias="s" class="more2 gdash showAll_facets" href="javascript:void(0);" onclick="doSearch(1, \'\', \'\', \'' . $chk_id . '\', false);"><span>' . dgettext ( "amnesty" , 'Reset all') . '</span></a></div>';
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
					$res .= '<div class="mt10 clrfix" id="showall_facets_' . $facetfield . '"><a data-alias="s" class="more2 gdash showAll_facets" href="javascript:void(0);" onClick="showAll(\'' . $facetfield . '\');"><span>' . dgettext ( "amnesty" , 'Show all') . '</span></a></div>';
				}
				$res .= '</div>';
			}
		}

		/*
		 * facet_queries
		*/
		if (!empty($facetqueries)) {
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

	public function getFacet($default_facets) {
		return $default_facets;
	}
	public function getFacetExtra($default_facets) {
		return $default_facets;
	}
	public function getSolrQuery($default_facets) {
		return $default_facets;
	}
}
?>
