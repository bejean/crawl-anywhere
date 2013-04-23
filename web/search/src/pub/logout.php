<?php
$rootpath = dirname(__FILE__);
require_once("../init.inc.php");

$login = ($config->getDefault("login", "0")=="1");
if ($login && $user->getName()!="") {
	unset($_SESSION["user"]);
}
header('Location: index.php');
?>