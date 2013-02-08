<?php
$url = "mongodb://localhost/crawler";
$connection = new MongoClient($url);
$db = $connection->selectDB ( "crawler" );
$coll_counters = $db->selectCollection ( "counters" );
$counter = $coll_counters->findAndModify(
		array("_id" => "sources"),
		array('$inc' => array('seq' => 1)),
		array("seq" => true),
		array("new" => true)
);
$id = $counter["seq"];
var_dump($counter);
echo $id;
?>