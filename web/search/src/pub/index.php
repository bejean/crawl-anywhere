<?php
$rootpath = dirname(__FILE__);
require_once("../init.inc.php");

$aLexicons = getSolrLexicons($config->get("solr.host"), $config->get("solr.port"), $config->get("solr.baseurl"), $config->get("solr.corename"));

$aCountriesForm = getMappingArray("", "code_countries.txt", true, $aLexicons, "country");
$aLanguagesForm = getMappingArray("", "code_languages.txt", true, $aLexicons, "language");
$aLanguagesStemmedForm = getMappingArray("", "code_languages_stemmed.txt", true, $aLexicons, "language");
$aContentTypeForm = getMappingArray("", "code_contenttype.txt", true, $aLexicons, "contenttyperoot");

$solrMainContentLanguage = getSolrMainContentLanguage($aLexicons, $user_language);

$debug = false;
if (isset($_GET["debug"]))
	$debug = ($_GET["debug"] == "1");

?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
    <meta http-equiv='Content-Type' content='text/html; charset=utf-8'/>
    <title><?php echo $config->get("application.title");?></title>
    <meta http-equiv='Cache-Control' content='no-cache' />
    <meta http-equiv='Pragma' content='no-cache' />
    <meta http-equiv='Cache' content='no store' />
    <meta http-equiv='Expires' content='0' />
    <meta name='robots' content='index, nofollow' />
       
	<link rel="stylesheet" href="css/initial.css" type="text/css" media="screen" />
	<link rel="stylesheet" href="css/styles.css" type="text/css" media="screen, projection" />
	<link rel="stylesheet" href="css/tagcloud.css" type="text/css" media="screen" />

