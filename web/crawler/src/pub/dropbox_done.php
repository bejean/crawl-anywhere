<?php
require_once("../init_gpc.inc.php");
require_once("../init.inc.php");
require_once("themes/theme.class.inc.php");
require_once("themes/" . $theme_name . "/theme_" . $theme_name . ".class.inc.php");
$theme = new Theme($config, $user, $id_account_current, $db);

$uid = POSTGET("uid");
$oauth_token = POSTGET("oauth_token");
$not_approved = POSTGET("not_approved");

echo $theme->generateHtmlStart();
?>

<script type="text/javascript">
<!--

// onLoad
$(document).ready(function(){ 
<?php 
if ($not_approved=='true') {
?>
	$( '#token_uid', window.opener.document ).val('not_approved');	
	$( '#status_dropbox_step1_status', window.opener.document).html("<img src='images/error_12.png'>&nbsp;<span class='help'>Access to Dropbox was denied !</span>");
<?php 
} else {
?>
	$( '#token_uid', window.opener.document ).val('<?php echo $uid; ?>');	
	$( '#dropbox_continue', window.opener.document ).removeAttr("disabled");
<?php 
}
?>

setTimeout(function(){
	window.close();
}, 3000);

});

//-->
</script>  

</head>
    <body>
        <div id="conteneur">
<?php 
if ($not_approved=='true') {
?>
	connection refused
<?php 
} else {
?>
	connection approved
<?php 
}
?>

<input type='button' id='close' value='Close' onClick='window.close();'>

        </div>
    </body>
</html>
