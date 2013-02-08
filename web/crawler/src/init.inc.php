<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
$rootpath = dirname(__FILE__);

error_reporting(E_ERROR);
header('Content-Type: text/html; Charset=UTF-8');
set_time_limit (300);
//require_once("lib/adodb5/adodb.inc.php");
require_once("lib/config.class.inc.php");
require_once("lib/mongo.inc.php");
require_once("lib/string.inc.php");
require_once("lib/user.inc.php");
require_once("lib/log.class.inc.php");

$session_name = 'crawlanywhere';
session_name($session_name);
session_start();


//
// $config initialisation
//
if (isset($_GET["config"]) && $_GET["config"]!="") {
	$config_file =$_GET["config"];
}
else {
	if (isset($_POST["param"])) {
		// 	var param = $.base64.encode(user + '|' + name + '|' + plugins + '|' + document.location);
		$param=explode("|",base64_decode($_POST["param"]));
		$config_file = trim($param[2]);
	}
	else {
		if (isset($_SESSION["APP_Parent"]) && $_SESSION["APP_Parent"]!="") {
			$config_file ="config_" . $_SESSION["APP_Parent"] . ".ini";
		}
		else {
			if (isset($_SESSION["config"])) {
				$config_file = $_SESSION["config"];
			}
			else {
				$config_file ="config.ini";
			}
		}
	}
}
if (!isset($_SESSION["config"]) || $_SESSION["config"] != $config_file) {
	session_unset();
	$_SESSION["config"] = $config_file;
}

$configFilePath = $rootpath . "/config/" . $config_file;
if (!file_exists($configFilePath)) {
	echo "Configuration file not found [".$configFilePath."]";
	exit();
}
$config = new ConfigTool();
$config->setConfigFromFile($configFilePath);

$debug=($config->get("application.debug")=="1");
$debug_file = $config->getDefault("application.logfile", "");

$cLog = new Logger($debug_file);
if ($debug) $cLog->setDebug(true);

$theme_name = $config->getDefault("application.theme_name", "ca");

//
// $user initialisation
//
if (isset($_SESSION["user"]))
{
	$user = $_SESSION["user"];
}
else
{
	$user = new User();
	$_SESSION["user"] = $user;
}

//
// $id_account_current
//
if (isset($_SESSION["id_account_current"]) && !isset($_POST["id_account"]))
{
	$id_account_current = $_SESSION["id_account_current"];
}
else
{
	if ($user->getLevel()=="2") {
		if (isset($_POST["id_account"])) {
			$id_account_current = $_POST["id_account"];
		} else {
			$id_account_current = "1";
		}
	} else {
		$id_account_current = $user->getIdAccount();
	}
	$_SESSION["id_account_current"] = $id_account_current;
}

$cache_enabled = ($config->getDefault("crawler.cache.type", "") != "");

//$isEnterprise = ($config->get("application.enterprise") == '1');

if (!isset($_SESSION["source_types"]))
{
	$plugins_available = $config->getDefault("plugins.available", "");
	$aplugins_available = array_map('trim',explode(',', $plugins_available));

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_select($mg, "plugins");
		$stmt->setQuery(array( "type" => "cnx" ));
		$stmt->setSort(array( "name" => 1 ));
		$count = $stmt->execute();
		if ($count>0) {
			$cursor = $stmt->getCursor();
			while ($cursor->hasNext()) {
				$rs = $cursor->getNext();
					
				$id = $rs["id"];
				$name = $rs["name"];
				$mnemo = $rs["mnemo"];
				$link = $rs["link"];
					
				if (!empty($plugins_available)) {
					if (in_array($mnemo, $aplugins_available)) {
						$aSourceTypes[$id]=array("name" => $name, "mnemo" => $mnemo, "link" => $link);
					}
				} else {
					$aSourceTypes[$id]=array("name" => $name, "mnemo" => $mnemo, "link" => $link);
				}
			}
		}
	}
	$_SESSION["source_types"] = $aSourceTypes;
}
else
{
	$aSourceTypes = $_SESSION["source_types"];
}


///////////////////////////////////////////////////////////
function isAvailableMenuItem($Items, $Item, $config)
{
	if ((trim($Items) == "") || ($Items=="undefined"))
		return true;

	$aItems = explode(",", $config->get("pages.available"));
	foreach ($aItems as $value) {
		if (trim($value) == $Item)
			return true;
	}

	return false;
}


/*
 * MongoDB data constraints
 */
