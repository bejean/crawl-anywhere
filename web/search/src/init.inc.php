<?php
//error_reporting(E_ERROR);
header('Content-Type: text/html; Charset=UTF-8');
set_time_limit (300);
require_once("lib/config.class.inc.php");
require_once("lib/solr.solr_php_client.class.inc.php");
require_once("lib/adodb5/adodb.inc.php");
require_once("lib/db.inc.php");
require_once("lib/user.inc.php");
require_once("lib/log.class.inc.php");

session_start();

//
// $config initialisation
//
$config_file = "";
$key = "";
if (isset($_GET["key"]) && $_GET["key"]!="") {
	//$key = urldecode($_GET["key"]);
	//$key = convert_uudecode($key);
	$key = base64_decode($_GET["key"]);

	$param=explode("|",$key);
	$config_file = trim($param[1]);
}
if ($config_file=="") {
	if (isset($_GET["config"]) && $_GET["config"]!="") {
		$config_file =$_GET["config"];
	}
	if (isset($_POST["config"]) && $_POST["config"]!="") {
		$config_file =$_POST["config"];
	}
	if ($config_file=="") {
		if (isset($_SESSION["config"])) {
			$config_file = $_SESSION["config"];
		}
		else {
			if (isset($_POST["config_save"]) && $_POST["config_save"]!="") {
				$config_file =$_POST["config_save"];
			}
		}
	}
}

if ($config_file=="") $config_file ="config.ini";

if (!isset($_SESSION["config"]) || $_SESSION["config"] != $config_file) {
	session_unset();
	$_SESSION["config"] = $config_file;
}

//
// core ?
//
if (isset($_GET["key"]) && $_GET["key"]!="") {
	$_SESSION["core"] = "";
	if ($param[0]!='') $_SESSION["core"] = $param[0];
} else {
	if (isset($_POST["core"])) {
		$_SESSION["core"] = $_POST["core"];
	}
}

$configFilePath = $rootpath . "/../config/" . $config_file;
if (!file_exists($configFilePath)) {
	echo "Configuration file not found [".$configFilePath."]";
	exit();
}
$config = new ConfigTool();
$config->setConfigFromFile($configFilePath);
$config->setCookiePath("/");
$config->setCookieDomain($_SERVER['SERVER_NAME']) ;
$config->setCookieExpire(3600*10);

$debug=($config->get("application.debug")=="1");
$debug_file = $config->getDefault("application.logfile", "");

$cLog = new Logger($debug_file);
if ($debug) $cLog->setDebug(true);
$cLog->log_debug("------ search2 ------");
$cLog->log_debug("config = " . $config_file);
$cLog->log_debug("key = " . $key);


// $user initialisation
//
if (isset($_SESSION["user"]))
{
	$user = $_SESSION["user"];
}
else
{
	$user = new User();
	$_SESSION["user"] = $user;
}

if (!isset($_SESSION["core"])) {
	if (isset($_POST["core_save"]) && $_POST["core_save"]!="") {
		$_SESSION["core"] = $_POST["core_save"];
	}
}

if (isset($_SESSION["core"]) && $_SESSION["core"]!="") {
	$u = parse_url($_SESSION["core"]);
	
	//$u= parse_url("http://localhost:8180/solr_9ecc275c-72af-4429-aae4-6dd604d6ea2b/core/");
	
	$solr_port = "";
	if (isset($u["port"])) $solr_port = $u["port"];
	if ($solr_port=="") $solr_port = "80";
	$config->set("solr.host", $u["host"]);
	$config->set("solr.port", $solr_port);
	$config->set("solr.baseurl", $u["path"]);
	$config->set("solr.corename", "");
}

//
// localisation
//
$user_language = getUserDefaultLanguage();

$locale = "";
if (isset($_GET["locale"]) && $_GET["locale"]!="") {
	$locale =$_GET["locale"];
}
if (isset($_POST["locale"]) && $_POST["locale"]!="") {
	$locale =$_POST["locale"];
}
if ($locale=="") {
	if (isset($_SESSION["locale"])) {
		$locale = $_SESSION["locale"];
	}
	else {
		if ($user_language=='en') $locale = 'en';
		if ($user_language=='fr') $locale = 'fr';
		if ($locale=='') $locale = $config->getDefault("application.locale", "en");
	}
}
if (!isset($_SESSION["locale"]) || $_SESSION["locale"] != $locale) {
	$_SESSION["locale"] = $locale;
}
initGettext("search", $locale, dirname(__FILE__) . "/locale");

