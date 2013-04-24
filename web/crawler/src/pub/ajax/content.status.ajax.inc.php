<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
require_once("../../init_gpc.inc.php");
require_once("../../init.inc.php");

$action = $_GET["action"];
$limit = $_GET["limit"];
if ($limit=="") $limit="0";

require_once("content.common.ajax.inc.php");

require_once_all('../sources/*.inc.php');

if (!isset($_SESSION["crawl_countries"]))
{
	$handle = fopen("../ressources/code_countries.txt", "rb");
	while (!feof($handle) ) {
		$line = trim(fgets($handle));
		if ($line!="")
		{
			$parts = explode(';', $line);
			$aCountries[trim($parts[1])] = ucwords(strtolower(trim($parts[0])));
		}
	}
	fclose($handle);
	$_SESSION["crawl_countries"] = $aCountries;
}
else
{
	$aCountries = $_SESSION["crawl_countries"];
}

if (!isset($_SESSION["crawl_languages"]))
{
	$handle = fopen("../ressources/code_languages.txt", "rb");
	while (!feof($handle) ) {
		$line = trim(fgets($handle));
		if ($line!="")
		{
			$parts = explode(';', $line);
			$aLanguages[trim($parts[0])] = trim($parts[1]);
		}
	}
	fclose($handle);
	$_SESSION["crawl_languages"] = $aLanguages;
}
else
{
	$aLanguages = $_SESSION["crawl_languages"];
}

if ($action=="showstatus")
{
	$res .= "<center><table>";

	/*
	 $res .= "<tr><td class='head'>Logged as</td><td>";
	$res .= $user->getName();
	if (isset($_SESSION["APP_Parent"]) && $_SESSION["APP_Parent"]!="") {
	$res .= " (" . $_SESSION["APP_Parent"] . ")";
	}
	$res .= "</td></tr>";
	*/
	if (!$_SESSION["mysolrserver_url"]) {
		if ($user->getLevel()==2) {
			$res .= "<tr><td class='head'>Crawler DB</td><td>";
			$res .= $config->get("database.dbname");
			$res .= "</td></tr>";
		}

		$crawler_witness_files_path = $config->get("crawler.witness_files_path");
		if ($crawler_witness_files_path!="")
		{
			// Crawler state
			$res .= "<tr><td class='head'>Crawler state</td><td>";
			//$filecount = count(glob($crawler_witness_files_path . "/crawler*.pid"));
			if (file_exists ($crawler_witness_files_path . "/crawler.pid"))
				//if ($filecount>0)
				$res .= "Crawler is running";
			else
				$res .= "Crawler is not running";
			$res .= "</td></tr>";

			$ndx = 1;
			$monitor = trim($config->get("monitor.process_".$ndx));
			while ($monitor!="" &&  $monitor!="undefined") {
				$aItems = explode("|", $monitor);
				if (count($aItems)==3) {
					$res .= "<tr><td class='head'>" . $aItems[0] . " state</td><td>";
					$filecount = count(glob($aItems[2] . "/j*.xml"));
					if (file_exists ($crawler_witness_files_path . "/" . $aItems[1]))
						$res .= $aItems[0] . " is running - queue size = " . $filecount . " item(s)";
					else
						$res .= $aItems[0] . " is not running - queue size = " . $filecount . " item(s)";
					$res .= "</td></tr>";
				}
				$ndx++;
				$monitor = trim($config->get("monitor.process_".$ndx));
			}
		}
		else
		{
			$res .= "<tr><td class='head'>Crawler state</td><td>";
			$res .= "Unable to provide crawler state";
			$res .= "</td></tr>";
		}
	}

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$res .= "<tr><td class='head'>Number of sources</td><td>";
		$res .= mg_row_count($mg, "sources", array('$and' => array(array("deleted" => 0), array("id_account" => intval($id_account_current)))));
		$res .= "</td></tr>";
			
		$res .= "<tr><td class='head'>Number of crawled URLs</td><td>";
		$res .= mg_row_count($mg, "pages", array("id_account" => $id_account_current));
		$res .= "</td></tr>";

		$res .= "<tr><td class='head'>Number of enabled sources</td><td>";
		$res .= mg_row_count($mg, "sources", array('$and' => array(array("enabled" => 1), array("deleted" => 0), array("id_account" => intval($id_account_current)))));				
		$res .= "</td></tr>";

		$res .= "<tr><td class='head'>Number of sources to be crawled</td><td>";
		// TODO: V4 - $stmt->addWhereClause("enabled = 1 and deleted = 0 and (crawl_nexttime is null or crawl_nexttime <= now()) and id_account = " . $id_account_current);
		$res .= mg_row_count($mg, "sources", array('$and' => array(array("enabled" => 1), array("deleted" => 0), array("id_account" => intval($id_account_current)))));				
		$res .= "</td></tr>";

		$res .= "<tr><td class='head'>Number of sources to be crawled for the first time</td><td>";
		// TODO: V4 - $stmt->addWhereClause("enabled = 1 and deleted = 0 and crawl_nexttime is null and id_account = " . $id_account_current);
		$res .= mg_row_count($mg, "sources", array('$and' => array(array("enabled" => 1), array("deleted" => 0), array("id_account" => intval($id_account_current)))));				
		$res .= "</td></tr>";
	}

	$res .= "</table></center>";
	print $res;
	exit();
}


