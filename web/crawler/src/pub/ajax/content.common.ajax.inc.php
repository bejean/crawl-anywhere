<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
if ($action=="stopsource" || $action=="pausesource" || $action=="resumesource")
{
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_update($mg, "sources");
		$query = array("id" => intval($_GET["id"]));
		$stmt->setQuery ($query);
		
		if ($action=="stopsource") {
			$stmt->addColumnValue("crawl_process_status", "5");
		}
		if ($action=="pausesource") {
			$stmt->addColumnValue("crawl_process_status", "2");
		}
		if ($action=="resumesource") {
			$stmt->addColumnValue("crawl_process_status", "3");
			$stmt->addColumnValue("crawl_priority", "1");
		}

		$stmt->addColumnValueDate("crawl_nexttime");

		$stmt->execute();
		/*
		if (!$rs)
		{
			$res = "Error";
		}
		else
		{
			$res = "Success";
		}
		*/
		$res = "Success";
	}
	print ($res);
	exit();
}
?>