//
// Theme
//
$theme_name = $config->getDefault("application.theme_name", "ca");
require_once("themes/theme.class.inc.php");
require_once("themes/" . $theme_name . "/theme_" . $theme_name . ".class.inc.php");
$theme = new Theme($config, $locale, $theme_name);
initGettext($theme_name, $locale, dirname(__FILE__) . "/pub/themes/" . $theme_name . "/locale", false);

if (isset($_SESSION["lexicons-" . $theme->getSolrUrl()])) {
	$aLexicons = $_SESSION["lexicons-" . $theme->getSolrUrl()];
}
else {
	$aLexicons = getSolrLexicons($theme->getSolrHost(), $theme->getSolrPort(), $theme->getSolrBaseUrl(), $theme->getSolrCore()); // language,country,contenttyperoot
	$_SESSION["lexicons-" . $theme->getSolrUrl()] = $aLexicons;
}

$aCountries = getMappingArray("countries", "code_countries.txt", true, $aLexicons, "country");
$aLanguages = getMappingArray("languages", "code_languages.txt", true, $aLexicons, "language");
$aLanguagesStemmed = getMappingArray("languages_stemmed", "code_languages_stemmed.txt", true, $aLexicons, "language");
$aContentType = getMappingArray("contenttype", "code_contenttype.txt", true, $aLexicons, "contenttyperoot");
$aContentTypeImage = getMappingArray("contenttypeimage", "code_contenttype.txt", false, $aLexicons, "contenttyperoot", 2);

$solrMainContentLanguage = getSolrMainContentLanguage($aLexicons, $user_language);

/*
$aCountriesForm = getMappingArray("", "code_countries.txt", true, $aLexicons, "country");
$aLanguagesForm = getMappingArray("", "code_languages.txt", true, $aLexicons, "language");
$aLanguagesStemmedForm = getMappingArray("", "code_languages_stemmed.txt", true, $aLexicons, "language");
$aContentTypeForm = getMappingArray("", "code_contenttype.txt", true, $aLexicons, "contenttyperoot");
$solrMainContentLanguage = getSolrMainContentLanguage($aLexicons, $user_language);
*/

$solr_version = getSolrVersion($theme->getSolrHost(), $theme->getSolrPort(), $theme->getSolrBaseUrl(), $theme->getSolrCore());
	
//
// initialisation des globales
//
$usecollections = $config->get("search.use_collections");
$usetags = $config->get("search.use_tags");
$usetagcloud = $config->get("search.use_tagcloud");
//if (!empty($solr_version) && $solr_version{0}!='4') $usetagcloud = 0;
if (!empty($solr_version) && solrVersionAsANumber($solr_version) < 430) $usetagcloud = 0;

$useadvanced = $config->get("search.use_advanced");
$usecountry = $config->get("search.use_country");
$uselanguage = $config->get("search.use_language");
$usecontenttype = $config->get("search.use_contenttype");
$usesourcename = $config->get("search.use_sourcename");
$uselocation = $config->get("search.use_location");

$facet_union = ($config->getDefault("facet.mode_union", "0", false)=="1");
$facet_union = false;
$facetuse = $theme->getFacet($config->get("facet.use"));
$facetlimit = $config->get("facet.limit", "10");

$facetextra = $theme->getFacetExtra($config->getDefault("facet.use_extra", ""));
$facetqueries = loadFacetQueries($config);

$resultshowsource = ($config->get("results.showsource")=='1');
$resultshowmeta = ($config->get("results.showmeta")=='1');

$resultshowpermalink = ($config->get("results.show_permalink")=='1');
$resultshowrss = ($config->get("results.show_rss")=='1');

$results_img_height=$config->getDefault("results.img_height", "0");
$results_img_width=$config->getDefault("results.img_width", "0");

$search_requesthandler = $config->getDefault("search.requesthandler", "");

$search_multilingual = ($config->get("search.multilingual")=='1');
$search_language_code = $config->get("search.language_code");

$groupsize = $config->getDefault("results.groupsize", "0");
$groupdisplaysize = $config->getDefault("results.groupdisplaysize", "3");