if ($action=="showrunning")
{
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_select($mg, "sources");
		//$query_status = array('$or' => array(array(crawl_process_status => "1"), array(crawl_process_status => "2"), array(crawl_process_status => "3"), array(crawl_process_status => "4")));
		//$query = array('$and' => array(array(enabled => "1"), array(deleted => "0"), array(id_account => $id_account_current), array(id_account => $id_account_current), $query_status));
		//TODO: V4
		/*
		 $stmt->setWhereClause("
		 	(
		 		enabled = 1 
		 		and deleted = 0 
		 		and id_account = " . $id_account_current . " 
		 		and not crawl_process_status=0 
		 		and (
		 			(crawl_process_status=2) 
		 			or (crawl_process_status=3) 
		 			or (crawl_process_status=4) 
		 			or (crawl_lasttime_start > crawl_lasttime_end) 
		 			or (crawl_lasttime_end is null and crawl_lasttime_start is not null)
		 			)
		 	) 
		 	and 
		 	(
		 		(crawl_process_status=2) 
		 		or (crawl_process_status=3) 
		 		or (crawl_process_status=4) 
		 		or (running_crawl_lastupdate is not null and running_crawl_lastupdate > DATE_SUB(NOW(),INTERVAL 5 MINUTE))
		 	)
		 ");
		 

		 
		 $ts = new MongoDate(strtotime("now") - (5*60));
		 query_running_crawl_lastupdate = array("running_crawl_lastupdate" => array('$gt' => $ts));
		 
		$query_1 = array('$and' => array(enabled => "1", deleted => "0", id_account => intval($id_account_current), array('$not' => array(crawl_process_status => "0"))));
		*/		
		
		$query = array('$and' => array(array(enabled => "1"), array(deleted => "0"), array(id_account => intval($id_account_current)), array(crawl_process_status => array('$ne' => "0"))));

		$stmt->setQuery($query);
		$stmt->setSort(array( "crawl_lasttime_start" => 1 ));
		$count = $stmt->execute();		
		if ($count==0)
		{
			print "";
			exit();
		}
		$cursor = $stmt->getCursor();
		
		$res .= "<center><table>";
		$res .= "<tr><th style='width:25%;'>Title</th><th>Start crawl date</th><th style='width: 70px;'>Processed<br/>pages</th><th style='width: 70px;'>Remaining<br/>pages</th><th style='width: 30px;'>Status</th><th style='width: 30px;'>Action</th>";
		if (!$_SESSION["mysolrserver_url"] && $cache_enabled  && $rs["type"]=='1') {
			$res .= "<th style='width: 30px; text-align: center;'>Status</th><th style='width: 30px; text-align: center;'>Action</th>";
		}
		$res .= "</tr><tbody>";

		while ($cursor->hasNext())
		{
			$rs = $cursor->getNext();
			$processing_info = "";
			if (!$_SESSION["mysolrserver_url"]) {
				$processing_info = $rs["processing_info"];
		
				if (startswith($processing_info, "<")) {
					$xml_processing_info = simplexml_load_string($processing_info);
					$elapsedtime = (string)$xml_processing_info->elapsedtime;
					$elapsedtime = $elapsedtime / 1000;
					$currentspeed = (string)$xml_processing_info->currentspeed;
					$averagespeed = $rs["running_crawl_item_processed"] / $elapsedtime;
					$estimatedtime = $rs["running_crawl_item_to_process"] / $averagespeed;
										
					$estimatedtime_unit = "sec";
					if ($estimatedtime>=3600) {
						$estimatedtime = $estimatedtime / 3600;
						$estimatedtime_unit = "h";
					} else {
						if ($estimatedtime>=60) {
							$estimatedtime = $estimatedtime / 60;
							$estimatedtime_unit = "mn";
						}
					}

					$elapsedtime_unit = "sec";
					if ($elapsedtime>=3600) {
						$elapsedtime = $elapsedtime / 3600;
						$elapsedtime_unit = "h";
					} else {
						if ($elapsedtime>=60) {
							$elapsedtime = $elapsedtime / 60;
							$elapsedtime_unit = "mn";
						}
					}
				}
				else {
					$processing_info = "";
				}
			}
			
			$res .= "<tr>";
			$res .= "<td>";
			$res .= $rs["name"] . " (id=" . $rs["id"] . ")";
			$res .= "</td>";

			/*
			 $res .= "<td>";
			//$res .= "<a href='". $rs["url"] . "' target='_blank'>". $rs["url"] . "</a>";
			$res .= getStartingUrls($rs["url"]);
			$res .= "</td>";
			*/
				
			$res .= "<td>";
			$t = $rs["crawl_lasttime_start"]->sec;
			$res .= date('Y-m-d h:i:s', $t);
			if (!empty($processing_info)) {
				$res .= "<br><br>Effective elapsed<br/>time : " . round($elapsedtime, 2) . " " . $elapsedtime_unit;
			}
			$res .= "</td>";

			$res .= "<td>";
			$res .= $rs["running_crawl_item_processed"];
			if (!empty($processing_info)) {
				$res .= "<br><br>Average speed :<br/>" . round($averagespeed, 2) . " pages/sec";
				if ($rs["crawl_process_status"] == "1") {
					$res .= "<br>Current speed :<br/>" . round($currentspeed, 2) . " pages/sec";
				}
			}
			$res .= "</td>";

			$res .= "<td>";
			$res .= $rs["running_crawl_item_to_process"];
			if (!empty($processing_info)) {
				$res .= "<br><br>Estimated remaining<br>time : " . round($estimatedtime, 2) . " " . $estimatedtime_unit;
			}
			$res .= "</td>";

			if (!$_SESSION["mysolrserver_url"] && $cache_enabled && $rs["type"]=='1') {

				if ($rs["crawl_process_status"] == "5") {
					$res .= "<td style='background-color: orange; text-align: center; vertical-align:middle'>";
					$res .= "Stopping";
					$res .= "</td><td></td>";
				}
				if ($rs["crawl_process_status"] == "4") {
					$res .= "<td style='background-color: orange; text-align: center; vertical-align:middle'>";
					$res .= "Scheduled pause";
					$res .= "</td><td></td>";
				}
				if ($rs["crawl_process_status"] == "3") {
					$res .= "<td style='background-color: orange; text-align: center; vertical-align:middle'>";
					$res .= "Resuming";
					$res .= "</td><td></td>";
				}
				if ($rs["crawl_process_status"] == "2") {
					$res .= "<td style='background-color: orange; text-align: center; vertical-align:middle'>";
					$res .= "Pause</td>";
					$res .= "<td style='text-align: center; vertical-align:middle'><a href='#' onClick='resumeSource(" . $rs["id"] . ");return false;' title='Resume'><img src='images/resume.png'></a>";
					$res .= "</td>";
				}
				if ($rs["crawl_process_status"] == "1") {
					$res .= "<td style='background-color: green; text-align: center'>";
					$res .= "Crawling</td>";
					$res .= "<td style='text-align: center; vertical-align:middle'><a href='#' onClick='pauseSource(" . $rs["id"] . ");return false;' title='Pause'><img src='images/pause.png'></a>";
					$res .= "&nbsp;<a href='#' onClick='stopSource(" . $rs["id"] . ");return false;' title='Stop'><img src='images/stop.png'></a>";
					$res .= "</td>";
				}
			}
			$res .= "</tr>";
		}
		$res .= "</tbody></table></center>";
		print $res;
		exit();
	}
}

