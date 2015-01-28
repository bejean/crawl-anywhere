<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
require_once("../../init_gpc.inc.php");
require_once("../../init.inc.php");
require_once("../../lib/feedfinder.class.inc.php");
require_once("../../lib/csvimporter.class.inc.php");

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

$action = POSTGET("action");

require_once("content.common.ajax.inc.php");

if ($action=="loadsources")
{
	$sources_page_size = $config->getDefault("sources.browsepagemaxsize", "100");
	$sources_browsebyalphabet = ($config->getDefault("sources.browsebyalphabet", "0")=="1");

	if ($sources_browsebyalphabet) {
		$start = $_GET["start"];
	}
	else {
		$start = "all";
	}

	$suspicious = $_GET["suspicious"];
	if ($suspicious=="1")
	$suspicious_checked = " checked";
	else
	$suspicious_checked = "";


	$inerror = $_GET["inerror"];
	if ($inerror=="1")
	$inerror_checked = " checked";
	else
	$inerror_checked = "";

	$nocountry = $_GET["nocountry"];
	if ($nocountry=="1")
	$nocountry_checked = " checked";
	else
	$nocountry_checked = "";

	$nolanguage = $_GET["nolanguage"];
	if ($nolanguage=="1")
	$nolanguage_checked = " checked";
	else
	$nolanguage_checked = "";

	$onepage = $_GET["onepage"];
	if ($onepage=="1")
	$onepage_checked = " checked";
	else
	$onepage_checked = "";

	$deleted = $_GET["deleted"];
	if ($deleted=="1")
	$deleted_checked = " checked";
	else
	$deleted_checked = "";

	$filter_status = "1";
	if (isset($_GET["filter_status"]) && $_GET["filter_status"]!="undefined")
	$filter_status = $_GET["filter_status"];

	$filter_type = "";
	if (isset($_GET["filter_type"]) && $_GET["filter_type"]!="undefined")
	$filter_type = $_GET["filter_type"];

	$filter_collection = "";
	if (isset($_GET["filter_collection"]) && $_GET["filter_collection"]!="undefined")
	$filter_collection = $_GET["filter_collection"];

	$filter_tag = "";
	if (isset($_GET["filter_tag"]) && $_GET["filter_tag"]!="undefined")
	$filter_tag = $_GET["filter_tag"];

	$filter_country = "";
	if (isset($_GET["filter_country"]) && $_GET["filter_country"]!="undefined")
	$filter_country = $_GET["filter_country"];

	$filter_language = "";
	if (isset($_GET["filter_language"]) && $_GET["filter_language"]!="undefined")
	$filter_language = $_GET["filter_language"];

	$filter_target = "";
	if (isset($_GET["filter_target"]) && $_GET["filter_target"]!="undefined")
	$filter_target = $_GET["filter_target"];

	$page = $_GET["page"];

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$query_id = array ("id_account" => intval($id_account_current));

 		if ($suspicious == "1") { 		
 			$q1 = array("crawl_status_message" => "");
 			$q2 = array("crawl_status" => "0");
 			$q3 = array("crawl_lasttime_end" => array('$ne' => null));
 			$q4 = array("crawl_lastpagecount" => array( '$lt' => 3 )); 			
 			$query_suspicious = array ('$and' => array($q1, $q2, $q3, $q4));
  		}
		
 		if ($inerror == "1") {
 			$q1 = array("crawl_status" => array('$ne' => "0"));
 			$q2 = array("crawl_status_message" => array('$ne' => ""));
 			$query_in_error = array ('$or' => array($q1, $q2));
 		}

 		if ($nocountry == "1")
 			$query_no_country = array ("country" => "unknown");

 		if ($nolanguage == "1")
 			$query_no_language = array ("language" => "xx");
 			
 		if ($deleted == "1")
 			$query_deleted = array ("deleted" => "1");
 		else
 			$query_deleted = array ("deleted" => "0");

		$query_enabled = array ("enabled" => $filter_status);

		if ($filter_collection!="") {
			$q1 = array("collection" => $filter_collection);
			$re = new MongoRegex("/^" . $filter_collection . ",/");
			$q2 = array("collection" => array('$regex' => $re));
			$re = new MongoRegex("/," . $filter_collection . "$/");
			$q3 = array("collection" => array('$regex' => $re));
			$re = new MongoRegex("/," . $filter_collection . ",/");
			$q4 = array("collection" => array('$regex' => $re));
			$query_collection = array ('$or' => array($q1, $q2, $q3, $q4));
			//$query_collection = $q1;
		}
		
 		if ($filter_tag!="") {
			$q1 = array("tag" => $filter_tag);
			$re = new MongoRegex("/^" . $filter_tag . ",/");
			$q2 = array("tag" => $re);
			$re = new MongoRegex("/," . $filter_tag . "$/");
			$q3 = array("tag" => $re);
			$re = new MongoRegex("/," . $filter_tag . ",/");
			$q4 = array("tag" => $re);
			$query_tag = array ('$or' => array($q1, $q2, $q3, $q4));
 		}
 			
 		if ($filter_country!="")
		$query_country = array ("country" => $filter_country);
		
 		if ($filter_language!="")
 		$query_language = array ("language" => $filter_language);
 			
 		if ($filter_target!="")
 		$query_language = array ("id_target" => intval($filter_target));
 			
 		if ($filter_type!="")
		$query_type = array ("type" => $filter_type);
		
		// TODO: V4
// 		$res = "<div id='sources_selector'>";
			
// 		if ($sources_browsebyalphabet) {
// 			$stmt = new db_stmt_select("sources");
// 			$stmt->addColumn ("lower(substring(name,1,1)) as alpha");
// 			$stmt->setWhereClause($Where);
// 			$stmt->setOrderBy("alpha");
// 			$stmt->setGroupBy("alpha");

// 			$s = $stmt->getStatement();
// 			$rs = $db->Execute($s);
// 			if (!$rs)
// 			{
// 				print $s;
// 				exit();
// 			}
// 			$rs->Move(0);
// 			while (!$rs->EOF)
// 			{
// 				$alpha = $rs->fields["alpha"];
// 				if ($start=="") $start=$alpha;

// 				if ($start!=$alpha)
// 				$res .= "<a href='javascript:void(0)' onClick='loadSources(\"" . $alpha . "\", 1);'>" . $alpha . "</a>&nbsp;&nbsp;";
// 				else
// 				$res .= $alpha . "&nbsp;&nbsp;";

// 				$rs->MoveNext();
// 			}

// 			if ($start!="all")
// 			$res .= "<a href='javascript:void(0)' onClick='loadSources(\"all\", 1);'>all</a>&nbsp;";
// 			else
// 			$res .= "all&nbsp;";

// 			$res .= "</div><br/><br/>";
// 		}

		$res .= "<div id='sources_options'>";

		$res2 = "";

		$res2 .= "<tr><td>Show only sources with this status</td><td><select id='filter_status' name='filter_status' style='editInputSelect' onChange='loadSources(\"" . $start . "\", 1);'>";
		$selected = "";
		if ($filter_status=="1") $selected=" selected";
		$res2 .= "<option value='1'" . $selected . ">Enabled</option>";
		$selected = "";
		if ($filter_status=="0") $selected=" selected";
		$res2 .= "<option value='0'" . $selected . ">Disabled</option>";
		$selected = "";
		if ($filter_status=="2") $selected=" selected";
		$res2 .= "<option value='2'" . $selected . ">Test</option>";
		$res2 .= "</select></td></tr>";

 		$aTypes = getAvailableSourceType($config, false, $id_account_current);
 		if ($aTypes!=null && count($aTypes) > 0) {
 			$res2 .= "<tr><td>";
 			$res2 .= "Show only sources of this type</td><td><select id='filter_type' name='filter_type' style='editInputSelect' onChange='loadSources(\"" . $start . "\", 1);'>";
 			$res2 .= "<option value=''>any</option>";

 			for ($j=0; $j<=count($aTypes); $j++) {
 				if ($aTypes[$j]!="") {
 					$res2 .= "<option value='" . $aTypes[$j] . "'";
 					if ($filter_type==$aTypes[$j]) $res2 .= " selected";
 					$res2 .= ">" .$aSourceTypes[$aTypes[$j]]['name'] . "</option>";
 				}
 			}
 			$res2 .= "</select></td></tr>";
 		}

	 	$aCollections = getAvailableTagsCollections($config, false, $id_account_current, "collection");
 		if ($aCollections!=null && count($aCollections) > 0) {
 			$res2 .= "<tr><td>";
 			$res2 .= "Show only sources within this collection</td><td><select id='filter_collection' name='filter_collection' style='editInputSelect' onChange='loadSources(\"" . $start . "\", 1);'>";
 			$res2 .= "<option value=''>any</option>";

 			for ($j=0; $j<=count($aCollections); $j++) {
 				
 				if ($aCollections[$j]!="") {
 					//$s1 = $aCollections[$j];
 					//$s2 = htmlspecialchars($aCollections[$j], ENT_QUOTES);
 					$res2 .= "<option value='" . htmlspecialchars($aCollections[$j], ENT_QUOTES) . "'";
 					if (stripslashes($filter_collection)==$aCollections[$j]) $res2 .= " selected";
 					$res2 .= ">" . str_replace("_", " ", $aCollections[$j]) . "</option>";
 				}
 			}
 			$res2 .= "</select></td></tr>";
 		}
 		
 		$aTags = getAvailableTagsCollections($config, false, $id_account_current, "tag");
 		if ($aTags!=null && count($aTags) > 0) {
 			$res2 .= "<tr><td>";
 			$res2 .= "Show only sources with this tag</td><td><select id='filter_tag' name='filter_tag' style='editInputSelect' onChange='loadSources(\"" . $start . "\", 1);'>";
 			$res2 .= "<option value=''>any</option>";

 			for ($j=0; $j<=count($aTags); $j++) {
 				
 				if ($aTags[$j]!="") {
 					$res2 .= "<option value='" . htmlspecialchars($aTags[$j], ENT_QUOTES) . "'";
 					if (stripslashes($filter_tag)==$aTags[$j]) $res2 .= " selected";
 					$res2 .= ">" . str_replace("_", " ", $aTags[$j]) . "</option>";
 				}
 			}
 			$res2 .= "</select></td></tr>";
 		}
		
		$query = array ('$and' => array(array("deleted" => "0"), array("id_account" => intval($id_account_current))));
 		$stmt = new mg_stmt_distinct($mg, "sources");
 		$stmt->setQuery($query);
 		$stmt->setKey("country");
 		$aCountry = $stmt->command(); 			
 			
 		if (count($aCountry)>1) {
 			sort($aCountry);
 			$res2 .= "<tr><td>";
 			$res2 .= "Show only sources from this country</td><td><select id='filter_country' name='filter_country' style='editInputSelect' onChange='loadSources(\"" . $start . "\", 1);'>";
 		 	$res2 .= "<option value=''>any</option>";
 			
 			for ($j=0; $j<=count($aCountry); $j++) {
 			 	$c = trim(strtoupper($aCountry[$j]));
 			 	if (!empty($c)) {
 					$res2 .= "<option value='" . $c . "'";
 			 		if ($filter_country==$c) $res2 .= " selected";
 			 		$res2 .= ">" . str_replace("_", " ", $aCountries[$c]) . "</option>";
 			 	}
 			}
 			$res2 .= "</select></td></tr>";
 		}
 			
 		$stmt = new mg_stmt_distinct($mg, "sources");
 		$stmt->setQuery($query);
 		$stmt->setKey("language");
 		$aLanguage = $stmt->command();
 		
 		if (count($aLanguage)>1) {
 			sort($aLanguage);
 			$res2 .= "<tr><td>";
 			$res2 .= "Show only sources within this language</td><td><select id='filter_language' name='filter_language' style='editInputSelect' onChange='loadSources(\"" . $start . "\", 1);'>";
 			$res2 .= "<option value=''>any</option>";
 			 		
 			for ($j=0; $j<=count($aLanguage); $j++) {
 				$l = trim($aLanguage[$j]);
 				if (!empty($l)) {
 					$res2 .= "<option value='" . $l . "'";
 					if ($filter_language==$l) $res2 .= " selected";
 					$res2 .= ">" . str_replace("_", " ", $aLanguages[$l]) . "</option>";
 				}
 			}
 			$res2 .= "</select></td></tr>";
 		}
 			
 		$aTargets = getAvailableTargets($config, $id_account_current);
 		if ($aTargets!=null && count($aTargets) > 0) {
 			$res2 .= "<tr><td>";
 			$res2 .= "Show only sources with this target</td><td><select id='filter_target' name='filter_target' style='editInputSelect' onChange='loadSources(\"" . $start . "\", 1);'>";
			$res2 .= "<option value=''>any</option>";
			foreach ($aTargets as $key => $value)
 			{
 				$res2 .= "<option value='" . $key . "'";
 				if ($filter_target==strtolower(trim($key))) $res2 .= " selected";
 				$res2 .= ">" . $value . "</option>";
 			}
 			$res2 .= "</select></td></tr>";
 		}

 		$res2 .= "<tr><td>";
 		$res2 .= '<a href="#" onClick="$(\'#sources_advanced_options\').toggle();">Show / hide more options</a>';
 		$res2 .= "</td><td></td></tr>";

		$res .= "<table style='border: none;'>" . $res2 . "</table>";

		$res .= "</div>";
		
		$display_mode = "none";
		if ($suspicious_checked!='' || $inerror_checked!='' || $nocountry_checked!='' || $nolanguage_checked!='' || $onepage_checked!='' || $deleted_checked!='') $display_mode = "block";
		
		$res .= "<div id='sources_advanced_options' style='display:" . $display_mode . ";'>";
		$res .= "<input type='checkbox' name='chk_suspicious' id='chk_suspicious'" . $suspicious_checked . " onClick='loadSources(\"" . $start . "\", 1);'>Show only suspicious sources<br/>";
		$res .= "<input type='checkbox' name='chk_inerror' id='chk_inerror'" . $inerror_checked . " onClick='loadSources(\"" . $start . "\", 1);'>Show only in error sources<br/>";
		$res .= "<input type='checkbox' name='chk_nocountry' id='chk_nocountry'" . $nocountry_checked . " onClick='loadSources(\"" . $start . "\", 1);'>Show only sources without country specified<br/>";
		$res .= "<input type='checkbox' name='chk_nolanguage' id='chk_nolanguage'" . $nolanguage_checked . " onClick='loadSources(\"" . $start . "\", 1);'>Show only sources without language specified<br/>";
		$res .= "<input type='checkbox' name='chk_deleted' id='chk_deleted'" . $deleted_checked . " onClick='loadSources(\"" . $start . "\", 1);'>Show only deleted sources<br/>";
		$res .= "<input type='checkbox' name='chk_onepage' id='chk_onepage'" . $onepage_checked . " onClick='loadSources(\"" . $start . "\", 1);'>Show all sources in one page<br/>";
		$res .= "</div>";

// 		if ($start!="all")
// 		$Where = $Where . " and name like '" . $start . "%'";

		$query = array ('$and' => array($query_id, $query_deleted, $query_enabled));
		if (isset($query_type)) {
			$query = array ('$and' => array($query, $query_type));
		}
		if (isset($query_suspicious)) {
			$query = array ('$and' => array($query, $query_suspicious));
		}
		if (isset($query_in_error)) {
			$query = array ('$and' => array($query, $query_in_error));
		}
		if (isset($query_no_country)) {
			$query = array ('$and' => array($query, $query_no_country));
		}
		if (isset($query_no_language)) {
			$query = array ('$and' => array($query, $query_no_language));
		}
		if (isset($query_country)) {
			$query = array ('$and' => array($query, $query_country));
		}		
		if (isset($query_language)) {
			$query = array ('$and' => array($query, $query_language));
		}		
		if (isset($query_tag)) {
			$query = array ('$and' => array($query, $query_tag));
		}		
		if (isset($query_collection)) {
			$query = array ('$and' => array($query, $query_collection));
		}
		
		$RowCount = mg_row_count($mg, "sources", $query);
		
		$res .= "<br/><br/>";
		$res .= "<div id='sources_count'>";
		$res .= $RowCount . " source(s) found<br/>";
		$res .= "</div>";
		//$res .= "<br/><br/>";
		$res .= "</div>";
		$res .= "<br/>";

		if ($user->getLevel()>0)
		{
			$sources_export_import = ($config->getDefault("sources.export_import", "")!="0");
			$sources_max_number = $config->getDefault("sources.max_number", "0");
			if ($sources_max_number>0 && $RowCount>=$sources_max_number)
				$sources_max_number_reached = 'true';
			else
				$sources_max_number_reached = 'false';

			
			$res .= "<div style='width: 100%; overflow: hidden;'>";
			
			
			$res .= "<div style='width: 300px; float: left;' class='menu_button_on_left'>";
			$res .= "Add new&nbsp;<a href='#' onClick='showSelectSourceType(" . $sources_max_number_reached . "); return false;'><img src='images/edit_add_32.png'></a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
			if ($sources_export_import) {
				$res .= "Export&nbsp;<a href='#' onClick='exportSources(); return false;'><img src='images/export_32.png'></a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				$res .= "Import&nbsp;<a href='#' onClick='importSources(); return false;'><img src='images/import_32.png'></a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
			}
			$res .= "</div>";
			
			$res .= "<div style='margin-left: 320px;' class='menu_button_on_right'>";
			$res .= "Crawl Now&nbsp;All<a href='#' onClick='crawlNowAllSource(); return false;'><img src='images/timer_32.png'></a>";
			$res .= "Reset&nbsp;All<a href='#' onClick='resetAllSource(); return false;'><img src='images/reset_32.png'></a>";
			$res .= "Delete&nbsp;All<a href='#' onClick='deleteAllSource(); return false;'><img src='images/trash_32.png'></a>";
			$res .= "</div>";
			
			$res .= "</div>";
				
		}

		if ($onepage!="1")
		$res .= "<center>" . displayPagination ($RowCount, $sources_page_size , $start, $page) . "</center><br />";

		$res .= "<center><table border='0' cellspacing='0' cellpadding='0'>";
		$res .= "<tr><th class='checkbox' onClick='srcCheck();'><input id='chkAll' type='checkbox'></th><th>Title</th><th>Type</th><th>Collections</th><th>Tags</th><th class='status'>Enabled</th><th>Page count</th><th class='action'>&nbsp;</th></tr>";

		$stmt = new mg_stmt_select($mg, "sources");
		$stmt->setQuery($query);
		$stmt->setSort(array("name_sort" => 1,  "name" => 1 ));
		
		$skip = (int)(($page-1)*$sources_page_size);
		$limit = $sources_page_size;
		
		$stmt->execute($skip, $limit);
		$cursor = $stmt->getCursor();

		$count=0;		
		while (($count<$sources_page_size || $onepage=="1") && $cursor->hasNext())
		{
			$rs = $cursor->getNext();
			$query = array ('$and' => array(array("type" => "cnx"),array("id" => intval($rs["type"]))));
			mg_get_value($mg, "plugins", "class_php", $query, $class);
			
			$source = SourceFactory::createInstance($class, $config, $id_account_current, $mg, $aLanguages, $aCountries);
			$source->load($rs);
				
			$count++;
			$res .= "<tr>";

			if (trim($rs["crawl_status_message"])!="" || $rs["crawl_lastpagecount"]=='1')
			$res .= "<td rowspan='2' ";
			else
			$res .= "<td ";

			$res .= "class='checkbox'><input name='srcChk' id='src_" . $rs["id"] . "' type='checkbox' value='" . $rs["id"] . "'></td>";

			if (trim($rs["crawl_status_message"])!="" || $rs["crawl_lastpagecount"]=='1')
			$res .= "<td rowspan='2'>";
			else
			$res .= "<td>";
			$res .= $rs["name"] . " (id=" . $rs["id"] . ")";
			if ($source->getType()=='1') {
				$res .= "&nbsp;<img src='images/question_12.png' title='" . getStartingUrlsAsString($source->getUrl(), "<br />") . "'>";
			}
			$res .= "</td>";

			$res .= "<td width='50px'>";
			
			if (!empty($aSourceTypes[$rs["type"]][link])) {
				$res .= "<a href='" . $aSourceTypes[$rs["type"]]['link'] . "' target='_blank'>";
			}
			
			$res .= "<img src='images/" . $aSourceTypes[$rs["type"]]['mnemo'] . "_icone.png' height='32' width='32'>";
			$res .= $aSourceTypes[$rs["type"]]['name'];
			if (!empty($aSourceTypes[$rs["type"]]['link'])) {
				$res .= "</a>";
			}
			$res .= "</td>";

			$res .= "<td>";
			$collection = $rs["collection"];
			$collection = str_replace(",", ", ", $collection);
			$collection = str_replace("_", " ", $collection);
			$res .= $collection;
			$res .= "</td>";

			$res .= "<td>";
			$tag = $rs["tag"];
			$tag = str_replace(",", ", ", $tag);
			$tag = str_replace("_", " ", $tag);
			$res .= $tag;
			$res .= "</td>";

			$res .= "<td class='status'>";
			if ($rs["enabled"])
			$res .= "Yes";
			else
			$res .= "No";

			$res .= "</td>";

			$crawl_status = $rs["crawl_process_status"];
			if ($crawl_status == "1") {
				$res .= "<td style='background-color: green;'>";
			} else {
				if (($crawl_status == "2") || ($crawl_status == "3") || ($crawl_status == "4") || ($crawl_status == "5")) {
					$res .= "<td style='background-color: orange;'>";
				} else {
					$res .= "<td>";
				}
			}
			$c = $rs["crawl_pagecount_pushed"];
			if ($c=='0') $c = $rs["crawl_pagecount_success"];
			if ($c=='0') $c = $rs["crawl_pagecount"];
			$res .= $c;
			if ($crawl_status == "1") {
				$res .= "<br>(Crawling)";
			}
			if ($crawl_status == "2") {
				$res .= "<br>(Pause)";
			}
			if ($crawl_status == "3") {
				$res .= "<br>(Resuming)";
			}
			if ($crawl_status == "4") {
				$res .= "<br>(Scheduled Pause)";
			}
			if ($crawl_status == "5") {
				$res .= "<br>(Stopping)";
			}
			$res .= "</td>";

			$res .= "<td class='action'>";
			$res .= "<a href='#' onClick='editSource(" . $rs["id"] . ",\"" . $aSourceTypes[$rs["type"]]['mnemo'] . "\"); return false;' title='Edit'><img src='images/button_edit.png'></a>";
			if ($user->getLevel()>0 && $crawl_status != "3" && $crawl_status != "5") {
				if ($crawl_status != "1" && $crawl_status != "2") {
					$res .= "&nbsp;<a href='#' onClick='crawlNowSource(" . $rs["id"] . ");return false;' title='Crawl now'><img src='images/timer.png'></a>";
					$res .= "&nbsp;<a href='#' onClick='resetSource(" . $rs["id"] . ");return false;' title='Reset'><img src='images/reset_16.png'></a>";
					// TODO: V4 - reactivate all these features
					$res .= "&nbsp;<a href='#' onClick='clearSource(" . $rs["id"] . ");return false;' title='Clear'><img src='images/clear.png'></a>";
					//$res .= "&nbsp;<a href='#' onClick='cleanSource(" . $rs["id"] . ");return false;' title='Clean'><img src='images/clean.png'></a>";
					if ($cache_enabled) $res .= "&nbsp;<a href='#' onClick='resetCacheSource(" . $rs["id"] . ");return false;' title='Reset with cache'><img src='images/reset_cache.png'></a>";
					$res .= "&nbsp;<a href='#' onClick='rescanSource(" . $rs["id"] . ");return false;' title='Rescan'><img src='images/rescan.png'></a>";
					$res .= "&nbsp;<a href='#' onClick='deeperSource(" . $rs["id"] . ");return false;' title='Deeper'><img src='images/deeper.png'></a>";
					$res .= "&nbsp;<a href='log.php?id=" . $rs["id"] . "' title='Log' target='log'><img src='images/log_16.png'></a>";
					$res .= "&nbsp;<a href='#' onClick='deleteSource(" . $rs["id"] . ");return false;' title='Delete'><img src='images/trash_16.png'></a>";
				} else {
					if ($cache_enabled && $rs["type"]=='1') {
						if ($crawl_status == "2") {
							$res .= "&nbsp;<a href='#' onClick='resumeSource(" . $rs["id"] . ");return false;' title='Resume'><img src='images/resume.png'></a>";
						}
						if ($crawl_status == "1") {
							$res .= "&nbsp;<a href='#' onClick='pauseSource(" . $rs["id"] . ");return false;' title='Pause'><img src='images/pause.png'></a>";
							$res .= "&nbsp;<a href='#' onClick='stopSource(" . $rs["id"] . ");return false;' title='Stop'><img src='images/stop.png'></a>";
						}
						$res .= "</td>";
					}
				}
			} else {
				if ($user->getLevel()>0 && $crawl_status == "5") {
					$res .= "&nbsp;<a href='#' onClick='resetSource(" . $rs["id"] . ");return false;' title='Reset'><img src='images/reset_16.png'></a>";
				}
			}
			$res .= "</td>";
			$res .= "</tr>";

			if (trim($rs["crawl_status_message"])!="" || $rs["crawl_lastpagecount"]=='1')
			{
				$res .= "<tr>";
				$res .= "<td colspan='9'><img src='images/warning.png'>&nbsp;&nbsp;&nbsp;";
				if (trim($rs["crawl_status_message"])!="") {
					$res .= "Last crawl status : " . $rs["crawl_status"] . "&nbsp;&nbsp;(";
					$res .= wordwrap($rs["crawl_status_message"], 100, "\n", true) . ")";
				} else {
					$res .= "Only one crawled page ! Check source settings (starting url, host and host aliases)";
				}
				$res .= "<br>[<a href='log.php?id=" . $rs["id"] . "' title='Log' target='log'>See log file</a>]";
				$res .= "</td>";
				$res .= "</tr>";
			}
		}
		$res .= "</table></center>";

		if ($onepage!="1")
		$res .= "<br /><center>" . displayPagination ($RowCount, $sources_page_size , $start, $page) . "</center>";

		print $res;
		exit();
	}
}


