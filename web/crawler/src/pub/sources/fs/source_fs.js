function startCreation() {
    $("#sources").hide();
    $("#source_details").show();
	return true;
}

function checkSource() {

	var name = trim($("#source_name").val());
	var root_dir = trim($("#source_root_dir").val());
	var tag = trim($("#source_tag").val());
	
	if (name=="" || root_dir=="")
	{
		alert ("Source name and root directory cannot be empty !");
		return false;
	}


	if (tag.indexOf("_")!=-1) {
		alert ("The character \"_\" is not allowed in tag value");
		return false;
	}

	if (!json2XmlRule()) return false;
	json2XmlSchedule();

	return true;
	
}