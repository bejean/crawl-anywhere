<?php
//============================================================================
// (c) 2009-2010, Eolya - All Rights Reserved.
// This source code is the property of Eolya.
// The license applying to this source code is available at :
// http://www.crawl-anywhere.com/licenses/
//============================================================================

function db_exists($prefix='') 
{
    $exists = false;
    $db = db_connect ($config, "", "", "information_schema", $prefix);
    if ($db) 
    {    
        $stmt = new db_stmt_select("schemata");
        $stmt->addColumn ("count(*)"); 
        $stmt->setWhereClause("schema_name = '" . $config->get("database.dbname") . "'");
        
        $s = $stmt->getStatement();
        $rs = $db->Execute($s);
        if ($rs) 
        { 
            $exists = ($rs->fields[0] == "1");
        }  
    } 
    return $exists;   
}    


function db_connect ($config, $username, $password, $dbname, $prefix='') {

	if ($prefix!='') $prefix = $prefix . ".";
	
    $conn = &ADONewConnection($config->get($prefix . "database.adapter"));

    if ($conn)
    {    	
        if ($config->get($prefix . "database.port") != "")
        {
            $conn->port = $config->get($prefix . "database.port");
        }

        if ($username != "")
        {
            $loc_username = $username;
        }
        else
        {
            $loc_username = $config->get($prefix . "database.username");
        }

        if ($password != "")
        {
            $loc_password = $password;
        }
        else
        {
            $loc_password = $config->get($prefix . "database.password");
        }

        if ($dbname != "")
        {
            $loc_dbname = $dbname;
        }
        else
        {
            $loc_dbname = $config->get($prefix . "database.dbname");
        }
        $host = $config->get($prefix . "database.host");
        if ($conn->NConnect($host,$loc_username,$loc_password,$loc_dbname)) {
            $conn->Execute("set names 'utf8'");
            return $conn;
        }
        else
        {
        	$msg=$conn->ErrorMsg();
        	print($msg);
            return false;
        }
    }
    else {
        return false;
    }
}

class db_stmt_insert
{
    var $tablename;
    var $columns;
    var $values;

    function db_stmt_insert($tablename)
    {
        $this->tablename = $tablename;
    }

    function addColumnValue ($column, $value, $function)
    {
        //$value = str_replace("'", "''", $value); 
        //$value = str_replace("\\", "\\\\", $value); 
        
        if ($this->columns != "")
        {
            $this->columns .= ",";
        }
        $this->columns .= $column;

        if ($this->value != "")
        {
            $this->value .= ",";
        }

        if ($function != "")
        {
            $this->value .= $function . "(";
        }

        $this->value .= "'" . $value . "'";

        if ($function != "")
        {
            $this->value .=  ")";
        }
    }

    function getStatement () {
        return "INSERT INTO " . $this->tablename . "(" . $this->columns . ") VALUES (" . $this->value . ")";
    }
}

class db_stmt_update
{
    var $tablename;
    var $columnsvalues;
    var $whereclause;  

    function db_stmt_update($tablename)
    {
        $this->tablename = $tablename;
    }

    function addColumnValue ($column, $value, $function)
    {
        //$value = str_replace("'", "''", $value); 
        //$value = str_replace("\\", "\\\\", $value); 
        
        if ($this->columnsvalues != "")
        {
            $this->columnsvalues .= ",";
        }
        $this->columnsvalues .= $column . "=";


        if ($function != "")
        {
            $this->columnsvalues .= $function . "(";
        }

        $this->columnsvalues .= "'" . $value . "'";

        if ($function != "")
        {
            $this->columnsvalues .=  ")";
        }
    }
    
    function setWhereClause ($whereclause)
    {
        $this->whereclause = $whereclause;
    }
    
    function addWhereClause ($whereclause)
    {
        $this->whereclause = $this->whereclause . " " . $whereclause;
    }    

    function getStatement () {
        return "UPDATE " . $this->tablename . " SET " . $this->columnsvalues . " WHERE " . $this->whereclause;
    }
}

class db_stmt_select
{
    var $tablename;
    var $columns;
    var $whereclause;
    var $groupby;
    var $orderby;
    var $limit;
    //var $whereop = "WHERE";

    function db_stmt_select($tablename)
    {
        $this->tablename = $tablename;
        $this->whereclause = "";
        $this->orderby = "";
        $this->limit = ""; 
    }

    function addColumn ($column)
    {
        if ($this->columns != "")
        {
            $this->columns .= ",";
        }
        $this->columns .= $column;
    }

    function setWhereClause ($whereclause)
    {
        $this->whereclause = $whereclause;
    }
    
    function addWhereClause ($whereclause)
    {
        $this->whereclause = $this->whereclause . " " . $whereclause;
    }

    /*
    function setWhereOpToON() 
    {
        $this->whereop = "ON";
    }
    */
    
    function setOrderBy ($orderby)
    {
        $this->orderby = $orderby;
    }
    
    function setGroupBy ($groupby)
    {
        $this->groupby = $groupby;
    }

    function setLimit ($limit)
    {
        $this->limit = $limit;           
    }
    
    function getStatement () {
        $stmt = "SELECT " . $this->columns . " FROM " . $this->tablename;
        if ($this->whereclause != "")
        {
            //$stmt  .= " " . $this->whereop . " " . $this->whereclause;
            $stmt  .= " WHERE " . $this->whereclause;
        }
        if ($this->groupby != "")
        {
            $stmt  .= " GROUP BY " . $this->groupby;
        }
        if ($this->orderby != "")
        {
            $stmt  .= " ORDER BY " . $this->orderby;
        }
        if ($this->limit != "")
        {
            $stmt  .= " limit " . $this->limit;
        }
        return $stmt;
    }
}

class db_stmt_delete
{
    var $tablename;
    var $whereclause;

    function db_stmt_delete($tablename)
    {
        $this->tablename = $tablename;
        $this->whereclause = "";
    }

    function setWhereClause ($whereclause)
    {
        $this->whereclause = $whereclause;
    }

    function addWhereClause ($whereclause)
    {
        $this->whereclause = $this->whereclause . " " . $whereclause;
    }

    function getStatement () {
        $stmt = "DELETE FROM " . $this->tablename;
        if ($this->whereclause != "")
        {
            $stmt  .= " WHERE " . $this->whereclause;
        }
        return $stmt;
    }
}

function db_row_count($db, $table, $where) {
	$s = "select count(*) as cc from " . $table;
	if ($where!="") $s .= " where " . $where;
	$rs = $db->Execute($s);
	if (!$rs)
	{
		return -1;
	}
	return $rs->fields["cc"];
}

function db_get_value($db, $table, $field, $where, &$value) {
	$s = "select " . $field . " from " . $table . " where " . $where;
	$rs = $db->Execute($s);
	if (!$rs)
	{
		return -1;
	}
	if ($rs->RecordCount()!=1) return -1;
	$value = $rs->fields[$field];
	return 1;
}

function db_escape_value ($str) {
	return addslashes ( stripslashes ( $str ) );
}
?>
