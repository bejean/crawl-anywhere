<?php
require_once("../init_gpc.inc.php");
require_once("../init.inc.php");

$id = $_GET['id'];
$time = $_GET['time'];

if ($id=='') exit();

//TODO: V4 - Tester
$mg = mg_connect ($config, "", "", "");
if ($mg)
{
	$stmt = new mg_stmt_select($mg,"sources");

	$query = array ("id_source" => intval($id));
	
	if ($time!='') {
	//	$where .= " and createtime = '" . $time . "'";
	} else {
		$RowCount = mg_row_count($mg, "sources_log", $query);
	}
	
	$stmt->setQuery($query);
	$stmt->setSort(array("createtime" => -1));
	$count = $stmt->execute();
	if ($count==0)
	{
		print "Error while reading log !";
		exit();
	}
	//print $s;
	
	if ($time=='') {
		header('Content-type: text/html; charset=UTF-8');
		
		if ($RowCount==0) {
			print ("No log available !");
			exit();
		}
		
		while ($cursor->hasNext())
		{
			$rs = $cursor->getNext();
			print ("<a href='log.php?id=" . $id . "&time=" . $rs["createtime"] . "'>". $rs["createtime"] . "</a><br>");
		}
	}
	else {
		header('Content-type: text/plain; charset=UTF-8');
		print ($rs["log"]);
	}

}
?>