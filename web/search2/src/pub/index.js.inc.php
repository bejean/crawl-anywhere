

<script type="text/javascript">
	<!--

	function getAjaxUrl() {
		return 'search.ajax.php?solr_url=<?php echo $theme->getSolrUrl(); ?>';
	}
	
	Array.prototype.in_array = function(p_val) {
		for(var i = 0, l = this.length; i < l; i++) {
			if(this[i] == p_val) {
				return true;
			}
		}
		return false;
	}

	function setSearchCrit(crit)
	{
		$("input#search_crit").val(crit);
		doSearch(0, "", "", "", false);
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

	function doLogout() {
		document.location.href="logout.php";
	}

	function doSearch(page, fq, fq_previous, id, ischeckbox, cols, tags)
	{
		var crit = $("input#search_crit").val();  

<?php 
	if (empty($search_default)) {
?>
		if (crit=="") return false;
<?php 
	}
?>
		$("input#page").val(page);  	

		$("input#bookmark_collection").val(cols);  	
		$("input#bookmark_tag").val(tags);  	

		var checked = false;
		if (id!="")
		{
			var checked = $("input#"+id).prop('checked'); // as of Jquery 1.9, change .attr by .prop
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
	    //$("#result").html("<br />&nbsp;<br /><center><img src='images/ajax-loader.gif'></center>"); 

		$.ajax({
			type: "post",
			data: $("#search_form").serialize(), // assuming this == the form
			url: getAjaxUrl(),
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

/*
	var data = google.visualization.arrayToDataTable([
			['Country', 'Popularity'],
			['Egypt', 18],
			['Syria', 7],
			['united states', 6],
			['Yemen', 6],
			['Mexico', 4],
			['Pakistan', 4],
			['Europe And Central Asia', 3],
			['Africa', 2],
			['Cambodia', 2]
			]);

	var options = {};
	options['dataMode'] = 'regions';

	var container = document.getElementById('map_canvas');
	var geomap = new google.visualization.GeoMap(container);
	geomap.draw(data, options);
*/

	        		
			}
		});	    
	}
	
	function doDidYouMeanSearch(crit)
	{
		$("input#search_crit").val(crit);  
		doSearch(0, "", "", "", false, "", "");
	}

	$(function() {  
		$("#search_do").click(function() {  
			var crit = $("input#crit").val();  
			if (crit != "") doSearch(0, "", "", "", false, "", "");
			return false;
		});  
	});  	
	
	function doBookmark(a)
	{ 
    	var bookmarkUrl = a.href;
    	var bookmarkTitle = a.title;
     
    	if (window.sidebar) { // For Mozilla Firefox Bookmark
    		window.sidebar.addPanel(bookmarkTitle, bookmarkUrl,"");
			return false;
		} else if( window.external || document.all) { // For IE Favorite
    		try {
    			window.external.AddFavorite( bookmarkUrl, bookmarkTitle);
    			return false;
			} catch(err){
    			alert('Your browser does not support this bookmark action.');
    	    }
    	//} else if(window.opera) { // For Opera Browsers
    	//	$("a.jQueryBookmark").attr("href",bookmarkUrl);
    	//	$("a.jQueryBookmark").attr("title",bookmarkTitle);
    	//	$("a.jQueryBookmark").attr("rel","sidebar");
    	} else { // for other browsers which does not support
    		 alert('Your browser does not support this bookmark action');
    	}
    	return true;
	} 	

	$(document).ready(function(){

<?php if ($theme->useTwitterBootstrap()) { ?>
		$('#search_crit').typeahead({
		    source: function(query, process) {
		        return $.ajax({
		            url: getAjaxUrl() + "&action=autocomplete&mode=tb",
		            type: 'get',
		            data: {q: query},
		            dataType: 'json',
		            success: function(json) {
		                //alert (json);
		                return typeof json.options == 'undefined' ? false : process(json.options);
		            }
		        });
		    }
		});
<?php } else { ?>
		$("#search_crit").suggest(getAjaxUrl() + "&action=autocomplete",{});
<?php } ?>
	});
	
	$(window).load(function(){
		// Le code placé ici sera déclenché
		// au chargement complet de la page.

		var cols = "";
		var tags = "";		

		var q = "<?php echo addslashes(getRequestParam("q")) ?>";				
		if (q!="") {
			cols = "<?php echo getRequestParam("c") ?>";
			tags = "<?php echo getRequestParam("t") ?>";
		}

<?php if ($usecollections) { ?>
		loadCollections(cols);
<?php } ?>
<?php if ($usetags) { ?>
		loadTags(tags);
<?php } ?>
<?php if ($usetagcloud) { ?>
		loadTagCloud();
<?php } ?>

		if (q!="")
		{
			$("#fq").val(""); 
			$("#search_crit").val(q); 
			
			var ql = "<?php echo getRequestParam("ql") ?>";
			$("#search_querylanguage").val(ql);
		
			var wv = "<?php echo getRequestParam("wv") ?>";
			if (wv=="1" && (ql=="en" || ql=="fr"))
			{
				$("#search_word_variations").prop("disabled", false);
				$("#search_word_variations").prop("checked", true);
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
		}
		doSearch(0, "", "", "", false, cols, tags);
		
	});

	function loadCollections(cols) {
	    $("#div_query_collections_values").html("<img src='images/ajax-loader.gif'>"); 
	    $.get(getAjaxUrl(), {action: 'fiedvalues', field: 'collection'}, 
	    	    function(data) {
	    	        //alert(data);
	    	        var aCols = cols.split(',');	    	        
	    	        var html = "";
	    	        var aCol = data.split('|');
	    	        for (var i=0;i<aCol.length;i++) {
		    	        if (aCol[i]!="") {
							var items = aCol[i].split(':');
			    	    	//html += '<div><input type="checkbox" value="' + items[0] + '" name="search_collection[]"/>' + items[0] + ' (' + items[1] + ')' + '</div>';
			    	    	html += '<span class="option_check"><input type="checkbox" value="' + items[0] + '" name="search_collection[]"';
			    	    	if (aCols.in_array(items[0])) html += ' checked="checked"';
			    	    	html += '/>' + items[0] + '</span>';
		    	        }
	    	        }
	    	        if (html == "") html = "<?php echo _('none') ?>";
	    	        $("#div_query_collections_values").html(html);
	    	    });    
	}
	
	function loadTags(tags) {
	    $("#div_query_tags_values").html("<img src='images/ajax-loader.gif'>"); 
	    $.get(getAjaxUrl(), {action: 'fiedvalues', field: 'tag'}, 
	    	    function(data) {
	    	        //alert(data);
	    	        var aTags = tags.split(',');
	    	        var html = "";
	    	        var aCol = data.split('|');
	    	        for (var i=0;i<aCol.length;i++) {
		    	        if (aCol[i]!="") {
							var items = aCol[i].split(':');
			    	    	//html += '<div><input type="checkbox" value="' + items[0] + '" name="search_tag[]"/>' + items[0] + ' (' + items[1] + ')' + '</div>';
			    	    	html += '<span class="option_check"><input type="checkbox" value="' + items[0] + '" name="search_tag[]"';
			    	    	if (aTags.in_array(items[0])) html += ' checked="checked"';
			    	    	html += '/>' + items[0] + '</span> ';
		    	        }
	    	        }
	    	        if (html == "") html = "<?php echo _('none') ?>";
	    	        $("#div_query_tags_values").html(html);
	    	    });    
	}

	function loadTagCloud() {
	    $("#tagcloud").html("<img src='images/ajax-loader.gif'>"); 
	    $.get(getAjaxUrl(), {action: 'gettagcloud', field: 'tag_cloud', uid: Math.random().toString(36).substr(2,9)}, 
	    	    function(data) {
	    	        //alert(data);
	    	        var html = data;
	    	        if (html == "") html = "<?php echo _('none') ?>";
	    	        $("#tagcloud").html(html);
	    	    });    
	}
	
	// Reader dialog =====================================
	function doReader(id, crit, lang)
	{
		readerShowDialog ();	

		$.ajax({
			type: "post",
			data: {action: 'gettext', id: id, search_crit: crit, search_querylanguage: lang}, 
			url: getAjaxUrl(),
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
			        top:            '10%', 
			        left:           '20%',
			        margin:   		'auto',
			        cursor:         'wait' 
			    }
		}); 
	}

	// Preferences dialog =====================================
	function doPreferences()
	{
		preferencesShowDialog ();	

		$.ajax({
			type: "post",
			data: {action: 'preferences_display'}, 
			url: getAjaxUrl(),
			success: function(data) {
				$("#preferences-dialog-text").html(data);
			}
		});
			    
	}

	function preferencesShowDialog () {
		//$("#reader-dialog-text").html("<center><img src='images/ajax-loader.gif'></center>");
		$.blockUI({ 
				message: $('#preferences-dialog'), 
				overlayCSS:  { 
	        		backgroundColor: '#000', 
	        		opacity:         0.2 
	    		}, 
			    css: { 
			        width:          '400px', 
			        top:            '10%', 
			        left:           '30%',
			        margin:   		'auto',
			        cursor:         'wait' 
			    }
		}); 
	}

	function preferencesCloseDialog() {

		var facet_union = $('#config_facet_union').prop('checked')?1:0;
		
		$.ajax({
			type: "post",
			data: {action: 'preferences_save', facet_union: facet_union}, 
			url: getAjaxUrl(),
			success: function(data) {
			}
		});
		
	 	$.unblockUI()
	}
	//-->
	</script>