<!-- IE hacks -->
<!--[if lte IE 6]>
<link rel="stylesheet" type="text/css" href="css/ie6.css" media="screen,projection" />
<![endif]-->
<!--[if lte IE 7]>
<link rel="stylesheet" type="text/css" href="css/ie7.css" media="screen,projection" />
<![endif]-->

 	<!--script src="http://bubble.websnapr.com/A8Jmh9RTb7ql/swi/" type="text/javascript"></script-->
    <script type="text/javascript" src="js/jquery-1.5.1.min.js"></script>       
    <script type="text/javascript" src="js/autocomplete.js"></script>       
 	<script type='text/javascript' src='js/jquery.blockUI.js'></script>
 	<script type='text/javascript' src='js/jquery.ae.image.resize.min.js'></script>
  	<!--script type="text/javascript" src="js/jquery.qtip-1.0.0-rc3.min.js"></script-->
    
    <script type="text/javascript">
	<!--
	function doDidYouMeanSearch(crit)
	{
		$("input#search_crit").val(crit);  
		doSearch(0, "", "", "", false);
	}

	// Reader dialog =====================================
	function doReader(id, crit)
	{
		readerShowDialog ('title', 'text');	

		$.ajax({
			type: "post",
			data: {action: 'gettext', id: id, search_crit: crit}, 
			url: 'search.ajax.php',
			success: function(data) {
				$("#reader-dialog-text").html(data);
			}
		});	    
	}

	function readerShowDialog () {
		$("#reader-dialog-text").html("<center><img src='images/ajax-loader.gif'></center>");
		$.blockUI({ 
				message: $('#reader-dialog'), 
				overlayCSS:  { 
	        		backgroundColor: '#000', 
	        		opacity:         0.2 
	    		}, 
			    css: { 
			        width:          '800px', 
			        top:            '20%', 
			        left:           '20%',
			        margin:   		'auto',
			        cursor:         'wait' 
			    }
		}); 
	}

	function setSearchCrit(crit)
	{
		$("input#search_crit").val(crit);
		doSearch(0, "", "", "", false);
	}
	
	
	function doSearch(page, fq, fq_previous, id, ischeckbox)
	{
		var crit = $("input#search_crit").val();  
		
		if (crit=="")
			return false;
		
		$("input#page").val(page);  	

		var checked = false;
		if (id!="")
		{
			var checked = $("input#"+id).attr('checked');
			if (ischeckbox) checked = !checked;
		}

		if (checked)
		{			
			// on retire fq de fq_previous
			var temp = unescape(fq_previous);
			fq = unescape(fq);
			var aPrevious = temp.split('||');

			f = "";
			for (var i=0; i<aPrevious.length; i++)
			{
				if (aPrevious[i]!=fq)
				{
					if (f!="")
						f += escape("||");
					f += escape(aPrevious[i]);
				}
			}
		}
		else
		{
			// on ajoute fq à fq_previous
			var f = fq_previous;
			if (fq!="")
			{
				if (f!="")
				{
					f = fq + escape("||") + fq_previous;
				}
				else
				{
					f = fq;
				}
			}
		}
		
		$("input#fq").val(f);  		
	    $("#result").html("<br />&nbsp;<br /><center><img src='images/ajax-loader.gif'></center>"); 

		$.ajax({
			type: "post",
			data: $("#search_form").serialize(), // assuming this == the form
			url: 'search.ajax.php',
			success: function(data) {
			        //alert(data);
			        $("#result").hide();
			        $("#result").html(data);
			        $("#result").show();
<?php 				
if ($results_img_height>0 && $results_img_width>0) {
?>
			        $( ".resizeme" ).aeImageResize({ height: <?php echo $results_img_height; ?>, width: <?php echo $results_img_width; ?> });
<?php 				
}
?>
			}
		});	    
	}

	function loadCollections() {
	    $("#div_query_collections_values").html("<img src='images/ajax-loader.gif'>"); 
	    $.get('search.ajax.php', {action: 'fiedvalues', field: 'collection'}, 
	    	    function(data) {
	    	        //alert(data);
	    	        
	    	        var html = "";
	    	        var aCol = data.split('|');
	    	        for (var i=0;i<aCol.length;i++) {
		    	        if (aCol[i]!="") {
							var items = aCol[i].split(':');
			    	    	//html += '<div><input type="checkbox" value="' + items[0] + '" name="search_collection[]"/>' + items[0] + ' (' + items[1] + ')' + '</div>';
			    	    	html += '<div><input type="checkbox" value="' + items[0] + '" name="search_collection[]"/>' + items[0] + '</div>';
		    	        }
	    	        }
	    	        if (html == "") {
	    	        	html = "none";
	    	        	$("#div_query_collections").hide();
	    	        } 
	    	        else {
	    	        	$("#div_query_collections").show();
		    	        $("#div_query_collections_values").html(html);
	    	        }
	    	    });    
	}
	function loadTags() {
	    $("#div_query_tags_values").html("<img src='images/ajax-loader.gif'>"); 
	    $.get('search.ajax.php', {action: 'fiedvalues', field: 'tag'}, 
	    	    function(data) {
	    	        //alert(data);
	    	        
	    	        var html = "";
	    	        var aCol = data.split('|');
	    	        for (var i=0;i<aCol.length;i++) {
		    	        if (aCol[i]!="") {
							var items = aCol[i].split(':');
			    	    	//html += '<div><input type="checkbox" value="' + items[0] + '" name="search_tag[]"/>' + items[0] + ' (' + items[1] + ')' + '</div>';
			    	    	html += '<div><input type="checkbox" value="' + items[0] + '" name="search_tag[]"/>' + items[0] + '</div>';
		    	        }
	    	        }
	    	        if (html == "") {
		    	        html = "none";
	    	        	$("#div_query_tags").hide();
	    	        }
	    	        else {
	    	        	$("#div_query_tags").show();
	    	        	$("#div_query_tags_values").html(html);
	    	        }
	    	    });    
	}

	function switchMode(mode)
	{
		if (mode=="advanced")
		{
			$("#switch_advanced").hide();
			$("#switch_simple").show();
			$("#search_advanced").show();
			$("input#mode").val("advanced");  	
		}
		else
		{
			$("#switch_advanced").show();
			$("#switch_simple").hide();
			$("#search_advanced").hide();
			$("input#mode").val("simple");  	
		}
	}
	
	$(function() {  
		$("#search_do").click(function() {  
		    // validate and process form here
			var crit = $("input#crit").val();  
			if (crit != "")
				doSearch(0, "", "", "", false);
			return false;
		});  
		/*
		$("input#crit").keyup(function (e) {
		    if (e.keyCode == 13) {
		        // Do something
		    }
		});
		*/	
	});  



	/*
	$(function() {  
		$("#search_clear").click(function() {  
			$("#search_crit").val(""); 
			$("#fq").val(""); 

			$("#search_querylanguage").val("");
	 		$("#search_word_variations").attr("disabled", true);
		    $("#search_word_variations").attr("checked", false);

			//$("#search_org").val(""); 
			//$("#search_language").val(""); 
			//$("#search_country").val(""); 
			$("#search_mimetype").val(""); 
		    $("#result").html(""); 
		});  
	}); 
	*/ 	
	
    $(document).ready(function(){
	    $("select#search_querylanguage").change(function () {
		    var lang = $("#search_querylanguage").val();
		    if (lang=="")
		    {
		 		$("#search_word_variations").attr("disabled", true);
			    $("#search_word_variations").attr("checked", false);
		    }
		    else
		    {
		    	$("#search_word_variations").attr("disabled", false);
		    }
	    }).change();

	    $("#search_crit").suggest("search.ajax.php?action=autocomplete",{});
	    
	});

	function loadTagCloud() {
	    $("#tagcloud").html("<img src='images/ajax-loader.gif'>"); 
	    $.get('search.ajax.php', {action: 'gettagcloud', field: 'tag_cloud'}, 
	    	    function(data) {
	    	        //alert(data);
	    	        var html = data;
	    	        if (html == "") html = "none";
	    	        $("#tagcloud").html(html);
	    	    });    
	}
	
	$(window).load(function(){
		// Le code placé ici sera déclenché
		// au chargement complet de la page.
		
<?php 				
		if ($usecollections=="1") {
?>
			loadCollections();
<?php 				
		}
?>

<?php 				
		if ($usetags=="1") {
?>
			loadTags();
<?php 				
		}
?>

<?php 				
		if ($usetagcloud=="1") {
?>
			loadTagCloud();
<?php 				
		}
?>
		
		var q = "<?php echo addslashes(getRequestParam("q")) ?>";				
		if (q!="")
		{
			$("#fq").val(""); 
			$("#search_crit").val(q); 
			
			var ql = "<?php echo getRequestParam("ql") ?>";
			$("#search_querylanguage").val(ql);
		
			var wv = "<?php echo getRequestParam("wv") ?>";
			if (wv=="1" && (ql=="en" || ql=="fr"))
			{
				$("#search_word_variations").attr("disabled", false);
				$("#search_word_variations").attr("checked", true);
			}

			var lan = "<?php echo getRequestParam("lang") ?>";
			$("#search_language").val(lan);
			
			var country = "<?php echo getRequestParam("country") ?>";
			$("#search_country").val(country);

			var org = "<?php echo addslashes(getRequestParam("org")) ?>";
			$("#search_org").val(org); 
			
			var mime = "<?php echo getRequestParam("mime") ?>";
			$("#search_mimetype").val(mime); 			
			
			if (country!="" || lan!="" || mime!="" || org!="")
				switchMode("advanced");	
		
			doSearch(1, "", "", "", false);
		}
	});
	
	//-->
	</script>  
        