if ($action=="showinterrupted")
{
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_select($mg, "sources");
		$query = array('$and' => array(array(enabled => "1"), array(deleted => "0"), array(id_account => $id_account_current), array(id_account => $id_account_current)));;
		//TODO: V4
		//$stmt->setWhereClause("(enabled = 1 and deleted = 0 and id_account = " . $id_account_current . " and (crawl_lasttime_start > crawl_lasttime_end) or (crawl_lasttime_end is null and crawl_lasttime_start is not null)) and (running_crawl_lastupdate is not null and running_crawl_lastupdate < DATE_SUB(NOW(),INTERVAL 5 MINUTE))");
		
		$stmt->setQuery($query);
		$stmt->setSort(array( "crawl_lasttime_start" => 1 ));
		$count = $stmt->execute();		
		if ($count==0)
		{
			print "";
			exit();
		}
		$cursor = $stmt->getCursor();
	
		$res .= "<center><table border='0' cellspacing='0' cellpadding='0'>";
		$res .= "<tr><th>Title</th><th>Starting URL</th><th>Start crawl date</th><th>Next crawl date</th><th>Processed pages</th><th>Remaining pages</th></tr><tbody>";

		while ($cursor->hasNext())
		{
			$rs = $cursor->getNext();
			$res .= "<tr>";

			$res .= "<td class='name'>";
			$res .= $rs["name"] . " (id=" . $rs["id"] . ")";
			$res .= "</td>";

			$res .= "<td class='url'>";
			//$res .= "<a href='". $rs["url"] . "' target='_blank'>". $rs["url"] . "</a>";
			$res .= getStartingUrls($rs["url"]);
			$res .= "</td>";

			$res .= "<td class='date'>";
			$res .= $rs["crawl_lasttime_start"];
			$res .= "</td>";

			$res .= "<td class='date'>";
			$res .= $rs["crawl_nexttime"];
			$res .= "</td>";

			$res .= "<td class='count'>";
			$res .= $rs["running_crawl_item_processed"];
			$res .= "</td>";

			$res .= "<td class='count'>";
			$res .= $rs["running_crawl_item_to_process"];
			$res .= "</td>";

			$res .= "</tr>";
		}

		$res .= "</tbody></table></center>";

		print $res;
		exit();
	}
}

