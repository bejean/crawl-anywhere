<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
require_once("../../init_gpc.inc.php");
require_once("../../init.inc.php");

$action = POSTGET("action");

if ($action=="showaccountlist") {

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$res = "<h2>Accounts</h2>";

		$stmt = new mg_stmt_select($mg, "accounts");
		$stmt->setFields (array("id" => "true", "name" => "true"));
		$stmt->setSort(array( "name" => 1 ));
		$count = $stmt->execute();
		if ($count>0) {
			$cursor = $stmt->getCursor();		
			while ($cursor->hasNext()) {
				$rs = $cursor->getNext();
				$account = $rs["name"];
				$res .= $account;
				$res .= "&nbsp;<a href='#' onClick='editAccount(" . $rs["id"] . ");return false;' title='Edit'><img src='images/button_edit.png'></a>";
				$res .= "<br />";
			}
		}
	}

	$res .= "<br /><br /><br /><br /><br /><br />Add new account<a href='#' onClick='displayAddAccount(); return false;'><img src='images/edit_add_32.png'></a>&nbsp;&nbsp;";

	print $res;
	exit();
}

if ($action=="displayaccount") {

	$res = "<br /><br /><br />";

	$id = $_GET["id"];
	if ($id=="")
	{
		print ("");
		exit();
	}

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_select($mg, "accounts");
		$query = array ("id" => intval($id));
		$stmt->setQuery ($query);
		$count = $stmt->execute();
		if ($count==0) {
			print $s;
			exit();
		}
		
		$cursor = $stmt->getCursor();
		$rs = $cursor->getNext();
		
		$res .= "<form name='account_edit' id='account_edit' action=''><center><table border='0' cellspacing='0' cellpadding='0'>";
		$res .= "<tbody>";

		$res .= "<tr>";
		$res .= "<td class='head'>Id</td>";
		$res .= "<td>" . $rs["id"] . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Name</td>";
		$res .= "<td><input class='editInputText' type='text' name='account_name' id='account_name' value='" . fi($rs["name"]) . "'></td>";
		$res .= "</tr>";

		$engine = $rs["id_engine"];
		$aEngines = getAvailableEngines($config);
		if ($aEngines!=null) {
			$res .= "<tr>";
			$res .= "<td class='head'>Engine</td>";
			$res .= "<td>";
			$res .= "<select id='id_engine' name='id_engine' style='editInputSelect'>";
			foreach ($aEngines as $key => $value)
			{
				$res .= "<option value='" . $key . "'";
				if ($engine==strtolower(trim($key))) $res .= " selected";
				$res .= ">" . $value . "</option>";
			}
			$res .= "</select>";
			$res .= "</td>";
			$res .= "</tr>";
		}

		$target = $rs["id_target"];
		$aTargets = getAvailableTargets($config, $id);
		if ($aTargets!=null) {
			$res .= "<tr>";
			$res .= "<td class='head'>Default target</td>";
			$res .= "<td>";
			$res .= "<select id='id_target' name='id_target' style='editInputSelect'>";
			foreach ($aTargets as $key => $value)
			{
				$res .= "<option value='" . $key . "'";
				if ($target==strtolower(trim($key))) $res .= " selected";
				$res .= ">" . $value . "</option>";
			}
			$res .= "</select>";
			$res .= "</td>";
			$res .= "</tr>";
		}

		$res .= "</table></center>";
		$res .= "<br/>";
		$res .= "<input type='hidden' id='account_id' name ='account_id' value='". $rs["id"] ."'>";
		$res .= "<input type='hidden' id='action' name ='action' value='saveaccount'>";
		$res .= "<div class='menu_button_on_right'><span id='account_save_result'></span>";
		$res .= "<a href='#' onClick='cancelAccount();return false;'><img src='images/button_cancel_32.png'></a>&nbsp;&nbsp;";
		$res .= "<a href='#' onClick='saveAccount();return false;'><img src='images/button_ok_32.png'></a>&nbsp;&nbsp;";

		if ($rs["id"]!=1) {
			$count_user = mg_row_count($mg, "users", array("id_account" => intval($rs["id"])));
			$count_target = mg_row_count($mg, "targets", array("id_account" => intval($rs["id"])));
			$count_source = mg_row_count($mg, "sources", array("id_account" => intval($rs["id"])));
			if ($count_user==0 && $count_target==0 && $count_source==0) $res .= "<a href='#' onClick='deleteAccount();return false;'><img src='images/trash_32.png'></a>&nbsp;&nbsp;";
		}

		$res .= "</div></form>";
	}
	print $res;
	exit();
}

