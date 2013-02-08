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

$mg = mg_connect ($config, "", "", "");

require_once("themes/theme.class.inc.php");
require_once("themes/" . $theme_name . "/theme_" . $theme_name . ".class.inc.php");
$theme = new Theme($config, $user, $id_account_current, $mg);

$page = "";
if (isset($_GET["page"])) 
	$page = $_GET["page"];

if ($user->getChangePassword()) {
	$page = "users";
	$user->setChangePassword(false);
}
                    
if ($page=="") {
	$page = $config->get("pages.default");
    if ($page=="" || $page=="undefined") {
    	$page = "status";
    }
}   

if ($page=="logout") {
	$user = new User();
	$_SESSION["user"] = $user;	
    header("Status: 301 Moved Permanently", false, 301);
	header("location:login.php");  
    exit();
}

if ($user->getName()=="")
{
    header("Status: 301 Moved Permanently", false, 301);
	header("location:login.php");  
    exit();
}	
	
echo $theme->generateHtmlStart();
?> 


<script type="text/javascript">
<!--
function accountOnChange() {
    document.account_selector.submit();
	return true;
}
//-->
</script>  

<?php      
echo "<!-- " . $config_file . "-->";
?> 
    </head>
    <body>
        <div id="conteneur">
<?php      
echo $theme->generateTop($page);                   
require_once("content." . $page . ".inc.php");
echo $theme->generateFooter();
?>  
        </div>
    </body>
</html>