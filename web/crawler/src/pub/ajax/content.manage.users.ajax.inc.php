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

if ($action=="showuserlist") {

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$res = "<h2>Users</h2>";

		$stmt = new mg_stmt_select($mg, "users");
		$stmt->setFields (array("id" => "true", "user_name" => "true"));
		$stmt->setSort(array( "user_name" => 1 ));
		$count = $stmt->execute();
		if ($count>0) {
			$cursor = $stmt->getCursor();		
			while ($cursor->hasNext()) {
				$rs = $cursor->getNext();
				$user = $rs["user_name"];
				$res .= $user;
				$res .= "&nbsp;<a href='#' onClick='editUser(" . $rs["id"] . ");return false;' title='Edit'><img src='images/button_edit.png'></a>";
				$res .= "<br />";
			}
		}		
	}

	$res .= "<br /><br /><br /><br /><br /><br />Add new user<a href='#' onClick='displayAddUser(); return false;'><img src='images/edit_add_32.png'></a>&nbsp;&nbsp;";

	print $res;
	exit();
}

if ($action=="displayuser") {

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
		$stmt = new mg_stmt_select($mg, "users");
		$query = array ("id" => intval($id));
		$stmt->setQuery ($query);
		$count = $stmt->execute();
		if ($count==0) {
			print $s;
			exit();
		}
		
		$cursor = $stmt->getCursor();
		$rs = $cursor->getNext();
	
		$res .= "<form name='user_edit' id='user_edit' action=''><center><table border='0' cellspacing='0' cellpadding='0'>";
		$res .= "<tbody>";

		$res .= "<tr>";
		$res .= "<td class='head'>Id</td>";
		$res .= "<td>" . $rs["id"] . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Name</td>";
		$res .= "<td>" . $rs["user_name"];
		if ($user->getId()=="1") {
			$res .= "&nbsp;<a href='#' onClick='logonAs(\"" . $rs["user_name"] . "\")';return false;'><img src='images/login.png' title='Logon as'></a>";
		}
		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Password</td>";
		$res .= "<td><input class='editInputTextSmall' type='password' name='user_password' id='user_password' value=''> (leave empty will not change the password)</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Confirm password</td>";
		$res .= "<td><input class='editInputTextSmall' type='password' name='user_password2' id='user_password2' value=''></td>";
		$res .= "</tr>";

		if ($rs["id"]!=1 && $user->getLevel() > 1) {
			$res .= "<tr>";
			$res .= "<td class='head'>Requiere change password at next logon</td>";
			$res .= "<td><input type='checkbox' name='change_password' id='change_password' value='1'";
			if ($rs["change_password_next_logon"]=="1") $res .= " checked";
			$res .= "></td>";
			$res .= "</tr>";
		}

		if ($user->getLevel() ==2) {
			$res .= "<tr>";
			$res .= "<td class='head'>Level</td>";
			$res .= "<td>";

			if ($rs["id"]!=1) {
				$res .= "<select id='user_level' name='user_level' style='editInputSelect' onChange='userLevelOnChange();'>";
				$res .= "<option value='0'";
				if ($rs["user_level"]=="0") $res .= " selected";
				$res .= ">" . getUserLevelLabel("0") . "</option>";
				$res .= "<option value='1'";
				if ($rs["user_level"]=="1") $res .= " selected";
				$res .= ">" . getUserLevelLabel("1") . "</option>";
				$res .= "<option value='2'";
				if ($rs["user_level"]=="2") $res .= " selected";
				$res .= ">" . getUserLevelLabel("2") . "</option>";
				$res .= "</select>";
			}
			else {
				$res .= getUserLevelLabel($rs["user_level"]);
				$res .= "<input type='hidden' id='user_level' name ='user_level' value='". $rs["user_level"] ."'>";
			}

			$res .= "</td>";
			$res .= "</tr>";

			$res .= "<tr>";
			$res .= "<td class='head'>Belongs to account</td>";
			$res .= "<td>";
				
			$res .= "<div id='user_belong_all' name='user_belong_all'";
			if ($rs["user_level"]=="2") {
				$res .= " style='display: block'";
			}
			else
			{
				$res .= " style='display: none'";
			}
			$res .= ">All</div>";

			$res .= "<div id='user_belong' name='user_belong'";
			if ($rs["user_level"]=="2") {
				$res .= " style='display: none'";
			}
			else
			{
				$res .= " style='display: block'";
			}
			$res .= ">";
				
			$account = $rs["id_account"];
			$aAccounts = getAvailableAccounts($config);
			if ($aAccounts!=null) {
				$res .= "<select id='id_account' name='id_account' style='editInputSelect'>";
				foreach ($aAccounts as $key => $value)
				{
					$res .= "<option value='" . $key . "'";
					if (($account==strtolower(trim($key))) || ($account=="0" && $key=="1")) $res .= " selected";
					$res .= ">" . $value . "</option>";
				}
				$res .= "</select>";
			} else {
				
			}

			$res .= "</div>";
			$res .= "</td>";
			$res .= "</tr>";
		}
		$res .= "</table></center>";

		$res .= "<br/>";

		$res .= "<input type='hidden' id='user_id' name ='user_id' value='". $rs["id"] ."'>";
		$res .= "<input type='hidden' id='action' name ='action' value='saveuser'>";

		$res .= "<div class='menu_button_on_right'><span id='user_save_result'></span>";
		$res .= "<a href='#' onClick='cancelUser();return false;'><img src='images/button_cancel_32.png' title='Cancel'></a>&nbsp;&nbsp;";

		$res .= "<a href='#' onClick='saveUser();return false;'><img src='images/button_ok_32.png' title='Save and close'></a>&nbsp;&nbsp;";

		if ($rs["id"]!=1 && $user->getLevel() > 1) {
			$res .= "<a href='#' onClick='deleteUser();return false;'><img src='images/trash_32.png' title='Delete'></a>&nbsp;&nbsp;&nbsp;";
		}

		$res .= "</div></form>";
	}
	print $res;
	exit();
}