if ($action=="displaysource")
{
	$id = $_GET["id"];
	if ($id=="")
	{
		print ("");
		exit();
	}

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_select($mg,"sources");
		$query = array ("id" => intval($id));
		$stmt->setQuery($query);
		$count = $stmt->execute();
		if ($count==0)
		{
			print "";
			exit();
		}

		$cursor = $stmt->getCursor();
		$rs = $cursor->getNext();
			
		$query = array ('$and' => array(array("type" => "cnx"),array("id" => intval($rs["type"]))));
		mg_get_value($mg, "plugins", "class_php", $query, $class);
		$source = SourceFactory::createInstance($class, $config, $id_account_current, $mg, $aLanguages, $aCountries);
		$source->load($rs);
	
		$res= "<div id='opt_switch'>";
		$res .= "<center>";
		$res .= "[<a href='javascript:void(0)' onClick='showOptionGroup(\"main\");' id='show_opt_main'>Main options</a>]&nbsp;&nbsp;";
		$res .= "[<a href='javascript:void(0)' onClick='showOptionGroup(\"advanced\");' id='show_opt_advanced'>Advanced options</a>]&nbsp;&nbsp;";
		$res .= "[<a href='javascript:void(0)' onClick='showOptionGroup(\"status\");' id='show_opt_status'>Status</a>]";
		$res .= "</center>";
		$res .= "</div>";

		$res .= "<br>";

		$res .= "<form name='source_edit' id='source_edit' action=''><center>";

		$res .= "<table border='0' cellspacing='0' cellpadding='0'>";
		
		$res .= "<tbody id='opt_connect' style='display:none'>";
		$res .= $source->displayPageConnect();
		$res .= "</tbody>";
		
		$res .= "<tbody id='opt_main'>";
		$res .= $source->displayPageMain();
		$res .= "</tbody>";
		
		$res .= "<tbody id='opt_advanced' style='display:none'>";
		$res .= $source->displayPageAdvanced();
		$res .= "</tbody>";
		
		$res .= "<tbody id='opt_status' style='display:none'>";
		$res .= $source->displayPageStatus();
		$res .= "</tbody>";

		$res .= "</table></center>";

		$res .= "<br/>";

		$res .= "<input type='hidden' id='source_id' name ='source_id' value='". $rs["id"] ."'>";
		$res .= "<input type='hidden' id='source_type' name ='source_type' value='". $rs["type"] ."'>";
		$res .= "<input type='hidden' id='action' name ='action' value='updatesource'>";

		$res .= "<div id='opt_button' class='menu_button_on_right'><span id='source_save_result'></span>";
		$res .= "<a href='#' onClick='cancelSource();return false;'><img src='images/button_cancel_32.png'></a>&nbsp;&nbsp;";

		if ($user->getLevel()>0)
		{
			$res .= "<a href='#' onClick='updateSource();return false;'><img src='images/button_ok_32.png'></a>&nbsp;&nbsp;";
			$res .= "<a href='#' onClick='deleteSource();return false;'><img src='images/trash_32.png'></a>&nbsp;&nbsp;&nbsp;";
		}

		$res .= "</div></form>";


		print $res;
		exit();
	}
}

