<?php
//error_reporting(E_ERROR);
header('Content-Type: text/html; Charset=UTF-8');
set_time_limit (300);
require_once("lib/config.class.inc.php");
require_once("lib/solr.solr_php_client.class.inc.php");

session_start();

//
// $config initialisation
//
$config_file = "";

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

if (!isset($_SESSION["core"])) {
	if (isset($_POST["core_save"]) && $_POST["core_save"]!="") {
		$_SESSION["core"] = $_POST["core_save"];
	}
}

if (isset($_SESSION["core"]) && $_SESSION["core"]!="") {
	$u = parse_url($_SESSION["core"]);
	$solr_port = "";
	if (isset($u["port"])) $solr_port = $u["port"];
	if ($solr_port=="") $solr_port = "80";

	$config->set("solr.host", $u["host"]);
	$config->set("solr.port", $solr_port);
	$config->set("solr.baseurl", $u["path"]);
	$config->set("solr.corename", "");
}

$config->setCookiePath("/");
$config->setCookieDomain($_SERVER['SERVER_NAME']) ;
$config->setCookieExpire(3600*10);

$solr_version = getSolrVersion($config->get("solr.host"), $config->get("solr.port"), $config->get("solr.baseurl"), $config->get("solr.corename"));

//
// initialisation des globales
//
$usecollections = $config->get("search.use_collections");
$usetags = $config->get("search.use_tags");
$usetagcloud = $config->get("search.use_tagcloud");
if ($solr_version{0}!='4') $usetagcloud = 0;

$useadvanced = $config->get("search.use_advanced");
$usecountry = $config->get("search.use_country");
$uselanguage = $config->get("search.use_language");
$usecontenttype = $config->get("search.use_contenttype");
$usesourcename = $config->get("search.use_sourcename");
$uselocation = $config->get("search.use_location");

$facet_union = ($config->getDefault("facet.mode_union", "0", false)=="1");
$facetcollections = $config->get("facet.use_collections");
$facettags = $config->get("facet.use_tags");
$facetcountry = $config->get("facet.use_country");
$facetlanguage = $config->get("facet.use_language");
$facetcontenttype = $config->get("facet.use_contenttype");
$facetsourcename = $config->get("facet.use_sourcename");
$facetextra = $config->getDefault("facet.use_extra", "");

$resultshowsource = ($config->get("results.showsource")=='1');
$resultshowmeta = ($config->get("results.showmeta")=='1');

$results_img_height=$config->getDefault("results.img_height", "0");
$results_img_width=$config->getDefault("results.img_width", "0");

$search_multilingual = ($config->get("search.multilingual")=='1');
$search_language_code = $config->get("search.language_code");

$groupsize = $config->getDefault("results.groupsize", "0");
$groupdisplaysize = $config->getDefault("results.groupdisplaysize", "3");

$user_language = getUserDefaultLanguage();

//
// localisation
//
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
		if ($user_language=='en') $locale = 'en_US';
		if ($user_language=='fr') $locale = 'fr_FR';
		if ($locale=='') $locale = $config->getDefault("application.locale", "fr_FR");
	}
}
if (!isset($_SESSION["locale"]) || $_SESSION["locale"] != $locale) {
	$_SESSION["locale"] = $locale;
}

initGettext("search", $locale, dirname(__FILE__) . "/locale");

$aCountries = getMappingArray("countries", "code_countries.txt", true, null, "");
$aLanguages = getMappingArray("languages", "code_languages.txt", true, null, "");
$aLanguagesStemmed = getMappingArray("languages_stemmed", "code_languages_stemmed.txt", true, null, "");
$aContentType = getMappingArray("contenttype", "code_contenttype.txt", true, null, "");
$aContentTypeImage = getMappingArray("contenttypeimage", "code_contenttype.txt", false, null, "", 2);


function initGettext($domain, $locale, $ressource_path) {

	switch ($locale) {
		case 'en':
			$locale = 'en_US';
			break;
		default;
		$locale = 'fr_FR';
	}

	//echo $domain . "<br>";
	//echo $locale . "<br>";
	//echo $ressource_path . "<br>";
	putenv("LANG=".$locale . ".utf8"); 			// On modifie la variable d'environnement
	setlocale(LC_MESSAGES, $locale . ".utf8"); 	// On modifie les informations de localisation en fonction de la langue
	bindtextdomain($domain, $ressource_path); 	// On indique le chemin vers les fichiers .mo
	textdomain($domain); 						// Le nom du domaine par défaut
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
	if (empty($name) || !isset($_SESSION[$name])) {
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
	}
	else {
		$aMapping = $_SESSION[$name];
	}
	return $aMapping;
}

function getSolrVersion($solr_host, $solr_port, $solr_baseurl, $solr_corename) {
	if ($solr_baseurl=="undefined") $solr_baseurl = "";
	if ($solr_corename=="undefined") $solr_corename = "";
	$solr = new Solr();
	if ($solr->connect($solr_host, $solr_port, $solr_baseurl, $solr_corename))
	{
		return $solr->getVersion();
	}
	return "";
}

function getSolrLexicons($solr_host, $solr_port, $solr_baseurl, $solr_corename) {
	//if (!isset($_SESSION['lexicons'])) {
	if ($solr_baseurl=="undefined") $solr_baseurl = "";
	if ($solr_corename=="undefined") $solr_corename = "";
	$solr = new Solr();
	if ($solr->connect($solr_host, $solr_port, $solr_baseurl, $solr_corename))
	{
		$lexicons = $solr->getFiedValues('language,country,contenttyperoot');
		//$_SESSION['lexicons'] = $lexicons;
	}
	//}
	//else {
	//	$lexicons = $_SESSION['lexicons'];
	//}
	return $lexicons;
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


?>
