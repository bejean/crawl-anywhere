<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================

function xmlAppendChild($xml, $name, $source) {
	$child = $xml->addChild($name);
	if (!empty($source)) {
		$elt = new SimpleXMLElement($source);
		$dom_child = dom_import_simplexml($child);
		$dom_source  = dom_import_simplexml($elt);
		$dom_source  = $dom_child->ownerDocument->importNode($dom_source, TRUE);
		$dom_child->appendChild($dom_source);
	}
	return $xml;
}


?>