<?php
class Theme extends ThemeBase implements iTheme {	
	
	function generateHtmlStart() {
		
		$res =  '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">';
		$res .= '<html xmlns="http://www.w3.org/1999/xhtml">';
		$res .= '<head>';
		$res .= '<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>';
		$res .= '<meta http-equiv="Cache-Control" content="no-cache" />';
		$res .= '<meta http-equiv="Pragma" content="no-cache" />';
		$res .= '<meta http-equiv="Cache" content="no store" />';
		$res .= '<meta http-equiv="Expires" content="0" />';
		$res .= '<meta name="robots" content="index, nofollow" />';
		
		$res .= '<script type="text/javascript" src="js/jquery-1.7.1.min.js"></script>';
		$res .= '<script type="text/javascript" src="js/autocomplete.js"></script>';
		$res .= '<script type="text/javascript" src="js/jquery.blockUI.js"></script>';
		$res .= '<script type="text/javascript" src="js/jquery.ae.image.resize.min.js"></script>';
		
		$res .= '<link rel="stylesheet" type="text/css" href="css/reset.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'layout.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'autocomplete.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'styles.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="' . $this->getCssPath() . 'tagcloud.css" media="screen" />';
		
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
		
		if ($login && $user->getName()!="") {
			$res .= '&nbsp;-&nbsp;<a href="javascript:void(0);" onclick="doLogout();">' . _("Logout") . '</a>';
		}
		
/*		$res .= '<br/><a href="javascript:void(0);" onclick="doPreferences();">' . _("Preferences") . '</a>';
*/		
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
}
?>
