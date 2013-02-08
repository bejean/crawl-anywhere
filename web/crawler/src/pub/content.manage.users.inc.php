<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================

//require_once("../init.inc.php");
//$ajax_alias = $config->getDefault("crawler.ajaxalias", "/crawler_ajax")
?>

<script type="text/javascript">
<!--

//var ajax_url = '<?php echo $ajax_alias . "/content.users.ajax.inc.php" ?>';
var ajax_url = '<?php echo "ajax/content.manage.users.ajax.inc.php" ?>';

// onLoad
$(document).ready(function(){ 
<?php if ($user->getLevel() > 1) { ?>
    showUserList();
<?php } else { ?>
	editUser('<?php echo $user->getId(); ?>');
<?php } ?>
});

// show user list
function showUserList()
{
    //alert(start);
    $("#user_list").html("<img src='images/ajax-loader.gif'>");     
    $.get(ajax_url, {action: 'showuserlist'}, 
    function(data) {
        //alert(data);
        $("#user_list").html(data);
    });    
}

function editUser(id)
{
    //alert(id);
    $("#user_details").html("<img src='images/ajax-loader.gif'>"); 
    $.get(ajax_url, {action: 'displayuser', id: id}, 
    function(data) {
        //alert(data);
        $("#user_details").html(data);
    });    
}

function displayAddUser()
{
    $("#user_details").html("<img src='images/ajax-loader.gif'>"); 
    $.get(ajax_url, {action: 'display_add_user'}, 
    function(data) {
        //alert(data);
        $("#user_details").html(data);
    });    
}

function createUser()
{
	//alert($("#user_name").val());
	var name = trim($("#user_name").val());
	//alert("password");
	var password = trim($("#user_password").val());
	//alert("password2");
	var password2 = trim($("#user_password2").val());

	if (name=="" || password=="")
	{
		alert ("user name and password cannot be empty !");
		return false;
	}

	if (password!=password2)
	{
		alert ("password confirmation doesn't match password");
		return false;
	}
	
	$.ajax({
		  type: "post",
		  data: $("#user_add").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
			   	cancelUser();
			   	showUserList();
			   	}
			});
}

function saveUser()
{
	var password = trim($("#user_password").val());
	var password2 = trim($("#user_password2").val());

	if ((password!="") && (password!=password2))
	{
		alert ("password confirmation doesn't match password");
		return false;
	}

	//$("#action").val("saveuser");
	$.ajax({
		  type: "post",
		  data: $("#user_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
<?php if ($user->getLevel() > 1) { ?>
				cancelUser();
				showUserList();
<?php } else { ?>
				window.location.href = "index.php?page=status";
<?php } ?>
				}
			});
}

//load sources
function cancelUser()
{
<?php if ($user->getLevel() > 1) { ?>
    $("#user_details").html("");
<?php } else { ?>
	window.location.href = "index.php?page=status";
<?php } ?>
}

function deleteUser()
{
	var answer = confirm ("Do you really want to delete this user ?");
	if (!answer)
		return;

	$("#action").val("deleteuser");
	$.ajax({
		  type: "post",
		  data: $("#user_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
			   	cancelUser();
			   	showUserList();
				}
			});
}

function logonAs(user_name) {
	$("#user_name").val(user_name);
	$("#user_logon_as_form").submit();
	return true;
}

function userLevelOnChange() {
	var level = $("#user_level").val();
	if (level==2) {
		$("#user_belong_all").show();
		$("#user_belong").hide();
	} else {
		$("#user_belong_all").hide();
		$("#user_belong").show();
	}
	return true;
}

//-->
</script>  
    <div id="user_list">
    </div>
    <div id="user_details">
    </div>
    <div id="user_logon_as">
    <form id="user_logon_as_form" name="user_logon_as_form" action="login.php" method="post">
    <input type="hidden" id="action" name ="action" value="loginas" />
    <input type="hidden" id="user_name" name ="user_name" value="" />
    </form>
    </div>
    
    
