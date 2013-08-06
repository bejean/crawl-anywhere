<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================

?>
<div id="contenu">
    <h2>Crawler settings (read only)</h2>
    <div id="settings">
<?php
$crawler_xml_file = $config->get("crawler.properties");
if (!file_exists($crawler_xml_file)) {
	echo "Crawler configuration file not found (" . $crawler_xml_file . ")";
}
else {
	$xml = simplexml_load_file($crawler_xml_file);
	echo '<pre>';
	$str = $xml->asXml();
	$str = str_replace("<", "&lt;", $str);
	$str = str_replace(">", "&gt;", $str);
	echo $str;
	echo '</pre>';
}
?>
    </div>   
</div>