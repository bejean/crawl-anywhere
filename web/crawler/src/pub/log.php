<?php
require_once("../init_gpc.inc.php");
require_once("../init.inc.php");

$id = $_GET['id'];
$row = $_GET['row'];

if ($id=='') exit();

//TODO: V4 - Tester
$mg = mg_connect ($config, "", "", "");
if ($mg)
{
	$stmt = new mg_stmt_select($mg,"sources_log");

	$query = array ("id_source" => intval($id));

	$stmt->setQuery($query);
	$stmt->setSort(array("createtime" => -1));
	$count = $stmt->execute();

	header('Content-type: text/html; charset=UTF-8');

	if ($count==0) {
		print ("No log available !");
		exit();
	}

	$cursor = $stmt->getCursor();
	if ($row=="") {
		$row=0;
		while ($cursor->hasNext()) {
			$rs = $cursor->getNext();

			$t = $rs["createtime"]/1000;
			$created = date('Y-m-d H:i:s', $t);
			//1.37271406957E+12


			print ("<a href='log.php?id=" . $id . "&row=" . $row . "'>". $created . "</a><br>");
			$row++;
		}
	}
	else {
		$cursor->skip($row);
		$rs = $cursor->getNext();
		print ('<pre>' . $rs["log"] . '</pre>');
	}

}
?>