<?php
require_once("../init.inc.php");
require_once_all('sources/*.inc.php');

$db = db_connect ($config, "", "", "");

$stmt = new db_stmt_select("sources");
$stmt->addColumn ("*");
$s = $stmt->getStatement();

$rs = $db->Execute($s);
if (!$rs)
{
	echo "Error while reading sources table !";
	exit();
}

$count = 0;
while (!$rs->EOF)
{
	$id = $rs->fields["id"];

	// load	
	db_get_value($db, "plugins", "class_php", "id=" . $rs->fields["type"], $class);
	$source = SourceFactory::createInstance($class, $config, $id_account_current, $db, null, null);
	$source->load($rs->GetRowAssoc());

	// delete
	$delstmt = new db_stmt_delete('sources');
	$delstmt->setWhereClause("id = '" . $id . "'");
	$s = $delstmt->getStatement();

	if (!$db->Execute($s))
	{
		echo 'Error while deleting record ' . $id;
		exit();
	}
	else
	{
		// insert
		$s = $source->getSqlStmt('insert');
		if (!$db->Execute($s))
		{
			echo 'Error while writing record ' . $id;
			echo '</br>Stop upgrade (' . $count . ' sources upgraded)';
			echo '</br>' . $db->ErrorMsg();
			echo '</br>' . $s;
			exit();
		}
		$count++;
	}
	$rs->MoveNext();
}
echo 'Upgrade successful (' . $count . ' sources upgraded)';

?>