<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
require_once("../init_gpc.inc.php");
require_once("../init.inc.php");

$action = POSTGET("action");

//TODO: V4
if (($action=="get_available_tags") || ($action=="get_available_collections")) {
	$id = "1";
	if (isset($_GET["uuid"]) && $_GET["uuid"]!="") {
		$uuid = $_GET["uuid"];
		$db = db_connect ($config, "", "", "");
		if ($db)
		{
			$ret = db_get_value($db, "accounts", "id", "id_mysolrserver_instance='" . $uuid . "'", $id);
			if ($ret==-1) $id = "1";
		}
	}
}

//TODO: V4
if ($action=="get_available_tags")
{
	$ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
	$ret .= "<status>ok</status>";

	$aTags = getAvailableTagsCollections($config, true, $id, 'tag');
	if ($aTags!=null) {
		$ret .= "<tags>";
		for ($j=0; $j<=count($aTags); $j++) {
			if ($aTags[$j]!="") {
				$ret .= "<tag>" . $aTags[$j] . "</tag>";
			}
		}
		$ret .= "</tags>";
	}
	$ret .= "</result>";
	header('Content-type: text/xml');
	echo $ret;
	exit ();
}

//TODO: V4
if ($action=="get_available_collections")
{
	$ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
	$ret .= "<status>ok</status>";

	$aCollections = getAvailableTagsCollections($config, true, $id, 'collection');
	if ($aCollections!=null) {
		$ret .= "<collections>";
		for ($j=0; $j<=count($aCollections); $j++) {
			if ($aCollections[$j]!="") {
				$ret .= "<collection>" . $aCollections[$j] . "</collection>";
			}
		}
		$ret .= "</collections>";
	}
	$ret .= "</result>";
	header('Content-type: text/xml');
	echo $ret;
	exit ();
}

//TODO: V4
if ($action=="create_account")
{
	$instance_name =  POSTGET("instance_name");
	$crawler_engine_id = POSTGET("engine_id");
	$user_name = POSTGET("user_name");
	$target_name = POSTGET("target_name");
	$target_parameter = POSTGET("target_parameter");

	$debug_msg = "";

	$db = db_connect ($config, "", "", "");
	if ($db)
	{
		// l'engine existe-t-il ?
		if (db_row_count($db, "engines", "id = " . $crawler_engine_id)== 0) {
			$ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
			$ret .= "<status>ko</status></result>";
			echo $ret;
			$debug_msg .= "Error engine doesn't exist [" . $crawler_engine_id . "]";
			if ($debug) print ("</br>" . $debug_msg);
			exit ();
		}

		// Create account
		$stmt = new db_stmt_insert("accounts");

		$stmt->addColumnValue("id_engine", intval($crawler_engine_id));
		$stmt->addColumnValue("createtime", "", NOW);
		$stmt->addColumnValue("name", $instance_name, "");
		$stmt->addColumnValue("enabled", "1", "");
			
		$s = $stmt->getStatement();
		$rs = $db->Execute($s);
		if (!$rs) {
			$ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
			$ret .= "<status>ko</status></result>";
			echo $ret;
			$debug_msg .= "Error creating crawler account [" . $s . "]";
			if ($debug) print ("</br>" . $debug_msg);
			exit ();
		}
		$id = $db->Insert_ID();

		// Create the target
		$stmt = new db_stmt_insert("targets");

		$stmt->addColumnValue("id_account", intval($id));
		$stmt->addColumnValue("createtime", "", NOW);
		$stmt->addColumnValue("name", $target_name);
		$stmt->addColumnValue("target_parameters", $target_parameter);

		$s = $stmt->getStatement();
		$rs = $db->Execute($s);
		if (!$rs) {
			// Effacer l'account (rollback)

			$ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
			$ret .= "<status>ko</status></result>";
			echo $ret;
			$debug_msg .= "Error creating target [" . $s . "]";
			if ($debug) print ("</br>" . $debug_msg);
			exit ();
		}
		$id_target = $db->Insert_ID();

		// Create user
		$stmt = new db_stmt_insert("users");

		$stmt->addColumnValue("id_account", intval($id));
		$stmt->addColumnValue("createtime", "", NOW);
		$stmt->addColumnValue("user_name", $user_name, "");
		$stmt->addColumnValue("user_level", "1", "");

		$s = $stmt->getStatement();
		$rs = $db->Execute($s);
		if (!$rs) {
			// Effacer l'account (rollback)

			$ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
			$ret .= "<status>ko</status></result>";
			echo $ret;
			$debug_msg .= "Error creating user [" . $s . "]";
			if ($debug) print ("</br>" . $debug_msg);
			exit ();
		}

		// Je mets a jour l'account avec l'id de l'engine et l'id de la target
		$stmt = new db_stmt_update("accounts");
		$stmt->addColumnValue("id_engine", intval($crawler_engine_id), "");
		$stmt->addColumnValue("id_target", intval($id_target), "");
		$stmt->setWhereClause("id = '" . $id . "'");
		$s = $stmt->getStatement();
		$rs = $db->Execute($s);
	}
	else {
		$ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
		$ret .= "<status>ko</status></result>";
		echo $ret;
		$debug_msg .= "Error connecting DB [" . $s . "]";
		if ($debug) print ("</br>" . $debug_msg);
		exit ();
	}
	$ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
	$ret .= "<status>ok</status>";
	$ret .= "<id>" . $id . "</id></result>";
	echo $ret;
	exit ();
}

//TODO: V4
if ($action=="delete_account")
{
	$id = POSTGET("account_id");
	$full_delete = false;
	if (POSTGET("full")=="1")
	$full_delete = true;

	$debug_msg = "";

	$db = db_connect ($config, "", "", "");
	if ($db)
	{
		$stmt = new db_stmt_update("accounts");
		$stmt->addColumnValue("deleted", "1");
		$stmt->addColumnValue("deletedtime", "", NOW);
		$stmt->setWhereClause("id = '" . $id . "'");
		$s = $stmt->getStatement();
			
		$rs = $db->Execute($s);
		if (!$rs)
		{
			$ret = "<?xml version=\"1.0\" encoding=\"utf-8\"?><result>";
			$ret .= "<status>ko</status></result>";
			echo $ret;
			$debug_msg .= "Error deleting account id [" . $s . "]";
			if ($debug) print ("</br>" . $debug_msg);
			exit ();
		}

		if ($full_delete) {
			// delete pages
			$stmt = new db_stmt_delete("pages");
			$stmt->setWhereClause("id_account = '" . $id . "'");
			$s = $stmt->getStatement();
			$rs = $db->Execute($s);

			// delete sources
			$stmt = new db_stmt_delete("sources");
			$stmt->setWhereClause("id_account = '" . $id . "'");
			$s = $stmt->getStatement();
			$rs = $db->Execute($s);

			// delete users
			$stmt = new db_stmt_delete("users");
			$stmt->setWhereClause("id_account = '" . $id . "'");
			$s = $stmt->getStatement();
			$rs = $db->Execute($s);

			// delete targets
			$stmt = new db_stmt_delete("targets");
			$stmt->setWhereClause("id_account = '" . $id . "'");
			$s = $stmt->getStatement();
			$rs = $db->Execute($s);

			// delete account
			$stmt = new db_stmt_delete("accounts");
			$stmt->setWhereClause("id = '" . $id . "'");
			$s = $stmt->getStatement();
			$rs = $db->Execute($s);
		}
	}

	$ret = "OK";
	echo $ret;
	exit ();
}
?>