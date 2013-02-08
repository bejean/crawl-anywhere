<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
require_once("../init_gpc.inc.php");
require_once("../init.inc.php");

function drawSubMenuItem($page, $label) {
	print('&nbsp;&nbsp;[<a href="index.php?page=manage&subpage=' . $page . '">' . $label . '</a>]&nbsp;&nbsp;');
}

if ($user->getLevel() > 1) {
	$subpage = "accounts";
	if (isset($_GET["subpage"]))
	$subpage = $_GET["subpage"];
	?>
<div id="manage_menu"><?php 
drawSubMenuItem("accounts", "Accounts");
drawSubMenuItem("users", "Users");
drawSubMenuItem("targets", "Targets");
drawSubMenuItem("engines", "Engines");
drawSubMenuItem("settings", "Settings");
?></div>
<?php
} else {
	$subpage = "users";
	?>
<!-- div id="manage_menu">[My account]</div -->
	<?php
}
require_once("content.manage." . $subpage . ".inc.php");
?>