$search_default = $config->getDefault("search.default", "");
$search_default_sort = $config->getDefault("search.default_sort", "");



//
// Initialisation Gettext
//
function initGettext($domain, $locale, $ressource_path, $default = TRUE) {

	switch ($locale) {
		case 'en':
			$locale = 'en_US';
			break;
		default:
			$locale = 'fr_FR';
	}

	//echo $domain . "<br>";
	//echo $locale . "<br>";
	//echo $ressource_path . "<br>";
	if ($default) {
		putenv("LANG=".$locale . ".utf8"); 			// On modifie la variable d'environnement
		setlocale(LC_MESSAGES, $locale . ".utf8"); 	// On modifie les informations de localisation en fonction de la langue
	}
	bindtextdomain($domain, $ressource_path); 	// On indique le chemin vers les fichiers .mo
	if ($default) textdomain($domain); 						// Le nom du domaine par défaut
	bind_textdomain_codeset($domain, 'UTF-8');
	//print_r(error_get_last());
	//exit();
}

//
// fonctions utilitaires génériques
//
function getRequestParam($paramName) {
	if (isset($_POST[$paramName])) return $_POST[$paramName];
	if (isset($_GET[$paramName])) return $_GET[$paramName];
	return "";
}

function startsWith($haystack,$needle,$case=true) {
	if ($case) return (strcmp(substr($haystack, 0, strlen($needle)),$needle)===0);
	return (strcasecmp(substr($haystack, 0, strlen($needle)),$needle)===0);
}

function endsWith($haystack,$needle,$case=true) {
  $expectedPosition = strlen($haystack) - strlen($needle);
  if($case) return strrpos($haystack, $needle, 0) === $expectedPosition;
  return strripos($haystack, $needle, 0) === $expectedPosition;
}


/**
 * Récupération d'une variable HTTP (POST ou GET)
 *
 * @param  $varName string le nom de la variable
 * @param  $varValue string la valeur en retour
 * @param [$defaultValue] string valeur fournie si la variable n'est pas définie
 * @return false si la variable n'est pas trouvée
 */
function GetHttpVar( $varName, &$varValue, $defaultValue='') {
	$res = true;
	if (isset( $_REQUEST[ $varName]))
	$varValue = Tool::StripHTTPVar( $_REQUEST[ $varName]);
	elseif (isset( $_GET[ $varName]))
	$varValue = Tool::StripHTTPVar( $_GET[ $varName]);
	elseif (isset( $_POST[ $varName]))
	$varValue = Tool::StripHTTPVar( $_POST[ $varName]);
	else {
		$varValue = $defaultValue;
		$res = false;
	}
	return $res;
} // GetHttpVar


/**
 * Unstrip slashes if necessary, and handle arrays/string vars
 *
 * @return unstriped array/string
 */
function StripHTTPVar($mixed) {
	if (get_magic_quotes_gpc()) {
		if (is_array( $mixed)) {
			$res = Array();
			foreach ($mixed as $k=>$v) {
				$res[$k] = stripslashes( $v);
			}
			return $res;
		} else {
			return stripslashes( $mixed);
		}
	} else
	return $mixed;
} // StripHTTPVar


//
// fonctions utilitaires
//
function getMappingArray($name, $file_name, $ucw, $aLexicons, $field, $col_number = 1) {
	//if (empty($name) || !isset($_SESSION[$name])) {
		$handle = fopen(dirname(__FILE__) . "/ressources/" . $file_name, "rb");
		while ($handle && !feof($handle) ) {
			$line = trim(fgets($handle));
			if ($line!="") {
				$parts = explode(';', $line);
				$key = trim(strtolower($parts[0]));
				if (empty($aLexicons) || empty($field) || array_key_exists ( strtoupper($key) , $aLexicons[$field] )  || array_key_exists ( strtolower($key) , $aLexicons[$field] )) {
					$aMapping[trim(strtolower($parts[0]))] = strtolower($parts[$col_number]);
					if ($ucw) $aMapping[trim(strtolower($parts[0]))] = ucwords($aMapping[trim(strtolower($parts[0]))]);
				} else {
					$i = 1;
				}
			}
		}
		fclose($handle);
		if (!empty($name)) $_SESSION[$name] = $aMapping;
	//}
	//else {
	//	$aMapping = $_SESSION[$name];
	//}
	return $aMapping;
}

