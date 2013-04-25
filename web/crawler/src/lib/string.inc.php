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
?>