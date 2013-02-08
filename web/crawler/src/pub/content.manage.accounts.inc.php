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
var ajax_url = '<?php echo "ajax/content.manage.accounts.ajax.inc.php" ?>';

// onLoad
$(document).ready(function(){ 
    showAccountList();
});

// show user list
function showAccountList()
{
    //alert(start);
    $("#account_list").html("<img src='images/ajax-loader.gif'>");     
    $.get(ajax_url, {action: 'showaccountlist'}, 
    function(data) {
        //alert(data);
        $("#account_list").html(data);
    });    
}

function editAccount(id)
{
    //alert(id);
    $("#account_details").html("<img src='images/ajax-loader.gif'>"); 
    $.get(ajax_url, {action: 'displayaccount', id: id}, 
    function(data) {
        //alert(data);
        $("#account_details").html(data);
    });    
}

function displayAddAccount()
{
    $("#account_details").html("<img src='images/ajax-loader.gif'>"); 
    $.get(ajax_url, {action: 'display_add_account'}, 
    function(data) {
        //alert(data);
        $("#account_details").html(data);
    });    
}

function createAccount()
{
	//alert($("#account_name").val());
	var name = trim($("#account_name").val());

	if (name=="")
	{
		alert ("Account name cannot be empty !");
		return false;
	}
	
	$.ajax({
		  type: "post",
		  data: $("#account_add").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
			   	cancelAccount();
			   	showAccountList();
			   	}
			});
}

function saveAccount()
{
	//$("#action").val("saveuser");
	$.ajax({
		  type: "post",
		  data: $("#account_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
				cancelAccount();
				showAccountList();
				}
			});
}

//load sources
function cancelAccount()
{
    $("#account_details").html("");
}

function deleteAccount()
{
	var answer = confirm ("Do you really want to delete this account ?");
	if (!answer)
		return;

	$("#action").val("deleteaccount");
	$.ajax({
		  type: "post",
		  data: $("#account_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
			   	cancelAccount();
			   	showAccountList();
				}
			});
}

//-->
</script>  
    <div id="account_list">
    </div>
    <div id="account_details">
    </div>
