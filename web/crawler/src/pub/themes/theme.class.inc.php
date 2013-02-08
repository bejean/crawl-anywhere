<?php
interface iTheme {
	public function generateHtmlStart();
	public function generateTop($page);
	public function generateFooter();
}

abstract class ThemeBase {
	protected $config;
	protected $user;
	protected $mg;
	protected $current_account_id;
	
	function __construct($config, $user, $current_account_id, $mg) {
		$this->config = $config;
		$this->user = $user;
		$this->current_account_id = $current_account_id;
		$this->mg = $mg;
	}
	
	protected function generateMenuItem($page, $label, $minuserlevel, $currentpage, $pages_available) {
		$res = "";
		if ($this->isAvailableMenuItem($pages_available, $page, $this->config) && $this->user->getLevel() >= $minuserlevel) {
			if ($page==$currentpage)
			$res .= "<li class='active'>";
			else
			$res .= "<li>";
			$res .= "<a href='index.php?page=" . $page . "'>" . $label . "</a>";
			$res .= "</li>";
		}
		return $res;
	}
	
	protected function isAvailableMenuItem($items, $item)
	{
		if ((trim($items) == "") || ($items=="undefined")) return true;
	
		$aItems = explode(",", $this->config->get("pages.available"));
		foreach ($aItems as $value) {
			if (trim($value) == $item) return true;
		}
		return false;
	}
}
?>