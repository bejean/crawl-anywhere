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

if ($action=="showtargetlist") {

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$res = "<h2>Targets</h2>";

		$stmt = new mg_stmt_select($mg, "targets");
		$stmt->setFields (array("id" => "true", "name" => "true"));
		$stmt->setSort(array( "name" => 1 ));
		$count = $stmt->execute();
		if ($count>0) {
			$cursor = $stmt->getCursor();		
			while ($cursor->hasNext()) {
				$rs = $cursor->getNext();
				$res .= $rs["name"];
				$res .= "&nbsp;<a href='#' onClick='edittarget(" . $rs["id"] . ");return false;' title='Edit'><img src='images/button_edit.png'></a>";
				$res .= "<br />";
			}
		}
	}
	
	$res .= "<br /><br /><br /><br /><br /><br />Add new target<a href='#' onClick='displayAddTarget(); return false;'><img src='images/edit_add_32.png'></a>&nbsp;&nbsp;";

	print $res;
	exit();
}

if ($action=="displaytarget") {

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
		$stmt = new mg_stmt_select($mg, "targets");
		$query = array ("id" => intval($id));
		$stmt->setQuery ($query);
		$count = $stmt->execute();
		if ($count==0) {
			print $s;
			exit();
		}
		
		$cursor = $stmt->getCursor();
		$rs = $cursor->getNext();

		$res .= "<form name='target_edit' id='target_edit' action=''><center><table border='0' cellspacing='0' cellpadding='0'>";
		$res .= "<tbody>";

		$res .= "<tr>";
		$res .= "<td class='head'>Id</td>";
		$res .= "<td>" . $rs["id"] . "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Name</td>";
		$res .= "<td>";
		if ($id == '1') {
			$res .= $rs["name"] . " <input type='hidden' id='name' name='name' value='" . $rs["name"] . "'>";
			$res .= "<input type='hidden' id='name' name='name' value='" . $rs["name"] . "'>";
		}
		else {
			$res .= "<input id='name' name='name' value='" . $rs["name"] . "'>";
		}
		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Target type</td>";
		$res .= "<td>";
		$res .= "<select id='target_type' name='target_type' style='editInputSelect'>";
		$res .= "<option value='solr' " . ($rs["target_type"] == 'solr' ? 'selected' : '') . ">Solr</option>";
		$res .= "<option value='es' " . ($rs["target_type"] == 'es' ? 'selected' : '') . ">elasticsearch</option>";
		$res .= "</select>";
		$res .= "</td></tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Target paramater</td>";
		$res .= "<td><input id='target_parameters' name ='target_parameters' class='editInputText' value='" . $rs["target_parameters"] . "'>";
		$res .= "<span class='help'>Optionnal.<br>";
		$res .= "<u>Solr:</u><br>provide Solr core url (http://localhost:8080/solr/crawler/).<br>";
		$res .= "<u>elasticsearch:</u><br>provide cluster url including index name (http://localhost:9200/crawler/).";
		$res .= "</span></td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Ouput queue directory</td>";
		$res .= "<td><input id='queue_dir' name ='queue_dir' class='editInputText' value='" . $rs["queue_dir"] . "'>";
		$res .= "<span class='help'>Optional. Use absolute or relative path. Relative path is relative to crawler installation directory.</span>";
		$res .= "</td>";
		$res .= "</tr>";

		$res .= "<tr>";
		$res .= "<td class='head'>Available for account</td>";
		$res .= "<td>";
		if ($id == '1') {
			$res .= "All <input type='hidden' id='id_account' name='id_account' value='" . $rs["id_account"] . "'>";
		}
		else
		{
			$account = $rs["id_account"];
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
		}
		$res .= "</td>";
		$res .= "</tr>";

		$res .= "</table></center>";

		$res .= "<br/>";

		$res .= "<input type='hidden' id='action' name='action' value='savetarget'>";
		$res .= "<input type='hidden' id='id' name='id' value='" . $id . "'>";

		$res .= "<div class='menu_button_on_right'><span id='target_save_result'></span>";
		$res .= "<a href='#' onClick='cancelTarget();return false;'><img src='images/button_cancel_32.png'></a>&nbsp;&nbsp;";
			
		$res .= "<a href='#' onClick='saveTarget();return false;'><img src='images/button_ok_32.png'></a>&nbsp;&nbsp;";

		if ($rs["id"]!=1) {
			$count_source = mg_row_count($mg, "sources", array("id_target" => intval($rs["id"])));
			$count_source = mg_row_count($mg, "sources", array("id_target" => intval($rs["id"])));
			if ($count_account==0 && $count_source==0) $res .= "<a href='#' onClick='deleteTarget();return false;'><img src='images/trash_32.png'></a>&nbsp;&nbsp;";
		}

		$res .= "</div></form>";
	}
	print $res;
	exit();
}

