<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================

function startswith($hay, $needle) {
  return substr($hay, 0, strlen($needle)) == $needle;
}

function endswith($hay, $needle) {
  return substr($hay, -strlen($needle)) == $needle;
}

function remove_accents($str, $charset='utf-8') {
	$str = htmlentities($str, ENT_NOQUOTES, $charset);
	$str = preg_replace('#&([A-za-z])(?:acute|cedil|caron|circ|grave|orn|ring|slash|th|tilde|uml);#', '\1', $str);
	$str = preg_replace('#&([A-za-z]{2})(?:lig);#', '\1', $str); // pour les ligatures e.g. '&oelig;'
	$str = preg_replace('#&[^;]+;#', '', $str); // supprime les autres caractères
	return $str;
}

function remove_leading_empty_words($str) {
	$words = file(dirname(__FILE__) . '/../pub/ressources/empty_words.txt', FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
	if ($words==FALSE) return $str;
	foreach ($words as $word) {
		$word = trim($word);
		$pattern = $word . ' ';
		if (strtolower(substr($str, 0, strlen($pattern))) == strtolower($pattern)) {
		    $str = trim(substr($str, strlen($pattern)));
		}	
		$pattern = $word . "'";
		if (strtolower(substr($str, 0, strlen($pattern))) == strtolower($pattern)) {
		    $str = trim(substr($str, strlen($pattern)));
		}	
	}
	return $str;
}

function is_blank($value) {
	return empty($value) && !is_numeric($value);
}

function fjsp($value) {
	return htmlspecialchars(addslashes($value), ENT_QUOTES, "UTF-8", false);
}

//////////////////////////////////////////////////////////
// function encodeForInput($value) {
// 	return htmlentities($value, ENT_QUOTES, "UTF-8", false);
// }


function fi($value) {
	return htmlspecialchars($value, ENT_QUOTES, "UTF-8", false);
}
?>