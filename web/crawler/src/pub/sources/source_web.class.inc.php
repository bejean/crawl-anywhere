<?php
class SourceWeb extends SourceBase implements iSource {

	public function getType() {
		return "1";
	}

	public function getUrl() {
		return $this->getValue('url', '');
	}

	public function displayPageConnect() {
		return "";
	}
	
	public function displayPageMain() {

		$res = '';

		$res .= "<tr>";
		$res .= "<td class='head'>Name (*)</td>";
		$res .= "<td>";

		$value = $this->getValue('name', '');
		if (empty($value)) {
			$res .= "<span id='status_source_name_error'><img src='images/error_12.png'>&nbsp;Provide source name is mandatory !<br></span><span id='status_source_name_ok' style='display: none'><img src='images/ok_12.png'>&nbsp;</span>";
		}
		else {
			$res .= "<span id='status_source_name_error' style='display: none'><img src='images/error_12.png'>&nbsp;Provide source name is mandatory !<br></span><span id='status_source_name_ok'><img src='images/ok_12.png'>&nbsp;</span>";
		}
		$res .= "<input class='editInputText' type='text' name='source_name' id='source_name' value='" . encodeForInput($this->getValue('name', '')) . "'  onBlur='checkParameter(\"source_name\");'></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Host (*)</td>";
		$res .= "<td>";

		$value = $this->getValue('url_host', '');
		if (empty($value)) {
			$res .= "<span id='status_source_host_error'><img src='images/error_12.png'>&nbsp;Provide source host is mandatory !<br></span><span id='status_source_host_ok' style='display: none'><img src='images/ok_12.png'>&nbsp;</span>";
		}
		else {
			$res .= "<span id='status_source_host_error' style='display: none'><img src='images/error_12.png'>&nbsp;Provide source host is mandatory !<br></span><span id='status_source_host_ok'><img src='images/ok_12.png'>&nbsp;</span>";
		}
		$res .= "<input class='editInputText' type='text' name='source_host' id='source_host' value='" . $this->getValue('url_host', '') . "' onBlur='checkParameter(\"source_host\");'>";
		$res .= "<br/><span class='help'>The source's host domain (don't specify protocol). Example : www.website.com<br/>The crawler will retrieve only the urls belonging to this domain.<br>You must specify one or more starting url below !</span></td>";
		$res .= "</tr>";
			
		$res .= "<tr>";
		$res .= "<td class='head'>Starting URLs (*)</td>";

		$url = $this->getValue('url', '');

		$urlJson = '{ "urls": [';
		if (!empty($url)) {
			if (substr($url, 0, 1) == "<") {
				$urlXml = simplexml_load_string($url);
				$result = $urlXml->xpath('/urls/url');
				$sep = "";
				while(list( , $node) = each($result)) {
					$urlJson .= $sep . '{ "url": "' . (string)$node->url . '", "home": "' . (string)$node->home . '", "mode": "' . (string)$node->mode . '", "allowotherdomain": "' . (string)$node->allowotherdomain . '", "onlyfirstcrawl": "' . (string)$node->onlyfirstcrawl . '" }';
					$sep = ",";
				}
			}
			else {
				$urlJson .= '{ "url": "' . $url . '",  "home": "", "mode": "s", "allowotherdomain": "0", "onlyfirstcrawl": "0" }';
			}
		}
		$urlJson .= '] }';

		$res .= "<td><span id='status_source_url_error'><img src='images/error_12.png'>&nbsp;Provide one or more starting url is mandatory !<br></span><span id='status_source_url_ok' style='display: none'><img src='images/ok_12.png'>&nbsp;</span>";
		$res .= "<div id='url'>";
		$res .= "</div>";
		$res .= "<input type='hidden' name='source_url' id='source_url' value='" . $urlJson . "'>";
		$res .= "<input type='hidden' name='source_url_xml' id='source_url_xml' value='" . $url . "'>";
		$res .= "<a href='javascript:addUrl();'><img src='images/plus_12.png'>&nbsp;Add url</a>";
		$res .= "&nbsp;&nbsp;<a href='javascript:scanRSS();'><img src='images/plus_12.png'>&nbsp;Scan RSS</a>";
		$res .= "<br/><span class='help'>All the urls used by the crawler in order to start the crawl process</span></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Host aliases</td>";
		$res .= "<td><input class='editInputText' type='text' name='source_alias' id='source_alias' value='" . $this->getValue('alias_host', '') . "'>";
		$res .= "<br/><span class='help'>The source's host domain aliases (don't specify protocol). Example : www2.website.com, support.website.com, *.website.com, www.website.*<br/>The crawler will also accept the urls belonging to these domains</span></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Status</td>";
		$res .= "<td>";
		$res .= "<select id='source_enabled' name='source_enabled' style='editInputSelect'>";
		$res .= "<option value='1'";
		if ($this->getValue('enabled', '')=="1") $res .= " selected";
		$res .= ">Enabled</option>";
		$res .= "<option value='0'";
		if ($this->getValue('enabled', '')=="0") $res .= " selected";
		$res .= ">Disabled</option>";
		$res .= "<option value='2'";
		if ($this->getValue('enabled', '')=="2") $res .= " selected";
		$res .= ">Test</option>";
		$res .= "</select>";
		$res .= "</td>";
		$res .= "</tr>";

		$target = $this->getValue('id_target', '');
		if (!$_SESSION["mysolrserver_url"]) {
			$aTargets = getAvailableTargets($this->config, $this->id_account_current);
			if ($aTargets!=null) {
				$res .= "<tr>";
				$res .= "<td class='head'>Target</td>";
				$res .= "<td>";
				$res .= "<select id='id_target' name='id_target' style='editInputSelect'>";
				foreach ($aTargets as $key => $value) {
					$res .= "<option value='" . $key . "'";
					if (!empty($target) && $target==strtolower(trim($key))) $res .= " selected";
					$res .= ">" . $value . "</option>";
				}
				$res .= "</select>";
				$res .= "</td>";
				$res .= "</tr>";
			}
		} else {
			if (empty($target)) {
				mg_get_value($this->mg, "accounts", "id_target", array("id" => intval($this->id_account_current)), $defaultTargetId);
				$target = $defaultTargetId;
			}
			$res .= "<input type='hidden' id='id_target' name='id_target' value='" . $target . "'>";
		}

		$res .= "<tr>";
		$res .= "<td class='head'>Collections</td>";
		$res .= "<td><input class='editInputText' type='text' name='source_collection' id='source_collection' value='" . str_replace("_", " ", $this->getValue('collection', '')) . "'>";

		$aCollections = getAvailableTagsCollections($this->config, false, $this->id_account_current, "collection");
		if ($aCollections!=null) {
			$res .= "<br />";
			for ($j=0; $j<=count($aCollections); $j++) {
				if ($aCollections[$j]!="") {
					$aCollections[$j] = str_replace("_", " ", $aCollections[$j]);
					$res .= "<a href='javascript:void(0)' onClick='addCollection(\"" . $aCollections[$j] . "\");'><span class='nobr'><img id='collection_" . $aCollections[$j] . "' src='images/plus_12.png'>&nbsp;<label for='tag_" . $aCollections[$j] . "'>" . $aCollections[$j] . "</label></a>&nbsp;&nbsp;</span> ";
				}
			}
		}
		$res .= "<br/><span class='help'>Use comma as separator.</span></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Tags</td>";
		$res .= "<td><input class='editInputText' type='text' name='source_tag' id='source_tag' value='" . encodeForInput(str_replace("_", " ", $this->getValue('tag', ''))) . "'>";
		$aTags = getAvailableTagsCollections($this->config, false, $this->id_account_current, "tag");
		if ($aTags!=null) {
			$res .= "<br />";
			for ($j=0; $j<=count($aTags); $j++) {
				if ($aTags[$j]!="") {
					$aTags[$j] = str_replace("_", " ", $aTags[$j]);
					$res .= "<a href='javascript:void(0)' onClick='addTag(\"" . $aTags[$j] . "\");'><span class='nobr'><img id='tag_" . $aTags[$j] . "' src='images/plus_12.png'>&nbsp;<label for='tag_" . $aTags[$j] . "'>" . $aTags[$j] . "</label></a>&nbsp;&nbsp;</span> ";
				}
			}
		}
		$res .= "<br/><span class='help'>Use comma as separator.<br>These tags will be associated with crawled pages if they are contained in pages.</span></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Country</td>";
		$res .= "<td>";
		$res .= "<select id='source_country' name='source_country' style='editInputSelect'>";

		$country = strtolower($this->getValue('country', ''));
		foreach ($this->aCountries as $key => $value)
		{
			$res .= "<option value='" . $key . "'";
			if ($country==strtolower(trim($key))) $res .= " selected";
			$res .= ">" . $value . "</option>";
		}
		$res .= "</select>";
		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Main language</td>";
		$res .= "<td>";
		$res .= "<select id='source_language' name='source_language' style='editInputSelect' onChange='showLanguageRestrictionList();'>";

		$language = strtolower($this->getValue('language', ''));
		foreach ($this->aLanguages as $key => $value)
		{
			$res .= "<option value='" . $key . "'";
			if ($language==strtolower(trim($key))) $res .= " selected";
			$res .= ">" . $value . "</option>";
		}
		$res .= "</select>";
		$res .= "<div id='language_detection_list' name='language_detection_list'";
		if ((strtolower(trim($this->getValue('language', '')))=="xx") || ($this->data==null))
		$res .= " style='display:block'";
		else
		$res .= " style='display:none'";
		$res .= ">Limit detection to this languages list (<a href='http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes' target='_blank'>ISO 639-1 codes</a> separated by \",\")&nbsp;<input class='editInputTextMedium' type='text' name='source_language_detection_list' id='source_language_detection_list' value='" . $this->getValue('language_detection_list', '') . "'></div>";

		$res .= "</td>";
		$res .= "</tr>";

		return $res;
	}