if ($action=="shownext" or $action=="showenqueued")
{
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_select($mg, "sources");

		if ($action=="shownext") {
			//TODO: V4
			//$stmt->setWhereClause("enabled = 1 and deleted = 0 and crawl_nexttime > now() and id_account = " . $id_account_current);
			$query = array('$and' => array(array(enabled => "1"), array(deleted => "0"), array(id_account => intval($id_account_current))));
		} else {
			//TODO: V4
			//$stmt->setWhereClause("enabled = 1 and deleted = 0 and (crawl_process_status='' or crawl_process_status='0') and id_account = " . $id_account_current);
			$query = array('$and' => array(array(enabled => "1"), array(deleted => "0"), array(id_account => intval($id_account_current))));
			//$query = array('$and' => array(array(enabled => "1"), array(deleted => "0")));
		}
				
		$stmt->setQuery($query);
		$stmt->setSort(array( "crawl_priority" => -1, "crawl_nexttime" => 1 ));
		
		if ($limit!="0")
			$stmt->setLimit($limit);
		
		$count = $stmt->execute();		
		if ($count==0)
		{
			print "";
			exit();
		}
		$cursor = $stmt->getCursor();

		$res .= "<center><table>";
		$res .= "<tr><th>Title</th><th>Starting URL</th><th>Next crawl date</th></tr><tbody>";

		while ($cursor->hasNext())
		{
			$rs = $cursor->getNext();
					
			$query = array ('$and' => array(array("type" => "cnx"),array("id" => intval($rs["type"]))));
			mg_get_value($mg, "plugins", "class_php", $query, $class);
				
			$source = SourceFactory::createInstance($class, $config, $id_account_current, $mg, $aLanguages, $aCountries);
			$source->load($rs);		
			
			$res .= "<tr>";

			$res .= "<td class='name'>";
			$res .= $rs["name"] . " (id=" . $rs["id"] . ")";
			$res .= "</td>";

			$res .= "<td class='url'>";
			$res .= getStartingUrls($source->getUrl());
			$res .= "</td>";

			$res .= "<td>";
			$res .= date('Y-m-d h:i:s', $rs["crawl_nexttime"]->sec) ;
			$res .= "</td>";

			$res .= "</tr>";
		}

		$res .= "</tbody></table></center>";

		print $res;
		exit();
	}
}

?>