if ($action=="display_add_account")
{
	$res = "<br /><br /><br />";

	$res .= "<form name='account_add' id='account_add'>";

	$res .= "<center><table border='0' cellspacing='0' cellpadding='0'>";
	$res .= "<tbody>";

	$res .= "<tr>";
	$res .= "<td class='head'>Name</td>";
	$res .= "<td><input class='editInputText' type='text' name='account_name' id='account_name'></td>";
	$res .= "</tr>";

	$aEngines = getAvailableEngines($config);
	if ($aEngines!=null) {
		$res .= "<tr>";
		$res .= "<td class='head'>Engine</td>";
		$res .= "<td>";
		$res .= "<select id='id_engine' name='id_engine' style='editInputSelect'>";
		foreach ($aEngines as $key => $value)
		{
			$res .= "<option value='" . $key . "'>" . $value . "</option>";
		}
		$res .= "</select>";
		$res .= "</td>";
		$res .= "</tr>";
	}

	$res .= "</table></center>";

	$res .= "<input type='hidden' id='action' name ='action' value='createaccount'>";

	$res .= "</form>";

	$res .= "<div class='menu_button_on_right'><span id='account_save_result'></span>";
	$res .= "<a href='#' onClick='cancelAccount();return false;'><img src='images/button_cancel_32.png'></a>&nbsp;&nbsp;";
	$res .= "<a href='#' onClick='createAccount();return false;'><img src='images/button_ok_32.png'></a>&nbsp;&nbsp;&nbsp;";
	$res .= "</div>";

	print $res;
	exit();
}

if ($action=="createaccount")
{
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_insert($mg, "accounts", $mg_account_defaults);

		$stmt->addColumnValueDate("createtime");
		$stmt->addColumnValue("name", $_POST["account_name"]);
		$stmt->addColumnValue("id_engine", intval($_POST["id_engine"]));

		if (!$stmt->checkNotNull ($mg_account_not_null)) {
			$res = "Error&nbsp;&nbsp;&nbsp;";
		} else {
			$stmt->execute();
			$res = "Success&nbsp;&nbsp;&nbsp;";
		}
		$res = "Success&nbsp;&nbsp;&nbsp;";
		//$res .= $s;
	}

	print ($res);
	exit();
}

if ($action=="saveaccount")
{
	$id = $_POST["account_id"];
	if ($id=="")
	{
		$res = "Error&nbsp;&nbsp;&nbsp;";
		print ($res);
		exit();
	}
	
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_update($mg, "accounts");
		$query = array ("id" => intval($id));
		$stmt->setQuery ($query);
		$stmt->addColumnValue("name", $_POST["account_name"]);
		$stmt->addColumnValue("id_engine", intval($_POST["id_engine"]));
		$stmt->addColumnValue("id_target", intval($_POST["id_target"]));

		$stmt->execute();
		$res = "Success&nbsp;&nbsp;&nbsp;";
	}
	print ($res);
	exit();
}

if ($action=="deleteaccount")
{
	$id = $_POST["account_id"];
	if ($id=="")
	{
		$res = "Error&nbsp;&nbsp;&nbsp;";
		print ($res);
		exit();
	}
	
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_delete($mg, "accounts");
		$query = array ("id" => intval($id));
		$stmt->setQuery ($query);
		
		$stmt->execute();
		$res = "Success&nbsp;&nbsp;&nbsp;";
	}
	print ($res);
	exit();
}

?>