$mg_source_defaults = array(
		"id_target" => 1, 
		"deleted" => "0", 
		"type" => "0", 
		"enabled" => "1", 
		"crawl_status" => "0", 
		"crawl_status_message" => "", 
		"crawl_lastpagecount" => 0, 
		"crawl_pagecount" => 0, 
		"crawl_pagecount_success" => 0, 
		"crawl_pagecount_pushed" => 0, 
		"crawl_process_status" => "0", 
		"crawl_minimal_period" => "0",
		"crawl_firstcompleted" => "0",
		"crawl_mode" => "0",
		"language" => "xx",
		"country" => "unknown");
$mg_source_not_null = array("id_account", "createtime", "name");

$mg_account_defaults = array(
		"id_target" => 1,
		"id_engine" => 1,
		"deleted" => "0",
		"enabled" => "1");
$mg_account_not_null = array("name");

$mg_engine_defaults = array(
		"enabled" => "1");
$mg_engine_not_null = array("name");

$mg_target_defaults = array(
		"output_type" => "default",
		"output_type" => "solr"
		);
$mg_target_not_null = array("id_account", "name");

$mg_user_defaults = array(
		"id_account" => 1,
		"user_level" => "0",
		"change_password_next_logon" => "0"
);
$mg_user_not_null = array("user_name", "user_password", "user_level", "change_password_next_logon");

///////////////////////////////////////////////////////////
function POSTGET($param)
{
	if (isset($_POST[$param]))
	{
		if ($_POST[$param]!="")
			return $_POST[$param];
	}
	if (isset($_GET[$param]))
	{
		if ($_GET[$param]!="")
			return $_GET[$param];
	}
	return "";
}


///////////////////////////////////////////////////////////
function readurl($url) {

	if (trim($url) == "")
	{
		return "";
	}

	if (!parse_url ($url))
	{
		return "";
	}

	// Creation d'une ressource CURL
	$ch = curl_init();

	// definition de l'URL et autres options
	curl_setopt($ch, CURLOPT_URL, $url);
	//curl_setopt($ch, CURLOPT_USERPWD, $username . ":" . $password);
	curl_setopt($ch, CURLOPT_POST, false);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER,true);
	curl_setopt($ch, CURLOPT_TIMEOUT,300);
	curl_setopt($ch, CURLOPT_FAILONERROR, 1);

	// Recuperation de l'URL et passage au navigateur
	$response = curl_exec($ch);
	//$errmsg = "";
	$errno = curl_errno($ch);
	if ($errno) {
		//$errmsg = curl_error($ch);
		$errno = curl_getinfo($ch, CURLINFO_HTTP_CODE);
		curl_close($ch);
		return $errno;
	}
	curl_close($ch);
	return $response;
}


///////////////////////////////////////////////////////////
function write_file($filename,$newdata) {
	$f=fopen($filename,"w");
	fwrite($f,$newdata);
	fclose($f);
}

///////////////////////////////////////////////////////////
function getAvailableTagsCollections($config, $includeDisabled, $id_account, $field) {
	$aTagsFinal = null;
	$mg = mg_connect ($config, "", "", "");
	if ($mg) {
		if ($includeDisabled) 
			$query = array ('$and' => array('$or' => array(array("deleted" => "0"), array("deleted" => NULL))), array("id_account" => $id_account));
		else
			$query = array ('$and' => array(array('$or' => array(array("deleted" => "0"), array("deleted" => NULL))), array("enabled" => "1"), array("id_account" => $id_account)));
		
		$stmt = new mg_stmt_distinct($mg, "sources");
		$stmt->setQuery($query);
		$stmt->setKey($field);
		$ar = $stmt->command();

		if (count($ar)>0) {
			foreach ($ar as $tags) {
				$tags = strtolower(trim($tags));
				$tags = str_replace(" ", "", $tags);
				if ($tags!="") {
					if ($aTagsFinal == null) {
						$aTagsFinal = explode(",", $tags);
					}
					else {
						$aTags = explode(",", $tags);
						$aTagsFinal = array_merge ($aTagsFinal, $aTags);
					}
				}
			}
			$aTagsFinal = array_unique($aTagsFinal);
			sort($aTagsFinal);
		}
	}
	return $aTagsFinal;
}

///////////////////////////////////////////////////////////
function getAvailableSourceType($config, $includeDisabled, $id_account) {
	$aTypes = null;
	$mg = mg_connect ($config, "", "", "");
	if ($mg) {
		if ($includeDisabled) 
			$query = array ('$and' => array('$or' => array(array("deleted" => "0"), array("deleted" => NULL))), array("id_account" => $id_account));
		else
			$query = array ('$and' => array(array('$or' => array(array("deleted" => "0"), array("deleted" => NULL))), array("enabled" => "1"), array("id_account" => $id_account)));
		
		$stmt = new mg_stmt_distinct($mg, "sources");
		$stmt->setQuery($query);
		$stmt->setKey("type");
		$aTypes = $stmt->command();
	}
	return $aTypes;
}

