<?php
class Theme extends ThemeBase implements iTheme {	
	
	function generateHtmlStart() {
		
		$res =  '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">' . "\n";
		$res .= '<html xmlns="http://www.w3.org/1999/xhtml">' . "\n";
		$res .= '<head>' . "\n";
		$res .= '<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>' . "\n";
		$res .= '<meta http-equiv="Cache-Control" content="no-cache" />' . "\n";
		$res .= '<meta http-equiv="Pragma" content="no-cache" />' . "\n";
		$res .= '<meta http-equiv="Cache" content="no store" />' . "\n";
		$res .= '<meta http-equiv="Expires" content="0" />' . "\n";
		$res .= '<meta name="robots" content="index, nofollow" />' . "\n";
		
		$res .= '<script type="text/javascript" src="js/jquery-1.7.1.min.js"></script>' . "\n";
		$res .= '<script type="text/javascript" src="js/autocomplete.js"></script>' . "\n";
		$res .= '<script type="text/javascript" src="js/jquery.blockUI.js"></script>' . "\n";
		$res .= '<script type="text/javascript" src="js/jquery.ae.image.resize.min.js"></script>' . "\n";
		
		$res .= '<link rel="stylesheet" type="text/css" href="css/reset.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'layout.css" media="screen" />' . "\n";
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'autocomplete.css" media="screen" />' . "\n";
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'styles.css" media="screen" />' . "\n";
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'tagcloud.css" media="screen" />' . "\n";
		
		return $res;
	}
	
	function generateTop() {
		
		global $login, $user;
		
		$res =  '<div id="header">';
		$res .= '<div id="header_logo"><img src="' . $this->getImagePath() . $this->config->get("logo.file") . '"></div>';
		$res .= '<div id="header_text"><span>' . $this->config->get("application.title") . '</span></div>';
		$res .= '<div id="header_tools">';
		
		switch($this->locale) {
			case 'en';
			$res .= '<a href="?locale=fr">Français</a>';
			break;
			default;
			$res .= '<a href="?locale=en">English</a>';
		}
		
		$res .= '&nbsp;-&nbsp;<a href="javascript:void(0);" onclick="doPreferences();">' . _("Preferences") . '</a>';

		if ($login && $user->getName()!="") {
			$res .= '&nbsp;-&nbsp;<a href="javascript:void(0);" onclick="doLogout();">' . _("Logout") . '</a>';
		}
		
		$res .= '</div>';
		$res .= '</div>';
				
		return $res;
	}

	function generateFooter() {
		
		$footer = $this->config->get("application.footer");
		
		$res =  '<div id="footer">';
		$res .= '<p>' . $footer . '</p>';
		$res .= '</div>';
		
		return $res;
	}
		
