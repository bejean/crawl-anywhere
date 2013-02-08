<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================
?>

<script type="text/javascript">
<!--

var ajax_url = '<?php echo "ajax/content.sources.ajax.inc.php" ?>';

// onLoad
$(document).ready(function(){ 
    $("#source_details").hide();
    $("#source_type_select").hide();
    loadSources('', 1);  

});


function showOptionGroup(group) {
    $('#opt_main').hide();  
    $('#opt_advanced').hide();  
    $('#opt_status').hide();  
    if (group=='main') $('#opt_main').show();  
    if (group=='advanced') $('#opt_advanced').show();  
    if (group=='status') $('#opt_status').show();  
}

// load sources
function loadSources(start, page)
{
    //alert(page);
    var inerror = $('#chk_inerror').attr('checked')?1:0;
    var suspicious = $('#chk_suspicious').attr('checked')?1:0;
    var nocountry = $('#chk_nocountry').attr('checked')?1:0;
    var nolanguage = $('#chk_nolanguage').attr('checked')?1:0;
    var onepage = $('#chk_onepage').attr('checked')?1:0;
    //var disabled = $('#chk_disabled').attr('checked')?1:0;
    var deleted = $('#chk_deleted').attr('checked')?1:0;
    var filter_status = $('#filter_status').val();
    var filter_type = $('#filter_type').val();
    var filter_tag = $('#filter_tag').val();
    var filter_collection = $('#filter_collection').val();
    var filter_country = $('#filter_country').val();
    var filter_language = $('#filter_language').val();
    var filter_target = $('#filter_target').val();

    $("#sources").html("<img src='images/ajax-loader.gif'>"); 
    $.get(ajax_url, {action: 'loadsources', start: start, page: page, suspicious: suspicious, inerror: inerror, nocountry: nocountry, nolanguage: nolanguage, onepage: onepage, deleted: deleted, filter_type: filter_type, filter_status: filter_status, filter_tag: filter_tag, filter_collection: filter_collection, filter_country: filter_country, filter_language: filter_language, filter_target: filter_target}, 
    function(data) {
        //alert(data);
        $("#sources").html(data);
    });    
}

// load sources
/*
function displaySource(id)
{
    $("#source_details").html("<img src='images/ajax-loader.gif'>"); 
    $.get('content.sources.ajax.inc.php', {action: 'displaysource', id: id}, 
    function(data) {
        //alert(data);
        $("#source_details").html(data);
    });    
}
*/

// load sources
function cancelSource()
{
    $("#source_details").hide();
    $("#sources").show();
}

function checkParameter(id) {
	var value = trim($("#"+id).val().trim());
	if (value=='') {
		$("#status_"+id+"_error").show();
		$("#status_"+id+"_ok").hide();
	} else {
		$("#status_"+id+"_error").hide();
		$("#status_"+id+"_ok").show();
	}
}

function editSource(id, type_mnemo)
{
    //alert(id);
    $("#source_details").html("<img src='images/ajax-loader.gif'>"); 
    replacejsfile(type_mnemo);

    update_add_rule_form(type_mnemo);
       
    $.get(ajax_url, {action: 'displaysource', id: id}, 
    function(data) {
        //alert(data);
        $("#sources").hide();
        $("#source_details").html(data);
        urlDisplay();
        ruleDisplay();            
        scheduleDisplay();
        $("#source_details").show();        
    });    
}

function indexnow(id)
{
	var answer = confirm ("Do you really want to crawl this source now ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'indexnow', id: id}, function(data) {
        alert(data);;
    });
}

function resetSource(id)
{
	var answer = confirm ("Do you really want to reset this source ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'resetsource', id: id}, function(data) {
        alert(data);;
    });
}

function clearSource(id)
{
	var answer = confirm ("Do you really want to clear this source ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'clearsource', id: id}, function(data) {
        alert(data);;
    });
}

function cleanSource(id)
{
	var answer = confirm ("Do you really want to clean this source ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'cleansource', id: id}, function(data) {
        alert(data);;
    });
}

function resetCacheSource(id)
{
	var answer = confirm ("Do you really want to reset this source ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'resetcachesource', id: id}, function(data) {
        alert(data);;
    });
}

function rescanSource(id)
{
	var answer = confirm ("Do you really want to rescan this source ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'rescansource', id: id}, function(data) {
        alert(data);;
    });
}

function deeperSource(id)
{
	var answer = confirm ("Do you really want to recrawl deeper this source ?");
	if (!answer)
		return;
    $.get(ajax_url, {action: 'deepersource', id: id}, function(data) {
        alert(data);;
    });
}

function displayAddSource(type_mnemo)
{
    $("#source_details").html("<img src='images/ajax-loader.gif'>"); 
    replacejsfile(type_mnemo);
    
    $.get(ajax_url, {action: 'display_add_source', type: type_mnemo}, 
    function(data) {
        //alert(data);
    	$("#source_details").html(data);
        startCreation();
    });    
    
}

function updateSource()
{
	saveSource("updatesource");
	loadSources('', 1);
}

function createSource()
{
	saveSource("createsource");
	loadSources('', 1);
}

function saveSource(mode)
{
	if (!checkSource()) return;
	
	$("#action").val(mode);
	$.ajax({
		  type: "post",
		  data: $("#source_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				$("#source_save_result").html(data);
			   	cancelSource();
			   	loadSources('', 1);
			   	}
			});
}