if ($action=="display_add_target")
{
	$res = "<br /><br /><br />";

	$res .= "<form name='target_add' id='target_add'>";

	$res .= "<center><table border='0' cellspacing='0' cellpadding='0'>";
	$res .= "<tbody>";

	$res .= "<tr>";
	$res .= "<td class='head'>Name</td>";
	$res .= "<td><input class='editInputText' type='text' name='name' id='name'></td>";
	$res .= "</tr>";

	$res .= "<tr>";
	$res .= "<td class='head'>Target type</td>";
	$res .= "<td>";
	$res .= "<select id='target_type' name='target_type' style='editInputSelect'>";
	$res .= "<option value='solr'>Solr</option>";
	$res .= "<option value='es'>elasticsearch</option>";
	$res .= "</select>";
	$res .= "</td></tr>";

	$res .= "<tr>";
	$res .= "<td class='head'>Solr core url</td>";
	$res .= "<td><input id='target_parameters' name='target_parameters' class='editInputText' value=''>";
	$res .= "<span class='help'>Optionnal.<br>";
	$res .= "<u>Solr:</u><br>provide Solr core url (http://localhost:8080/solr/crawler/).<br>";
	$res .= "<u>elasticsearch:</u><br>provide cluster url including index name (http://localhost:9200/crawler/).";
	$res .= "</span></td>";
	$res .= "</tr>";

	$res .= "<tr>";
	$res .= "<td class='head'>Ouput queue directory</td>";
	$res .= "<td><input id='queue_dir' name ='queue_dir' class='editInputText' value=''>";
	$res .= "<span class='help'>Optional. Use absolute or relative path. Relative path is relative to crawler installation directory.</span>";
	$res .= "</td>";
	$res .= "</tr>";

	$aAccounts = getAvailableAccounts($config);
	if ($aAccounts!=null) {
		$res .= "<tr>";
		$res .= "<td class='head'>Available for account</td>";
		$res .= "<td>";
		$res .= "<select id='id_account' name='id_account' style='editInputSelect'>";
		foreach ($aAccounts as $key => $value)
		{
			$res .= "<option value='" . $key . "'";
			if ($key=="1") $res .= " selected";
			$res .= ">" . $value . "</option>";
		}
		$res .= "</select>";
		$res .= "</td>";
		$res .= "</tr>";
	}

	$res .= "</table></center>";

	$res .= "<input type='hidden' id='output_type' name='output_type' value='default'>";
	$res .= "<input type='hidden' id='action' name='action' value='createtarget'>";

	$res .= "</form>";

	$res .= "<div class='menu_button_on_right'><span id='target_save_result'></span>";
	$res .= "<a href='#' onClick='cancelTarget();return false;'><img src='images/button_cancel_32.png'></a>&nbsp;&nbsp;";
	$res .= "<a href='#' onClick='createTarget();return false;'><img src='images/button_ok_32.png'></a>&nbsp;&nbsp;&nbsp;";
	$res .= "</div>";

	print $res;
	exit();
}

if ($action=="createtarget")
{
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_insert($mg, "targets", $mg_target_defaults);

		$stmt->addColumnValueDate("createtime");
		$stmt->addColumnValue("id_account", $_POST["id_account"]);
		$stmt->addColumnValue("name", $_POST["name"]);
		$stmt->addColumnValue("output_type", $_POST["output_type"]);
		$stmt->addColumnValue("target_type", $_POST["target_type"]);
		$stmt->addColumnValue("target_parameters", $_POST["target_parameters"]);
		$stmt->addColumnValue("queue_dir", $_POST["queue_dir"]);
		
		if (!$stmt->checkNotNull ($mg_target_not_null)) {
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

if ($action=="savetarget")
{
	$id = $_POST["id"];
	if ($id=="")
	{
		$res = "Error&nbsp;&nbsp;&nbsp;";
		print ($res);
		exit();
	}
	
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_update($mg, "targets");
		$query = array ("id" => intval($id));
		$stmt->setQuery ($query);

		$stmt->addColumnValue("name", $_POST["name"]);
		$stmt->addColumnValue("id_account", $_POST["id_account"]);
		$stmt->addColumnValue("target_type", $_POST["target_type"]);
		$stmt->addColumnValue("target_parameters", $_POST["target_parameters"]);
		$stmt->addColumnValue("queue_dir", $_POST["queue_dir"]);
		
		$stmt->execute();
		$res = "Success&nbsp;&nbsp;&nbsp;";
	}
	print ($res);
	exit();
}

if ($action=="deletetarget")
{
	$id = $_POST["id"];
	if ($id=="")
	{
		$res = "Error&nbsp;&nbsp;&nbsp;";
		print ($res);
		exit();
	}
	
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_delete($mg, "targets");
		$query = array ("id" => intval($id));
		$stmt->setQuery ($query);
	
		$stmt->execute();
		$res = "Success&nbsp;&nbsp;&nbsp;";
	}
	print ($res);
	exit();
}

?>