	public function generateBody() {
		$res = <<<EOD
			<div id="wrapper">
EOD;
		$res .= $this->generateTop();
	
		$res .= <<<EOD
				<div class='clear'></div>
				<div id="search">
EOD;
	
		$res .= $this->drawSearchForm();
	
		$res .= <<<EOD
					<div class='clear'></div>
				</div>
				<div id="result"></div>
EOD;
			
		$res .= $this->generateFooter();
	
		$res .= <<<EOD
			</div>
	
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
	
		global $aLanguagesForm, $aCountriesForm, $aContentTypeForm;
	
		global $aLanguagesStemmedForm;
		global $solrMainContentLanguage;
	
		$defaultQueryLanguage = $config->getDefault("search.default_query_language", "fr");
		$language_stemming_by_default = $config->get("search.language_stemming_by_default");
	
		$search_label = _('Search');
		$core = isset($_SESSION["core"]) ? $uuid = $_SESSION["core"] : '';
		$config_file = isset($_SESSION["config"]) ? $uuid = $_SESSION["config"] : '';
	
		$res = <<<EOD
		<div style="float:left; width:100%;">
		<form name="search_form" id="search_form" action="">
			<input type="hidden" name="config_save" id="config_save" value="$config_file">
			<input type="hidden" name="core" id="core_save" value="$core">
			<input type="hidden" name="action" id="action" value="search">
			<input type="hidden" name="page" id="page">
			<input type="hidden" name="fq" id="fq">
			<input type="hidden" name="mode" id="mode" value="simple">
			<input type="hidden" name="search_itemperpage" id="search_itemperpage" value="20">
			<input type="hidden" name="search_word_variations" id="search_word_variations" value="$language_stemming_by_default">
			<input type="hidden" name="bookmark_tag" id="bookmark_tag" value="">
			<input type="hidden" name="bookmark_collection" id="bookmark_collection" value="">
			<div>
				<div style='float: left; width: 650px;'>
				<input id="search_crit" type="text" value="" name="search_crit" class="" autocomplete="off" />
				</div>
				<div class="buttonrow">
					<input id="search_do" type="submit" name="search_do" value="$search_label"/>
	
EOD;
	
		if ($useadvanced) {
			$label1 = _('Advanced search');
			$label2 = _('Simple search');
			$res .= <<<EOD
			<br/>
			<span id="switch_advanced"><a href='javascript:void(0)' onClick='switchMode("advanced");'>$label1</a></span>
			<span id="switch_simple" style='display:none'><a href='javascript:void(0)' onClick='switchMode("simple");'>$label2</a></span>
	
EOD;
		}
		$res .= <<<EOD
			</div>
					</div>
	
			<div id="search_option">
	
EOD;
	
		if (count($aLanguagesForm)==0) {
			$res .= '<div id="div_query_language" class="option">';
			$res .= '<input type="hidden" name="search_querylanguage" id="search_querylanguage" value="' . $defaultQueryLanguage . '">';
				$res .= '</div>';
						}
	
						if (count($aLanguagesForm)==1) {
						$keys = array_keys($aLanguagesForm);
						$res .= '<div id="div_query_language" class="option">';
								$res .= '<input type="hidden" name="search_querylanguage" id="search_querylanguage" value="' . $keys[0] . '">';
								$res .= '</div>';
	}
	
	if (count($aLanguagesForm)>1) {
	
		$label1 = _('The language of the query is');
		$label2 = _('Not specified');
	
		$res .= <<<EOD
				<div id="div_query_language" class="option">
					<div id="div_query_language_label" style="width:220px; float:left">
						<span class="label">$label1</span>
					</div>
					<div id="st_option" style="margin-left:100px;">
						<select name="search_querylanguage" id="search_querylanguage">
							<option value="">$label2</option>
	
EOD;
	
		foreach ($aLanguagesForm as $key => $value)
		{
		$res .= "<option value='" . $key . "'";
		if ($solrMainContentLanguage==strtolower($key)) $res .= " selected='selected'";
				$res .= ">" . $value . "</option>";
			}
	
				$res .= <<<EOD
				</select>
					</div>
				</div>
	
EOD;
	
		}
	
		if ($usecollections) {
			$res .= <<<EOD
						<div id="div_query_collections" class="option">
					<div id="div_query_collections_label" style="width:90px; float:left">
						<span class="label">Collections</span>
					</div>
					<div id="st_option" style="margin-left:100px;">
						<div id="div_query_collections_values">
						</div>
					</div>
				</div>
	
EOD;
		}
	
		if ($usetags) {
			$res .= <<<EOD
					<div id="div_query_tags" class="option">
						<div id="div_query_tags_label" style="width:90px; float:left">
							<span class="label">Tags</span>
						</div>
						<div id="st_option" style="margin-left:100px;">
							<div id="div_query_tags_values">
							</div>
						</div>
					</div>
	
EOD;
		}
	
		if ($useadvanced) {
			$res .= <<<EOD
				<div id="search_advanced" style='display:none'>
	
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
	
				foreach ($aLanguagesForm as $key => $value) {
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
	
				foreach ($aCountriesForm as $key => $value) {
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
	
				foreach ($aContentTypeForm as $key => $value) {
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
		<div id="tagcloud"></div>
	
EOD;
		}
	
		$res .= <<<EOD
			</div>
				</form>
	</div>
	
EOD;
		return $res;
	}
	
	public function generateResults() {
	
		Global $response, $search_bookmark, $item_per_page, $page, $fqstr, $fqitms, $crit, $querylang, $queryField, $groupsize, $groupdisplaysize, $item_per_page_option, $facetextra, $facetqueries, $facet_union, $resultshowpermalink, $resultshowrss, $initialSearch, $aContentType, $aLanguages;
		
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
		$res .=	"<div id='result_summary''>";
				$res .=	"<p class='count'><span><b>" . $numFound . "</b> " . _('document(s) match your query') . "</span></p>";
	
				if ($resultshowpermalink || $resultshowrss) {
				$res .= "<p class='subscribe'>";
				if ($resultshowpermalink) {
				$res .= "<a onClick='doBookmark(this); return false;' href='" . $search_bookmark . "' target='_blank'>Bookmark<img src='images/bookmark.png' title='" . _('Bookmark link to this search') . "' height='20' width='20'></a>";
				}
				if ($resultshowrss)
					$res .= "<a href='rss.php" . $search_bookmark . "' target='_blank'>RSS <img src='images/rss.png' title='Bookmark link to this search as a RSS feed' height='20' width='20'></a>";
							$res .= "</p>";
				}
	
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
				$res .= '<div id="result_container">';
	
				// facet
				$res .= '<div id="result_facet"><br/><br/>';
	
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
	
				if ($facetval!="")
				{
					$res .= '<table><tr><td valign="top"><input type="checkbox" id="' . $chk_id . '" ' . $checked . ' onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', true);">&nbsp;</td>';
					$res .= '<td><a href="#" onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', false);">' . $facetval . '</a> (' . $facetcnt . ')</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table>';
				}
			}
			$res .= '</div></div>';
		}
	}
	
	/*
	 * facet_queries
	*/
	if (!empty($facetqueries)) {
		foreach($facetqueries as $facetfield => $query) {
	
			$label = $facetfield;
			$label2 = dgettext ( "amnesty" , $label);
	
			$res .= '<div id="facet_'. $facetfield . '"><h4 class="hand" onClick="$(\'#facet_'. $facetfield . '_data\').toggle(400);">' . $label2. '</h4>';
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
	
					$res .= '<table><tr><td valign="top"><input type="checkbox" id="' . $chk_id . '" ' . $checked . ' onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', true);">&nbsp;</td>';
					$res .= '<td><a href="#" onclick="doSearch(1, \'' . $crit_fq . '\', \'' . $fqstr . '\', \'' . $chk_id . '\', false);">' . $facetval . '</a> (' . $facetcnt . ')</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table>';
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
					function changeItemPerPage() {
						$('#search_itemperpage').val($('#results_itemperpage').val());
						doSearch(0, \"\", \"\", \"\", false, \"\", \"\");
					}
					</script>
					";
		$res .= $this->displayPagination ($numFoundPaginate, $item_per_page, $page, $fqstr );
		$res .= "&nbsp;<select id='results_itemperpage' name='results_itemperpage' onChange='changeItemPerPage();'>";
		$item_per_page_values="10,20,50,100";
		$aValues = explode(",",$item_per_page_values);
		foreach($aValues as $value){
			$res .= "<option value='" . $value . "'";
			if ($item_per_page_option==$value)
				$res .= " selected";
			$res .= ">" . $value . "</option>";
		}
		$res .= "</select><label for='results_itemperpage'>" . _('Items per page') . "</label>";
		$res .= '</div>';
	
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
	$res .= '<div class="clear"></div>';
	
	// 				if ($groupsize>0) {
	// 					$res .= '<div>';
	// 					foreach ($response->clusters as $cluster) {
	// 						$res .= $cluster->labels[0] . '<br/>';
	// 					}
	// 					$res .= '</div>';
	// 				}
	
		if ($this->debug)
		$res .= $response->getRawResponse();

		return $res;
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
	
	
}
?>
