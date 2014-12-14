<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================

$mg_last_error = '';

//============================================================================
function mg_get_last_error () {
	global $mg_last_error;
	return $mg_last_error;
}

//============================================================================
function mg_connect ($config, $username, $password, $dbname) {
	global $mg_last_error;
	
	$mg_last_error = '';
	$loc_port = "";
	if ($config->get("database.port") != "")
	{
		$loc_port = $config->get("database.port");
	}

	if ($username != "")
	{
		$loc_username = $username;
	}
	else
	{
		$loc_username = $config->get("database.username");
	}

	if ($password != "")
	{
		$loc_password = $password;
	}
	else
	{
		$loc_password = $config->get("database.password");
	}

	if ($dbname != "")
	{
		$loc_dbname = $dbname;
	}
	else
	{
		$loc_dbname = $config->get("database.dbname");
	}
	$loc_host = $config->get("database.host");

	// Specifying the authentication database in the connection URI (preferred)
	$url = "mongodb://";
	if (!empty($loc_username)) $url .= "$loc_username:$loc_password@";
	$url .= "$loc_host";
	if (!empty($loc_port)) $url .= ":$loc_port";
	$url .= "/$loc_dbname";
	try {
		$connection = new MongoClient($url);
		//$info = var_export ($connection, true);
		$db = $connection->selectDB ( $loc_dbname );
		return $db;
	} catch (Exception $e) {
		$mg_last_error = $e->getMessage();
	}
	return null;
}

//============================================================================
class mg_stmt_select
{
	var $collection;
	var $query;
	var $sort;
	var $fields;
	var $cursor;
	var $limit;

	function mg_stmt_select($mg, $collectionname)
	{
		try {
			$this->collection = $mg->selectCollection ( $collectionname );
			return $this->collection;
		} catch (Exception $e) {
		}
		return null;
	}

	function setQuery ($query)
	{
		$this->query = $query;
	}

	function setFields ($fields)
	{
		$this->fields = $fields;
	}

	function setSort ($sort)
	{
		$this->sort = $sort;
	}

	function setLimit ($limit)
	{
		$this->limit = $limit;
	}
	
	function execute($skip='', $limit='')
	{
		if (isset($this->query)) {
			if (isset($this->fieds)) {
				if (!empty($skip) && !empty($limit))
					$this->cursor = $this->collection->find( $this->query, $this->fieds)->limit($limit)->skip($skip);
				else
					$this->cursor = $this->collection->find( $this->query, $this->fieds);
			} else {
				if (!empty($skip) && !empty($limit))
					$this->cursor = $this->collection->find( $this->query)->limit($limit)->skip($skip);
				else
					$this->cursor = $this->collection->find( $this->query);
			}
		} else {
			$this->cursor = $this->collection->find();
		}
		if (!$this->cursor) return 0;
		return $this->cursor->count();
	}

	function getCursor()
	{
		if (isset($this->sort)) $this->cursor->sort( $this->sort );
		if (isset($this->limit)) $this->cursor->limit( intval($this->limit) );
		return $this->cursor;
	}
}



//============================================================================
class mg_stmt_distinct
{
	var $mg;
	var $collectionname;
	var $query;
	var $key;

	function mg_stmt_distinct($mg, $collectionname)
	{
		$this->mg = $mg;
		$this->collectionname = $collectionname;
	}

	function setQuery ($query)
	{
		$this->query = $query;
	}

	function setKey ($key)
	{
		$this->key = $key;
	}

	function command()
	{
		$ret = $this->mg->command(
				array(
						"distinct" => $this->collectionname,
						"key" => $this->key,
						"query" => $this->query
				)
		);
		return $ret['values'];
	}
}


//============================================================================
class mg_stmt_group
{
	//var $mg;
	var $collection;
	var $query;
	var $key;

	function mg_stmt_group($mg, $collectionname)
	{
		try {
			$this->collection = $mg->selectCollection ( $collectionname );
			return $this->collection;
		} catch (Exception $e) {
		}
		return null;
	}

	function setQuery ($query)
	{
		$this->query = $query;
	}

	function setKey ($key)
	{
		$this->key = $key;
	}

	function excute()
	{
		return $this->collection->group(
				$this->key,
				array("count" => 0),
				"function (obj, prev) { prev.count++; }",
				$this->query
				);
	}
}

//============================================================================
class mg_stmt_insert
{
	var $mg;
	var $collection;
	var $data;

