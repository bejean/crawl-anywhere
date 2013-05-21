<?php

class User
{
	var $id;
	var $name;
	var $password;
	var $email;
	var $level;
	var $uuid;
	var $expiry;
	var $plan;
	var $postal_address;
	var $country_code;
	var $enabled;
	var $paiment_status;
	var $paiment_price_due;
	var $renew_plan;
	var $renew_date_end;
	var $tva_intra;
	var $cLog;


	function User()
	{
		$this->name = "";
		$this->level = -1;
		$this->cLog = NULL;
	}

	function isAdmin()
	{
		return ($level == 1);
	}

	function setId($id)
	{
		$this->id = $id;
	}

	function setLogger($cLog)
	{
		$this->cLog = $cLog;
	}

	function setName($name)
	{
		$this->name = $name;
	}

	function setLevel($level)
	{
		$this->level = $level;
	}

	function getId()
	{
		return $this->id;
	}

	function getName()
	{
		return $this->name;
	}

	function getPassword()
	{
		return $this->password;
	}

	function getEmail()
	{
		return $this->email;
	}

	function getLevel()
	{
		return $this->level;
	}

	function getUuid()
	{
		return $this->uuid;
	}

	function getExpiry()
	{
		$d = substr($this->expiry, 0, strpos($this->expiry, " "));
		return $d;
	}

	function getExpiryDays()
	{
		$expiry = strtotime($this->expiry);
		$today = time();
		return round(abs($today-$expiry)/60/60/24);
	}

	function getPlan()
	{
		return $this->plan;
	}

	function getAddress()
	{
		return $this->postal_address;
	}

	function getCountryCode()
	{
		return $this->country_code;
	}
	 
	function isEnabled() {
		return ($this->enabled == "1");
	}

	function isPaimentCompleted() {
		return ($this->paiment_status == "completed" || $this->paiment_status == "");
	}

	function getPaimentStatus() {
		return $this->paiment_status;
	}

	function getPaimentPriceDue() {
		return $this->paiment_price_due;
	}

	function isRenewing() {
		return ($this->renew_plan!="");
	}

	function getRenewingPlan() {
		return $this->renew_plan;
	}

	function getRenewingDateEnd() {
		return $this->renew_date_end;
	}

	function getTVA() {
		return $this->tva_intra;
	}

	function login($config, $name, $password, $password_crypted, $prefix='')
	{
		$db = db_connect ($config, "", "", "", $prefix);
		if ($db)
		{
			$stmt = new db_stmt_select("users");
			$stmt->addColumn("*");

			$whereclause = "user_name='" . $name . "'";
			$whereclause .= " and (user_password = '" . $password_crypted . "' or user_password = '" . $password . "')";
			$stmt->setWhereClause($whereclause);
			$s = $stmt->getStatement();
			if (!is_null($this->cLog)) $this->cLog->log_debug("User.inc.php - login - " .$s);
			$rs = $db->Execute($s);
			if ($rs)
			{
				if ($rs->RecordCount()==1)
				{
					$this->id = $rs->fields["id"];
					$this->name = $rs->fields["user_name"];
					$this->password = $password;
					$this->email = $rs->fields["user_email"];
					$this->level = $rs->fields["user_level"];
					$this->uuid = $rs->fields["uuid"];
					$this->expiry = $rs->fields["subscription_date_end"];
					$this->plan = $rs->fields["subscription_type"];
					$this->postal_address = $rs->fields["invoice_address"];
					$this->country_code = $rs->fields["invoice_country_code"];
					$this->enabled = $rs->fields["enabled"];
					$this->paiment_status = $rs->fields["paiment_status"];
					$this->paiment_price_due = $rs->fields["paiment_price_due"];
					$this->renew_plan = $rs->fields["renew_type"];
					$this->renew_date_end = $rs->fields["renew_date_end"];
					$this->tva_intra = $rs->fields["invoice_tva_intra"];

					// mise a jour login_lasttime
					$stmt = new db_stmt_update("users");
					$stmt->addColumnValue("login_lasttime", "", "now");
					$stmt->setWhereClause("id = '" .$this->id . "'");
					$s = $stmt->getStatement();
					$rs = $db->Execute($s);
					return true;
				}
			}
		}
		return false;
	}

	function load($config, $id, $prefix = '')
	{
		$db = db_connect ($config, "", "", "", $prefix);
		if ($db)
		{
			$stmt = new db_stmt_select("users");
			$stmt->addColumn("*");

			$whereclause = "id='" . $id . "'";
			$stmt->setWhereClause($whereclause);
			$s = $stmt->getStatement();
			$rs = $db->Execute($s);
			if ($rs)
			{
				if ($rs->RecordCount()==1)
				{
					$this->id = $rs->fields["id"];
					$this->password = decrypt($rs->fields["user_password"]);
					$this->name = $rs->fields["user_name"];
					$this->email = $rs->fields["user_email"];
					$this->level = $rs->fields["user_level"];
					$this->expiry = $rs->fields["subscription_date_end"];
					$this->plan = $rs->fields["subscription_type"];
					$this->postal_address = $rs->fields["invoice_address"];
					$this->country_code = $rs->fields["invoice_country_code"];
					$this->enabled = $rs->fields["enabled"];
					$this->paiment_status = $rs->fields["paiment_status"];
					$this->paiment_price_due = $rs->fields["paiment_price_due"];
					$this->renew_plan = $rs->fields["renew_type"];
					$this->renew_date_end = $rs->fields["renew_date_end"];
					$this->tva_intra = $rs->fields["invoice_tva_intra"];

					return true;
				}
			}
		}
		return false;
	}

	function reload($config, $prefix = '')
	{
		$db = db_connect ($config, "", "", "", $prefix);
		if ($db)
		{
			$stmt = new db_stmt_select("users");
			$stmt->addColumn("*");

			$whereclause = "id='" . $this->id . "'";
			$stmt->setWhereClause($whereclause);
			$s = $stmt->getStatement();
			$rs = $db->Execute($s);
			if ($rs)
			{
				if ($rs->RecordCount()==1)
				{
					$this->password = decrypt($rs->fields["user_password"]);
					$this->email = $rs->fields["user_email"];
					$this->level = $rs->fields["user_level"];
					$this->expiry = $rs->fields["subscription_date_end"];
					$this->plan = $rs->fields["subscription_type"];
					$this->postal_address = $rs->fields["invoice_address"];
					$this->country_code = $rs->fields["invoice_country_code"];
					$this->enabled = $rs->fields["enabled"];
					$this->paiment_status = $rs->fields["paiment_status"];
					$this->paiment_price_due = $rs->fields["paiment_price_due"];
					$this->renew_plan = $rs->fields["renew_type"];
					$this->renew_date_end = $rs->fields["renew_date_end"];
					$this->tva_intra = $rs->fields["invoice_tva_intra"];

					return true;
				}
			}
		}
		return false;
	}

}

?>