if ($action=="display_add_user")
{
	$res = "<br /><br /><br />";

	$res .= "<form name='user_add' id='user_add'>";

	$res .= "<center><table border='0' cellspacing='0' cellpadding='0'>";
	$res .= "<tbody>";

	$res .= "<tr>";
	$res .= "<td class='head'>Name</td>";
	$res .= "<td><input class='editInputText' type='text' name='user_name' id='user_name'></td>";
	$res .= "</tr>";

	$res .= "<tr>";
	$res .= "<td class='head'>Password</td>";
	$res .= "<td><input class='editInputTextSmall' type='password' name='user_password' id='user_password' value=''></td>";
	$res .= "</tr>";

	$res .= "<tr>";
	$res .= "<td class='head'>Confirm password</td>";
	$res .= "<td><input class='editInputTextSmall' type='password' name='user_password2' id='user_password2' value=''></td>";
	$res .= "</tr>";

	$res .= "<tr>";
	$res .= "<td class='head'>Requiere change password at next logon</td>";
	$res .= "<td><input type='checkbox' name='change_password' id='change_password' value='1'></td>";
	$res .= "</tr>";

	$res .= "<tr>";
	$res .= "<td class='head'>Level</td>";
	$res .= "<td>";
	$res .= "<select id='user_level' name='user_level' style='editInputSelect' onChange='userLevelOnChange();'>";
	$res .= "<option value='0'>" . getUserLevelLabel("0") . "</option>";
	$res .= "<option value='1'>" . getUserLevelLabel("1") . "</option>";
	$res .= "<option value='2'>" . getUserLevelLabel("2") . "</option>";
	$res .= "</select>";
	$res .= "</td>";
	$res .= "</tr>";

	$res .= "<tr>";
	$res .= "<td class='head'>Belongs to account</td>";
	$res .= "<td>";

	$res .= "<div id='user_belong_all' name='user_belong_all' style='display: none'>All</div>";

	$res .= "<div id='user_belong' name='user_belong' style='display: block'>";

	$account = 1;
	$aAccounts = getAvailableAccounts($config);
	if ($aAccounts!=null) {
		$res .= "<select id='id_account' name='id_account' style='editInputSelect'>";
		foreach ($aAccounts as $key => $value)
		{
			$res .= "<option value='" . $key . "'";
			if ($account==strtolower(trim($key))) $res .= " selected";
			$res .= ">" . $value . "</option>";
		}
		$res .= "</select>";
	}

	$res .= "</div>";
	$res .= "</td>";
	$res .= "</tr>";


	$res .= "</table></center>";

	$res .= "<input type='hidden' id='action' name ='action' value='createuser'>";

	$res .= "</form>";

	$res .= "<div class='menu_button_on_right'><span id='user_save_result'></span>";
	$res .= "<a href='#' onClick='cancelUser();return false;'><img src='images/button_cancel_32.png'></a>&nbsp;&nbsp;";
	$res .= "<a href='#' onClick='createUser();return false;'><img src='images/button_ok_32.png'></a>&nbsp;&nbsp;&nbsp;";
	$res .= "</div>";

	print $res;
	exit();
}

