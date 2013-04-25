<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================

class User
{
	var $id;
	var $name;
	var $level;
	var $id_account;
	var $change_password;

	function User()
	{
		$this->name = "";
		$this->level = -1;
	}

	function isAdmin()
	{
		return ($level == 1);
	}

	function setId($id)
	{
		$this->id = $id;
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

	function getChangePassword()
	{
		return $this->change_password;
	}

	function setChangePassword($val)
	{
		$this->change_password = $val;
	}

	function getName()
	{
		return $this->name;
	}

	function getLevel()
	{
		return $this->level;
	}

	function getIdAccount()
	{
		return $this->id_account;
	}

	private function login_internal($config, $name, $password)
	{
		$mg = mg_connect ($config, "", "", "");
		if ($mg)
		{
			$stmt = new mg_stmt_select($mg,"users");
			$query_username = array('user_name' => $name);
			if (isset($password) && !empty($password)) 
			{
				$query_password = array( '$or' => array( array('user_password' => md5($password)), array('user_password' => $password)));
				$query = array ('$and' => array($query_username, $query_password));
			}
			else 
			{
				$query = $query_username;
			}

			$stmt->setQuery($query);
				
			$count = $stmt->execute();
			if ($count==1) 
			{
				$cursor = $stmt->getCursor();
				$rs = $cursor->getNext();
					
				$this->id = $rs["id"];
				$this->name = $rs["user_name"];
				$this->level = intval($rs["user_level"]);
				$this->id_account = $rs["id_account"];
				$this->change_password = ($rs["change_password_next_logon"]=="1");

				// mise a jour login_lasttime
				$stmt = new mg_stmt_update($mg, "users");
				$stmt->addColumnValueDate ("login_lasttime");
				$stmt->setQuery(array('id' => $this->id));
				$stmt->execute();
				return true;
			}
		}
		return false;
	}

	function login($config, $name, $password) 
	{
		if (!isset($password) || empty($password)) return false;
		return $this->login_internal($config, $name, $password);
	}

	function loginAs($config, $name) 
	{
		return $this->login_internal($config, $name, null);
	}
}

?>
