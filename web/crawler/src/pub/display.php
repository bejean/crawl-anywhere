<?php
require_once("../init_gpc.inc.php");
require_once("../init.inc.php");

$filename = $_GET['file'];
$format = $_GET['format'];
$mode = $_GET['mode'];

$temp_path = $config->getDefault("application.tmp_path", "/opt/crawler/tmp");
$filename = $temp_path . '/' . $filename;

if (($filename != "") && (file_exists($filename)))
{
	if ($mode=='download') {
		$size = filesize($filename);
		header("Content-Type: application/force-download; name=\"" . basename($filename) . "\"");
		header("Content-Transfer-Encoding: binary");
		header("Content-Length: $size");
		header("Content-Disposition: attachment; filename=\"" . basename($filename) . "\"");
		header("Expires: 0");
		header("Cache-Control: no-cache, must-revalidate");
		header("Pragma: no-cache");
		readfile($filename);
		exit();
	}
	else {
		if ($format!='') {
			if ($format!='html') header('Content-type: text/html; charset=UTF-8');
			if ($format!='text') header('Content-type: text/plain; charset=UTF-8');
			if ($format!='xml') header('Content-type: text/xml; charset=UTF-8');
		}
		else {
			header('Content-type: text/html; charset=UTF-8');
		}

		$fh = fopen($filename, 'r');
		$data = fread($fh, filesize($filename));
		fclose($fh);
		echo $data;
	}
	//unlink($filename);
}
?>