	public function displayPageAdvanced() {

		$res .= "<!--tr>";
		$res .= "<td class='head'>Language advanced rules</td>";
		$res .= "<td><textarea name='source_language_advanced' id='source_language_advanced' rows='6' cols='70' class='editInputTextarea'>" . $this->getValue('language_advanced', '') . "</textarea></td>";
		$res .= "</tr-->";

		//$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Protocol strategy</td>";
		$res .= "<td>";
		$res .= "If the same web page is discovered with both http and https protocol&nbsp;";
		$res .= "<select id='source_crawl_protocol_strategy' name='source_crawl_protocol_strategy' style='editInputSelect'>";
		$res .= "<option value='1'";
		if ($this->getValue('protocol_strategy', '')=="1") $res .= " selected";
		$res .= ">Keep only http page</option>";
		$res .= "<option value='0'";
		if ($this->getValue('protocol_strategy', '')=="0") $res .= " selected";
		$res .= ">Consider http and https as different pages</option>";
		$res .= "<option value='2'";
		if ($this->getValue('protocol_strategy', '')=="2") $res .= " selected";
		$res .= ">Keep only https page</option>";
		$res .= "</select>";
		$res .= "</td>";
		$res .= "</tr>";
		
		$res .= "<tr>";
		$res .= "<td class='head'>Check deleted</td>";
		$res .= "<td>";
		$res .= "<select id='source_crawl_checkdelete_strategy' name='source_crawl_checkdelete_strategy' style='editInputSelect'>";
		$res .= "<option value='0'";
		if ($this->getValue('checkdeleted_strategy', '')=="0") $res .= " selected";
		$res .= ">Default (as defined in global configuration file)</option>";
		$res .= "<option value='1'";
		if ($this->getValue('checkdeleted_strategy', '')=="1") $res .= " selected";
		$res .= ">After each crawl of this web site</option>";
		$res .= "</select>";
		$res .= "<br /><span class='help'>The crawler can check if previously crawled pages still exist on the web site</span>";
		$res .= "</td>";
		$res .= "</tr>";
		
		$res .= "<tr>";
		$res .= "<td class='head'>Crawl max depth</td>";
		$res .= "<td>";

		$param = $this->config->getDefault("source.max_depth", "");
		if (($param != "" && !empty($param)) || $param=="0") {
			$res .= "<input type='hidden' name='source_crawl_max_depth' id='source_crawl_max_depth' value='" . $this->config->get("source.max_depth") . "'> Default";
		} else {
			$res .= "<input class='editInputTextSmall' type='text' name='source_crawl_max_depth' id='source_crawl_max_depth' value='" . $this->getValue('crawl_maxdepth', '') . "'> (0 for default)";
		}		

		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Crawl child pages only</td>";
		$res .= "<td>";
		$res .= "<select id='source_crawl_child_only' name='source_crawl_child_only' style='editInputSelect'>";
		$res .= "<option value='2'";
		if ($this->getValue('crawl_childonly', '')=="2") $res .= " selected";
		$res .= ">Default</option>";
		$res .= "<option value='1'";
		if ($this->getValue('crawl_childonly', '')=="1") $res .= " selected";
		$res .= ">Yes</option>";
		$res .= "<option value='0'";
		if ($this->getValue('crawl_childonly', '')=="0") $res .= " selected";
		$res .= ">No</option>";
		$res .= "</select>";
		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Maximum number of simultaneous pages crawled</td>";
		$res .= "<td>";

		$param = $this->config->getDefault("source.max_simultaneous_item_per_source", "");
		if (($param != "" && !empty($param)) || $param=="0") {
			$res .= "<input type='hidden' id='source_crawl_url_concurrency' name='source_crawl_url_concurrency' value='" . $this->config->get("source.max_depth") . "'> Default";
		} else {
			$res .= "<select id='source_crawl_url_concurrency' name='source_crawl_url_concurrency' style='editInputSelect'>";
			$res .= "<option value='0'";
			if ($this->getValue('crawl_url_concurrency', '')=="0") $res .= " selected";
			$res .= ">Default</option>";
			$res .= "<option value='1'";
			if ($this->getValue('crawl_url_concurrency', '')=="1") $res .= " selected";
			$res .= ">1</option>";
			$res .= "<option value='2'";
			if ($this->getValue('crawl_url_concurrency', '')=="2") $res .= " selected";
			$res .= ">2</option>";
			$res .= "<option value='4'";
			if ($this->getValue('crawl_url_concurrency', '')=="4") $res .= " selected";
			$res .= ">4</option>";
			$res .= "<option value='6'";
			if ($this->getValue('crawl_url_concurrency', '')=="6") $res .= " selected";
			$res .= ">6</option>";
			$res .= "<option value='8'";
			if ($this->getValue('crawl_url_concurrency', '')=="8") $res .= " selected";
			$res .= ">8</option>";
			$res .= "<option value='12'";
			if ($this->getValue('crawl_url_concurrency', '')=="12") $res .= " selected";
			$res .= ">12</option>";
			$res .= "<option value='16'";
			if ($this->getValue('crawl_url_concurrency', '')=="16") $res .= " selected";
			$res .= ">16</option>";
			$res .= "<option value='20'";
			if ($this->getValue('crawl_url_concurrency', '')=="20") $res .= " selected";
			$res .= ">20</option>";
			$res .= "<option value='24'";
			if ($this->getValue('crawl_url_concurrency', '')=="24") $res .= " selected";
			$res .= ">24</option>";
			$res .= "<option value='28'";
			if ($this->getValue('crawl_url_concurrency', '')=="28") $res .= " selected";
			$res .= ">28</option>";
			$res .= "<option value='32'";
			if ($this->getValue('crawl_url_concurrency', '')=="32") $res .= " selected";
			$res .= ">32</option>";
			$res .= "<option value='36'";
			if ($this->getValue('crawl_url_concurrency', '')=="36") $res .= " selected";
			$res .= ">36</option>";
			$res .= "<option value='40'";
			if ($this->getValue('crawl_url_concurrency', '')=="40") $res .= " selected";
			$res .= ">40</option>";
			$res .= "</select>";
		}

		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Maximum number of pages read per minutes</td>";
		$res .= "<td>";
		
		$res .= "<select id='source_crawl_url_per_minute' name='source_crawl_url_per_minute' style='editInputSelect'>";
		$res .= "<option value='0'";
		if ($this->getValue('crawl_url_per_minute', '')=="0") $res .= " selected";
		$res .= ">No limit</option>";
		$res .= "<option value='4'";
		if ($this->getValue('crawl_url_per_minute', '')=="4") $res .= " selected";
		$res .= ">4</option>";
		$res .= "<option value='6'";
		if ($this->getValue('crawl_url_per_minute', '')=="6") $res .= " selected";
		$res .= ">6</option>";
		$res .= "<option value='8'";
		if ($this->getValue('crawl_url_per_minute', '')=="8") $res .= " selected";
		$res .= ">8</option>";
		$res .= "<option value='10'";
		if ($this->getValue('crawl_url_per_minute', '')=="10") $res .= " selected";
		$res .= ">10</option>";
		$res .= "<option value='12'";
		if ($this->getValue('crawl_url_per_minute', '')=="12") $res .= " selected";
		$res .= ">12</option>";
		$res .= "<option value='16'";
		if ($this->getValue('crawl_url_per_minute', '')=="16") $res .= " selected";
		$res .= ">16</option>";
		$res .= "<option value='20'";
		if ($this->getValue('crawl_url_per_minute', '')=="20") $res .= " selected";
		$res .= ">20</option>";
		$res .= "<option value='24'";
		if ($this->getValue('crawl_url_per_minute', '')=="24") $res .= " selected";
		$res .= ">24</option>";
		$res .= "<option value='28'";
		if ($this->getValue('crawl_url_per_minute', '')=="28") $res .= " selected";
		$res .= ">28</option>";
		$res .= "<option value='32'";
		if ($this->getValue('crawl_url_per_minute', '')=="32") $res .= " selected";
		$res .= ">32</option>";
		$res .= "<option value='36'";
		if ($this->getValue('crawl_url_per_minute', '')=="36") $res .= " selected";
		$res .= ">36</option>";
		$res .= "<option value='40'";
		if ($this->getValue('crawl_url_per_minute', '')=="40") $res .= " selected";
		$res .= ">40</option>";
		$res .= "</select>";
		$res .= "<option value='44'";
		if ($this->getValue('crawl_url_per_minute', '')=="44") $res .= " selected";
		$res .= ">44</option>";
		$res .= "</select>";
		$res .= "<option value='48'";
		if ($this->getValue('crawl_url_per_minute', '')=="48") $res .= " selected";
		$res .= ">48</option>";
		$res .= "</select>";
		$res .= "<option value='52'";
		if ($this->getValue('crawl_url_per_minute', '')=="52") $res .= " selected";
		$res .= ">52</option>";
		$res .= "</select>";
		$res .= "<option value='56'";
		if ($this->getValue('crawl_url_per_minute', '')=="56") $res .= " selected";
		$res .= ">56</option>";
		$res .= "</select>";
		$res .= "<option value='60'";
		if ($this->getValue('crawl_url_per_minute', '')=="60") $res .= " selected";
		$res .= ">60</option>";
		$res .= "</select>";
		$res .= "</select>";		
		
		$res .= "</td>";
		$res .= "</tr>";
		
		$res .= "<tr>";
		$res .= "<td class='head'>User agent (optional)</td>";
		$res .= "<td><input class='editInputText' type='text' name='source_user_agent' id='source_user_agent' value='" . $this->getValue('user_agent', '') . "'></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Ignored session id fields in url</td>";
		$res .= "<td><input class='editInputText' type='text' name='source_url_ignore_fields' id='source_url_ignore_fields' value='" . $this->getValue('url_ignore_fields', '') . "'></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Ignored fields in url (others than session id)</td>";
		$res .= "<td><input class='editInputText' type='text' name='source_url_ignore_fields_no_session_id' id='source_url_ignore_fields_no_session_id' value='" . $this->getValue('url_ignore_fields_no_session_id', '') . "'>";
		$res .= "<br><span class='help'>Use <strong>*</strong> in order to remove all parameters in urls. Regular expressions are allowed (for instance \"<strong>utm_.*</strong>\")</span>";
		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Automatic HTML page cleaning</td>";
		$res .= "<td>";
		$res .= "<select id='source_automatic_cleaning' name='source_automatic_cleaning' style='editInputSelect'>";
		$res .= "<option value='0'";
		if ($this->getValue('automatic_cleaning', '')=="0") $res .= " selected";
		$res .= ">No</option>";
		$res .= "<option value='4'";
		if ($this->getValue('automatic_cleaning', '')=="4" || $this->getValue('automatic_cleaning', '')=="") $res .= " selected";
		$res .= ">Snacktory extractor</option>";
		$res .= "<option value='1'";
		if ($this->getValue('automatic_cleaning', '')=="1") $res .= " selected";
		$res .= ">Boilerpipe article extractor</option>";
		$res .= "<option value='2'";
		if ($this->getValue('automatic_cleaning', '')=="2") $res .= " selected";
		$res .= ">Boilerpipe default extractor</option>";
		$res .= "<option value='3'";
		if ($this->getValue('automatic_cleaning', '')=="3") $res .= " selected";
		$res .= ">Boilerpipe canola extractor</option>";
		$res .= "</select>";
		$res .= "<br /><br />Test cleaning this page:<br />";
		$res .= "<input class='editInputTextMedium2' type='text' name='test_url_cleaning' id='test_url_cleaning' value=''>";
		$res .= "<input type='button' value='Test' onClick='testUrlCleaning();'></td>";
		$res .= "</td>";
		$res .= "</tr>";

		$param = intval($this->config->getDefault("crawler.period", "6"));

		$res .= "<tr>";
		$res .= "<td class='head'>Recrawl period</td>";
		$res .= "<td>";
		$res .= "<select id='crawl_minimal_period' name='crawl_minimal_period' style='editInputSelect'>";
		$res .= "<option value='0'";
		if ($this->getValue('crawl_minimal_period', '')=="0") $res .= " selected";
		$res .= ">default</option>";
		$res .= "<option value='999999'";
		if ($this->getValue('crawl_minimal_period', '')=="999999") $res .= " selected";
		$res .= ">on demand</option>";
		if ($param<=1) {
			$res .= "<option value='1'";
			if ($this->getValue('crawl_minimal_period', '')=="1") $res .= " selected";
			$res .= ">1 hour</option>";
		}
		if ($param<=3) {
			$res .= "<option value='3'";
			if ($this->getValue('crawl_minimal_period', '')=="3") $res .= " selected";
			$res .= ">3 hours</option>";
		}
		if ($param<=6) {
			$res .= "<option value='6'";
			if ($this->getValue('crawl_minimal_period', '')=="6") $res .= " selected";
			$res .= ">6 hours</option>";
		}
		if ($param<=12) {
			$res .= "<option value='12'";
			if ($this->getValue('crawl_minimal_period', '')=="12") $res .= " selected";
			$res .= ">12 hours</option>";
		}
		if ($param<=24) {
			$res .= "<option value='24'";
			if ($this->getValue('crawl_minimal_period', '')=="24") $res .= " selected";
			$res .= ">1 day</option>";
		}
		if ($param<=48) {
			$res .= "<option value='48'";
			if ($this->getValue('crawl_minimal_period', '')=="48") $res .= " selected";
			$res .= ">2 days</option>";
		}
		if ($param<=72) {
			$res .= "<option value='72'";
			if ($this->getValue('crawl_minimal_period', '')=="72") $res .= " selected";
			$res .= ">3 days</option>";
		}
		if ($param<=96) {
			$res .= "<option value='96'";
			if ($this->getValue('crawl_minimal_period', '')=="96") $res .= " selected";
			$res .= ">4 days</option>";
		}
		if ($param<=120) {
			$res .= "<option value='120'";
			if ($this->getValue('crawl_minimal_period', '')=="120") $res .= " selected";
			$res .= ">5 days</option>";
		}
		if ($param<=144) {
			$res .= "<option value='144'";
			if ($this->getValue('crawl_minimal_period', '')=="144") $res .= " selected";
			$res .= ">6 days</option>";
		}
		if ($param<=168) {
			$res .= "<option value='168'";
			if ($this->getValue('crawl_minimal_period', '')=="168") $res .= " selected";
			$res .= ">1 week</option>";
		}
		if ($param<=336) {
			$res .= "<option value='336'";
			if ($this->getValue('crawl_minimal_period', '')=="336") $res .= " selected";
			$res .= ">2 weeks</option>";
		}
		if ($param<=504) {
			$res .= "<option value='504'";
			if ($this->getValue('crawl_minimal_period', '')=="504") $res .= " selected";
			$res .= ">3 weeks</option>";
		}
		if ($param<=772) {
			$res .= "<option value='772'";
			if ($this->getValue('crawl_minimal_period', '')=="772") $res .= " selected";
			$res .= ">4 weeks</option>";
		}
		$res .= "</select>";
		$res .= "<br><span class='help'><strong>Minimal</strong> period between to crawl";
		$res .= "<br>when the source crawl ends, the next crawl date is set based on this parameter</span>";
		$res .= "</td>";
		$res .= "</tr>";

		if (!$_SESSION["mysolrserver_url"]) {
			$res .= "<tr>";
			$res .= "<td class='head'>Schedules</td>";

			$scheduleJson = '{ "schedules": [';
			$schedule = $this->getValue('crawl_schedule', '');
			if (isset($schedule) && $schedule!="") {
				$scheduleXml = simplexml_load_string($schedule);
				$result = $scheduleXml->xpath('/schedules/schedule');
				$sep = "";
				while(list( , $node) = each($result)) {
					$scheduleJson .= $sep . '{ "day": "' . (string)$node->day . '", "start": "' . (string)$node->start . '", "stop": "' . (string)$node->stop . '", "enabled": "' . (string)$node->enabled . '" }';
					$sep = ",";
				}
			}
			$scheduleJson .= '] }';

			$res .= "<td>";
			$res .= "<div id='schedule'>";
			$res .= "</div>";
			$res .= "<input type='hidden' name='source_schedule' id='source_schedule' value='" . $scheduleJson . "'>";
			$res .= "<input type='hidden' name='source_schedule_xml' id='source_schedule_xml' value='" . $schedule . "'>";
			$res .= "<a href='javascript:addSchedule();'><img src='images/plus_12.png'>&nbsp;Add schedule</a>";
			$res .= "<br /><span class='help'>Define allowed and disallowed crawl time. Source crawl will pause and restart according to these schedules";
			$res .= "<br />This parameter requires MongoDB to be configured</span>";
			$res .= "</td>";
			$res .= "</tr>";
		}
		$res .= "<tr>";
		$res .= "<td class='head'>Crawl rules by url</td>";
		$res .= "<td>";

		$rules = $this->getValue('crawl_filtering_rules', '');

		$rulesJson = '{ "rules": [';
		if (!empty($rules)) {
			if (substr($rules, 0, 1) == "<") {
				$rulesXml = simplexml_load_string($rules);
				$result = $rulesXml->xpath('/rules/rule');
				$sep = "";
				while(list( , $node) = each($result)) {
					$rulesJson .= $sep . '{ "ope": "' . (string)$node->ope . '", "mode": "' . (string)$node->mode . '", "pat": "' . str_replace("\\", "\\\\", (string)$node->pat) . '", "meta": "' . str_replace("\\", "\\\\", (string)$node->meta) . '", "metap": "' . (string)$node->metap . '", "ignoreparam": "' . (string)$node->ignoreparam . '" }';
					$sep = ",";
				}
			}
			else {
				$aRules = explode("\n", $rules);
				$sep = "";
				for ($i=0; $i<count($aRules); $i++) {
					if ($aRules[$i]!="") {
						$aItems = explode(":", $aRules[$i]);
						$rulesJson .= $sep . '{ "ope": "' . $aItems[0] . '", "mode": "' . $aItems[1] . '", "pat": "' . $aItems[3] . '", "meta": "", "metap": "", "ignoreparam": "" }';
						$sep = ",";
					}
				}
			}
		}
		$rulesJson .= '] }';

		$res .= "<div id='rule'>";
		$res .= "</div>";
		$res .= "<input type ='hidden' name='source_crawl_filtering_rules' id='source_crawl_filtering_rules' value='" . $rulesJson . "'>";
		$res .= "<input type ='hidden' name='source_crawl_filtering_rules_xml' id='source_crawl_filtering_rules_xml' value='" . $rules . "'>";
		$res .= "<a href='javascript:addRule();'><img src='images/plus_12.png'>&nbsp;Add rule</a>";

		$res .= "<br /><br />Test this URL :<br />";
		$res .= "<input class='editInputTextMedium2' type='text' name='test_url' id='test_url' value=''>";
		$res .= "<input type='button' value='Test' onClick='testFilteringRules();'><div id='filtering_rules_test_result'></div>";

		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Metadata</td>";
		$res .= "<td><textarea name='source_metadata' id='source_metadata' rows='6' cols='70' class='editInputTextarea'>" . encodeForInput($this->getValue('metadata', '')) . "</textarea>";
		$res .= "<br><span class='help'>These metadatas will be added into the output xml files</span>";
		$res .= "<br><span class='help'>Syntax:</span>";
		$res .= "<br><span class='help'>meta_name1:value1</span>";
		$res .= "<br><span class='help'>meta_name1:value2</span>";
		$res .= "<br><span class='help'>meta_name2:value3</span>";
		$res .= "</td>";
		$res .= "</tr>";
		
		$res .= "<tr>";
		$res .= "<td class='head'>Authentication Mode</td>";
		$res .= "<td>";
		$res .= "<select id='auth_mode' name='auth_mode' style='editInputSelect'>";
		$res .= "<option value='0'";
		if ($this->getValue('auth_mode', '')=="0") $res .= " selected";
		$res .= ">None</option>";
		$res .= "<option value='1'";
		if ($this->getValue('auth_mode', '')=="1") $res .= " selected";
		$res .= ">POST (form)</option>";
		$res .= "<option value='2'";
		if ($this->getValue('auth_mode', '')=="2") $res .= " selected";
		$res .= ">GET (form)</option>";
		$res .= "<option value='3'";
		if ($this->getValue('auth_mode', '')=="3") $res .= " selected";
		$res .= ">Basic (web server)</option>";
		$res .= "</select>";
		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Authentication login</td>";
		$res .= "<td><input class='editInputTextMedium' type='text' name='auth_login' id='auth_login' value='" . $this->getValue('auth_login', '') . "'>";
		$res .= "<input type='button' value='Test' onClick='testAuthentication();'>";
		$res .= "</td></tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Authentication password</td>";
		$res .= "<td><input class='editInputTextMedium' type='text' name='auth_passwd' id='auth_passwd' value='" . $this->getValue('auth_passwd', '') . "'></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Authentication param</td>";
		$res .= "<td><input class='editInputText' type='text' name='auth_param' id='auth_param' value='" . $this->getValue('auth_param', '') . "'>";
		$res .= "<br><span class='help'>Example : http://www.server.com/login.asp|login=&#36;&#36;auth_login&#36;&#36;|password=&#36;&#36;auth_passwd&#36;&#36;</span></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Comment</td>";
		$res .= "<td><textarea name='source_comment' id='source_comment' rows='6' cols='70' class='editInputTextarea'>" . encodeForInput($this->getValue('comment', '')) . "</textarea></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Source contact</td>";
		$res .= "<td><textarea name='source_contact' id='source_contact' rows='6' cols='70' class='editInputTextarea'>" . encodeForInput($this->getValue('contact', '')) . "</textarea></td>";
		$res .= "</tr>";

		return $res;
	}