if ($action=="createuser")
{
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_insert($mg, "users", $mg_user_defaults);

		$stmt->addColumnValueDate("createtime");
		$stmt->addColumnValue("user_name", $_POST["user_name"]);
		if ($_POST["user_level"]=="2") {
			$stmt->addColumnValue("id_account", 0);
		} else {
			$stmt->addColumnValue("id_account", intval($_POST["id_account"]));
		}
		$stmt->addColumnValue("user_password", $_POST["user_password"]);
		$stmt->addColumnValue("user_level", $_POST["user_level"]);

		if (isset($_POST["change_password"]) && (trim($_POST["change_password"] == "1"))) {
			$stmt->addColumnValue("change_password_next_logon", "1");
		}
		else {
			$stmt->addColumnValue("change_password_next_logon", "0");
		}
		
		if (!$stmt->checkNotNull ($mg_user_not_null)) {
			$res = "Error&nbsp;&nbsp;&nbsp;";
		} else {
			$stmt->execute();
			$res = "Success&nbsp;&nbsp;&nbsp;";
		}
		$res = "Success&nbsp;&nbsp;&nbsp;";
	}

	print ($res);
	exit();
}

if ($action=="saveuser")
{
	$id = $_POST["user_id"];
	if ($id=="")
	{
		$res = "Error&nbsp;&nbsp;&nbsp;";
		print ($res);
		exit();
	}
	
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_update($mg, "users");
		$query = array ("id" => intval($id));
		$stmt->setQuery ($query);

		if ($_POST["user_level"]=="2") {
			$stmt->addColumnValue("id_account", 0);
		} else {
			if (isset($_POST["id_account"])) $stmt->addColumnValue("id_account", intval($_POST["id_account"]));
		}
		if (isset($_POST["user_password"]) && (trim($_POST["user_password"] != ""))) {
			$stmt->addColumnValue("user_password", trim($_POST["user_password"]));
		}
		
		if (isset($_POST["user_level"])) $stmt->addColumnValue("user_level", $_POST["user_level"]);
		
		if (isset($_POST["change_password"]) && (trim($_POST["change_password"] == "1"))) {
			if (isset($_POST["user_password"]) && (trim($_POST["user_password"] != "")) && $_POST["user_id"] == $user->getId()) {
				$stmt->addColumnValue("change_password_next_logon", "0");
			}
			else {
				$stmt->addColumnValue("change_password_next_logon", "1");
			}
		}
		else {
			$stmt->addColumnValue("change_password_next_logon", "0");
		}
		
		$stmt->execute();
		$res = "Success&nbsp;&nbsp;&nbsp;";
	}
	print ($res);
	exit();
}

if ($action=="deleteuser")
{
	$id = $_POST["user_id"];
	if ($id=="")
	{
		$res = "Error&nbsp;&nbsp;&nbsp;";
		print ($res);
		exit();
	}
	
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_delete($mg, "users");
		$query = array ("id" => intval($id));
		$stmt->setQuery ($query);
		
		$stmt->execute();
		$res = "Success&nbsp;&nbsp;&nbsp;";
	}
	print ($res);
	exit();
}

?>
