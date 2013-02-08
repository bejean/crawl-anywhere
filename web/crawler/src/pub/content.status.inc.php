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

var default_list_length = 10;
//var ajax_url = '<?php echo $ajax_alias . "/content.status.ajax.inc.php" ?>';
var ajax_url = '<?php echo "ajax/content.status.ajax.inc.php" ?>';

// onLoad
$(document).ready(function(){ 
    showStatus();
    showRunning();     
    //showInterrupted(default_list_length); 
    showEnqueued(default_list_length); 
    //showNext(default_list_length); 
});

// show status
function showStatus()
{
<?php
if ($user->getLevel()!=2) {
?>
	return;
<?php
} else {
?>
    //alert(start);
    $("#status").html("<img src='images/ajax-loader.gif'>");     
    $.get(ajax_url, {action: 'showstatus'}, 
    function(data) {
        //alert(data);
        $("#status").html(data);
    });    
<?php
}
?>
}

// show running crawls
function showRunning()
{
    //alert(start);
    $("#running_crawls").html("<img src='images/ajax-loader.gif'>");     
    $.get(ajax_url, {action: 'showrunning'}, 
    function(data) {
        //alert(data);
        $("#running_crawls").html(data);
    });    
}

// show interrupted crawls
function showInterrupted(limit)
{
    //alert(start);
    $("#interrupted_crawls").html("<img src='images/ajax-loader.gif'>");     
    $.get(ajax_url, {action: 'showinterrupted', limit: limit},  
    function(data) {
        //alert(data);
        $("#interrupted_crawls").html(data);
    });    
}

// show enqueued crawls
function showEnqueued(limit)
{
    //alert(start);
    $("#enqueued_crawls").html("<img src='images/ajax-loader.gif'>");     
    $.get(ajax_url, {action: 'showenqueued', limit: limit}, 
    function(data) {
        //alert(data);
        $("#enqueued_crawls").html(data);
    });    
}

// show next crawls
function showNext(limit)
{
    //alert(start);
    $("#next_crawls").html("<img src='images/ajax-loader.gif'>");     
    $.get(ajax_url, {action: 'shownext', limit: limit}, 
    function(data) {
        //alert(data);
        $("#next_crawls").html(data);
    });    
}

function resumeSource(id)
{
	var answer = confirm ("Do you really want to resume this source ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'resumesource', id: id}, function(data) {
        alert(data);;
    });
}

function pauseSource(id)
{
	var answer = confirm ("Do you really want to pause this source ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'pausesource', id: id}, function(data) {
        alert(data);;
    });
}

function stopSource(id)
{
	var answer = confirm ("Do you really want to stop this source (next crawl will restart from 0) ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'stopsource', id: id}, function(data) {
        alert(data);;
    });
}

//-->
</script>  
<div id="contenu">
<?php
if ($user->getLevel()==2) {
?>	
    <h2 id="title">Crawler status&nbsp;&nbsp;&nbsp;<a href="javascript:void(0)" onClick="showStatus();"><img src="images/refresh.png"></a></h2>
    <div id="status">
    </div>
<?php
}
?>	
    <h2>Running crawls&nbsp;&nbsp;&nbsp;<a href="javascript:void(0)" onClick="showRunning();"><img src="images/refresh.png"></a></h2>
    <div id="running_crawls">
    </div>
    <!-- 
    <h2>Interrupted crawls&nbsp;&nbsp;&nbsp;<a href="javascript:void(0)" onClick="showInterrupted(default_list_length);"><img src="images/refresh.png"></a></h2>
    <div id="interrupted_crawls">
    </div>
    -->
    <h2>Next crawls&nbsp;&nbsp;&nbsp;<a href="javascript:void(0)" onClick="showEnqueued(default_list_length);"><img src="images/refresh.png"></a></h2>
    <div id="enqueued_crawls">
    </div>
    <!-- 
    <h2>Next crawls&nbsp;&nbsp;&nbsp;<a href="javascript:void(0)" onClick="showNext(default_list_length);"><img src="images/refresh.png"></a></h2>
    <div id="next_crawls">
    </div>
    -->

</div>