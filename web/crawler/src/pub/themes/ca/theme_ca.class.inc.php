<?php
class Theme extends ThemeBase implements iTheme {
		
	public function generateHtmlStart() {
		$res =  "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";
		$res .= "<html>\n";
		$res .= "        <head>\n";
		$res .= "            <meta http-equiv='Content-Type' content='text/html; charset=utf-8'/>";
		$res .= "            <title>" . $this->config->get("application.title") . "</title>";
		$res .= "            <meta http-equiv='Cache-Control' content='no-cache' />";
		$res .= "            <meta http-equiv='Pragma' content='no-cache' />";
		$res .= "            <meta http-equiv='Cache' content='no store' />";
		$res .= "            <meta http-equiv='Expires' content='0' />";
		$res .= "            <meta name='robots' content='index, nofollow' />";
		$res .= "            <link href='themes/common.css' rel='stylesheet' type='text/css' />";
		$res .= "            <link href='themes/ca/styles.css' rel='stylesheet' type='text/css' />";
		$res .= "            <link href='themes/menu.css' rel='stylesheet' type='text/css' />";
		$res .= "            <script type='text/javascript' src='js/json2.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery-1.5.1.min.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery.blockUI.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery.url.packed.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery-ui-1.8.10.custom.min.js'></script>";
		$res .= "            <script type='text/javascript' src='js/jquery.upload-1.0.2.min.js'></script>";
		$res .= "            <script type='text/javascript' src='js/util.js'></script>";
		$res .= "            <script type='text/javascript' src='js/php-js.js'></script>";
		
		$res .= "            <script type='text/javascript' src='sources/loadjs.js'></script>";
		$res .= "            <script type='text/javascript' src='sources/empty.js'></script>";
		return $res;
	}

	public function generateTop($page) {

		$res =  "<div id='header'>";
		$res .= "    <h1 id='header'>" . $this->config->get("application.title") . "</h1>";
		$res .= "</div>";
		$res .= "<div>";
		$res .= "    <ul id='menu'>";

		$pages_available = $this->config->get("pages.available");

		$res .= $this->generateMenuItem("status", "Status", 0, $page, $pages_available, $this->user);

		$res .= $this->generateMenuItem("sources", "Sources", 0, $page, $pages_available, $this->user);

		if ($this->isAvailableMenuItem($pages_available, "search", $this->config)) {
			$search_rooturl = $this->config->getDefault( "search.rooturl", "" );
			if ($core_url!="") {
				$key = base64_encode ($core_url);
				$search_rooturl .= "?key=" . $key;
			}
			if ($search_rooturl!="") $res .= "<li><a href='" . $search_rooturl . "' target='search'>Search</a></li>";
		}

		if ($this->user->getLevel()=="2") {
			$res .= $this->generateMenuItem("manage", "Manage", 0, $page, $pages_available, $this->user);
			if ($this->isAvailableMenuItem($pages_available, "mongodb", $this->config)) {
				$mongodb_rooturl = $this->config->getDefault( "mongodb.rooturl", "" );
				if ($mongodb_rooturl!="") $res .= "<li><a href='" . $mongodb_rooturl . "' target='mongodb'>MongoDB</a></li>";
			}
		}
		else {
			$res .= $this->generateMenuItem("manage", "My account", 0, $page, $pages_available, $this->user);
		}

		$res .= $this->generateMenuItem("logout", "Logout", 0, $page, $pages_available, $this->user);

		$res .= "    </ul>";
		$res .= "</div>";
		$res .=  "<div id='account_selector'><div>";
		$res .= "<form id='account_selector' name ='account_selector' method='POST' action='index.php?page=status'>";
		$res .= "    <b>Logged in as </b> " . $this->user->getName() . " (" . getUserLevelLabel($this->user->getLevel()) . ")";
		if (($this->user->getLevel()=="2") && (mg_row_count($this->mg, "accounts", "") > 1)) {
			$res .= "&nbsp;-&nbsp;<b>Account </b> ";
			$aAccounts = getAvailableAccounts($this->config);
			if ($aAccounts!=null) {
				$res .= "<select id='id_account' name='id_account' style='editInputSelect' onChange='accountOnChange();'>";
				foreach ($aAccounts as $key => $value)
				{
					$res .= "<option value='" . $key . "'";
					if ($key==$this->id_account_current) $res .= " selected";
					$res .= ">" . $value . "</option>";
				}
				$res .= "</select>";
			}
		}
		$res .= "</form>";
		$res .= "</div>";
		$res .= "</div>";

		return $res;
	}

	public function generateFooter() {

		$res  = "<br/><br/>";

		$footer = $this->config->get("application.footer");
		if ($footer!='')
		{
			$res .= "<div id='footer'>";
			$res .= "	<p>" . $footer . "</p>";
			$res .= "</div>";
		}
		return $res;
	}

}

?>