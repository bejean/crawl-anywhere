function startCreation() {
    $("#sources").hide();
    $("#source_details").show();
	return true;
}

function checkSource() {

	var name = trim($("#source_name").val());
	//var alphabet = name.charAt(0);
	var url = trim($("#source_url").val());
	var host = trim($("#source_host").val());
	var tag = trim($("#source_tag").val());
	
	if (name=="" || url=='{ "urls": [] }' || host=="")
	{
		alert ("Source name, source host and starting url cannot be empty !");
		return false;
	}

	if (!host.match(/^[a-z0-9_\.-]+$/i)) {
		alert ("Invalid host name ! Don't specif protocol. Something like www.site.com expected.");
		return false;
	}

	if (tag.indexOf("_")!=-1) {
		alert ("The character \"_\" is not allowed in tag value");
		return false;
	}

	if (!json2XmlUrl()) return false;
	json2XmlRule();
	json2XmlSchedule();

	return true;
	
}