function getSolrVersion($solr_host, $solr_port, $solr_baseurl, $solr_corename) {
	if ($solr_baseurl=="undefined") $solr_baseurl = "";
	if ($solr_corename=="undefined") $solr_corename = "";
	$solr = new Solr();
	if ($solr->connect($solr_host, $solr_port, $solr_baseurl, $solr_corename)) {
		return $solr->getVersion();
	}
	return "";
}

function getSolrLexicons($solr_host, $solr_port, $solr_baseurl, $solr_corename) {
	//if (!isset($_SESSION['lexicons'])) {
	if ($solr_baseurl=="undefined") $solr_baseurl = "";
	if ($solr_corename=="undefined") $solr_corename = "";
	$solr = new Solr();
	if ($solr->connect($solr_host, $solr_port, $solr_baseurl, $solr_corename)) {
		$lexicons = $solr->getFiedValues('language,country,contenttyperoot');
		return $lexicons;
	}
}

function getSolrMainContentLanguage($aLexicons, $default) {
	$ret = $default;
	if (!empty($aLexicons)) {
		$ret = key(array_slice($aLexicons['language'], 0, 1));
	}
	return strtolower($ret);
}

function getUserDefaultLanguage() {
	if (isset($_SERVER["HTTP_ACCEPT_LANGUAGE"]))
	return parseDefaultLanguage($_SERVER["HTTP_ACCEPT_LANGUAGE"]);
	else
	return parseDefaultLanguage(NULL);
}

function parseDefaultLanguage($http_accept, $deflang = "en") {
	if(isset($http_accept) && strlen($http_accept) > 1)  {
		# Split possible languages into array
		$x = explode(",",$http_accept);
	foreach ($x as $val) {
		#check for q-value and create associative array. No q-value means 1 by rule
		if(preg_match("/(.*);q=([0-1]{0,1}\.\d{0,4})/i",$val,$matches))
	$lang[$matches[1]] = (float)$matches[2];
	else
	$lang[$val] = 1.0;
	}

	#return default language (highest q-value)
	$qval = 0.0;
	foreach ($lang as $key => $value) {
		if ($value > $qval) {
			$qval = (float)$value;
			$deflang = $key;
		}
	}
	}
	return strtolower($deflang);
}


/*
 * Method to return the last occurrence of a substring within a string
*/
function strLastIndexOf($sub_str,$instr) {
	if(strstr($instr,$sub_str)!="") {
		return(strlen($instr)-strpos(strrev($instr),$sub_str));
	}
	return(-1);
}

/*
 * 
 */
function loadFacetQueries($config) {
	$queries = $config->getDefault("facet.use_queries", "");
	if (empty($queries)) return null;
	$aQueries = array();
	$aTmp = explode(',', $queries);
	foreach($aTmp as $field) {
		$q = loadFacetOneQuery($config, $field);
		if ($q!=null) $aQueries[$field] = $q;
	}
	return $aQueries;	
	//facet.use_queries								= publishtime
	//facet.query_publishtime							= publishtime|date|NOW/DAY-7DAYS TO NOW|NOW/MONTH-3MONTHS TO NOW|NOW/MONTH-6MONTHS TO NOW|NOW/YEAR-1YEAR TO NOW|NOW/YEAR-2YEAR TO NOW|NOW/YEAR-5YEAR TO NOW
}

function loadFacetOneQuery($config, $field) {
	$query = $config->getDefault("facet.query_" . $field, "");
	if (empty($query)) return null;
	$aQuery = array();
	$aTmp = explode('|', $query);
	$aQuery['field'] = $aTmp[0];
	$aQuery['mnemo'] = $aTmp[1];
	$aConditions = array();
	for ($i=2; $i<count($aTmp); $i++) {
		$c = explode(':', $aTmp[$i]);
		$c2 = array();
		$c2['mnemo'] = $c[0];
		$c2['condition'] = $c[1];
		$aConditions[]=$c2;
	}
	$aQuery['conditions'] = $aConditions;
	return $aQuery;
}


function solrVersionAsANumber($solr_version) {
	$solr_version = str_replace('.', '', $solr_version);
	return intval(substr($solr_version, 0,3));
}


?>
