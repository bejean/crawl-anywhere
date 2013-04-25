<?php
/*
 * Login page
*/

$rootpath = dirname(__FILE__);
if (isset($_SESSION["user"])) {
	unset($_SESSION["user"]);
}

require_once("../init.inc.php");
require_once("../lib/std.encryption.class.inc.php");

$use_sts = ($config->getDefault("application.https", "0")=="1");
if ($use_sts && isset($_SERVER['HTTPS'])) {
	header('Strict-Transport-Security: max-age=500');
} elseif ($use_sts && !isset($_SERVER['HTTPS'])) {
	$url = 'https://'.$_SERVER["HTTP_HOST"].$_SERVER['REQUEST_URI'];
	header('Status-Code: 301');
	header('Location: ' . $url);
	exit();
}

//
//
//
function encrypt($value) {
	$crypt = new encryption_class;
	return $crypt->encrypt("mysolrserver", $value, strlen($value));
}
function decrypt($value) {
	$crypt = new encryption_class;
	return $crypt->decrypt("mysolrserver", $value);

}

$user_name = "";

$action="";
if (isset($_POST["action"]))
{
	$action=$_POST["action"];

	if ($action=="login")
	{
		if ($user->login($config, $_POST["user_name"], $_POST["user_password"], encrypt($_POST["user_password"]), 'login'))
		{
			$count = 0;
			$db = db_connect ($config, "", "", "", 'login');
			if ($db)
			{
				$Where = "id_user = '" . $user->getId() . "' and deleted = '0' and enabled = '1'";
				$count = db_row_count($db, "instances", $Where);
				if ($count==1) {
					db_get_value($db, "instances", "solr_url_root", $Where, $solr_core);
					if (!empty($solr_core)) {
 						$_SESSION["core"] = $solr_core;
 						header("Status: 301 Moved Permanently", false, 301);
 						header("Location: index.php");
 						exit();
					}
				}
			}
			$action_message = "Login successful !";
		}
		else
		{
			$action_message = "Login failed !";
			$user_name = $_POST["user_name"];
		}
	}
} else {
	unset($_SESSION["user"]);
}

/*
if ($user->getName()!="" && $action=="login") {
	$url = getDefaultInstance($db, $user, $config, $where);
	if (!empty($url)) {
		header("Status: 301 Moved Permanently", false, 301);
		header("Location: " . $url);
	}
}
*/

?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />
<meta http-equiv='Cache-Control' content='no-cache' />
<meta http-equiv='Pragma' content='no-cache' />
<meta http-equiv='Cache' content='no store' />
<meta http-equiv='Expires' content='0' />
<meta name='robots' content='index, nofollow' />
<script type="text/javascript" src="js/jquery-1.7.1.min.js"></script>
<link rel="stylesheet" type="text/css" href="css/reset.css" media="screen" />

<script language="javascript">

		var ajax_url = '<?php echo "ajax/login.ajax.inc.php" ?>';
	
		function trim(str)
		{
		    return str.replace(/^\s+/g,'').replace(/\s+$/g,'') 
		}
		
	    function init()
	    {
	        document.login.user_name.focus();
	    }
	
	    function doLogin()
	    {
	        if (trim(document.login.user_name.value) == "")
	        {
	            alert ("No user name");
	            return false;
	        }
	        if (trim(document.login.user_password.value) == "")
	        {
	            alert ("No password");
	            return false;
	        }
	        document.login.submit();
	        return true;
	    }

	    function checkEnter(e){ //e is event object passed from function invocation
			/*
	        var characterCode; // literal character code will be stored in this variable
	
	        if(e && e.which){ //if which property of event object is supported (NN4)
	            //e = e;
	            characterCode = e.which; //character code is contained in NN4's which propert
	        }
	        else{
	            e = event;
	            characterCode = e.keyCode; //character code is contained in IE's keyCode property
	            //e.SuppressKeyPress = true;
	        }
	        if(characterCode == 13){ //if generated character code is equal to ascii 13 (if enter key)
	        	return doLogin();
	            //e.Handled = true;
	            //e.SuppressKeyPress = true;
	            //return false;
	        }
	        else{
	            return true;
	        }
	 	    */
	    }
	    
	</script>

</head>
<body>
	<div id="container">

		<br /> <br /> <br /> <br />

		<div id="login">
			
		<?php
		if ($user->getName()=="" || $action=="") {
			
			// login form		
			if ($action=="login" && $action_message!="")
			{
		?>
		<center><strong><?php echo $action_message; ?></strong></center>	
				<br /> <br />
		<?php
			}
		?>
		
<center><strong>Login required in order to access your personal data</strong></center>	
<br />	
<form name='login' action='' method='post'><input type="hidden"
	name="action" value="login" />
<table align='center' border='0'>
	<tr>
		<td class="head">User name</td>
		<td><input id='user_name' name='user_name'
			value='<?php echo $user_name ?>' onKeyPress='checkEnter(event);'></td>
	</tr>
	<tr>
		<td class="head">Password</td>
		<td><input type='password' id='user_password' name='user_password'
			value='' onKeyPress='checkEnter(event);'> 
		</td>
	</tr>
	<tr>
		<td>&nbsp;</td>
		<td><input type="submit"
			value='login' onClick="doLogin();">
	</tr>
</table>
</form>

		<?php
		}
		else {
			echo getInstanceList($db, $user, $config, $Where);
		}
		?>

