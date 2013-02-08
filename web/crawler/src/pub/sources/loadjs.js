var currentfilename='sources/empty.js';

function replacejsfile(type_mnemo){

	var newfilename = 'sources/' + type_mnemo + '/source_' + type_mnemo + '.js';
    
    var replacedelements=0;
	var targetelement="script";
	var targetattr="src";
	var allsuspects=document.getElementsByTagName(targetelement);
	for (var i=allsuspects.length; i>=0; i--){ //search backwards within nodelist for matching elements to remove
		if (allsuspects[i] && allsuspects[i].getAttribute(targetattr)!=null && allsuspects[i].getAttribute(targetattr).indexOf(currentfilename)!=-1){
			var newelement=document.createElement('script');
			newelement.setAttribute("type","text/javascript");
			newelement.setAttribute("src", newfilename);
			allsuspects[i].parentNode.replaceChild(newelement, allsuspects[i]);
			currentfilename = newfilename;
			replacedelements+=1;
		}
	}
	if (replacedelements==0)
		alert("Replaced 0 instance of "+currentfilename+" with "+newfilename);
	if (replacedelements>1)
		alert("Replaced "+replacedelements+" instances of "+currentfilename+" with "+newfilename);
}