<?php if ($uselocation) { 
$yahoo_api_key = $config->get("yahoo_api_key");
?>	    
<script type="text/javascript"
src="http://api.maps.yahoo.com/ajaxymap?v=3.8&appid=<?php echo $yahoo_api_key; ?>">
</script>
<style type="text/css">
.map{
  height: 75%;
  width: 100%;
  margin : auto;
}
</style> 
<?php  } ?>       
          
</head>
<body>

<div id="wrapper">
	<div id="header">
	</div><!--/header-->
	
	<div id="main">

		<div id="logo">
<?php 	
	$logo_file = $config->get("logo.file");
	$logo_alt = $config->get("logo.alt");
	$logo_title = $config->get("logo.title");
	$logo_catcher = $config->get("logo.catcher");
	
	if ($logo_file!="") print ("<p><a href='#'><img src='images/" . $logo_file . "' alt='" . $logo_alt . "' title='" . $logo_title . "' /></a></p>");
	if ($logo_catcher!="") print ("<h3><em>" . $logo_catcher . "</em></h3>");
	
	$core = $_SESSION["core"];
?>				
		</div><!--/logo-->

 			<div id="search" >		

				<div class="form-container">
				<form name="search_form" id="search_form" action="">
					<input type="hidden" name="config_save" id="config_save" value="<?php echo $config_file; ?>">
					<input type="hidden" name="core" id="core_save" value="<?php echo $core; ?>">

					<input type="hidden" name="action" id="action" value="search">
					<input type="hidden" name="page" id="page">
					<input type="hidden" name="fq" id="fq">
					<input type="hidden" name="mode" id="mode" value="simple">
					<input type="hidden" name="search_itemperpage" id="search_itemperpage" value="20">

					<fieldset class="search_box">						
						<div>							
							<input id="search_crit" type="text" value="" name="search_crit" autocomplete="off" />
							<!--span class="loading"><img src="images/loading.png" alt="Loading" /></span-->
						</div>
						<div class="buttonrow">
							<input id="search_do" type="submit" name="search_do" value="<?php echo 'Search'; ?>"/>