function deleteSource()
{
	var answer = confirm ("Do you really want to delete this source ?");
	if (!answer)
		return;

	$("#action").val("deletesource");
	$.ajax({
		  type: "post",
		  data: $("#source_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				$("#source_save_result").html(data);
				cancelSource();
			   	loadSources('', 1);
				}
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


function showLanguageRestrictionList()
{
	if ($("#source_language").val()=="xx")
		$("#language_detection_list").show();
	else
		$("#language_detection_list").hide();
}


function testFilteringRules()
{
	json2XmlRule();
	var rules = trim($("#source_crawl_filtering_rules").val());
	var url = trim($("#test_url").val());

	if (rules=="" || url=="")
	{
		alert ("Rules and test url are requiered for test !");
		return false;
	}

	$("#action").val("testfilteringrules");
	$.ajax({
		  type: "post",
		  data: $("#source_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
					$("#filtering_rules_test_result").html(data);
				}
			});
}

function testAuthentication()
{
	var authMode = trim($("#auth_mode").val());
	var authLogin = trim($("#auth_login").val());
	var authPasswd = trim($("#auth_passwd").val());
	var authParam = trim($("#auth_param").val());
	var page = trim($("#source_url").val());

	if (authMode=="" || authLogin=="" || authPasswd=="" || authParam=="" || page=="")
	{
		alert ("Authentication informations and source url are requiered for test !");
		return false;
	}
	
	$("#action").val("testauthentication");
	$.ajax({
		  type: "post",
		  data: $("#source_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {
				//window.open(data,'test');
				if (data!="") window.open('display.php?file='+data,'test');
				}
			});
}

function testUrlCleaning()
{
	var url = trim($("#test_url_cleaning").val());
	var method = trim($("#source_automatic_cleaning").val());

	if (url=="")
	{
		alert ("An url have to be provided !");
		return false;
	}
	
	$("#action").val("testcleaning");
	$.ajax({
		  type: "post",
		  data: $("#source_edit").serialize(), // assuming this == the form
		  url: ajax_url,
		  success: function(data) {

					var offset = data.indexOf (' ');
					if (offset != -1) {
						var code = data.substring(0,offset);
						if (code.substring(0,4) == "ret=") {
							var msg = data.substring(offset+1);
							alert (msg);	
							return;
						}
					}
			  		if (data=='') {
				  		alert('The html cleaning returned an empty page. Do not choose this cleaning method !');
			  			return;
			  		}
			  		if (data=='404') {
				  		alert('The html cleaning web service was not found !');
				  		return;
			  		}
					window.open('display.php?file='+data,'test');
				}
			});
}

function addTag(tag) {
	if (trim($("#source_tag").val())=="")
		$("#source_tag").val(tag);
	else
		$("#source_tag").val($("#source_tag").val() + ", " + tag);
}

function addCollection(collection) {
	if (trim($("#source_collection").val())=="")
		$("#source_collection").val(collection);
	else
		$("#source_collection").val($("#source_collection").val() + ", " + collection);
}

// Select source type dialog ======================
function showSelectSourceType(maxcout_reached) {
	if (maxcout_reached) {
		alert ("You have reached the maximum number of sources !");
		return false;
	}
    $("#sources").hide();
    $("#source_type_select").show();        
/*	
    //alert(id);
    $("#source_type_select").html("<img src='images/ajax-loader.gif'>"); 
   
    $.get(ajax_url, {action: 'displaysourcetype'}, 
    function(data) {
        //alert(data);
        $("#sources").hide();
        $("#source_type_select").html(data);
        $("#source_type_select").show();        
    });    
  */
}

function cancelSelectSourceType()
{
    $("#source_type_select").hide();
    $("#sources").show();
}


function selectSourceCreate (type) {
    $.unblockUI(); 
    //displayAddSource($("#export-dialog-form-type").val());
    
    update_add_rule_form(type);
    
    displayAddSource(type);
    $("#source_type_select").hide();
    return false; 
}

function selectSourceCancel () {
    $.unblockUI(); 
    return false; 
}

function selectSourceShowDialog () {
	$.blockUI({ 
			message: $('#select-source-dialog-form'), 
			overlayCSS:  { 
        		backgroundColor: '#000', 
        		opacity:         0.2 
    		}, 
		    css: { 
		        width:          '400px', 
		        top:            '30%', 
		        left:           '20%',
		        margin:   		'auto',
		        cursor:         'wait' 
		    }
	}); 
}

function update_add_rule_form(type) {
	if (type=='web') {
		str  = "<option value='all' selected>Get page and extract links</option>";
		str += "<option value='links'>Extract links only</option>";
		str += "<option value='get'>Get page only</option>";
		str += "<option value='skip'>Ignore</option>";
		$('#add-rule-form-meta-tbody').show();
		$('#add-rule-form-ignoreparam-tbody').show();
	}
	else {
		str  = "<option value='all' selected>Index</option>";
		str += "<option value='skip'>Ignore</option>";
		$('#add-rule-form-meta-tbody').hide();
		$('#add-rule-form-ignoreparam-tbody').hide();
	}	
	$('#add-rule-form-mode').html(str);
}



// Url dialog =====================================
var currentUrl = -1;

function addOneUrl(url, mode, allowotherdomain, onlyfirstcrawl, show_error_msg) {

	if (trim(url) == "") return;
	
	var source_url = jQuery.trim($("#source_url").val());
	if (source_url == "") {
		source_url = '{ "urls": [] }';
	}
    
    var objJson = $.parseJSON(source_url);

    // already a starting url ?
    if (currentUrl==-1) {
	    for ( var i = 0; i < objJson.urls.length ; i++ ) {
	    	if (objJson.urls[i].url == url) {
	        	if (show_error_msg) alert ("This starting url already exists !");
	           	return;
	    	}
		}
    }

	if (mode=="s")
		allowotherdomain = 0;

    var itemJsonStr = '{ "url": "' + url + '", "mode": "' + mode + '", "allowotherdomain": "' + allowotherdomain + '", "onlyfirstcrawl": "' + onlyfirstcrawl + '" }';
	if (currentUrl==-1) {
	    var itemJson =  $.parseJSON(itemJsonStr);
    	objJson.urls.push( itemJson );
        $("#source_url").val(JSON.stringify(objJson, null, 2));
	} else {
		var urlJson = '{ "urls": [';
		var sep = "";	
		for ( var i = 0; i < objJson.urls.length ; i++ ) {
			if (currentUrl==i) {
				urlJson += sep + itemJsonStr;
			}
			else {
				urlJson += sep + '{ "url": "' + objJson.urls[i].url + '", "mode": "' + objJson.urls[i].mode + '", "allowotherdomain": "' + objJson.urls[i].allowotherdomain + '", "onlyfirstcrawl": "' + objJson.urls[i].onlyfirstcrawl + '" }';
			}
			sep = ",";
		}
		urlJson += '] }';		
	    $("#source_url").val(urlJson);
	}
}

function addUrlYes () {

    var url = $("#add-url-form-url").val().trim();

    var RegexUrl = /^(ftp|http|https):\/\//;
    if (!RegexUrl.test(url)) {
		alert ("URL invalid. Please provide protocol (http://, https:// or ftp://) !");
		return false;
    }

    var mode = $("#add-url-form-mode").val();
    var allowotherdomain = $("#add-url-form-allowsotherdomains").val();
    var onlyfirstcrawl = $("#add-url-form-onlyfirstcrawl").val();

    $.unblockUI(); 

    addOneUrl(url, mode, allowotherdomain, onlyfirstcrawl, true);
    
    urlDisplay();
    return false; 
}

function addUrlNo () {
    $.unblockUI(); 
    return false; 
}

function addUrlShowDialog (url, mode, allowsotherdomains, onlyfirstcrawl, ndx) {
//function addUrlShowDialog (url, home, mode, allowsotherdomains, onlyfirstcrawl, ndx) {
	currentUrl = ndx;
	$("#add-url-form-url").val(url);
	//$("#add-url-form-home").val(home);
	$("#add-url-form-mode").val(mode);
	$("#add-url-form-allowsotherdomains").val(allowsotherdomains);
	$("#add-url-form-onlyfirstcrawl").val(onlyfirstcrawl);
	$.blockUI({ 
			message: $('#add-url-form'), 
			overlayCSS:  { 
        		backgroundColor: '#000', 
        		opacity:         0.2 
    		}, 
		    css: { 
		        width:          '800px', 
		        top:            '30%', 
		        left:           '20%',
		        margin:   		'auto',
		        cursor:         'wait' 
		    }
	}); 
}

function urlDisplay() {
	var source_url = jQuery.trim($("#source_url").val());
	if (source_url == "") {
		source_url = '{ "urls": [] }';
	}

	if (source_url == '{ "urls": [] }') {
		$("#status_source_url_error").show();
		$("#status_source_url_ok").hide();	
	} else {
		$("#status_source_url_error").hide();
		$("#status_source_url_ok").show();
	}

	
	var objJson = $.parseJSON(source_url);
	var html = '<table>';
	for ( var i = 0; i < objJson.urls.length ; i++ ) {
		if (objJson.urls[i].url != "") {
			html += "<tr id='url_" + i + "'><td><table class='nob'><tr class='nob'><td colspan='4' class='nob'><a href='" + objJson.urls[i].url + "' target='_blank'>" + objJson.urls[i].url + "</a></td></tr><tr class='nob'>";
			html += "<td width='32%'>Mode : ";
			if ( objJson.urls[i].mode == "s")
				html += "Web site";
			if ( objJson.urls[i].mode == "l")
				html += "Links page";
			if ( objJson.urls[i].mode == "r")
				html += "RSS feed";
			if ( objJson.urls[i].mode == "m")
				html += "Sitemaps";
			html += "</td>";
			
			html += "<td width='26%'>Only first crawl : ";
			if ( objJson.urls[i].onlyfirstcrawl == "0")
				html += "No";
			else
				html += "Yes";
			html += "</td>";
	
			html += "<td width='32%'>Allow other domains : ";
			if ( objJson.urls[i].allowotherdomain == "0")
				html += "No";
			else
				html += "Yes";
			html += "</td>";
	
			html += "<td width='10%'><a href='javascript:editUrl(" + i + ")'><img src='images/button_edit.png'></a>&nbsp;<a href='javascript:delUrl(" + i + ")'><img src='images/trash_16.png'></a></td></tr></table></td>";
			html += "</tr>";
		}
	}
	html += "</table>";
	$("#url").html(html);
}


function addUrl() {
	addUrlShowDialog("","s","0","0",-1);
}

function delUrl(ndx) {
	var answer = confirm ("Do you really want to delete this url ?");
	if (!answer)
		return;

	var objJson = $.parseJSON($("#source_url").val());
	var urlJson = '{ "urls": [';
	var sep = "";	
	for ( var i = 0; i < objJson.urls.length ; i++ ) {
		if (ndx!=i) {
			urlJson += sep + '{ "url": "' + objJson.urls[i].url + '", "mode": "' + objJson.urls[i].mode + '", "allowotherdomain": "' + objJson.urls[i].allowotherdomain + '", "onlyfirstcrawl": "' + objJson.urls[i].onlyfirstcrawl + '" }';
			sep = ",";
		}
	}
	urlJson += '] }';		
	$("#source_url").val(urlJson);	
    urlDisplay();
}

function editUrl(ndx) {
	var objJson = $.parseJSON($("#source_url").val());
	addUrlShowDialog (objJson.urls[ndx].url, objJson.urls[ndx].mode, objJson.urls[ndx].allowotherdomain, objJson.urls[ndx].onlyfirstcrawl, ndx);
    //urlDisplay();
}

function json2XmlUrl() {
	$("#source_url_xml").val("");	

	var jsonStr = $("#source_url").val();
	if (jsonStr=="") return;
	
	var objJson = $.parseJSON($("#source_url").val());
	var urlXml = '<urls>';
	for ( var i = 0; i < objJson.urls.length ; i++ ) {
		if (objJson.urls[i].url != "") {
			urlXml += '<url><url>' + escapeHTML(objJson.urls[i].url) + '</url><mode>' + objJson.urls[i].mode + '</mode><allowotherdomain>' + objJson.urls[i].allowotherdomain + '</allowotherdomain><onlyfirstcrawl>' + objJson.urls[i].onlyfirstcrawl + '</onlyfirstcrawl></url>';
		}
	}
	urlXml += '</urls>';		
	$("#source_url_xml").val(urlXml);	
	return true;
}

// Rule dialog =====================================
var currentRule = -1;

function addRuleYes () {
    $.unblockUI(); 

	var source_crawl_filtering_rules = jQuery.trim($("#source_crawl_filtering_rules").val());
	if (source_crawl_filtering_rules == "") {
		source_crawl_filtering_rules = '{ "rules": [] }';
	}
    
	try {
		var objJson = $.parseJSON(source_crawl_filtering_rules);
	}
	catch(err) {
		alert (err);
		return
	}
	var ope = $("#add-rule-form-ope").val();
	var mode = $("#add-rule-form-mode").val();
	var pat = $("#add-rule-form-pat").val();
	var meta = $("#add-rule-form-meta").val();
	var ignoreparam = $("#add-rule-form-ignoreparam").val();
	//var metap = $("#add-rule-form-meta-propagate").val();
	var metap = 0;
	
    var itemJsonStr = '{ "ope": "' + ope + '", "mode": "' + mode + '", "pat": "' + pat.replace(/\\/g,"\\\\") + '", "meta": "' + meta.replace(/\\/g,"\\\\").replace(/\n/g,'|') + '", "metap": "' + metap + '", "ignoreparam": "' + ignoreparam + '" }';
	if (currentRule==-1) {
	    var itemJson =  $.parseJSON(itemJsonStr);
    	objJson.rules.push( itemJson );
        $("#source_crawl_filtering_rules").val(JSON.stringify(objJson, null, 2));
	} else {
		var ruleJson = '{ "rules": [';
		var sep = "";	
		for ( var i = 0; i < objJson.rules.length ; i++ ) {
			if (currentRule==i) {
				ruleJson += sep + itemJsonStr;
			}
			else {
				ruleJson += sep + '{ "ope": "' + objJson.rules[i].ope + '", "mode": "' + objJson.rules[i].mode + '", "pat": "' + objJson.rules[i].pat.replace(/\\/g,"\\\\") + '", "meta": "' + objJson.rules[i].meta.replace(/\\/g,"\\\\") + '", "metap": "' + objJson.rules[i].metap + '", "ignoreparam": "' + ignoreparam + '" }';
			}
			sep = ",";
		}
		ruleJson += '] }';		
	    $("#source_crawl_filtering_rules").val(ruleJson);
	}
    ruleDisplay();
    return false; 
}

function addRuleNo () {
    $.unblockUI(); 
    return false; 
}

function addRuleShowDialog (ope, mode, pat, meta, metap, ignoreparam, ndx, src_type) {
	currentRule = ndx;
	$("#add-rule-form-ope").val(ope);
	$("#add-rule-form-mode").val(mode);
	$("#add-rule-form-pat").val(pat);
	$("#add-rule-form-meta").val(meta.replace(/\|/g,'\n'));
	$("#add-rule-form-ignoreparam").val(ignoreparam);
	//$("#add-rule-form-meta-propagate").val(metap);
	$.blockUI({ 
			message: $('#add-rule-form'), 
			overlayCSS:  { 
        		backgroundColor: '#000', 
        		opacity:         0.2 
    		}, 
		    css: { 
		        width:          '800px', 
		        top:            '30%', 
		        left:           '20%',
		        margin:   		'auto',
		        cursor:         'wait' 
		    }
	}); 
}

function ruleDisplay() {
	var source_crawl_filtering_rules = jQuery.trim($("#source_crawl_filtering_rules").val());
	if (source_crawl_filtering_rules == "") {
		source_crawl_filtering_rules = '{ "rules": [] }';
	}
	
	try {
		var objJson = $.parseJSON(source_crawl_filtering_rules);
	}
	catch(err) {
		alert (err);
		return
	}
	var html = '<table id="add-rule-form-table"><tbody>';
	for ( var i = 0; i < objJson.rules.length ; i++ ) {
		if (objJson.rules[i].pat != "") {
			html += "<tr id='rule_" + i + "'><td width='10%'>" + objJson.rules[i].ope + "</td>";
			html += "<td width='50%'>" + escapeHTML (objJson.rules[i].pat) + "</td>";
			html += "<td width='27%'>";
			if (objJson.rules[i].mode=="all")
				html += "Get page and extract links";
			if (objJson.rules[i].mode=="links")
				html += "Extract links only";
			if (objJson.rules[i].mode=="skip")
				html += "Ignore";
			html += "</td>";
			html += "<td width='13%'>";
			html += "<a href='javascript:editRule(" + i + ")'><img src='images/button_edit.png'></a>&nbsp;";
			html += "<a href='javascript:delRule(" + i + ")'><img src='images/trash_16.png'></a>";
			html += "<a href='javascript:moveRule(" + i + ",-1)'><img src='images/up_16.png'></a>";
			html += "<a href='javascript:moveRule(" + i + ",1)'><img src='images/down_16.png'></a>";
			html += "</td></tr>";
		}
	}
	html += "</tbody></table>";
//	html += "<a href='javascript:startReorderRule();'><img src='images/reorder_12.png'>&nbsp;Reorder</a>";
	
	$("#rule").html(html);
}

function addRule() {
	addRuleShowDialog("match","a","","","", "", -1);
}

function delRule(ndx) {
	var answer = confirm ("Do you really want to delete this rule ?");
	if (!answer)
		return;

	try {
		var objJson = $.parseJSON($("#source_crawl_filtering_rules").val());
	}
	catch(err) {
		alert (err);
		return
	}
	var ruleJson = '{ "rules": [';
	var sep = "";	
	for ( var i = 0; i < objJson.rules.length ; i++ ) {
		if (ndx!=i) {
			ruleJson += sep + '{ "ope": "' + objJson.rules[i].ope + '", "mode": "' + objJson.rules[i].mode + '", "pat": "' + objJson.rules[i].pat.replace(/\\/g,"\\\\") + '", "meta": "' + objJson.rules[i].meta.replace(/\\/g,"\\\\") + '", "metap": "' + objJson.rules[i].metap + '", "ignoreparam": "' + objJson.rules[i].ignoreparam + '" }';
			sep = ",";
		}
	}
	ruleJson += '] }';		
	$("#source_crawl_filtering_rules").val(ruleJson);	
    ruleDisplay();
}

function editRule(ndx, src_type) {
	try {
		var objJson = $.parseJSON($("#source_crawl_filtering_rules").val());
	}
	catch(err) {
		alert (err);
		return
	}
	addRuleShowDialog (objJson.rules[ndx].ope, objJson.rules[ndx].mode, objJson.rules[ndx].pat, objJson.rules[ndx].meta, objJson.rules[ndx].metap, objJson.rules[ndx].ignoreparam, ndx);
    ruleDisplay();
}

function moveRule(ndx, offset) {
	//alert($("#source_crawl_filtering_rules").val());
	try {
		var objJson = $.parseJSON($("#source_crawl_filtering_rules").val());
	}
	catch(err) {
		alert (err);
		return
	}
	if (ndx==0 && offset==-1) return;
	if (ndx==objJson.rules.length-1 && offset==1) return;

	var ruleJson = '{ "rules": [';
	var sep = "";	
	for ( var i = 0; i < objJson.rules.length ; i++ ) {
		if (i!=ndx) {
			if (offset==1 || offset==-1) {
				ruleJson += sep + '{ "ope": "' + objJson.rules[i].ope + '", "mode": "' + objJson.rules[i].mode + '", "pat": "' + objJson.rules[i].pat.replace(/\\/g,"\\\\") + '", "meta": "' + objJson.rules[i].meta.replace(/\\/g,"\\\\") + '", "metap": "' + objJson.rules[i].metap + '", "ignoreparam": "' + objJson.rules[i].ignoreparam + '" }';
				sep = ",";
			}
			if (i==ndx+offset) {
				ruleJson += sep + '{ "ope": "' + objJson.rules[ndx].ope + '", "mode": "' + objJson.rules[ndx].mode + '", "pat": "' + objJson.rules[ndx].pat.replace(/\\/g,"\\\\") + '", "meta": "' + objJson.rules[ndx].meta.replace(/\\/g,"\\\\") + '", "metap": "' + objJson.rules[ndx].metap + '", "ignoreparam": "' + objJson.rules[i].ignoreparam + '" }';
				sep = ",";
			}
		}
	}
	ruleJson += '] }';		
    $("#source_crawl_filtering_rules").val(ruleJson);

    ruleDisplay();
}

function json2XmlRule() {	
	$("#source_crawl_filtering_rules_xml").val("");	
	var jsonStr = $("#source_crawl_filtering_rules").val();
	if (jsonStr=="") return;
	try {
		var objJson = $.parseJSON($("#source_crawl_filtering_rules").val());
	}
	catch(err) {
		alert (err);
		return
	}
	var ruleXml = '<rules>';
	for ( var i = 0; i < objJson.rules.length ; i++ ) {
		if (objJson.rules[i].pat != "") {
			ruleXml += '<rule><ope>' + objJson.rules[i].ope + '</ope><mode>' + objJson.rules[i].mode + '</mode><pat>' + escapeHTML(objJson.rules[i].pat) + '</pat><meta>' + escapeHTML(objJson.rules[i].meta) + '</meta><metap>' + escapeHTML(objJson.rules[i].metap) + '</metap><ignoreparam>' + escapeHTML(objJson.rules[i].ignoreparam) + '</ignoreparam></rule>';
		}
	}
	ruleXml += '</rules>';		
	$("#source_crawl_filtering_rules_xml").val(ruleXml);	
	return true;
}


// Error dialog =====================================
function errorShowDialog (msg) {
	$("#error-dialog-msg").html(msg);
	$.blockUI({ 
			message: $('#error-dialog'), 
			overlayCSS:  { 
        		backgroundColor: '#000', 
        		opacity:         0.2 
    		}, 
		    css: { 
		        width:          '800px', 
		        top:            '30%', 
		        left:           '20%',
		        margin:   		'auto',
		        cursor:         'wait' 
		    }
	}); 
}
function errorDialogOk () {
    $.unblockUI(); 
    return false; 
}

// Scan RSS dialog =====================================
function scanRSSShowDialog () {
	currentUrl = -1;
	$('#scan-rss-form-url').val('');
	$('#scan-rss-dialog-rss').html('');
	
		$.blockUI({ 
			message: $('#scan-rss-dialog'), 
			overlayCSS:  { 
        		backgroundColor: '#000', 
        		opacity:         0.2 
    		}, 
		    css: { 
		        width:          '800px', 
		        top:            '10%', 
		        left:           '20%',
		        margin:   		'auto',
		        cursor:         'wait' 
		    }
	}); 
}

function scanRSS() {
	scanRSSShowDialog();
}

function scanRSSCancel () {
    $.unblockUI(); 
    return false; 
}

function scanRSSProcess() {
	var url = $("#scan-rss-form-url").val();
    $("#scan-rss-dialog-rss").html("<br/><strong>Looking for RSS feeds...</strong><br />This could be long to process (several seconds to one or two minutes) !<br /><br /><img src='images/ajax-loader.gif'>"); 
    $.post(ajax_url, {action: 'scanrss', url: url}, 
    function(data) {
        //alert(data);
        $("#scan-rss-dialog-rss").html(data);
    });    
}

function AddRss() {

	var allowsotherdomains = $('#scan-rss-dialog-rss-allowsotherdomains').val();

	$('#scan-rss-dialog-rss input[type=checkbox]').each(function () {
		if (this.checked) {
			addOneUrl($(this).val(), "r", allowsotherdomains, 0, false);
		}
	});

    $.unblockUI(); 
    urlDisplay();
    return false; 
}


// Schedule dialog =====================================
var currentSchedule = -1;

function addOneSchedule(day, start, stop, enabled, show_error_msg) {

	if (trim(day) == "" || trim(start) == "" || trim(stop) == "") return;

	if (parseInt(stop)<=parseInt(start)) {
    	if (show_error_msg) alert ("Stop hour must be highter than start hour !");
		return;
	}		

	
	var source_schedule = jQuery.trim($("#source_schedule").val());
	if (source_schedule == "") {
		source_schedule = '{ "schedules": [] }';
	}
    
    var objJson = $.parseJSON(source_schedule);

    // already a schedule ?
    if (currentSchedule==-1) {
	    for ( var i = 0; i < objJson.schedules.length ; i++ ) {
	    	if (objJson.schedules[i].day == day && objJson.schedules[i].start == start && objJson.schedules[i].stop == stop) {
	        	if (show_error_msg) alert ("This schedule already exists !");
	           	return;
	    	}
		}
    }

    var itemJsonStr = '{ "day": "' + day + '", "start": "' + start + '", "stop": "' + stop + '" , "enabled": "' + enabled + '"}';
	if (currentSchedule==-1) {
	    var itemJson =  $.parseJSON(itemJsonStr);
    	objJson.schedules.push( itemJson );
        $("#source_schedule").val(JSON.stringify(objJson, null, 2));
	} else {
		var scheduleJson = '{ "schedules": [';
		var sep = "";	
		for ( var i = 0; i < objJson.schedules.length ; i++ ) {
			if (currentSchedule==i) {
				scheduleJson += sep + itemJsonStr;
			}
			else {
				scheduleJson += sep + '{ "day": "' + objJson.schedules[i].day + '", "start": "' + objJson.schedules[i].start + '", "stop": "' + objJson.schedules[i].stop + '", "enabled": "' + objJson.schedules[i].enabled + '"}';
			}
			sep = ",";
		}
		scheduleJson += '] }';		
	    $("#source_schedule").val(scheduleJson);
	}
}

function addScheduleYes () {

    var day = $("#add-schedule-form-day").val().trim();

    //var RegexUrl = /^(ftp|http|https):\/\//;
    //if (!RegexUrl.test(url)) {
	//	alert ("URL invalid. Please provide protocol (http://, https:// or ftp://) !");
	//	return false;
    //}

    var start = $("#add-schedule-form-start").val();
    var stop = $("#add-schedule-form-stop").val();
    var enabled = $("#add-schedule-form-enabled").val();

    addOneSchedule(day, start, stop, enabled, true);
    
    scheduleDisplay();

    $.unblockUI(); 
    return false; 
}

function addScheduleNo () {
    $.unblockUI(); 
    return false; 
}

function addScheduleShowDialog (day, start, stop, enabled, ndx) {
	currentSchedule = ndx;
	$("#add-schedule-form-day").val(day);
	$("#add-schedule-form-start").val(start);
	$("#add-schedule-form-stop").val(stop);
	$("#add-schedule-form-enabled").val(enabled);
	$.blockUI({ 
			message: $('#add-schedule-form'), 
			overlayCSS:  { 
        		backgroundColor: '#000', 
        		opacity:         0.2 
    		}, 
		    css: { 
		        width:          '800px', 
		        top:            '30%', 
		        left:           '20%',
		        margin:   		'auto',
		        cursor:         'wait' 
		    }
	}); 
}

function scheduleDisplay() {
<?php 
if ($_SESSION["mysolrserver_url"]) {
?>	
	return true;
<?php 
}		
?>	
	var source_schedule = jQuery.trim($("#source_schedule").val());
	if (source_schedule == "") {
		source_schedule = '{ "schedules": [] }';
	}

	var objJson = $.parseJSON(source_schedule);
	var html = '<table>';
	for ( var i = 0; i < objJson.schedules.length ; i++ ) {
			html += "<tr id='schedule_" + i + "'><td><table class='nob'><tr class='nob'>";
			html += "<td class='nob' width='24%'>Day : " + objJson.schedules[i].day + "</td>";
			html += "<td class='nob' width='22%'>Start : " + objJson.schedules[i].start + "</td>";
			html += "<td class='nob' width='22%'>Stop : " + objJson.schedules[i].stop + "</td>";
			html += "<td class='nob' width='22%'>Enabled : " + objJson.schedules[i].enabled + "</td>";

			html += "<td class='nob' width='10%'><a href='javascript:editSchedule(" + i + ")'><img src='images/button_edit.png'></a>&nbsp;";
			html += "<a href='javascript:delSchedule(" + i + ")'><img src='images/trash_16.png'></a></td>";

			html += "</tr></table></td></tr>";
	}
	html += "</table>";
	$("#schedule").html(html);
}

function addSchedule() {
	addScheduleShowDialog("","s","0","0",-1);
}

function delSchedule(ndx) {
	var answer = confirm ("Do you really want to delete this schedule ?");
	if (!answer)
		return;

	var objJson = $.parseJSON($("#source_schedule").val());
	var scheduleJson = '{ "schedules": [';
	var sep = "";	
	for ( var i = 0; i < objJson.schedules.length ; i++ ) {
		if (ndx!=i) {
			scheduleJson += sep + '{ "day": "' + objJson.schedules[i].day + '", "start": "' + objJson.schedules[i].start + '", "stop": "' + objJson.schedules[i].stop + '", "enabled": "' + objJson.schedules[i].enabled + '" }';
			sep = ",";
		}
	}
	scheduleJson += '] }';		
	$("#source_schedule").val(scheduleJson);	
    scheduleDisplay();
}

function editSchedule(ndx) {
	var objJson = $.parseJSON($("#source_schedule").val());
	addScheduleShowDialog (objJson.schedules[ndx].day, objJson.schedules[ndx].start, objJson.schedules[ndx].stop, objJson.schedules[ndx].enabled, ndx);
    scheduleDisplay();
}

function json2XmlSchedule() {
<?php 
if ($_SESSION["mysolrserver_url"]) {
?>	
	return true;
<?php 
}		
?>	
	$("#source_schedule_xml").val("");	

	var jsonStr = $("#source_schedule").val();
	if (jsonStr=="") return;
	
	var objJson = $.parseJSON($("#source_schedule").val());
	var scheduleXml = '<schedules>';
	for ( var i = 0; i < objJson.schedules.length ; i++ ) {
		scheduleXml += '<schedule><day>' + escapeHTML(objJson.schedules[i].day) + '</day><start>' + objJson.schedules[i].start + '</start><stop>' + objJson.schedules[i].stop + '</stop><enabled>' + objJson.schedules[i].enabled + '</enabled></schedule>';
	}
	scheduleXml += '</schedules>';		
	$("#source_schedule_xml").val(scheduleXml);	
	return true;
}


// Export dialog =====================================

function exportExport () {
	var mode = $("#export-dialog-form-mode").val();

	var ids = [];

	if (mode=='selection') {
	    $("INPUT[name='srcChk']").each(function() {
			if($(this).is(':checked')) ids.push($(this).val());
		});
		if (ids.length==0) {
			alert ('No selected web site !');
			return false;
		}
	}
	
	$("#export-dialog-message").html("<img src='images/ajax-loader-small.gif'>");
    $.post(ajax_url, {action: 'exportsources', mode: mode, ids: ids}, 
    function(data) {
        //alert(data);   
        //var link = "<a href='display.php?mode=download&file=" + data + "' target='download'>Download</a>";  
        //$("#export-dialog-message").html(link);
        var objJson = $.parseJSON(data);

        if (objJson.status=='success') {
        	var link = "<a href='display.php?mode=download&file=" + objJson.filename + "' target='download'>Download</a>";
        	$("#export-dialog-message").html(link);
        }
        else
        	$("#export-dialog-message").html("error");

    });    
    return false; 
}

function exportClose () {
    $.unblockUI(); 
    return false; 
}

function exportShowDialog () {
	$("#export-dialog-message").html("");
	$("#export-dialog-form-mode").val("all");
	$.blockUI({ 
			message: $('#export-dialog'), 
			overlayCSS:  { 
        		backgroundColor: '#000', 
        		opacity:         0.2 
    		}, 
		    css: { 
		        width:          '400px', 
		        top:            '30%', 
		        left:           '30%',
		        margin:   		'auto',
		        cursor:         'wait' 
		    }
	}); 
}

function srcCheck() {
	 $("INPUT[name='srcChk']").attr('checked', $('#chkAll').is(':checked'));    
}

function exportSources() {
	exportShowDialog ();
}

// Import dialog =====================================

function importImport () {

	var filename = $('#import-dialog-form-file').val().trim();
	filename = filename.toLowerCase(); 
		
	if (filename=="") {
		alert ("Select a file !");
		return false;
	}

	if (!filename.match(/[.](xml|csv)$/i)) {
		alert ("Import file name must have xml extention !");
		return false;
	}
	
	var url = ajax_url + '?action=importsources';
	url += '&match=' + $("#import-dialog-form-match").val();
	url += '&strategy=' + $("#import-dialog-form-strategy").val();
	url += '&status=' + $("#import-dialog-form-status").val();
	//url += '&check=' + $("#import-dialog-form-check").val();
	
	var reset = $('#import-dialog-form-reset').attr('checked')?"1":"0";
	url += '&reset=' + reset;
			
	$("#import-dialog-message").html("");
	$("#import-dialog-form-upload").upload(url, 
	function(data) {
        var objJson = $.parseJSON(data);
        if (objJson.status=='success') {
        	$("#import-dialog-message").html("success");
        }
        else
        	$("#import-dialog-message").html("error");
	}, 'html');

    return false; 
}

function importClose () {
    $.unblockUI(); 
    return false; 
}

function importShowDialog () {
	$("#export-dialog-message").html("");
	$("#import-dialog-form-match").val("name");
	$("#import-dialog-form-strategy").val("skip");
	$("#import-dialog-form-status").val("disabled");
	$("#import-dialog-form-check").attr('checked', true);    
	
	$.blockUI({ 
			message: $('#import-dialog'), 
			overlayCSS:  { 
        		backgroundColor: '#000', 
        		opacity:         0.2 
    		}, 
		    css: { 
		        width:          '500px', 
		        top:            '30%', 
		        left:           '25%',
		        margin:   		'auto',
		        cursor:         'wait' 
		    }
	}); 
}

function importSources() {
	importShowDialog ()
}



//-->
</script>

<div id="contenu">
	<h2>Crawler sources</h2>
	
<!--  	
	<a href="javascript:(function(){document.body.appendChild(document.createElement('script')).src='http://localhost/crawler/scripts/dombrowser.js?v=6.1.0';})();" class="bookmarklet nofocus">Page content browser</a>
	-->
	<div id="sources"></div>

	<div id="source_details"></div>

	<div id="source_type_select">
		<table border="0" style="width:30%; margin-left: auto; margin-right: auto;">
			<tr>
				<td colspan='2'><strong>Select the source type you want to create</strong></td>
			</tr>		
<?php 
foreach ($aSourceTypes as $id => $type) {
	$res2="";
	$res2 .= "<tr>";
	$res2 .= "<td>";
	$res2 .= "<img src='images/" . $type["mnemo"] . "_icone.png' height='32' width='32'>&nbsp;&nbsp;";
	$res2 .= $type["name"];
	$res2 .= "</td>";
	$res2 .= "<td style='width:80px; text-align: center;'>";
	$res2 .= "<input type='button' value='Create' onClick='selectSourceCreate(\"" . $type["mnemo"] . "\")'>";
	$res2 .= "</td>";
	$res2 .= "</tr>";
	echo $res2;	
}
?>
		</table>
		<div class='menu_button_on_right'>
			<a href='#' onClick='cancelSelectSourceType();'><img src='images/button_cancel_32.png' title='Cancel'></a>&nbsp;&nbsp;
		</div>
	</div>

	<div id="add-url-form" style="display: none; cursor: default">
		<div id="add-url-form-title"
			style="padding: 10px; text-align: center; background-color: #808080;">URL</div>
		<form>
			<fieldset>
				<table border='0'>
					<tr>
						<td width='45%'><label>Mode</label></td>
						<td><select id="add-url-form-mode">
								<option value='s' selected>Web site</option>
								<option value='l'>Links page (depth=1)</option>
								<option value='r'>RSS feed (depth=1)</option>
								<option value='m'>Sitemaps (depth=1)</option>
						</select></td>
					</tr>
					<tr>
						<td colspan='2'><label>Url</label> <br /> <input type="text"
							id="add-url-form-url" class='editInputText' /> <br /> <!-- 
		&nbsp;
		<div id="add-url-form-url-home" style="display: none;"><label>Web site
		home page for RSS feeds not hosted by there own server (feedburner,
		...)</label> <br />
		<input type="text" id="add-url-form-home" class='editInputText' /> <br />
	    --> &nbsp;
							</div>
						</td>
					</tr>
					<tr>
						<td><label>Allow other domains (Links page or RSS feed only)</label>
						</td>
						<td><select id="add-url-form-allowsotherdomains">
								<option value='0' selected>No</option>
								<option value='1'>Yes</option>
						</select></td>
					</tr>
					<tr>
						<td><label>Use only during first crawl</label></td>
						<td><select id="add-url-form-onlyfirstcrawl">
								<option value='0' selected>No</option>
								<option value='1'>Yes</option>
						</select></td>
					</tr>
					<tr>
						<td colspan='2'><input type="button" onClick='addUrlYes()'
							value="Ok" /> <input type="button" onClick='addUrlNo()'
							value="Cancel" /></td>
					</tr>
				</table>
			</fieldset>
		</form>
	</div>


	<div id="add-rule-form" style="display: none; cursor: default">
		<div id="add-rule-form-title"
			style="padding: 10px; text-align: center; background-color: #808080;">Rule</div>
		<form>
			<fieldset>
				<table border='0'>
					<tr>
						<td><label>Pattern</label></td>
						<td><select id="add-rule-form-ope">
								<option value='match' selected>match</option>
								<option value='path'>path</option>
						</select> <input type="text" id="add-rule-form-pat" class="editInputTextMedium3" /></td>
					</tr>
					<tr>
						<td><label>Mode</label></td>
						<td><select id="add-rule-form-mode">
								<option value='all' selected>Get page and extract links</option>
								<option value='links'>Extract links only</option>
								<option value='get'>Get page only</option>
								<option value='skip'>Ignore</option>
						</select></td>
					</tr>
					<tbody id='add-rule-form-ignoreparam-tbody'>
					<tr>
						<td><label>Ignored fields in url<br/>(others than session id)</label></td>
						<td>
						<input type=text" id="add-rule-form-ignoreparam" class='editInputText' />
						</td>
					</tr>
					</tbody>
					<tbody id='add-rule-form-meta-tbody'>
					<tr>
						<td><label>Metadata</label></td>
						<td>
						<textarea id="add-rule-form-meta"rows="3" cols="60"></textarea>
						<!--  
						<br/>
						Propagate on 
						<select id="add-rule-form-meta-propagate">
								<option value='0' selected>0</option>
								<option value='1'>1</option>
						</select>levels
						-->
						<br/><span class='help'>These metadatas will be added into the output xml files for pages matching this rule.</span>
						<br/><span class='help'>Syntax:</span>
						<br/><span class='help'>meta_name1:value1</span>
						<br/><span class='help'>-meta_name1:value2</span>
						<br/><span class='help'>meta_name2:value3</span>
						</td>
					</tr>
					</tbody>
					<tr>
						<td colspan='2'><input type="button" onClick='addRuleYes()'
							value="Ok" /> <input type="button" onClick='addRuleNo()'
							value="Cancel" /></td>
					</tr>
				</table>
			</fieldset>
		</form>
	</div>

	<div id="error-dialog" style="display: none; cursor: default">
		<div id="error-dialog-title"
			style="padding: 10px; text-align: center; background-color: #808080;">Error</div>
		<form>
			<fieldset>
				<table border='0'>
					<tr>
						<td>
							<div id="error-dialog-msg"></div>
						</td>
						<td></td>
					</tr>
					<tr>
						<td colspan='2'><input type="button" onClick='errorDialogOk()'
							value="Ok" /></td>
					</tr>
				</table>

			</fieldset>
		</form>
	</div>


	<div id="scan-rss-dialog" style="display: none; cursor: default">
		<div id="scan-rss-dialog-title"
			style="padding: 10px; text-align: center; background-color: #808080;">Scan
			for available RSS feeds</div>
		<div id="scan-rss-dialog-form">
			<form>
				<fieldset>
					<table border='0' style='margin-left: auto; margin-right: auto;'>
						<tr>
							<td><label>Scan starting this url</label></td>
							<td><input type="text" id="scan-rss-form-url"
								class='editInputText' /></td>
						</tr>
						<tr>
							<td colspan='2'><input type="button" onClick='scanRSSProcess()'
								value="Scan" /> <input type="button" onClick='scanRSSCancel()'
								value="Cancel" /></td>
						</tr>
					</table>
				</fieldset>
			</form>
		</div>
		<div id="scan-rss-dialog-rss"
			style="overflow: auto; max-height: 500px;"></div>
	</div>


	<div id="add-schedule-form" style="display: none; cursor: default">
		<div id="add-schedule-form-title"
			style="padding: 10px; text-align: center; background-color: #808080;">Schedule</div>
		<form>
			<fieldset>
				<table border='0'>
					<tr>
						<td><label>Days</label></td>
						<td><select id="add-schedule-form-day">
								<option value='all' selected>all</option>
								<option value='monday' selected>monday</option>
								<option value='tuesday' selected>tuesday</option>
								<option value='wednesday' selected>wednesday</option>
								<option value='thursday' selected>thursday</option>
								<option value='friday' selected>friday</option>
								<option value='saturday' selected>saturday</option>
								<option value='sunday' selected>sunday</option>
						</select></td>
						<td><label>Start hour</label></td>
						<td><select id="add-schedule-form-start">
								<option value='0' selected>0:00</option>
								<option value='1' selected>1:00</option>
								<option value='2' selected>2:00</option>
								<option value='3' selected>3:00</option>
								<option value='4' selected>4:00</option>
								<option value='5' selected>5:00</option>
								<option value='6' selected>6:00</option>
								<option value='7' selected>7:00</option>
								<option value='8' selected>8:00</option>
								<option value='9' selected>9:00</option>
								<option value='10' selected>10:00</option>
								<option value='11' selected>11:00</option>
								<option value='12' selected>12:00</option>
								<option value='13' selected>13:00</option>
								<option value='14' selected>14:00</option>
								<option value='15' selected>15:00</option>
								<option value='16' selected>16:00</option>
								<option value='17' selected>17:00</option>
								<option value='18' selected>18:00</option>
								<option value='19' selected>19:00</option>
								<option value='20' selected>20:00</option>
								<option value='21' selected>21:00</option>
								<option value='22' selected>22:00</option>
								<option value='23' selected>23:00</option>
						</select></td>
						<td><label>Stop hour</label></td>
						<td><select id="add-schedule-form-stop">
								<option value='1' selected>1:00</option>
								<option value='2' selected>2:00</option>
								<option value='3' selected>3:00</option>
								<option value='4' selected>4:00</option>
								<option value='5' selected>5:00</option>
								<option value='6' selected>6:00</option>
								<option value='7' selected>7:00</option>
								<option value='8' selected>8:00</option>
								<option value='9' selected>9:00</option>
								<option value='10' selected>10:00</option>
								<option value='11' selected>11:00</option>
								<option value='12' selected>12:00</option>
								<option value='13' selected>13:00</option>
								<option value='14' selected>14:00</option>
								<option value='15' selected>15:00</option>
								<option value='16' selected>16:00</option>
								<option value='17' selected>17:00</option>
								<option value='18' selected>18:00</option>
								<option value='19' selected>19:00</option>
								<option value='20' selected>20:00</option>
								<option value='21' selected>21:00</option>
								<option value='22' selected>22:00</option>
								<option value='23' selected>23:00</option>
								<option value='24' selected>24:00</option>
						</select></td>
						<td><label>Enabled</label></td>
						<td><select id="add-schedule-form-enabled">
								<option value='true' selected>yes</option>
								<option value='false' selected>no</option>
						</select></td>
					</tr>
					<tr>
						<td colspan='8'><input type="button" onClick='addScheduleYes()'
							value="Ok" /> <input type="button" onClick='addScheduleNo()'
							value="Cancel" /></td>
					</tr>
				</table>
			</fieldset>
		</form>
	</div>

	<!--
	<div id="select-source-dialog" style="display: none; cursor: default">
		<div id="select-source-dialog-title"
			style="padding: 10px; text-align: center; background-color: #808080;">Choose
			source type</div>
		<div id="select-source-dialog-form">
			<form>
				<fieldset>
					<table border='0' style='margin-left: auto; margin-right: auto;'>
						<tr>
							<td><label>Source type</label></td>
							<td><select id="export-dialog-form-type">
							</select></td>
						</tr>
						<tr>
							<td colspan='2'><input type="button"
								onClick='selectSourceCreate()' value="Create" /> <input
								type="button" onClick='selectSourceCancel()' value="Cancel" /> <span
								id='select-source-dialog-message'></span></td>
						</tr>
					</table>
				</fieldset>
			</form>
		</div>
	</div>
	-->
	
	<div id="export-dialog" style="display: none; cursor: default">
		<div id="export-dialog-title"
			style="padding: 10px; text-align: center; background-color: #808080;">Export
			sources definition</div>
		<div id="export-dialog-form">
			<form>
				<fieldset>
					<table border='0' style='margin-left: auto; margin-right: auto;'>
						<tr>
							<td><label>Export mode</label></td>
							<td><select id="export-dialog-form-mode">
									<option value='all' selected>all</option>
									<!-- <option value='current' selected>current (all web sites matching current criteria)</option>  -->
									<option value='selection' selected>selection (checked web
										sites)</option>
							</select></td>
						</tr>
						<tr>
							<td colspan='2'><input type="button" onClick='exportExport()'
								value="Export" /> <input type="button" onClick='exportClose()'
								value="Close" /> <span id='export-dialog-message'></span></td>
						</tr>
					</table>
				</fieldset>
			</form>
		</div>
	</div>

	<div id="import-dialog" style="display: none; cursor: default">
		<div id="import-dialog-title"
			style="padding: 10px; text-align: center; background-color: #808080;">Import
			sources definition</div>
		<div id="import-dialog-form-upload">
			<!-- form id='import-dialog-form-upload' -->
			<!-- form enctype="multipart/form-data" action="_URL_" method="post"  -->
			<fieldset>
				<table border='0' style='margin-left: auto; margin-right: auto;'>
					<tr>
					
					
					<tr>
						<td><label>Check existing web site by</label></td>
						<td><select id="import-dialog-form-match">
								<option value='id' selected>Id</option>
								<option value='name' selected>Name</option>
								<option value='host' selected>Host</option>
								<option value='none' selected>No check</option>
						</select></td>
					</tr>
					<tr>
						<td><label>When a web site exists</label></td>
						<td><select id="import-dialog-form-strategy">
								<option value='skip' selected>Leave it unchanged</option>
								<option value='replace' selected>Replace it</option>
						</select></td>
					</tr>
					<tr>
						<td><label>Create web site with status</label></td>
						<td><select id="import-dialog-form-status">
								<option value='0' selected>Disabled</option>
								<option value='1' selected>Enabled</option>
								<option value='2' selected>Test</option>
						</select></td>
					</tr>
					<tr>
						<td><label>Reset source</label></td>
						<td><input id="import-dialog-form-reset"
							name="import-dialog-form-reset" type="checkbox" value="0" /></td>
					</tr>
					<!--  
	<tr>
		<td><label>Check import file syntax only</label></td>
		<td><input id="import-dialog-form-check" name="import-dialog-form-check" type="checkbox" value="1" /></td>
	</tr>
	-->
					<td><label>Import file</label></td>
					<td><input type="hidden" name="MAX_FILE_SIZE" value="1000000" /> <input
						id="import-dialog-form-file" name="import-dialog-form-file"
						type="file" />
					</td>
					</tr>
					<tr>
						<td colspan='2'><input type="button" onClick='importImport()'
							value="Import" /> <input type="button" onClick='importClose()'
							value="Close" /> <span id='import-dialog-message'></span></td>
					</tr>
				</table>
			</fieldset>
			<!-- /form -->
		</div>
	</div>

</div>
