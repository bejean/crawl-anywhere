<?php

//
// Disable magic quote
//
if (get_magic_quotes_gpc()) {
	function stripslashes_gpc(&$value) { $value = stripslashes($value); }
	array_walk_recursive($_GET, 'stripslashes_gpc');
	array_walk_recursive($_POST, 'stripslashes_gpc');
	array_walk_recursive($_COOKIE, 'stripslashes_gpc');
	array_walk_recursive($_REQUEST, 'stripslashes_gpc');
}

// /*
// This script should be included to emulate magic quotes on.
// If magic quotes are already on, we don't apply any changes to the variables.
// */

// //this will set all other input data (from databases etc) to have slashes.
// //set_magic_quotes_runtime(TRUE);

// function addSlashesArray($array) {
// 	foreach ($array as $key => $val) {
// 		if (is_array($val)) {
// 			$array[$key] = addSlashesArray($val);
// 		} else {
// 			$array[$key] = addslashes($val);
// 		}
// 	}
// 	return $array;
// }

// if(!function_exists("get_magic_quotes_gpc") || !get_magic_quotes_gpc()) {
// 	/*
// 	All these global variables are not slash-encoded by default,
// 	because magic_quotes_gpc is not set by default!
// 	(And magic_quotes_gpc affects more than just $_GET, $_POST, and $_COOKIE)
// 	*/
// 	$_SERVER = addSlashesArray($_SERVER);
// 	$_GET = addSlashesArray($_GET);
// 	$_POST = addSlashesArray($_POST);
// 	//     $_COOKIE = addSlashesArray($_COOKIE);
// 	//     $_FILES = addSlashesArray($_FILES);
// 	//     $_ENV = addSlashesArray($_ENV);
// 	//     $_REQUEST = addSlashesArray($_REQUEST);
// 	//     $HTTP_SERVER_VARS = addSlashesArray($HTTP_SERVER_VARS);
// 	//     $HTTP_GET_VARS = addSlashesArray($HTTP_GET_VARS);
// 	//     $HTTP_POST_VARS = addSlashesArray($HTTP_POST_VARS);
// 	//     $HTTP_COOKIE_VARS = addSlashesArray($HTTP_COOKIE_VARS);
// 	//     $HTTP_POST_FILES = addSlashesArray($HTTP_POST_FILES);
// 	//     $HTTP_ENV_VARS = addSlashesArray($HTTP_ENV_VARS);
// 	//     if (isset($_SESSION)) { #These are unconfirmed (?)
// 	//         $_SESSION = addSlashesArray($_SESSION, '');
// 	//         $HTTP_SESSION_VARS = addSlashesArray($HTTP_SESSION_VARS, '');
// 	//     }
// }

// /*
// The $GLOBALS array is also slash-encoded, but when all the above are
// changed, $GLOBALS is updated to reflect those changes.  (Therefore
// $GLOBALS should never be modified directly).  $GLOBALS also contains
// infinite recursion, so it's dangerous...
// */
?>