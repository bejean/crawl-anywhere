function startCreation() {
    $("#sources").hide();
    $("#source_details").show();
    $('#opt_switch').hide();  
    $('#opt_main').hide();  
    $('#opt_advanced').hide();  
    $('#opt_status').hide();  
    $('#opt_button').hide();  
    $('#opt_connect').show();  
    $("#dropbox_continue").attr("disabled", "disabled");
	return true;
}


function checkSource() {

	var name = trim($("#source_name").val());
	var tag = trim($("#source_tag").val());
	
	if (name=="") {
		alert ("Source name and root directory cannot be empty !");
		return false;
	}

	if (tag.indexOf("_")!=-1) {
		alert ("The character \"_\" is not allowed in tag value");
		return false;
	}
	
	var token_key = trim($("#token_key").val());
	var token_secret = trim($("#token_secret").val());
	
	if (token_key=='' || token_secret=='') {
		alert ("Error in Dropbox configuration !");
		/*
		alert ("Error in Dropbox configuration. Retry Dropbox access request !");
		$("#status_token_access_ok").hide();
		$("#status_token_access_error").show();
		$("#status_dropbox_step1_status").html("<img src='images/error_12.png'>");
		$("#status_dropbox_step3_status").html("<img src='images/error_12.png'>");
		*/
		return false;		
	}

	if (!json2XmlRule()) return false;
	//json2XmlSchedule();

	return true;
}

function requestDropBoxAccess() {

	var ajax_url = 'sources/dropbox/source_dropbox.ajax.inc.php';

	$("#status_dropbox_step1_status").html("<img src='images/ajax-loader-small.gif'>");
	$("#dropbox_continue").attr("disabled","disabled");

	$.get(ajax_url, {action: 'dropboxlinkstep1'}, function(data) {
	        var objJson = $.parseJSON(data);
	        if (objJson.status=='success') {
	        	$("#dropboxurl").val(objJson.info_url);
	        	$("#dropboxtimestamp").val(objJson.timestamp);
	        	$("#status_dropbox_step1_status").html("<img src='images/ok_12.png'>&nbsp;<span class='help'>The Dropbox <strong>API Request Authorization</strong> page was opened. Click the <strong>Allow</strong> button !</span>");
	        	window.open($("#dropboxurl").val(), 'dropbox', 'width=1000,height=1000');
	        }
	        else {
	        	$("#dropboxurl").val('');
	        	$("#dropboxtimestamp").val("");
        		$("#status_dropbox_step1_status").html("<img src='images/error_12.png'>&nbsp;<span class='help'>Error while requesting Dropbox access !</span>");
	        }
	    });
}

function generateDropBoxAccess() {
	var url = $("#dropboxurl").val();
	if (url!="") {
		window.open($("#dropboxurl").val(), 'dropbox');
		$("#status_dropbox_step2_status").html("<img src='images/ok_12.png'>");
	}
	else {
		$("#status_dropbox_step2_status").html("<img src='images/error_12.png'>");
	}
}

function requestDropBoxAccessToken() {

	var ajax_url = 'sources/dropbox/source_dropbox.ajax.inc.php';
 	var dropboxtimestamp = trim($("#dropboxtimestamp").val());

 	$("#token_uid").val('');

	$("#status_dropbox_step3_status").html("<img src='images/ajax-loader-small.gif'>");
	$.get(ajax_url, {action: 'dropboxlinkstep2', dropboxtimestamp: dropboxtimestamp}, function(data) {
	        var objJson = $.parseJSON(data);
	        if (objJson.status=='success') {
	        	$("#token_key").val(objJson.token_key);
	        	$("#token_secret").val(objJson.token_secret);
	        	
	        	$("#status_token_access_ok").show();
	    		$("#status_token_access_error").hide();
	    		
	    		$("#status_dropbox_step3_status").html("<img src='images/ok_12.png'>");
	        }
	        else {
	        	$("#dropboxurl").val('');

	        	$("#status_token_access_ok").hide();
	    		$("#status_token_access_error").show();

	    		$("#status_dropbox_step3_status").html("<img src='images/error_12.png'>");
	        }
	    });
}

function requestDropBoxAccessToken2() {

	if ($("#token_uid").val()=='') {
		alert ('Access not yet validated or refused');
		return false;
	}
	
	if ($("#token_uid").val()=='not_approved') {
		alert ('Access refused');
		cancelSource();
		return false;
	}
	
	var ajax_url = 'sources/dropbox/source_dropbox.ajax.inc.php';
 	var dropboxtimestamp = trim($("#dropboxtimestamp").val());

	$("#status_dropbox_step3_status").html("<img src='images/ajax-loader-small.gif'>");
	$.get(ajax_url, {action: 'dropboxlinkstep2', dropboxtimestamp: dropboxtimestamp}, function(data) {
	        var objJson = $.parseJSON(data);
	        if (objJson.status=='success') {
	        	$("#token_key").val(objJson.token_key);
	        	$("#token_secret").val(objJson.token_secret);
	        	
        	    $('#opt_connect').hide();  
				$('#opt_main').show();  
				$('#opt_button').show();  
	        	    
	        	//$("#status_token_access_ok").show();
	    		//$("#status_token_access_error").hide();
	    		
	    		//$("#status_dropbox_step3_status").html("<img src='images/ok_12.png'>");
	        }
	        else {
	    		alert ('Access refused');
	    		cancelSource();
	        }
	    });
}