///////////////////////////////////////////////////////////
function getAvailableTargets($config, $account) {
	$aTargetsFinal = null;
	$mg = mg_connect ($config, "", "", "");
	if ($mg) {
		$stmt = new mg_stmt_select($mg, "targets");
		$stmt->setFields (array("id" => "true", "name" => "true"));
		$query = array ('$or' => array(array("id_account" => 0), array("id_account" => intval($account))));
		
		$stmt->setQuery($query);
		
		$stmt->setSort(array( "name" => 1 ));
		$count = $stmt->execute();
		if ($count>0) {
			$aTargetsFinal = array();
			$cursor = $stmt->getCursor();
			while ($cursor->hasNext()) {
				$rs = $cursor->getNext();
				$id = strtolower(trim($rs['id']));
				$name = trim($rs['name']);
				$aTargetsFinal[$id] = $name;
			}
		}
	}
	return $aTargetsFinal;
}

///////////////////////////////////////////////////////////
function getAvailableAccounts($config) {
	$aAccountsFinal = null;
	$mg = mg_connect ($config, "", "", "");
	if ($mg) {
		$stmt = new mg_stmt_select($mg, "accounts");
		$stmt->setFields (array("id" => "true", "name" => "true"));
		$stmt->setSort(array( "name" => 1 ));
		$count = $stmt->execute();
		if ($count>0) {
			$aAccountsFinal = array();
			$cursor = $stmt->getCursor();
			while ($cursor->hasNext()) {
				$rs = $cursor->getNext();
				$id = strtolower(trim($rs['id']));
				$name = trim($rs['name']);
				$aAccountsFinal[$id] = $name;
			}
		}
	}
	return $aAccountsFinal;
}

///////////////////////////////////////////////////////////
function getAvailableEngines($config) {
	$aEnginesFinal = null;
	$mg = mg_connect ($config, "", "", "");
	if ($mg) {
		$stmt = new mg_stmt_select($mg, "accounts");
		$stmt->setFields (array("id" => "true", "name" => "true"));
		$stmt->setSort(array( "name" => 1 ));
		$count = $stmt->execute();
		if ($count>0) {
			$aAccountsFinal = array();
			$cursor = $stmt->getCursor();
			while ($cursor->hasNext()) {
				$rs = $cursor->getNext();
				$id = strtolower(trim($rs['id']));
				$name = trim($rs['name']);
				$aEnginesFinal[$id] = $name;
			}
		}
	}
	return $aEnginesFinal;
}

///////////////////////////////////////////////////////////
function getStartingUrls($url) {
	if (substr($url, 0, 1) == "<") {
		$urlXml = simplexml_load_string($url);
		$result = $urlXml->xpath('/urls/url');
		$ret = "";
		$sep = "";
		while(list( , $node) = each($result)) {
			$ret .= $sep . "<a href='" . (string)$node->url . "' target='_blank'>" . wordwrap((string)$node->url, 30, "\n", true) . "</a>";
			$sep = "<br />";
		}
	} else {
		$ret = "<a href='" . $url . "' target='_blank'>" . wordwrap($url, 30, "\n", true) . "</a>";
	}
	return $ret;
}

///////////////////////////////////////////////////////////
function getStartingUrlsAsString($url, $sep) {
	if (substr($url, 0, 1) == "<") {
		$urlXml = simplexml_load_string($url);
		$result = $urlXml->xpath('/urls/url');
		$ret = "";
		while(list( , $node) = each($result)) {
			if ($ret!='') $ret .= $sep;
			$ret .= wordwrap((string)$node->url, 30, "\n", true);
		}
	} else {
		$ret = wordwrap($url, 30, "\n", true);
	}
	return $ret;
}

///////////////////////////////////////////////////////////
function getUserLevelLabel($level) {
	if ($level=="0") return "Read only";
	if ($level=="1") return "Account administrator";
	if ($level=="2") return "System administrator";
	return "";
}

//////////////////////////////////////////////////////////
function encodeForInput($value) {
	return htmlentities($value, ENT_QUOTES, "UTF-8", false);
}


//////////////////////////////////////////////////////////
function require_once_all($files) {
	foreach (glob($files) as $filename) {
		require_once $filename;
	}
}

function uuid()
{
	$md5 = md5(uniqid('', true));
	return substr($md5, 0, 8 ) . '-' .
			substr($md5, 8, 4) . '-' .
			substr($md5, 12, 4) . '-' .
			substr($md5, 16, 4) . '-' .
			substr($md5, 20, 12);
}

?>