<?php 
	if ($useadvanced==1) {
?>				
							<p>
							<span id="switch_advanced"><a href='javascript:void(0)' onClick='switchMode("advanced");'>Advanced search</a></span> 
							<span id="switch_simple" style='display:none'><a href='javascript:void(0)' onClick='switchMode("simple");'>Simple search</a></span>
							</p>
<?php 
	}
?>	
						</div>
<?php 
if ($usetagcloud) print ('<div id="tagcloud"></div>')
?>	
	
					</fieldset>

					<fieldset class="search_simple">						
						<div id="div_query_language" class="controlset">	
<?php 
	$selectQueryLanguage = $config->get("search.select_query_language");
	if ($selectQueryLanguage==1) {
?>
							<label for="search_querylanguage" class="form_text">The language of the query is</label>					
							<select name="search_querylanguage" id="search_querylanguage">
								<option value="">Not specified</option>								
<?php
		foreach ($aLanguagesStemmedForm as $key => $value)
		{
			print( "<option value='" . $key . "'");
			if ($solrMainContentLanguage==strtolower($key)) print (" selected='selected'");
			print( ">" . $value . "</option>");
		}
?>
							</select>		
<?php 
	}
	else {
		$defaultQueryLanguage = $config->get("search.default_query_language"); 
		if ($defaultQueryLanguage=="") $defaultQueryLanguage = "en";
?>
							<input type="hidden" name="search_querylanguage" id="search_querylanguage" value="<?php echo $defaultQueryLanguage ?>">	
<?php 
	}
		
	$selectLanguageStemming = $config->get("search.select_language_stemming");
	$language_stemming_by_default = ($config->get("search.language_stemming_by_default")=='1');
	if ($selectLanguageStemming==1) {
?>												
							<input id="search_word_variations" type="checkbox" value="1" name="search_word_variations" <?php if ($language_stemming_by_default) echo ' checked="checked"';?> />
							<label for="search_word_variations" class="form_text">Use word variations</label>							
<?php 
	}
	else {
		if ($language_stemming_by_default) 
			$val = '1';
		else
			$val = '0';
?>												
		<input id="search_word_variations" type="hidden" value="<?php echo $val; ?>" name="search_word_variations"/>
<?php 
	}
?>						
						</div>
						
						<div id="div_query_sort" class="controlset">	
							<label for='search_sort' class='form_text'><?php echo 'Sort results by'; ?></label>
							<select id='search_sort' name='search_sort'>	
								<option value=''><?php echo 'Relevance'; ?></option>
								<option value='createtime desc'>Date</option>
							</select>							
						</div>

<?php 				
			if ($usecollections=="1") {
?>
					<div id="div_query_collections" class="controlset" style="display:none">	
						<span class="label">Source collections</span>
						<div id="st_option">
						<div id="div_query_collections_values" style="width:100%">
<?php 
/*
						foreach ($xml->collections->collection as $collection) {
							echo '<div><input type="checkbox" value="' . $collection . '" name="search_collection[]"/>' . $collection . '</div>';
						}
*/
?>
						</div>
						</div>
					</div>			
<?php 
//					}
			} 
?>									
					
<?php if ($debug) { ?>
					<input type="checkbox" name="search_debug" id="search_debug" value="1">Debug output
<?php } ?>						
					</fieldset>

					<div id="search_advanced" style='display:none'>
						<fieldset class="search_advanced">		

