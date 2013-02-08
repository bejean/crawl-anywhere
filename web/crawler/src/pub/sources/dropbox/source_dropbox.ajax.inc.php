<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
require_once("../../../init_gpc.inc.php");
require_once("../../../init.inc.php");

$action = POSTGET("action");

if ($action=="dropboxlinkstep1"){
	$callback = $config->get("dropbox.callbackurl");
	
	$url = $config->get("crawlerws.rooturl");
	$url .= "/?action=" . $action;
	$url .= "&callback=" . $callback;
		
	$xmlstr = readurl($url);
	
	try {
		$xml = new SimpleXMLElement($xmlstr);
	} catch(Exception $e) {
		$cLog->log("Error - sources_dropbox.ajax;inc.php - action = " . $action . " - invalid xml - " . $url . " - " . $xmlstr);
	}
	
	if ($xml->errno == '0') {
		$arr = array('status' => 'success', 'info_url' => (String) $xml->info_url, 'timestamp' => (String) $xml->timestamp);
	} else {
		$arr = array('status' => 'error');
	}
	echo json_encode($arr);
	exit();
}

if ($action=="dropboxlinkstep2"){
	$timestamp="";
	if (isset($_GET["dropboxtimestamp"])) $timestamp = $_GET["dropboxtimestamp"];

	if ($timestamp=="") {
		$arr = array('status' => 'error');
		echo json_encode($arr);
		exit();		
	}
	
	$url = $config->get("crawlerws.rooturl");
	$url .= "/?action=" . $action;
	$url .= "&timestamp=" . $timestamp;
	$xmlstr = readurl($url);

	$xml = new SimpleXMLElement($xmlstr);

	if ($xml->errno == '0') {
		$arr = array('status' => 'success', 'token_key' => (String) $xml->token_key, 'token_secret' => (String) $xml->token_secret);
	} else {
		$arr = array('status' => 'error');
	}
	echo json_encode($arr);
	exit();
}

?>
