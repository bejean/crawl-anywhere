<?php

$debug = false;

/*
 * Ask Solr to provide debug inbformation in its response
 * 
 * timing  -- Provide debug info about timing of components, etc. only
 * query   -- Provide debug info about the query only
 * results -- Provide debug info about the results (currently explains)
 * 
**/
$debug_solr = 'query';


/*
 * Solr core settings
**/ 
$solr_host="localhost";
$solr_port="8080";
$solr_baseurl="/solr/";
$solr_corename="crawler";


/*
 * Query settings
**/
$query_field = 'content_ml';
$item_per_page = 10;

$sort_with_query = ''; // for relance = ''
$sort_without_query = 'createtime desc';

/*
 * Faceting settings
**/
$facet_union = true;

// limit  : nombre max d'éléments retournés pour la facette
// mode   : union ou intersection pour le filtrage en recherche lorsque plusieurs valeurs sont sélectionnées.
// values : liste blanche des valeurs conservées dans la facette. Si tableau vide, alors on garde tout.
$facets_conf = array (
		//'tag' => array('limit' => '10', 'mode' => 'AND', 'values' => array('bmw', 'audi')),
		//'source_str' => array('limit' => '10', 'mode' => 'OR', 'values' => array('turbo.fr')),
		'tag' => array('limit' => '10', 'mode' => 'AND', 'values' => array()),
		'source_str' => array('limit' => '10', 'mode' => 'OR', 'values' => array()),
		'contenttyperoot' => array('limit' => '10', 'mode' => 'OR', 'values' => array()),
		'language' => array('limit' => '10', 'mode' => 'OR', 'values' => array()),
		'tag_cloud' => array('limit' => '120', 'mode' => 'OR', 'values' => array())
);

/*
 * Tag cloud settings
**/
$tag_cloud_size = 40;
$tag_cloud_nbhours = 168;
$tag_cloud_country = ''; 		// code langue en minuscule. voir fichier code_countries.txt dans le meme repertoire.
$tag_cloud_language = ''; 		// code pays en minuscule. voir fichier code_languages.txt dans le meme repertoire.
$tag_cloud_query_add = false;   // true : complete la requete courante, false : remplace la requete courante

/*
 * Hidden query and filters
**/
$hidden_query = "";
//$hidden_query = "test";
$hidden_filters = array (
//		'tag' => array('mode' => 'OR', 'values' => array('bmw', 'audi'))
//		//'source_str' => array('mode' => 'OR', 'values' => array('Turbo.fr'))
);

?>