	function mg_stmt_insert($mg, $collectionname, $defaults=NULL)
	{
		try {
			$this->collection = $mg->selectCollection ( $collectionname );
			$this->mg = $mg;
				
			if (!is_null($defaults) && is_array($defaults)) {
				foreach ($defaults as $k => $v) {
					$this->data[$k]=$v;
				}
			}
			return $this->collection;
		} catch (Exception $e) {}
		return null;
	}

	function addColumnValue ($field, $value)
	{
		if (!isset($this->data) || empty($this->data)) $this->data = array();
		if (!isset($value) || (empty($value) && !is_numeric($value))) return;
		$this->data[$field]=$value;
	}

	function addColumnValueDate ($field, $value = null)
	{
		if (!isset($this->data) || empty($this->data)) $this->data = array();
		if (!isset($value) || empty($value)) $value = new MongoDate();
		$this->data[$field]=$value;
	}
	
	function checkNotNull ($key)
	{
		if (!is_null($key) && is_array($key)) {
			foreach ($key as $k) {
				if (!isset($this->data[$k])) return false;
			}
		}
		return true;
	}

	function execute($generateId = true)
	{
		if ($generateId) {
			$coll_counters = $this->mg->selectCollection ( "counters" );
			$counter = $coll_counters->findAndModify(
					array("_id" => $this->collection->getName()),
					array('$inc' => array('seq' => 1)),
					array("seq" => true),
					array("new" => true)
			);
			$id = $counter["seq"];
			$this->data["id"]=$id;
		}
		$this->collection->insert($this->data);
	}
}

//============================================================================
class mg_stmt_update
{
	var $collection;
	var $query;
	var $data;
	var $multiple;

	function mg_stmt_update($mg, $collectionname, $defaults=NULL, $multiple=FALSE)
	{
		$this->multiple = multiple;
		try {
			$this->collection = $mg->selectCollection ( $collectionname );

			if (!is_null($defaults) && is_array($defaults)) {
				foreach ($defaults as $k => $v) {
					$this->data[$k]=$v;
				}
			}
			return $this->collection;
		} catch (Exception $e) {
		}
		return null;
	}

	function addColumnValue ($field, $value)
	{
		if (!isset($this->data) || empty($this->data)) $this->data = array();
		$this->data[$field]=$value;
	}

	function addColumnValueDate ($field, $value = null)
	{
		if (!isset($this->data) || empty($this->data)) $this->data = array();
		if (!isset($value) || empty($value)) $value = new MongoDate();
		$this->data[$field]=$value;
	}

	function setQuery ($query)
	{
		$this->query = $query;
	}

	function checkNotNull ($key)
	{
		if (!is_null($key) && is_array($key)) {
			foreach ($key as $k) {
				if (!isset($this->data[$k])) return false;
			}
		}
		return true;
	}
	
	function execute()
	{
		if ($this->multiple) {
			$this->collection->update($this->query, array ('$set' => $this->data), array("multiple" => true));
		}
		else {
			$this->collection->update($this->query, array ('$set' => $this->data));
		}
	}
}

//============================================================================
class mg_stmt_delete
{
	var $collection;
	var $query;
	

	function mg_stmt_delete($mg, $collectionname)
	{
		try {
			$this->collection = $mg->selectCollection ( $collectionname );
			return $this->collection;
		} catch (Exception $e) {
		}
		return null;
	}

	function setQuery ($query)
	{
		$this->query = $query;
	}

	function execute () {
		$this->collection->remove($this->query);
	}
}

//============================================================================
function mg_row_count($mg, $collectionname, $query) {
	$collection = $mg->selectCollection ( $collectionname );
	if (isset($query) && !empty($query))
		$cursor = $collection->find( $query );
	else
		$cursor = $collection->find();
	return $cursor->count();
}

//============================================================================
function mg_get_value($mg, $collectionname, $field, $query, &$value) {
	$collection = $mg->selectCollection ( $collectionname );
	$cursor = $collection->find( $query, array($field => true));
	if ($cursor->count()!=1) return -1;
	$rs = $cursor->getNext();
	$value = $rs[$field];
	return 1;
}

//============================================================================
function mg_create_index($mg, $collectionname, $field, $ascending = true) {
	$collection = $mg->selectCollection ( $collectionname );
	$order = 1;
	if (!$ascending) $order = 0;
	return $collection->ensureIndex(array($field => $order));   // deprecated MongoDB PHP driver >= 1.5 !!!
	//return $collection->createIndex(array($field => $order)); // starting MongoDB PHP driver >= 1.5 !!!
}
?>
