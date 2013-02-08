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
var ajax_url = '<?php echo "ajax/content.manage.engines.ajax.inc.php" ?>';

// onLoad
$(document).ready(function(){ 
    showEngineList();
});

// show user list
function showEngineList()
{
    //alert(start);
    $("#engine_list").html("<img src='images/ajax-loader.gif'>");     
    $.get(ajax_url, {action: 'showenginelist'}, 
    function(data) {
        //alert(data);
        $("#engine_list").html(data);
    });    
}

function editEngine(id)
{
    //alert(id);
    $("#engine_details").html("<img src='images/ajax-loader.gif'>"); 
    $.get(ajax_url, {action: 'displayengine', id: id}, 
    function(data) {
        //alert(data);
        $("#engine_details").html(data);
    });    
}

function displayAddEngine()
{
    $("#engine_details").html("<img src='images/ajax-loader.gif'>"); 
    $.get(ajax_url, {action: 'display_add_engine'}, 
    function(data) {
        //alert(data);
        $("#engine_details").html(data);
    });    
}

function createEngine()
{
	//alert($("#engine_name").val());
	var name = trim($("#engine_name").val());

	if (name=="")
	{
		alert ("Engine name cannot be empty !");
		return false;
	}
	
	$.ajax({
		  type: "post",
		  data: $("#engine_add").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
			   	cancelEngine();
			   	showEngineList();
			   	}
			});
}

function saveEngine()
{
	//$("#action").val("saveuser");
	$.ajax({
		  type: "post",
		  data: $("#engine_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
				cancelEngine();
				showEngineList();
				}
			});
}

//load sources
function cancelEngine()
{
    $("#engine_details").html("");
}

function deleteEngine()
{
	var answer = confirm ("Do you really want to delete this engine ?");
	if (!answer)
		return;

	$("#action").val("deleteengine");
	$.ajax({
		  type: "post",
		  data: $("#engine_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//$("#source_save_result").html(data);
			   	cancelEngine();
			   	showEngineList();
				}
			});
}

//-->
</script>  
    <div id="engine_list">
    </div>
    <div id="engine_details">
    </div>