</div>
		<!-- /#login -->
		
		

<?php
function getDefaultInstance($db, $user, $config, $where) {

	$stmt = new db_stmt_select("instances");
	$stmt->addColumn ("*");

	$stmt->setWhereClause($where);
	$stmt->setOrderBy("name");
	$s = $stmt->getStatement();
	$rs = $db->Execute($s);
	if (!$rs) {
		return "";
	}

	$rs->MoveFirst();
	$count=0;
	$url="";
	while (!$rs->EOF) {
		if ($count==1) return ""; 
		$count++;
		$core_url = concatPath($rs->fields["solr_php_ws_root"] , "/core_" . $rs->fields["uuid"] . "/");
		//$url_search = "index.php?core=" . urlencode($core_url);
		//if ($search_php_config!="")
		//$url_search .= "&config=" . $search_php_config;
		//$key = base64_encode ($core_url . "|" . $config_file);
		$key = base64_encode ($core_url);		
		$url = "index.php?key=" . $key;
		$rs->MoveNext();
	}

	return $url;
}


function getInstanceList($db, $user, $config, $where) {
	
	$stmt = new db_stmt_select("instances");
	$stmt->addColumn ("*");
	
	$stmt->setWhereClause($where);
	$stmt->setOrderBy("name");
	$s = $stmt->getStatement();
	$rs = $db->Execute($s);
	if (!$rs) {
		return "";
	}
	
	$rs->MoveFirst();
	$count=0;
	$res2 = "";
	while (!$rs->EOF) {
		$count++;
		
		$core_url = concatPath($rs->fields["solr_php_ws_root"] , "/core_" . $rs->fields["uuid"] . "/");
		//$url_search = "index.php?core=" . urlencode($core_url);
		//if ($search_php_config!="")
		//$url_search .= "&config=" . $search_php_config;
		//$key = base64_encode ($core_url . "|" . $config_file);
		$key = base64_encode ($core_url);
				
		$res2 .= "<tr>";
		$res2 .= "<td>";
		$res2 .= "<a href='index.php?key=" . $key ."' title='Search'>";
		$res2 .= $rs->fields["name"]; // . " (id=" . $rs->fields["id"] . ")";
		$res2 .= "</a>";
		$res2 .= "</td>";
	
		if ($user->getPlan()!="perso") {
			$res2 .= "<td width='20%'>";
			$res2 .= "<img src='images/" . getSolrTypeMnemo($rs->fields["type"]) . "_icone.png' class='img_icone'>&nbsp;";
			$res2 .= getSolrTypeLabel($rs->fields["type"]);
			$res2 .= "</td>";
		}
	
		$res2 .= "</tr>";
		$rs->MoveNext();
	}
	
	
	if ($count==0) {
		$res = "<br /><div style='text-align: center;'>no instance yet !</div>";
	}
	else {
		$res = "<center>Choose an instance<br /><table border='0' cellspacing='0' cellpadding='0'>";
		
		if ($user->getPlan()!="perso")
			$res .= "<tr><th>Name</th><th>Type</th></tr>";
		else
			$res .= "<tr><th>Name</th></tr>";

		$res .= $res2;
		$res .= "</table></center>";
	}

	return $res;
}


//
//
//
function getSolrTypeLabel($type)
{
	switch($type)
	{
		case '1':
			return "API";
		case '2':
			return "Web Crawler";
		case '3':
			return "Wordpress";
		case '4':
			return "Drupal";
		case '5':
			return "Joomla";
		case '6':
			return "eZ Publish";
		case '7':
			return "Typo3";
	}
}
function getSolrTypeMnemo($type)
{
	switch($type)
	{
		case '1':
			return "api";
		case '2':
			return "crawler";
		case '3':
			return "wp";
		case '4':
			return "drupal";
		case '5':
			return "joomla";
		case '6':
			return "ez";
		case '7':
			return "typo3";
	}
}
function getSolrTypeCode($mnemo)
{
	switch($mnemo)
	{
		case 'api':
			return "1";
		case 'crawler':
			return "2";
		case 'wp':
			return "3";
		case 'drupal':
			return "4";
		case 'joomla':
			return "5";
		case 'ez':
			return "6";
		case 'typo3':
			return "7";
	}
}

//
//
//
function concatPath($rootPath, $addPath) {

	$rootPath = trim($rootPath);
	$addPath = trim($addPath);

	$ret = $rootPath;

	if (substr($ret, strlen($ret)-1, 1) != "/") $ret .= "/";
	if (substr($addPath, 0, 1) == "/") $addPath = substr($addPath, 1);
	$ret .= $addPath;

	return $ret;
}

?>
</div>
</body>
</html>