if ($action=="display_add_source")
{
	$type = $_GET["type"];

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$query = array ('$and' => array(array("type" => "cnx"),array("mnemo" => $type)));
		mg_get_value($mg, "plugins", "class_php", $query, $class);

		$source = SourceFactory::createInstance($class, $config, $id_account_current, $mg, $aLanguages, $aCountries);

		$res= "<div id='opt_switch'>";
		$res .= "<center>";
		$res .= "[<a href='javascript:void(0)' onClick='showOptionGroup(\"main\");' id='show_opt_main'>Main options</a>]&nbsp;&nbsp;";
		$res .= "[<a href='javascript:void(0)'  onClick='showOptionGroup(\"advanced\");' id='show_opt_advanced'>Advanced options</a>]&nbsp;&nbsp;";
		$res .= "</center>";
		$res .= "</div>";
		
		$res .= "<br>";
		
		$res .= "<form name='source_edit' id='source_edit'><center>";

		$res .= "<table border='0' cellspacing='0' cellpadding='0'>";

		$res .= "<tbody id='opt_connect' style='display:none'>";
		$res .= $source->displayPageConnect();
		$res .= "</tbody>";
		
		$res .= "<tbody id='opt_main'>";
		$res .= $source->displayPageMain();
		$res .= "</tbody>";
		
		$res .= "<tbody id='opt_advanced' style='display:none'>";
		$res .= $source->displayPageAdvanced();
		$res .= "</tbody>";
		
		$res .= "<tbody id='opt_status' style='display:none'>";
		$res .= "</tbody>";
		$res .= "</table></center>";

		$res .= "<input type='hidden' id='source_type' name ='source_type' value='". $type ."'>";
		$res .= "<input type='hidden' id='action' name ='action' value='createsource'>";

		$res .= "</form>";

		$res .= "<br/>";

		$res .= "<div  id='opt_button' class='menu_button_on_right'><span id='source_save_result'></span>";
		$res .= "<a href='#' onClick='cancelSource();return false;'><img src='images/button_cancel_32.png'></a>&nbsp;&nbsp;";
		$res .= "<a href='#' onClick='createSource();return false;'><img src='images/button_ok_32.png'></a>&nbsp;&nbsp;&nbsp;";
		$res .= "</div>";

		print $res;
		exit();
	}
}

