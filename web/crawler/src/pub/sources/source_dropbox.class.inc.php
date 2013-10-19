<?php
class SourceDropbox extends SourceBase implements iSource {

	protected $mime_type;
	
	function __construct($config, $id_account_current, $db, $aLanguages, $aCountries) {
        
		parent::__construct($config, $id_account_current, $db, $aLanguages, $aCountries);

        $this->mime_type[]=array(value => 'text/html', label => 'HTML');
		$this->mime_type[]=array(value => 'text/plain', label => 'Text');
        $this->mime_type[]=array(value => 'application/pdf', label => 'PDF');
        $this->mime_type[]=array(value => 'application/msword', label => 'Microsoft Word (doc)');
        $this->mime_type[]=array(value => 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', label => 'Microsoft Word (docx)');
        $this->mime_type[]=array(value => 'application/vnd.ms-excel', label => 'Microsoft Excel (xls)');
        $this->mime_type[]=array(value => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', label => 'Microsoft Excel (xlsx)');
        $this->mime_type[]=array(value => 'application/vnd.ms-powerpoint', label => 'Microsoft Powerpoint (ppt)');
        $this->mime_type[]=array(value => 'application/vnd.openxmlformats-officedocument.presentationml.presentation', label => 'Microsoft Powerpoint (pptx)');
        //application/rtf,
        //text/rtf,
    }

    public function getType() {
		return "3";
	}

	public function displayPageConnect() {
		$res = '';
		$res .= "<tr>";
		$res .= "<td class='head'>Please connect to Dropbox</td>";
		$res .= "<td>";
		
		$res .= "<input type='button' value='Request Dropbox access' onClick='requestDropBoxAccess();'>&nbsp;<span id='status_dropbox_step1_status'></span>";
		$res .= "<br><input id='dropbox_continue' type='button' value='Continue' onClick='requestDropBoxAccessToken2();'>&nbsp;<span id='status_dropbox_step3_status'></span>";
		$res .= "<br><input type='button' value='Cancel' onClick='cancelSource();'>";
		
		$res .= "<input class='editInputText' type='hidden' name='dropboxurl' id='dropboxurl' value=''>";
		$res .= "<input class='editInputText' type='hidden' name='dropboxtimestamp' id='dropboxtimestamp' value=''>";
		$res .= "<input class='editInputText' type='hidden' name='token_key' id='token_key' value='" . encodeForInput($this->getValue('token_key', '')) . "'>";
		$res .= "<input class='editInputText' type='hidden' name='token_secret' id='token_secret' value='" . encodeForInput($this->getValue('token_secret', '')) . "'>";
		$res .= "<input class='editInputText' type='hidden' name='token_uid' id='token_uid' value=''>";
		
		$res .= "</td>";
		$res .= "</tr>";
		return $res;
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

		/*
 		$res .= "<tr>";
 		$res .= "<td class='head'>Token access (*)</td>";
 		$res .= "<td>";
		
		$token_key = $this->getValue('token_key', '');
		$token_secret = $this->getValue('token_secret', '');
		if (empty($token_key) || empty($token_secret)) {
			$res .= "<span id='status_token_access_error'><img src='images/error_12.png'>&nbsp;Request Dropbox access is mandatory !<br></span><span id='status_token_access_ok' style='display: none'><img src='images/ok_12.png'>&nbsp;</span>";
			$res .= "<br><span style='color:red'>During this step, a popup window should open. It is possible that your browser requests a confirmation or blocks this popup.</span>";
		}
		else {
			$res .= "<span id='status_token_access_error' style='display: none'><img src='images/error_12.png'>&nbsp;Request Dropbox access is mandatory !<br></span><span id='status_token_access_ok'><img src='images/ok_12.png'>&nbsp;</span>";
		}
		$res .= "<br>1) <input type='button' value='Request Dropbox access' onClick='requestDropBoxAccess();'>&nbsp;<span id='status_dropbox_step1_status'></span>";
		$res .= "&nbsp;<span class='help'>The Dropbox <strong>API Request Authorization</strong> page was opened. Click the <strong>Allow</strong> button !</span>";
		$res .= "<br>2) <input type='button' value='Confirm Dropbox access was validated' onClick='requestDropBoxAccessToken();'>&nbsp;<span id='status_dropbox_step3_status'></span>";
	
		$res .= "<input class='editInputText' type='hidden' name='dropboxurl' id='dropboxurl' value=''>";
		$res .= "<input class='editInputText' type='hidden' name='dropboxtimestamp' id='dropboxtimestamp' value=''>";
		$res .= "<input class='editInputText' type='hidden' name='token_key' id='token_key' value='" . encodeForInput($this->getValue('token_key', '')) . "'>";
		$res .= "<input class='editInputText' type='hidden' name='token_secret' id='token_secret' value='" . encodeForInput($this->getValue('token_secret', '')) . "'>";
		$res .= "</td></tr>";
*/		
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
		$res .= "<td class='head'>File formats</td>";
		$res .= "<td>";
		
		$mt =$this->getValue('mimetype', '');
		if (!empty($mt))
			$mt = explode(',',$this->getValue('mimetype', ''));
		
		$res2 = '';
		foreach ($this->mime_type as $type) {
			if ($res2!='') $res2 .= '<br />';
			$res2 .= "<input type='checkbox' name='mimetype[]' id='mimetype' value='" . $type['value'] . "' " . (in_array($type['value'], $mt) || empty($mt) ? " checked='checked'" : "") . "> " . $type['label'];
		}
		$res .= $res2;
		
		$res .= "</td>";
		$res .= "</tr>";
				
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

		/*
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
			$res .= "<br /><span class='help'>This parameter requieres MongoDB to be configured</span>";
			$res .= "</td>";
			$res .= "</tr>";
		}
		*/
		
		$res .= "<tr>";
		$res .= "<td class='head'>Crawl rules by url</td>";
		$res .= "<td>";

		$rules = $this->getValue('crawl_filtering_rules', '');

		$rulesJson = '{ "rules": [';
		if (substr($rules, 0, 1) == "<") {
			$rulesXml = simplexml_load_string($rules);
			$result = $rulesXml->xpath('/rules/rule');
			$sep = "";
			while(list( , $node) = each($result)) {
				$rulesJson .= $sep . '{ "ope": "' . (string)$node->ope . '", "mode": "' . (string)$node->mode . '", "pat": "' . str_replace("\\", "\\\\", (string)$node->pat) . '" }';
				$sep = ",";
			}
		}
		else {
			$aRules = explode("\n", $rules);
			$sep = "";
			for ($i=0; $i<count($aRules); $i++) {
				if ($aRules[$i]!="") {
					$aItems = explode(":", $aRules[$i]);
					$rulesJson .= $sep . '{ "ope": "' . $aItems[0] . '", "mode": "' . $aItems[1] . '", "pat": "' . $aItems[3] . '" }';
					$sep = ",";
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
		$stmt = parent::initSQL($stmt, $values);
		
		// Common dynamic fields
		$xml = parent::initSQLParams($values);
		
		// Specific dynamic fields
		$xml->addChild('source_root_dir',$values["source_root_dir"]);
		
		$xml->addChild('token_key',$values["token_key"]);
		$xml->addChild('token_secret',$values["token_secret"]);
		
		$mt = "";
		if (isset($values["mimetype"])) $mt = implode ( "," , $values["mimetype"] );
		$xml->addChild('mimetype', $mt);
		
		$stmt->addColumnValue("params", $xml->asXML(), "");		
		
		return $stmt;
	}
}
?>
