<?php
$rootpath = dirname(__FILE__);
require_once("../init.inc.php");

$use_sts = ($config->getDefault("application.https", "0")=="1");
if ($use_sts && isset($_SERVER['HTTPS'])) {
	header('Strict-Transport-Security: max-age=500');
} elseif ($use_sts && !isset($_SERVER['HTTPS'])) {
	$url = 'https://'.$_SERVER["HTTP_HOST"].$_SERVER['REQUEST_URI'];
	header('Status-Code: 301');
	header('Location: ' . $url);
	exit();
}

/*
 * Some variables used as global by themes
 */
/*
$aLexicons = getSolrLexicons($config->get("solr.host"), $config->get("solr.port"), $config->get("solr.baseurl"), $config->get("solr.corename"));
$aCountriesForm = getMappingArray("", "code_countries.txt", true, $aLexicons, "country");
$aLanguagesForm = getMappingArray("", "code_languages.txt", true, $aLexicons, "language");
$aLanguagesStemmedForm = getMappingArray("", "code_languages_stemmed.txt", true, $aLexicons, "language");
$aContentTypeForm = getMappingArray("", "code_contenttype.txt", true, $aLexicons, "contenttyperoot");
$solrMainContentLanguage = getSolrMainContentLanguage($aLexicons, $user_language);
*/

/*
 * Generate page
 */
echo $theme->generateHtmlStart();
  
require_once("index.js.inc.php");
?> 
</head>
<body>
<?php 
echo $theme->generateBody();
?>
</body>
</html>