if ($action=="createsource")
{	
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$type = $_POST["source_type"];
		$query = array ('$and' => array(array("type" => "cnx"),array("mnemo" => $type)));
		mg_get_value($mg, "plugins", "class_php", $query, $class);
		
		$source = SourceFactory::createInstance($class, $config, $id_account_current, $mg, $aLanguages, $aCountries);

		$stmt = new mg_stmt_insert($mg, "sources", $mg_source_defaults);
		
		$stmt->addColumnValue("id_account", intval($id_account_current));
		$stmt->addColumnValueDate ("createtime");
		$stmt->addColumnValue("type", $source->getType(), ""); // Why this and not $type ?

		$source->buildSQL($stmt, $_POST, true);
		
		if (!$stmt->checkNotNull ($mg_source_not_null)) {
			$res = "Error&nbsp;&nbsp;&nbsp;" . createsource;
			$cLog->log("Content.sources.ajax.inc.php - action = " . $action . " - missing data");
		} else {			
			mg_create_index($mg, 'sources', 'name_sort');
			$stmt->execute();		
			$res = "Success&nbsp;&nbsp;&nbsp;";
		}
	}

	print ($res);
	exit();
}

if ($action=="updatesource")
{
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$type = $_POST["source_type"];
		$query = array ('$and' => array(array("type" => "cnx"),array("id" => intval($type)))); // TODO: why id and not mnemo as for creation
		mg_get_value($mg, "plugins", "class_php", $query, $class);
		
		$source = SourceFactory::createInstance($class, $config, $id_account_current, $mg, $aLanguages, $aCountries);

		$stmt = new mg_stmt_update($mg, "sources");
		$query = array("id" => intval($_POST["source_id"]));
		$stmt->setQuery ($query);
		
		$source->buildSQL($stmt, $_POST, false);
		mg_create_index($mg, 'sources', 'name_sort');
		$stmt->execute();
		/*
		$s = $stmt->getStatement();
		$rs = $db->Execute($s);
		if (!$rs)
		{
			$res = "Error&nbsp;&nbsp;&nbsp;" . $s;
			$cLog->log("Content.sources.ajax.inc.php - action = " . $action . " - " . $s);
		}
		else
		{	
		*/
		mg_get_value($mg, "sources", "crawl_process_status", $query, $crawl_status);
		if ($crawl_status == "1") {
			$stmt = new mg_stmt_update($mg, "sources");
			$stmt->setQuery ($query);
			$stmt->addColumnValue(crawl_process_status, "5");
			$stmt->execute();
		}
		$res = "Success&nbsp;&nbsp;&nbsp;";
		/*
		}
		*/
	}

	print ($res);
	exit();
}

