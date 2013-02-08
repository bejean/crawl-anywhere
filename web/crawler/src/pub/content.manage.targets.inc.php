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

//var ajax_url = '<?php echo $ajax_alias . "/content.targets.ajax.inc.php" ?>';
var ajax_url = '<?php echo "ajax/content.manage.targets.ajax.inc.php" ?>';

// onLoad
$(document).ready(function(){ 
    showTargetList();
});

// show target list
function showTargetList()
{
    //alert(start);
    $("#target_list").html("<img src='images/ajax-loader.gif'>");     
    $.get(ajax_url, {action: 'showtargetlist'}, 
    function(data) {
        //alert(data);
        $("#target_list").html(data);
    });    
}

function edittarget(id)
{
    //alert(id);
    $("#target_details").html("<img src='images/ajax-loader.gif'>"); 
    $.get(ajax_url, {action: 'displaytarget', id: id}, 
    function(data) {
        //alert(data);
        $("#target_details").html(data);
    });    
}

function displayAddTarget()
{
    $("#target_details").html("<img src='images/ajax-loader.gif'>"); 
    $.get(ajax_url, {action: 'display_add_target'}, 
    function(data) {
        //alert(data);
        $("#target_details").html(data);
    });    
}

function createTarget()
{	
	/*
	var name = trim($("#target_name").val());
	var output_type = trim($("#output_type").val());
	var target_type = trim($("#target_type").val());
	var target_parameters = trim($("#target_parameters").val());
	*/
	
	$.ajax({
		  type: "post",
		  data: $("#target_add").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
			   	cancelTarget();
			   	showTargetList();
			   	}
			});
}

function saveTarget()
{
	$.ajax({
		  type: "post",
		  data: $("#target_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
<?php if ($user->getLevel() > 1) { ?>
				cancelTarget();
				showTargetList();
<?php } else { ?>
				window.location.href = "index.php?page=status";
<?php } ?>
				}
			});
}

//load sources
function cancelTarget()
{
<?php if ($user->getLevel() > 1) { ?>
    $("#target_details").html("");
<?php } else { ?>
	window.location.href = "index.php?page=status";
<?php } ?>
}

function deleteTarget()
{
	var answer = confirm ("Do you really want to delete this target ?");
	if (!answer)
		return;

	$("#action").val("deletetarget");
	$.ajax({
		  type: "post",
		  data: $("#target_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
			   	cancelTarget();
			   	showTargetList();
				}
			});
}

//-->
</script>  
    <div id="target_list">
    </div>
    <div id="target_details">
    </div>
