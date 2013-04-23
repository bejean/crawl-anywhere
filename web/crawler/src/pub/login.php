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

unset($_SESSION["user"]);
unset($_SESSION["id_account_current"]);

if (isset($_POST["user_name"])) $user_name = $_POST["user_name"];

if (isset($_POST["action"]))
{
	$action=$_POST["action"];

	if ($action=="login")
	{
		if (isset($_POST["param"])) {
			// 	var param = $.base64.encode(user + '|' + name + '|' + plugins + '|' + document.location);
			$param=explode("|",base64_decode($_POST["param"]));
			$username = trim($param[0]);
			$_SESSION["mysolrserver_instance_name"] = trim($param[1]);
			$_SESSION["mysolrserver_url"] = trim($param[3]);
		}
		else {
			if (isset($_POST["back_url"])) $_SESSION["mysolrserver_url"] = $_POST["back_url"];
			if (isset($_POST["instance_name"])) $_SESSION["mysolrserver_instance_name"] = $_POST["instance_name"];
			$username = $_POST["user_name"];
		}
		if ($_SESSION["mysolrserver_url"]) 
		{
			$login = $user->loginAs($config, $username);
		}
		else {
			$login = $user->login($config, $username, $_POST["user_password"]);
		}
		
		if ($login)
		{
			$_SESSION["user"] = $user;
			if ($user->getLevel()=="2") {
				$id_account_current = "1";
			} else {
				$id_account_current = $user->getIdAccount();
			}				
			header("Status: 301 Moved Permanently", false, 301);
			header("Location: index.php");
			exit();
		}
		else
		{
			$action_message = "Login failed !";
		}
	}

	if ($action=="loginas")
	{
		if ($user->loginAs($config, $_POST["user_name"]))
		{
			$_SESSION["user"] = $user;
			if ($user->getLevel()=="2") 
			{
				$id_account_current = "1";
			} else {
				$id_account_current = $user->getIdAccount();
			}
			if (isset($_POST["back_url"])) $_SESSION["mysolrserver_url"] = $_POST["back_url"];
			if (isset($_POST["instance_name"])) $_SESSION["mysolrserver_instance_name"] = $_POST["instance_name"];

			header("Status: 301 Moved Permanently", false, 301);
			header("Location: index.php");
			exit();
		}
		else
		{
			$action_message = "Login failed !";
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

<script language="javascript">

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
<div id="conteneur">
<div id="header">
<h1 id="header"><?php print($config->get("application.title")); ?> -
Login</h1>
</div>
<br />
<br />
<br />
<br />
<div id="login">
<?php
if ($action=="login" && $action_message!="")
{
?>
<table align='center'>
	<tr>
		<td><?php echo $action_message; ?></td>
	</tr>
</table>
<br />
<br />
<?php
}
if (empty($action)) {
	$mg = mg_connect ($config, "", "", "");
	if ($mg==null && mg_get_last_error()!='') {
?>
<table align='center'>
	<tr>
		<td><?php echo "Database error : " . mg_get_last_error(); ?></td>
	</tr>
</table>
<br />
<br />
<?php
	}
}
?>
<form name='login' action='' method='post'>
<input type="hidden" name="action" value="login">
<table align='center' border='0'>
	<tr>
		<td class="head">User name</td>
		<td><input id='user_name' name='user_name'
			value='<?php echo $user_name ?>' onKeyPress='checkEnter(event);'></td>
	</tr>
	<tr>
		<td class="head">Password</td>
		<td><input type='password' id='user_password' name='user_password'
			value='' onKeyPress='checkEnter(event);'></td>
	</tr>
	<tr>
		<td colspan="2"><input type="submit" value="Login"
			onClick="doLogin();";></td>
	</tr>
</table>

</form>

</div>
<p id="footer"></p>
</div>
</body>
</html>
