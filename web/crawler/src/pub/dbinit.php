<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
require_once("../init_gpc.inc.php");
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

function dbinit($config, $collection, $ar) {
	$mg = mg_connect ($config, "", "", "");
	if ($mg) {
		$coll = $mg->selectCollection ($collection);
		foreach ($ar as $line) {
			$line=trim($line);
			if (!empty($line)) {
				$data = json_decode($line);
				$ret = $coll->insert($data);
			}
		}
	}
}

?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />
<title><?php print($config->get("application.title")); ?></title>
<meta http-equiv='Cache-Control' content='no-cache' />
<meta http-equiv='Pragma' content='no-cache' />
<meta http-equiv='Cache' content='no store' />
<meta http-equiv='Expires' content='0' />
<meta name='robots' content='index, nofollow' />
<link href='themes/ca/styles.css' rel='stylesheet' type='text/css' />
<script type="text/javascript" src="js/jquery-1.5.1.min.js"></script>

</head>
<body>
<div id="conteneur">
	<div id="header">
		<h1 id="header"><?php print($config->get("application.title")); ?> - Database initialisation</h1>
	</div>

<br />
<br />
<br />
<br />
<div id="dbinit">

<table align='center'>
	<tr>
		<td>

<?php

$loc_port = "";
if ($config->get("database.port") != "")
{
	$loc_port = $config->get("database.port");
}

if ($username != "")
{
	$loc_username = $username;
}
else
{
	$loc_username = $config->get("database.username");
}

if ($password != "")
{
	$loc_password = $password;
}
else
{
	$loc_password = $config->get("database.password");
}

if ($dbname != "")
{
	$loc_dbname = $dbname;
}
else
{
	$loc_dbname = $config->get("database.dbname");
}
$loc_host = $config->get("database.host");

if (!empty($db_version) && ($db_version==$ca_version)) {
	echo "Database version ($db_version) matching your Crawl-Anywhere version ($ca_version).<br />No problem detected !";
} else {

	if (!empty($db_version) && ($db_version!=$ca_version)) {
		echo "Database version ($db_version) not matching your Crawl-Anywhere version ($ca_version) !";
	}
	
	if (empty($db_version)) {
		$action='';
		if (isset($_POST["action"])) $action=$_POST["action"];
	
		if (empty($action)) {
			echo "Database not initialized !";
			
?>
<form name='login' action='' method='post'>
<input type="hidden" name="action" value="init">
<input type="submit" value="Initialize">
</form>
<?php
		} else {
			$dir_path = 'ressources/mongodb';
			if (is_dir($dir_path)) {
				if ($dh = opendir($dir_path)) {
					while (($file = readdir($dh)) !== false) {
						if (preg_match("/^init_db-(.*).json$/",$file,$matches)) {
							echo "fichier : " . $file . " - " . $matches[1] . "<br />";
							$ar = file ($dir_path . '/' . $file);
							dbinit($config, $matches[1], $ar);
						}
					}
					closedir($dh);
				}
			}
			echo "Database now initialized !";
			echo "<br /><br />";
			echo "<a href='index.php'>login now</a>";
			echo "<br /><br />";	
		}
	}
}
?>
<br />
<br />
<?php
echo 'DB Host :' . $loc_host . '<br />'; 
echo 'DB Port :' . $loc_port . '<br />'; 
echo 'DB Name :' . $loc_dbname . '<br />'; 
?>
		</td>
	</tr>
</table>
</div>

</div>
</body>
</html>