	public function displayPageStatus() {
		$res .= "<tr>";
		$res .= "<td class='head'>Id</td>";
		$res .= "<td>" . $this->getValue('id', '') . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Creation time</td>";
		$res .= "<td>" . $this->getValue('createtime', '') . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Last update time</td>";
		$res .= "<td>" . $this->getValue('updatetime', '') . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Last crawl start date</td>";
		$res .= "<td>" . $this->getValue('crawl_lasttime_start', '') . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Last crawl finish date</td>";
		$res .= "<td>" . $this->getValue('crawl_lasttime_end', '') . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Last crawl status</td>";
		if ($this->getValue('crawl_status', '')=="0")
		$res .= "<td>Success</td>";
		else
		$res .= "<td>Error or warning : " . $this->getValue('crawl_status', '') . " - " . $this->getValue('crawl_status_message', '') . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Last crawl page count</td>";
		$res .= "<td>" . $this->getValue('crawl_lastpagecount', '') . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Next crawl date</td>";
		$res .= "<td>" . $this->getValue('crawl_nexttime', '') . "</td>";
		$res .= "</tr>";

		return $res;
	}

	public function buildSQL($stmt, $values, $create = false) {
		
		// Common static fields
		$stmt = parent::initSQL($stmt, $values, $create);

		// Common dynamic fields
		$xml = parent::initSQLParams($values);
		
		// Specific dynamic fields
		$source_host = $values["source_host"];
		if (!preg_match("/^(http|https|ftp):\/\//i", $source_host))
		$source_host = "http://" . $source_host;
		$url_parts = parse_url($source_host);
		$xml->addChild('url_host',$url_parts["host"]);

		$source_alias = str_replace(" ", "", $values["source_alias"]);
		$source_alias = str_replace(";", ",", $source_alias);
		$xml->addChild('alias_host',$source_alias);

		$xml->addChild('protocol_strategy',$values["source_crawl_protocol_strategy"]);
		
		$xml->addChild('checkdeleted_strategy',$values["source_crawl_checkdelete_strategy"]);
		
		// url
		$xml->addChild('url');
		$urls = new SimpleXMLElement($values["source_url_xml"]);
		$domurl = dom_import_simplexml($xml->url);
		$domurls  = dom_import_simplexml($urls);
		$domurls  = $domurl->ownerDocument->importNode($domurls, TRUE);
		$domurl->appendChild($domurls);

		$xml->addChild('user_agent',$values["source_user_agent"]);
		$xml->addChild('url_ignore_fields',$values["source_url_ignore_fields"]);
		$xml->addChild('url_ignore_fields_no_session_id',$values["source_url_ignore_fields_no_session_id"]);
		$xml->addChild('crawl_childonly',$values["source_crawl_child_only"]);

		$xml->addChild('auth_mode',$values["auth_mode"]);
		$xml->addChild('auth_login',$values["auth_login"]);
		$xml->addChild('auth_passwd',$values["auth_passwd"]);
		$xml->addChild('auth_param',$values["auth_param"]);
		$stmt->addColumnValue("params", $xml->asXML());

		return $stmt;
	}
}
?>
