<?php
$rootpath = dirname(__FILE__);
require_once("../init.inc.php");

/*
 * Login ?
 */
$login = ($config->getDefault("login", "0")=="1");
if ($login && $user->getName()=="")
{
	$login_page = $config->getDefault("login.page", "");
	if (!empty($login_page)) {
		//ob_clean();
		header("Status: 301 Moved Permanently", false, 301);
		header("Location: " . $login_page);
		//ob_end_flush();
		exit();	
	}
}

$key = base64_decode($_GET["id"]);
$param=explode("|",$key);
$id_src = trim($param[0]);
$id = trim($param[1]);

//$id = $_GET['id'];
//$id_src = $_GET['idsrc'];

// $id_src appartient bien au user ?

// ouvre un fichier en mode binaire
$name = $id;
$offset = last_index_of('/',$name);
if ($offset!=-1) $name = substr($id, $offset);

// ' in file name
if (get_magic_quotes_gpc()) $id = stripcslashes($id);

$ct = returnMIMEType ( $name );

$db = db_connect ($config, "", "", "", "dropbox");
if ($db)
{
	$token_key='';
	$token_secret='';
	$ret = db_get_value($db, "sources", "params", "id='" . $id_src . "'", $value);
	
	$xml = simplexml_load_string($value);
	$params = json_decode(json_encode($xml),TRUE);
	foreach ($params as $key => $value) {
		if ($key=='token_key') $token_key = $value;
		if ($key=='token_secret') $token_secret = $value;
	}

	$url = $config->get("crawler.ws");
	$url .= "?action=dropboxgetfile";
	$url .= "&filepath=" . urlencode($id);
	$url .= "&tokenkey=" . $token_key;
	$url .= "&tokensecret=" . $token_secret;
	
	$fp = fopen($url, 'rb');

	// envoie les bons en-têtes
	header("Content-Type: " . $ct);
	//header("Content-Length: " . filesize($name));
	header('Content-Disposition: inline; filename="' . $name . '"');
	//header('Content-Disposition: attachment; filename="' . $name . '"');
	//header("Content-Transfer-Encoding: binary\n");

	// Saveas dialog box
	// header("content-type: application/octet-stream");

	// envoie le contenu du fichier, puis stoppe le script
	fpassthru($fp);
	fclose($fp);
}

/*
 Method to return the last occurrence of a substring within a
string
*/
function last_index_of($sub_str,$instr) {
	if(strstr($instr,$sub_str)!="") {
		return(strlen($instr)-strpos(strrev($instr),$sub_str));
	}
	return(-1);
}

function returnMIMEType($filename) {
	preg_match("|\.([a-z0-9]{2,4})$|i", $filename, $fileSuffix);

	switch(strtolower($fileSuffix[1])) {
		case "js" :
			return "application/x-javascript";

		case "json" :
			return "application/json";

		case "jpg" :
		case "jpeg" :
		case "jpe" :
			return "image/jpg";

		case "png" :
		case "gif" :
		case "bmp" :
		case "tiff" :
			return "image/".strtolower($fileSuffix[1]);

		case "css" :
			return "text/css";

		case "xml" :
			return "application/xml";

		case "doc" :
		case "docx" :
			return "application/msword";

		case "xls" :
		case "xlt" :
		case "xlm" :
		case "xld" :
		case "xla" :
		case "xlc" :
		case "xlw" :
		case "xll" :
			return "application/vnd.ms-excel";

		case "ppt" :
		case "pps" :
			return "application/vnd.ms-powerpoint";

		case "rtf" :
			return "application/rtf";

		case "pdf" :
			return "application/pdf";

		case "html" :
		case "htm" :
		case "php" :
			return "text/html";

		case "txt" :
			return "text/plain";

		case "mpeg" :
		case "mpg" :
		case "mpe" :
			return "video/mpeg";

		case "mp3" :
			return "audio/mpeg3";

		case "wav" :
			return "audio/wav";

		case "aiff" :
		case "aif" :
			return "audio/aiff";

		case "avi" :
			return "video/msvideo";

		case "wmv" :
			return "video/x-ms-wmv";

		case "mov" :
			return "video/quicktime";

		case "zip" :
			return "application/zip";

		case "tar" :
			return "application/x-tar";

		case "swf" :
			return "application/x-shockwave-flash";

		default :
			if(function_exists("mime_content_type")) {
			$fileSuffix = mime_content_type($filename);
		}

		return "unknown/" . trim($fileSuffix[0], ".");
	}
}


?>