<?php
require_once_all(dirname(__FILE__) . '/*.inc.php');

interface iSource {
	public function getType();
	public function load($ar);
	public function displayPageConnect();
	public function displayPageMain();
	public function displayPageAdvanced();
	public function displayPageStatus();
}

abstract class SourceBase {

	protected $config;
	protected $id_account_current;
	protected $db;
	protected $aLanguages;
	protected $aCountries;
	protected $data;
	protected $commons;

	function __construct($config, $id_account_current, $db, $aLanguages, $aCountries) {
		$this->config = $config;
		$this->id_account_current = $id_account_current;
		$this->db = $db;
		$this->aCountries = $aCountries;
		$this->aLanguages = $aLanguages;
		$this->data = null;

		$this->commons = array();
		$this->commons[] = 'id';
		$this->commons[] = 'id_account';
		$this->commons[] = 'id_target';
		$this->commons[] = 'createtime';
		$this->commons[] = 'updatetime';
		$this->commons[] = 'deleted';
		$this->commons[] = 'collection';
		$this->commons[] = 'tag';
		$this->commons[] = 'type';
		$this->commons[] = 'enabled';
		$this->commons[] = 'crawl_nexttime';
		$this->commons[] = 'crawl_lasttime_end';
		$this->commons[] = 'crawl_lasttime_start';
		$this->commons[] = 'crawl_status';
		$this->commons[] = 'crawl_status_message';
		$this->commons[] = 'crawl_lastpagecount';
		$this->commons[] = 'crawl_pagecount';
		$this->commons[] = 'crawl_pagecount_success';
		$this->commons[] = 'crawl_process_status';
		$this->commons[] = 'crawl_minimal_period';
		$this->commons[] = 'crawl_firstcompleted';
		$this->commons[] = 'crawl_mode';
		$this->commons[] = 'crawl_priority';
		$this->commons[] = 'name';
		$this->commons[] = 'language';
		$this->commons[] = 'country';
		$this->commons[] = 'running_crawl_lastupdate';
		$this->commons[] = 'running_crawl_item_processed';
		$this->commons[] = 'running_crawl_item_to_process';
		$this->commons[] = 'extra';
	}

	public function load($ar) {
		$this->data =null;
		$this->params =null;
		if ($ar==null) return;
		$this->data = $ar;
		$xml = simplexml_load_string($ar['params']);
		$params = json_decode(json_encode($xml),TRUE);
		foreach ($params as $key => $value) {
			if (is_array($value)) {
				if (count($value)==0)
					$value = "";
				else
					$value = $xml->{$key}->children()->asXML();
			}
			//$this->data[strtoupper($key)] = $value;
			$this->data[strtolower($key)] = $value;
		}
	}

	protected function getValue($name, $default) {
		if ($this->data==null) return '';
		$ret = $this->data[strtolower($name)];
		if($ret instanceof MongoDate) return date('Y-m-d h:i:s', $ret->sec);
		if (empty($ret) && !empty($default)) $ret = $default;
		return $ret;
	}
	
	protected function normalizeTagsCollections($value) {
		$value = strtolower(trim($value));
		$value = preg_replace ('/\s*,\s*/', ',', $value);
		$value = preg_replace ('/\s+/', '_', $value);
		$aValue = explode(',',$value);
		$aTag = array_unique($aValue);
		sort($aTag);
		$value = implode(',',$aTag);
		return $value;
	}

	public function getSqlStmt($mode) {
	//TODO: V4
		$mode = strtolower($mode);

		if ($mode!='insert' && $mode!='update') return "";

		if ($mode=='insert') $stmt = new db_stmt_insert("sources");
		if ($mode=='update') {
			$stmt = new db_stmt_update("sources");
			$stmt->setWhereClause("id = '" . $this->getValue('id', '') . "'");
		}

		$xml = new SimpleXMLElement('<params/>');

		foreach ($this->data as $key => $value) {
			$key = strtolower($key);
			if ($key=='params') continue;
			if (!in_array($key, $this->commons)) {
				if (!empty($value) && substr($value, 0, 1)=='<') {
					$child = $xml->addChild($key);
					$schedules = new SimpleXMLElement($value);
					$domschedule = dom_import_simplexml($child);
					$domschedules  = dom_import_simplexml($schedules);
					$domschedules  = $domschedule->ownerDocument->importNode($domschedules, TRUE);
					$domschedule->appendChild($domschedules);
				} else {
					$xml->addChild($key,$value);
				}
			}
			else {
				$stmt->addColumnValue($key,$value);
			}
		}
		$stmt->addColumnValue("params", $xml->asXML());

		return $stmt->getStatement();
	}

	protected function initSQL($stmt, $values, $create = false) {
			
		$stmt->addColumnValue("id_target", intval($values["id_target"]));
		$stmt->addColumnValue("enabled", $values["source_enabled"]);
		$stmt->addColumnValue("name", $values["source_name"]);
		$stmt->addColumnValue("country", $values["source_country"]);
		$stmt->addColumnValue("language", $values["source_language"]);
		$stmt->addColumnValueDate("updatetime");
		$stmt->addColumnValue("collection",$this->normalizeTagsCollections($values["source_collection"]));
		$stmt->addColumnValue("tag",$this->normalizeTagsCollections($values["source_tag"]));
		$stmt->addColumnValue("crawl_minimal_period", $values["crawl_minimal_period"]);
		if ($create) $stmt->addColumnValue("crawl_priority", "2");

		return $stmt;
	}

	protected function initSQLParams($values) {
			
		$xml = new SimpleXMLElement('<params/>');
		$xml->addChild('language_detection_list',$values["source_language_detection_list"]);

		$xml->addChild('crawl_maxdepth',$values["source_crawl_max_depth"]);
		$xml->addChild('crawl_url_concurrency',$values["source_crawl_url_concurrency"]);
		$xml->addChild('automatic_cleaning',$values["source_automatic_cleaning"]);

		// crawl_schedule
		$xml->addChild('crawl_schedule');
		if (!empty($values["source_schedule_xml"])) {
			$schedules = new SimpleXMLElement($values["source_schedule_xml"]);
			$domschedule = dom_import_simplexml($xml->crawl_schedule);
			$domschedules  = dom_import_simplexml($schedules);
			$domschedules  = $domschedule->ownerDocument->importNode($domschedules, TRUE);
			$domschedule->appendChild($domschedules);
		}

		// crawl_filtering_rules
		$xml->addChild('crawl_filtering_rules');
		if (!empty($values["source_crawl_filtering_rules_xml"])) {
			$rules = new SimpleXMLElement($values["source_crawl_filtering_rules_xml"]);
			$domsrule = dom_import_simplexml($xml->crawl_filtering_rules);
			$domrules  = dom_import_simplexml($rules);
			$domrules  = $domsrule->ownerDocument->importNode($domrules, TRUE);
			$domsrule->appendChild($domrules);
		}
		$xml->addChild('metadata',$values["source_metadata"]);
		$xml->addChild('contact',$values["source_contact"]);
		$xml->addChild('comment',$values["source_comment"]);

		return $xml;
	}
}

class SourceFactory {
	public function createInstance($class, $config, $current_account_id, $db, $aLanguages, $aCountries) {
		return new $class($config, $current_account_id, $db, $aLanguages, $aCountries);
	}
}

?>