if ($action=="deletesource")
{
	$id = $_GET["id"];
	if (empty($id)) {
		$res = "Error&nbsp;&nbsp;&nbsp;";
		print ($res);
		exit();
	}
	
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$stmt = new mg_stmt_update($mg, "sources", NULL, TRUE);
		if ($id=='all') {
			$query = array ("deleted" => "0");
		} else {
			$query = array("id" => intval($id));
		}
		$stmt->setQuery ($query);
		
		$stmt->addColumnValue("deleted", "1");
		$stmt->addColumnValue("crawl_mode", "2");
		$stmt->addColumnValue("crawl_priority", "2");
		$stmt->addColumnValueDate("crawl_nexttime");

		$stmt->execute();		
		$res = "Success&nbsp;&nbsp;&nbsp;";
	}

	print ($res);
	exit();
}

if ($action=="resetsource" || $action=="resetcachesource" || $action=="rescansource" || $action=="deepersource" || $action=="crawlnow" || $action=="clearsource") // || $action=="cleansource")
{
	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$id = $_GET["id"];
		if (empty($id)) {
			$res = "Error&nbsp;&nbsp;&nbsp;";
			print ($res);
			exit();
		}
		
		$stmt = new mg_stmt_update($mg, "sources");
	
		if (($action=="resetsource" || $action=="crawlnow") && ($id=='all')) {
			$query = array ("deleted" => "0");
		} else {
			$query = array("id" => intval($id));
		}
		
		$stmt->setQuery ($query);

		if ($action=="rescansource") {
			$stmt->addColumnValue("crawl_mode", "1");
		}
		if ($action=="resetsource") {
			$stmt->addColumnValue("crawl_mode", "2");
			$stmt->addColumnValue("crawl_firstcompleted", "0");
		}
		if ($action=="deepersource") {
			$stmt->addColumnValue("crawl_mode", "3");
		}
		if ($action=="resetcachesource") {
			$stmt->addColumnValue("crawl_mode", "4");
		}
		if ($action=="clearsource") {
			$stmt->addColumnValue("crawl_mode", "5");
		}
		if ($action=="cleansource") {
			$stmt->addColumnValue("crawl_mode", "6");
		}
		//if ($action=="crawlnow") {
		//	$stmt->addColumnValue("crawl_priority", "2");
		//} else {
			$stmt->addColumnValue("crawl_priority", "1");
		//}
		$stmt->addColumnValueDate("crawl_nexttime");

		$stmt->addColumnValue("crawl_process_status", "0");
		
		$stmt->execute();		
		/*
		$s = $stmt->getStatement();
		$rs = $db->Execute($s);
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

function displayPagination ($totalhits, $pagesize, $start, $page) {

	$res="";

	$blocksize = 8;
	$firstpage = 1;
	$lastpage = intval(abs((($totalhits-1) / $pagesize) + 1));

	$ndxstart = (($page-1) * $pagesize) + 1;
	$ndxstop = $page * $pagesize;
	if ($ndxstop > $totalhits)
	$ndxstop = $totalhits;

	if ($lastpage > 1) {

		$res .= "<br>";

		if ($page != 1)
		$res .= '<a href="#" onclick="loadSources(\'' . $start . '\', ' . ($page-1) . ');"><b><<</b></a>&nbsp;';

		for ($i=$firstpage; $i<=$lastpage; $i++) {
			if ($i!=$firstpage)
			$res .= ' ';

			if ($i==$page)
			$res .= '<font color=red>' . $i . '</font>';
			else
			{
				if ($i <= $blocksize || ($i > ($page - $blocksize) && $i < ($page + $blocksize)) || $i > ($lastpage - $blocksize))
				$res .= '<a href="#" onclick="loadSources(\'' . $start . '\', ' . $i . ');"><b>' . $i . '</b></a>';
				else
				{
					if ($i == ($page - $blocksize) || $i == ($page + $blocksize))
					$res .= '&nbsp;&nbsp;&nbsp;...&nbsp;&nbsp;&nbsp;';
				}
			}
		}

		if ($page != $lastpage)
		$res .= '&nbsp;<a href="#" onclick="loadSources(\'' . $start . '\', ' . ($page+1) . ');"><b>>></b></a>';
	}

	return $res;
}

if ($action == "testauthentication")
{
	if (isset($_POST["source_url"]))
	{
		$page = $_POST["source_url"];
	}
	if (isset($_POST["auth_mode"]))
	{
		$authMode = $_POST["auth_mode"];
	}
	if (isset($_POST["auth_login"]))
	{
		$authLogin = $_POST["auth_login"];
	}
	if (isset($_POST["auth_passwd"]))
	{
		$authPasswd = $_POST["auth_passwd"];
	}
	if (isset($_POST["auth_param"]))
	{
		$authParam = $_POST["auth_param"];
	}

	$url = $config->get("crawlerws.rooturl");
	$url .= "?action=" . $action;
	$url .= "&page=" . urlencode($page);
	$url .= "&auth_mode=" . $authMode;
	$url .= "&auth_login=" . urlencode($authLogin);
	$url .= "&auth_passwd=" . urlencode($authPasswd);
	$url .= "&auth_param=" . urlencode($authParam);
	$xmlstr = readurl($url);

	$xml = new SimpleXMLElement($xmlstr);
	$content = $xml->page;

	$temp_path = $config->getDefault("application.tmp_path", "/opt/crawler/tmp");
	$filename = $temp_path. "/" . uniqid() . ".html";
	write_file($filename, $content);
	print ($filename);

	exit();
}

if ($action == "testfilteringrules")
{
	if (isset($_POST["test_url"]))
	{
		$page = $_POST["test_url"];
	}
	if (isset($_POST["source_crawl_filtering_rules_xml"]))
	{
		$rules = $_POST["source_crawl_filtering_rules_xml"];
	}

	$flatrules = "";
	if (substr($rules, 0, 1) == "<") {
		$urlXml = simplexml_load_string($rules);
		$result = $urlXml->xpath('/rules/rule');
		$sep = "";
		while(list( , $node) = each($result)) {
			if ((string)$node->mode=="all") $mode = "a";
			if ((string)$node->mode=="skip") $mode = "s";
			if ((string)$node->mode=="links") $mode = "l";
			if ((string)$node->mode=="once") $mode = "o";
			if ((string)$node->mode=="get") $mode = "g";
			$flatrules .= $sep . (string)$node->ope . ':' .$mode . ':' . (string)$node->pat;
			$sep = "\n";
		}
	}
	else {
		$flatrules = $rules;
	}

	$url = $config->get("crawlerws.rooturl");
    $url .= 'testfilteringrules/';
	$url .= "?page=" . urlencode($page);
	$url .= "&rules=" . urlencode($flatrules);;
	$jsonstr = readurl($url);

	$arr = json_decode($jsonstr, true);
	$content = $arr['mode'];
	
	if ($content=="a") $content = "Get page and extract links";
	if ($content=="l") $content = "Extract links only";
	if ($content=="s") $content = "Ignore";
	if ($content=="o") $content = "Get page and extract links (once)";
	if ($content=="g") $content = "Get page only";

	print ($content);
	exit();
}

if ($action == "testcleaning")
{
	if (isset($_POST["test_url_cleaning"]))
	{
		$page = $_POST["test_url_cleaning"];
	}
	if (isset($_POST["source_automatic_cleaning"]))
	{
		$cleaningMethod = $_POST["source_automatic_cleaning"];
	}

	$url = $config->get("crawlerws.rooturl");
    $url .= 'testcleaning/';
	$url .= "?page=" . urlencode($page);
	$url .= "&method=" . $cleaningMethod;
	$jsonstr = readurl($url);
	
// 	if ($jsonstr==0 || $jsonstr==500) {
// 		print ('');
// 		exit();
// 	}

	$arr = json_decode($jsonstr, true);
	
// 	$xml = new SimpleXMLElement($xmlstr, LIBXML_NOCDATA);

// 	if (isset($xml->errno)) {
// 		print("err=" . $xml->errno . " " . $xml->errmsg);
// 		exit();
// 	}

	$content = "<html><head><title>Automatic HTML page cleaning</title>";
	$content .= "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'/>";
	$content .= "<link href='themes/ca/styles.css' rel='stylesheet' type='text/css' />";
	$content .= "</head><body>\n";
	$content .= "<table>\n";
	//$content .= "<tr><td>\nNo cleaning\n</td><td>\n" . $arr['title0'] . "<br/><br/>" . base64_decode ($arr['page0']) . "\n</td></tr>\n";
	$content .= "<tr><td>\nBoilerpipe arcticle extractor\n</td><td>\n" . $arr['title1'] . "<br/><br/>" . base64_decode ($arr['page1']) . "\n</td></tr>\n";
	$content .= "<tr><td>\nBoilerpipe default extractor\n</td><td>\n" . $arr['title2'] . "<br/><br/>" . base64_decode ($arr['page2']) . "\n</td></tr>\n";
	$content .= "<tr><td>\nBoilerpipe canola extractor\n</td><td>\n" . $arr['title3'] . "<br/><br/>" . base64_decode ($arr['page3']) . "\n</td></tr>\n";
	$content .= "<tr><td>\nSnacktory extractor\n</td><td>\n" . $arr['title4'] . "<br/><br/>" . base64_decode ($arr['page4']) . "\n</td></tr>\n";
	$content .= "</table>";
	$content .= "</body></html>";

	$temp_path = $config->getDefault("application.tmp_path", "/opt/crawler/tmp");
	$basename =  uniqid() . ".html";
	$filename = $temp_path. "/" . $basename;

	write_file($filename, $content);
	print ($basename);
	exit();
}

if ($action == "scanrss")
{
	if (isset($_POST["url"]))
	{
		$url = $_POST["url"];
	}
	else {
		print("invalid url !");
		exit();
	}

	$ret = "";

	$ff = new FeedFinder();

	$proxy_host = $config->get("proxy.host");
	$proxy_port = $config->get("proxy.port");
	if ($proxy_host!=undefined && $proxy_port!=undefined) {
		$proxy_exclude = $config->get("proxy.exclude");
		if ($proxy_exclude=="undefined") $proxy_exclude = "";
		$proxy_user = $config->get("proxy.username");
		if ($proxy_user=="undefined") $proxy_user = "";
		$proxy_passwd = $config->get("proxy.password");
		if ($proxy_passwd=="undefined") $proxy_passwd = "";
		$ff->SetProxy ($proxy_host, $proxy_port, $proxy_exclude, $proxy_user, $proxy_passwd);
	}

	$ff->Find($url, false, false, 60);

	$feeds = $ff->GetList();
	if ($feeds!=NULL)
	{
		$ret = "<br/><strong>Select RSS feeds you want to add (" . $ff->GetPageReadCount() . "found):</strong><br/><br/>";
		$ret .= "<form id='scan-rss-dialog-rss-add'><table style='margin-left:auto; margin-right:auto; width:700px'>";

		foreach ($feeds as $feed) {
			$count++;
			$title = trim($feed['title']);
			if ($title=="") $title = $feed['url'];
			$ret .= "<tr><td style='width:25px'><input type='checkbox' id='scan-rss-dialog-rss-add-val1' value='" . $feed['url'] . "' checked='checked'></td><td>" . $title . " <span style='font-size: 0.8em;'>(" .  $feed['url'] . ")</span></td></tr>";
		}
		$ret .= "</table>";

		$ret .= "<br/><label>Allow other domains</label>&nbsp;<select id='scan-rss-dialog-rss-allowsotherdomains'>";
		$ret .= "<option value='0'>No</option>";
		$ret .= "<option value='1' selected>Yes</option>";
		$ret .= "</select><br/>";

		$ret .= "<br/><input type='button' id='scan-rss-dialog-rss-add-ok' value='Add' onClick='AddRss();'>";
		$ret .= "</form><br />";

	}
	else {
		$ret = "<br/><strong>No RSS feeds found</strong><br/>";
	}

	print($ret);
	exit();
}

if ($action=="exportsources")
{
	$mode = "";
	$ids = "";
	if (isset($_POST["mode"])) $mode = $_POST["mode"];
	if (isset($_POST["ids"])) $ids = array_map('intval',$_POST["ids"]);

	$mg = mg_connect ($config, "", "", "");
	if ($mg)
	{
		$query = array ("name" => "version");
		mg_get_value($mg, "infos", "value", $query, $version);

		$xml = new SimpleXMLElement('<crawlanywhere/>');
		$version = $xml->addChild('version', $version);
		$sources = $xml->addChild('sources');

		$stmt = new mg_stmt_select($mg, "sources");

		$query = array ("id_account" => intval($id_account_current));
		if ($mode=='selection') {
			$query2 = array ("id" => array( '$in' => $ids));
		} else {
			$query2 = array('$or' => array(array ("deleted" => "0"), array ("deleted" => NULL)));
		}
		$query = array ('$and' => array($query,$query2));
		$stmt->setQuery ($query);
		$stmt->setSort(array( "name" => 1 ));
		$count = $stmt->execute();
		if ($count==0)
		{
			$arr = array('status' => 'error');
			echo json_encode($arr);
			exit();
		}
		
		$cursor = $stmt->getCursor();		
		while ($cursor->hasNext())
		{
			$rs = $cursor->getNext();
			$source = $sources->addChild('source');
			foreach ($rs as $key => $value) {
				if ($key[0]=='_') continue;
				if ($key=='name_sort') continue;
				
				if ($key=='params') {
					if (strlen($value)>0) {

						$params = new SimpleXMLElement($value);
						
						foreach ($params->children() as $item) {

							$n=(String)$item->getName();
							$v=(String)$item;
							$x=$item->asXml();

							//$r = print_r($item, true);
								
							if (!is_blank($v)) {
								$el2 = new SimpleXMLElement($x);
								$c2=$item->children();
								if ($c2) {
									$source = $c2[0]->asXml();
									$source = trim(preg_replace('/\t+/', '', $source));
									$source = trim(preg_replace('/\n+/', '', $source));
									$source = trim(preg_replace('/\s+/', '', $source));
									$source = xmlAppendChild($source, $n, $source);
									
// 									$v = $c2[0]->asXml();
// 									//$v = base64_encode($v);
// 									$el = $source->addChild($n, $v);
// 									//$el->addAttribute('base64', '1');
								}
								else {
									$el = $source->addChild($n, $v);
								}
							}
							else {
								$c=$item->children();
								if ($c) {
									$source = $c[0]->asXml();
									$source = trim(preg_replace('/\t+/', '', $source));
									$source = trim(preg_replace('/\n+/', '', $source));
									$source = trim(preg_replace('/\s+/', '', $source));
									$source = xmlAppendChild($source, $n, $source);
									
// 									$v = $c[0]->asXml();
// 									//$v = base64_encode($v);
// 									$el = $source->addChild($n, $v);
// 									//$el->addAttribute('base64', '1');
								}
							}
						}
					}
				} else {
					//if ($key=='params') $value = base64_encode($value);
					
					if($value instanceof MongoDate) $value = date('Y-m-d H:i:s', $value->sec);
					
// 					if (startswith($value, '<?xml'))  $value = base64_encode($value);
// 					$el = $source->addChild($key, $value);
// 					if (startswith($value, '<?xml')) $el->addAttribute('base64', '1');

					if (!is_blank($value)) {
						if (startswith($value, '<?xml')) {
							$source = xmlAppendChild($source, $key, $value);
						} else {
							$el = $source->addChild($key, $value);
						}
// 						$el = new SimpleXMLElement($value);
// 						$c=$item->children();
// 						if ($c) {
// 							$source = xmlAppendChild($source, $n, $value);	
// 						}
// 						else {
// 							$el = $source->addChild($key, $value);
// 						}
					} 
				}
			}
		}

		$temp_path = $config->getDefault("application.tmp_path", "/opt/crawler/tmp");
		$basename =  uniqid() . ".xml";;
		$filename = $temp_path. "/" . $basename;

		$dom = new DOMDocument();
		$dom->loadXML($xml->asXML());
		$dom->formatOutput = true;
		$formattedXML = $dom->saveXML();

		write_file($filename, $formattedXML);

		$arr = array('status' => 'success', 'filename' => $basename);
		echo json_encode($arr);
	}
	else {
		$arr = array('status' => 'error');
		echo json_encode($arr);
	}
	exit();
}

function update_field($field_name, $field_list) {
	if (count($field_list)==0) return true;
	if (!in_array($field_name, $field_list)) return false;
	return true;
}

if ($action=="importsources")
{
	$match = "";
	$strategy = "";
	$status = "";
	$check = "";
	$reset = "";
	$priority = "0";

	if (isset($_GET["match"])) $match = $_GET["match"];
	if (isset($_GET["strategy"])) $strategy = $_GET["strategy"];
	if (isset($_GET["status"])) $status = $_GET["status"];
	if (isset($_GET["check"])) $check = $_GET["check"];
	if (isset($_GET["reset"])) $reset = $_GET["reset"];
	if (isset($_GET["priority"])) $priority = $_GET["priority"];
	
	$filename = $_FILES['import-dialog-form-file']['name'];
	$filename = basename($filename);
	$file_temp = $_FILES['import-dialog-form-file']['tmp_name'];

	if (file_exists($file_temp)) {

		$import_use_custom = $config->getDefault("sources.import_use_custom", "");
		if (!empty($import_use_custom) && file_exists ('../../custom/' . $import_use_custom)) {
			$custom = "0";
			if (isset($_GET["custom"])) $custom = $_GET["custom"];
			if ($custom!="1") $import_use_custom = false;
		} else { 
			$import_use_custom = false;	
		}	
		
		if ($import_use_custom) {
			include '../../custom/' . $import_use_custom;
			$converter = new ImportConvert($file_temp);
			$xml = $converter->getXml();
		} else {
			$xml = simplexml_load_file($file_temp);
		}
		
		if (!empty($xml)) {

			$mg = mg_connect ($config, "", "", "");
			if ($mg)
			{
				$source = $xml->sources->source;
				foreach ($source as $item) {

					$mode = "insert";

					//$query = array ("id_account" => intval($id_account_current));
					$query = array ('$and' => array(array ("id_account" => intval($id_account_current)), array ("deleted" => "0")));
						
					if ($match=='id') {
						$query = array ('$and' => array($query, array ("id" => intval((string) $item->id))));
						$count = mg_row_count($mg, "sources", $query);
						if ($count > 1) continue;
						if ($count == 1) $mode = "update";
					}

					if ($match=='name') {
						$query = array ('$and' => array($query, array ("name" => (string) $item->name)));
						$count = mg_row_count($mg, "sources", $query);
						if ($count > 1) continue;
						if ($count == 1) $mode = "update";
					}
					
					$import_id_field = $config->getDefault("sources.import_id_field", "");
					if (!empty($import_id_field) && strtolower($import_id_field)!='name' && $match==$import_id_field) {
						$query = array ('$and' => array($query, array ("import_id" => (string) $item->$match)));
						$count = mg_row_count($mg, "sources", $query);
						if ($count > 1) {
							continue;
						}
						if ($count == 1) {
							$mode = "update";
						}
					}
												
// 					if ($match=='host') {
// 						$query = array ('$and' => array($query, array ("url_host" => (string) $item->url_host)));
// 						$count = mg_row_count($mg, "sources", $query);
// 						if ($count > 1) continue;
// 						if ($count == 1) $mode = "update";
// 					}

					if ($mode == 'insert') {
						$stmt = new mg_stmt_insert($mg, "sources", $mg_source_defaults);
						$stmt->addColumnValue("id_account", intval($id_account_current));
						$stmt->addColumnValueDate("createtime");
						$enabled = $status;
						$import_update_fields = Array();
					}
					else {
						if ($strategy=='skip') continue;
						$stmt = new mg_stmt_update($mg, "sources");
						$stmt->setQuery($query);
						$enabled = (string) $item->enabled;
						$import_update_fields = array_map('trim', explode(',', $config->getDefault("sources.import_update_fields", "")));
					}

					if (!empty($import_id_field) && strtolower($import_id_field)!='name') {
						$stmt->addColumnValue("import_id", (string) $item->$match);
					}
					
					if (update_field("deleted", $import_update_fields)) $stmt->addColumnValue("deleted", "0");
					if (update_field("enabled", $import_update_fields)) $stmt->addColumnValue("enabled", $enabled);

					if ($reset == "1") {
						$stmt->addColumnValue("crawl_mode", "2");
					}
					else {
						$stmt->addColumnValue("crawl_mode", "0");
					}
					if (update_field("crawl_firstcompleted", $import_update_fields)) $stmt->addColumnValue("crawl_firstcompleted", "0");
					if (update_field("crawl_priority", $import_update_fields)) $stmt->addColumnValue("crawl_priority", $priority);
					if (update_field("crawl_nexttime", $import_update_fields)) $stmt->addColumnValueDate("crawl_nexttime");
					
					if (update_field("crawl_process_status", $import_update_fields)) $stmt->addColumnValue("crawl_process_status", "0");
						
					$query = array ("id" => intval($id_account_current));
					mg_get_value($mg, "accounts", "id_target", $query, $id_target);			
					if (!empty($id_target))	{	
						if (update_field("id_target", $import_update_fields)) $stmt->addColumnValue("id_target", intval($id_target));
					}

					$ignore = array();
					$ignore[]='id';
					$ignore[]='id_account';
					$ignore[]='id_target';
					$ignore[]='createtime';
					$ignore[]='deleted';
					$ignore[]='enabled';
					$ignore[]='crawl_mode';
					$ignore[]='crawl_firstcompleted';
					$ignore[]='crawl_priority';
					$ignore[]='crawl_nexttime';
					$ignore[]='crawl_process_status';
					$ignore[]='crawl_lastpagecount';
					$ignore[]='crawl_pagecount';
					$ignore[]='crawl_pagecount_success';
					$ignore[]='running_crawl_item_processed';
					$ignore[]='running_crawl_item_to_process';

					$xml_items = array();
					$xml_items[]='url';
					$xml_items[]='crawl_filtering_rules';
					$xml_items[]='crawl_schedule';
						
					//$c = $item->children();
					
					foreach($item->children() as $name => $data) {
						if (!update_field($name, $import_update_fields)) continue;
						
						if (!in_array($name,$ignore)) {
							$d = (String)$data;							
							if (empty(trim($d)) && in_array($name,$xml_items)) {
								$x = $data->children();
								$d = $x[0]->asXML();
								$stmt->addColumnValue($name, $d);
							} else {
								$d = (String)$data;
								if ($name=='params') {
									$stmt->addColumnValue($name, base64_decode($d));
								} else {									
									if ($name=='name') {
										$stmt->addColumnValue('name_sort', strtolower(remove_leading_empty_words(remove_accents($d))));
									}
									$stmt->addColumnValue($name, $d);
								}
							}
						}
					}

					if ($mode == 'insert' && !$stmt->checkNotNull ($mg_source_not_null)) {
						$arr = array('status' => 'error');
						//echo json_encode($arr);
						//exit();
					} else {
						$rs = $stmt->execute();
					}
				}
			}
			$arr = array('status' => 'success');
			echo json_encode($arr);
		} else {
			$arr = array('status' => 'error');
			$arr = array('message' => 'parsing');
			echo json_encode($arr);
		}
	} else {
		$err = $_FILES['import-dialog-form-file']['error'];
		$arr = array('status' => 'error');
		if ($err==2) 
			$arr = array('message' => 'too big file');
		else 
			$arr = array('message' => 'unknow');
		echo json_encode($arr);
	}
	exit();
}

function country_language_cmp($a, $b) {
	if (strtoupper($a[0])=='UNKNOWN' || strtoupper($a[0])=='XX') return -1;
	if (strtoupper($b[0])=='UNKNOWN' || strtoupper($b[0])=='XX') return 1;
	return strcmp($a[1], $b[1]);
}

?>
