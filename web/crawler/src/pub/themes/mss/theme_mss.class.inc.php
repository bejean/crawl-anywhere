<?php
class Theme extends ThemeBase implements iTheme {
	
	function generateHtmlStart() {
	
		$res =  '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">';
		$res .= '<html xmlns="http://www.w3.org/1999/xhtml">';
		$res .= '<head profile="http://gmpg.org/xfn/11">';
		$res .= '<title>' . $this->config->get("application.title") . '</title>';
		$res .= '<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" /> ';
		$res .= '<meta name="robots" content="index, nofollow" /> ';
		$res .= '<link rel="stylesheet" type="text/css" href="themes/common.css" />';
		$res .= '<link rel="stylesheet" type="text/css" href="themes/mss/optimize/style.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="themes/mss/styles.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" href="themes/menu.css" media="screen" />';
		$res .= '<link rel="stylesheet" type="text/css" media="all" href="themes/mss/optimize/css/effects.css" />';
	
		$res .= '<!--[if IE 6]>';
		$res .= '<script type="text/javascript" src="optimize/includes/js/pngfix.js"></script>';
		$res .= '<script type="text/javascript" src="optimize/includes/js/menu.js"></script>';
		$res .= '<link rel="stylesheet" type="text/css" media="all" href="themes/mss/optimize/css/ie6.css" />';
		$res .= '<![endif]-->	';
	
		$res .= '<!--[if IE 7]>';
		$res .= '<link rel="stylesheet" type="text/css" media="all" href="themes/mss/optimize/css/ie7.css" />';
		$res .= '<![endif]-->';
	
		$res .= '<link rel="canonical" href="http://manager.mysolrserver.com/" /> ';
		$res .= '<link href="themes/mss/optimize/styles/grey.css" rel="stylesheet" type="text/css" /> ';
		$res .= '<style type="text/css"> ';
		$res .= '#logo img { display:none; }';
		$res .= '#logo .site-title, #logo .site-description { display:block; } ';
		$res .= '</style> ';
		$res .= '<link href="themes/mss/optimize/custom.css" rel="stylesheet" type="text/css" /> ';
		$res .= '<!-- Woo Custom Styling --> ';
		$res .= '<style type="text/css"> ';
		$res .= '#top {background-repeat:no-repeat}';
		$res .= '</style> ';
	
		$res .= "            <script type='text/javascript' src='js/json2.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery-1.5.1.min.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery.url.packed.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery-ui-1.8.10.custom.min.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery.upload-1.0.2.min.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery.blockUI.js'></script>";
		$res .= "            <script type='text/javascript' src='js/util.js'></script>";
	
		$res .= "            <script type='text/javascript' src='sources/loadjs.js'></script>";
		$res .= "            <script type='text/javascript' src='sources/empty.js'></script>";
		return $res;
	}
	
	function generateTop($page) {
	
		//global $isEnterprise;
	
		$res = "";
	
		$res =  '<div id="top">';
		$res .= '    <div id="header">';
		$res .= '    <div class="col-full">';
		$res .= '        <div id="logo" class="fl">';
		$res .= '			<span class="site-title"><a href="' . $this->config->get("application.url") . '">' . $this->config->get("application.title") . '</a></span>';
		$res .= '    		<span class="site-description">A Solr Server just for you</span>';
		$res .= '        </div><!-- /#logo -->';
	
		//$res .= '<ul id="nav" class="fr">';
		//if (!$isEnterprise)
		//$res .= '	<li id="menu-item-46" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-46"><a href="http://www.mysolrserver.com/">Home</a></li> ';
		//$res .= '	<li id="menu-item-38" class="menu-item menu-item-type-custom menu-item-object-custom current-menu-item page_item page-item-41 current_page_item menu-item-38"><a href="' . $_SESSION["mysolrserver_url"] . '">Manager</a></li>';
		//if (!$isEnterprise)
		//$res .= '	<li id="menu-item-45" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-45"><a href="http://www.mysolrserver.com/support/">Support</a></li> ';
		//$res .= '</ul>       ';
		$res .= '    </div><!-- /.col-full -->';
		$res .= '    </div><!-- /#header -->';
		$res .= '    <div id="featured">';
		$res .= '        <div id="page-title" class="col-full">';
		$res .= '            <h1>Manager</h1>';
		$res .= '        </div>';
		$res .= '   </div>    ';
		$res .= '</div><!-- /#top -->  ';
	
		$res .= $this->generateTopMenu($page);
	
		$res .= '<div id="content">  ';
		$res .= '<div class="col-full">  ';
		$res .= '	<div id="main" class="fullwidth">  ';
		$res .= '            <div class="post">   ';
		$res .= '                <div class="entry">  ';
	
		return $res;
	}
	
	function generateTopMenu($page) {
	
		$res = '<div id="menu">  ';
		$res .= "<div class='col-full'>";
		$res .= "    <ul id='menu'>";
	
		$pages_available = $this->config->get("pages.available");
	
		$res .= $this->generateMenuItem("sources", "Sources", 0, $page, $pages_available, $this->config, $this->user);
	
		$res .= $this->generateMenuItem("status", "Status", 0, $page, $pages_available, $this->config, $this->user);
	
		if ($this->isAvailableMenuItem($pages_available, "search", $this->config)) {
			$search_rooturl = $this->config->getDefault( "search.rooturl", "" );
			if ($search_rooturl!="") $res .= "<li><a href='" . $search_rooturl . "' target='search'>Search</a></li>";
		}

		if ($this->user->getLevel()=="2")
		$res .= $this->generateMenuItem("manage", "Manage", 0, $page, $pages_available, $this->config, $this->user);
		else
		$res .= $this->generateMenuItem("manage", "My account", 0, $page, $pages_available, $this->config, $this->user);
	
		$res .= $this->generateMenuItem("logout", "Logout", 0, $page, $pages_available, $this->config, $this->user);
	
		$res .= "<li class='large'>";
		$res .= '<a href="' . $_SESSION["mysolrserver_url"] . '">Back to manager</a>';
		$res .= "</li>";
	
		$res .= "    </ul>";
		$res .= "</div></div>";
	
		return $res;
	}
	
	function generateFooter() {
	
		//global $isEnterprise;
	
		$res =  '               	</div><!-- /.entry --> ';
		$res .= '            </div><!-- /.post --> ';
		$res .= '	</div><!-- /#main --> ';
		$res .= '</div><!-- /.col-full --> ';
		$res .= '</div><!-- /#content --> ';
	
		$res .= '<div id="footer">';
		$res .= '<div class="col-full">';
		$res .= '    <div id="footer-credits">';
		$res .= '	<div id="copyright" class="col-left">';
		//if (!$isEnterprise) {
		//	$res .= '    	<p>&copy; 2011 My Solr Server. All Rights Reserved.&nbsp;&nbsp;-&nbsp;&nbsp;<a href="http://www.mysolrserver.com/legal/">Legal</a></p>';
		//} else {
			$footer = $this->config->getDefault("application.footer",'');
			if ($footer!='')
			{
				$res .= "	<p>" . $footer . "</p>";
			}
		//}
		$res .= '    </div>';
		$res .= '	</div><!-- /#footer-widgets -->';
		$res .= '</div><!-- /.col-full -->';
		$res .= '</div><!-- /#footer -->';
	
		return $res;
	}
	
	
}

?>