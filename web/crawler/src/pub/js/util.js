function trim(str) {
    return str.replace(/^\s+/g,'').replace(/\s+$/g,'') 
}

function escapeHTML (str)
{
   var div = document.createElement('div');
   var text = document.createTextNode(str);
   div.appendChild(text);
   return div.innerHTML;
};


//function htmlEscape(str) {
//    return String(str)
//            .replace(/&/g, '&amp;')
//            .replace(/"/g, '&quot;')
//            .replace(/'/g, '&#39;')
//            .replace(/</g, '&lt;')
//            .replace(/>/g, '&gt;');
//}