<?php 				
			if ($usetags=="1") {
?>
					<div id="div_query_tags" class="controlset" style="display:none">		
						<span class="label">Source tag</span>
						<div id="st_option">
						<div id="div_query_tags_values" style="width:100%">
<?php
/*
						foreach ($xml->tags->tag as $tag) {
							echo '<div><input type="checkbox" value="' . $tag . '" name="search_tag[]"/>' . $tag . '</div>';
						}
*/
?>
						</div>
						</div>
					</div>		
<?php 
//					} 
				} 
?>

<?php if ($usesourcename) { ?>					
						<div>
							<label for="search_org">Source name</label>														
							<input id="search_org" type="text" value="" name="search_org" />
						</div>
<?php } ?>

<?php if ($uselanguage) { ?>	
						<div>
							<label for="search_language">Language</label>														
							<select style="" name="search_language" id="search_language">
								<option value="">Any languages</option>
<?php
foreach ($aLanguagesForm as $key => $value)
{
	print( "<option value='" . $key . "'>" . $value . "</option>");
}
?>	
							</select>
						</div>
<?php } ?>

<?php if ($usecountry) { ?>	
						<div>
							<label for="search_country">Country</label>														
							<select style="" name="search_country" id="search_country">
								<option value="">Any country</option>
<?php
foreach ($aCountriesForm as $key => $value)
{
	print( "<option value='" . $key . "'>" . $value . "</option>");
}
?>							
							</select>
						</div>
<?php } ?>

<?php if ($usecontenttype) { ?>	
						<div>
							<label for="search_mimetype">Format</label>														
								<select style="" name="search_mimetype" id="search_mimetype">
									<option value="">Any format</option>
<?php
foreach ($aContentTypeForm as $key => $value)
{
	print( "<option value='" . $key . "'>" . $value . "</option>");
}
?>								
							</select>						
						</div>			
<?php } ?>	

<?php if ($uselocation) { ?>	
<div class="map">    
<input type="hidden" id="search_location_lat" type="text" value="" name="search_location_lat" />
<input type="hidden" id="search_location_lng" type="text" value="" name="search_location_lng" />
<label for="search_location_radius">Location</label>
 (click a point on the map) and provide a maximum distance
<br />
Distance : 
<select style="" name="search_location_radius" id="search_location_radius">
<option value="">Anywhere</option>
<option value="1">1 km</option>
<option value="5">5 km</option>
<option value="10">10 km</option>
<option value="50">50 km</option>
<option value="100">100 km</option>
<option value="200">200 km</option>
</select>						
</div>
<div id="map" class="map"></div>  
<script type="text/javascript">
	// Create a map object
	var map = new YMap(document.getElementById('map'));
	var currentGeoPoint = null;

	// Add map zoom (long) control  
	map.addZoomLong();  
	   
	// Add the Pan Control  
	map.addPanControl();  
	
		// Set map type to either of: YAHOO_MAP_SAT, YAHOO_MAP_HYB, YAHOO_MAP_REG
	map.setMapType(YAHOO_MAP_REG);

	// Display the map centered on a geocoded location
	map.drawZoomAndCenter("Paris, France", 3);

	// Add an event to report to our Logger  
	YEvent.Capture(map, EventsList.MouseClick, reportPosition);  

	function reportPosition(_e, _c){
		$("#search_location_lat").val(_c.Lat);
		$("#search_location_lng").val(_c.Lon);
		map.removeMarkersAll();
		currentGeoPoint = new YGeoPoint( _c.Lat, _c.Lon );  
		map.addMarker(currentGeoPoint);  		
	}
</script>
<?php  } ?>   																	
						</fieldset>												
					</div> <!--/search_advanced-->								

 					</form>
				</div> <!--/search-container-->
			</div> <!--/search-->
			<div id="result"></div>

	</div><!--/main-->

<?php 
	$footer = $config->get("application.footer");
	if ($footer!='') {
?>
	<div id="footer">
		<p>
		<?php echo $footer; ?>
    	</p>
	</div><!--/ footer-->
<?php 
	}
?>
		
</div><!--/wrapper-->

<div id="reader-dialog" style="display: none; cursor: default">
<div id="reader-dialog-title"
	style="padding: 10px; text-align: left; background-color: #808080;">Reader<input style="float: right;" type="button" onClick='$.unblockUI()' value="Fermer" /></div>
<div id="reader-dialog-text" style="padding: 10px; text-align: left; overflow: auto; max-height: 400px;">
</div>
</div>
	
</body>
</html>