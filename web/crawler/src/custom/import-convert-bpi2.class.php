<?php
class ImportConvert {
	var $_filename;
	//var $_header = null;
	
	
	function __construct($filename) {
		$this->_filename = $filename;
	}
	
	function stripAccents($stripAccents){
		return strtr($stripAccents,'àáâãäçèéêëìíîïñòóôõöùúûüýÿÀÁÂÃÄÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝ','aaaaaceeeeiiiinooooouuuuyyAAAAACEEEEIIIINOOOOOUUUUY');
	}
	
// 	function stripAccents($str) {
// 		return strtr(utf8_decode($str), utf8_decode('àáâãäçèéêëìíîïñòóôõöùúûüýÿÀÁÂÃÄÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝ'), 'aaaaaceeeeiiiinooooouuuuyyAAAAACEEEEIIIINOOOOOUUUUY');
// 	}
	
	function getXml() {
		
		// lit les tableaux de conversion de code langue
		$arr639_1 = array();
		$handle = fopen ( "639-1.csv", "r" );
		if ($handle === FALSE) $handle = fopen ( "../../custom/639-1.csv", "r" );
		if ($handle !== FALSE) {
			while ( ($data = fgetcsv ( $handle, 10000, "," )) !== FALSE ) {
				$arr639_1[trim($data[1])]=trim($data[0]);
			}
		}
		$arr639_2 = array();
		$handle = fopen ( "639-2.csv", "r" );
		if ($handle === FALSE) $handle = fopen ( "../../custom/639-2.csv", "r" );
		if ($handle !== FALSE) {
			while ( ($data = fgetcsv ( $handle, 10000, "," )) !== FALSE ) {
				$arr639_2[utf8_decode($this->stripAccents(trim($data[1])))]=trim($data[0]);
			}
		}
		$arr3166 = array();
		$handle = fopen ( "3166-alpha-2.csv", "r" );
		if ($handle === FALSE) $handle = fopen ( "../../custom/3166-alpha-2.csv", "r" );
		if ($handle !== FALSE) {
			while ( ($data = fgetcsv ( $handle, 10000, "," )) !== FALSE ) {
				$arr3166[utf8_decode($this->stripAccents(trim($data[1])))]=trim($data[0]);
			}
		}
		
		if (($handle = fopen ( $this->_filename, "r" )) !== FALSE) {
			$header = null;
			$xml = new SimpleXMLElement('<?xml version="1.0" encoding="UTF-8"?><crawlanywhere></crawlanywhere>');
			$version = $xml->addChild('version');
			$sources = $xml->addChild('sources');
			while ( ($data = fgetcsv ( $handle, 10000, "," )) !== FALSE ) {
				if (empty($header)) {
					$header = $data;
				} else {
					//$data = array_combine ($header, $data);
					$data = $this->combine_arr ($header, $data);
					if ($data) {
						$source = $sources->addChild('source');
						$this->processLine($source, $data, $arr639_1, $arr639_2, $arr3166);
						//$src = var_dump($source);
					} else {
						$i=0;
					}
					
				}
			}
			fclose ( $handle );
			return $xml;
		}
		return null;
	}
		
	function processLine($source, $data, $arr639_1, $arr639_2, $arr3166) {
		
		$metas = array('metadata');
		
		$url_str = $data['starting_url'];
		$url = parse_url($url_str);
		if (count($url)==1) return;
		
		$name = $data['name'];
		$name = htmlspecialchars(trim(str_replace('¶', ' ', $name)));
		if (empty($name)) return;
					
		$source->addChild('type', '1');
		$source->addChild('id_target', '1');
		$source->addChild('name', $name);
		
		$country = $data['country'];
// 		if (strtolower($country)=='ukrainien') {
// 			$country = $country;
// 		}
		if (!empty($country)) $country = $arr3166[$this->stripAccents($country)];
		if (empty($country)) $country = 'fr';
		$source->addChild('country', strtoupper($country));	
		
		$lan = $data['language'];
		if (strpos($lan, ';')===false) {
			if (!empty($lan)) $lan = $arr639_2[$this->stripAccents($lan)];
			if (!empty($lan)) $lan = $arr639_1[$lan];
			if (empty($lan)) $lan = 'fr';
		} else {
			$lan='xx';
		}
		$source->addChild('language', strtolower($lan));
			
		$collection = strtolower($data['collection']);
		$collection = str_replace(',', '-', $collection);
		//$collection = str_replace(';', ',', $collection);
		$collection = implode(',', array_unique(array_map('trim', explode(';', $collection))));
		$collection = htmlspecialchars($collection);
		$source->addChild('collection', $collection);
		
		$host = htmlspecialchars($url['host']);
		$url_str = htmlspecialchars($url_str);
		
		$metadata='';
		foreach ($metas as $meta) {
			$metadata .= htmlspecialchars('bpi_' . $meta . ':' . preg_replace( "/\r|\n/", " ",$data[$meta]));
		}
		
		// param
		//		    	<collection>$collection</collection>
		
		$params = <<<EOD
		 	<params>
				<language_detection_list></language_detection_list>
		 		<crawl_maxdepth>2</crawl_maxdepth>
		    	<crawl_url_concurrency>0</crawl_url_concurrency>
		   		<crawl_url_per_minute>0</crawl_url_per_minute>
		    	<automatic_cleaning>4</automatic_cleaning>
		    	<crawl_schedule><schedules/></crawl_schedule>
		    	<crawl_filtering_rules><rules/></crawl_filtering_rules>
		    	<metadata>$metadata</metadata>
		    	<contact></contact>
		    	<comment></comment>
		    	<url_host>$host</url_host>
		    	<alias_host></alias_host>
		    	<protocol_strategy>1</protocol_strategy>
		    	<checkdeleted_strategy>0</checkdeleted_strategy>
		    	<url>
		    		<urls>
		    			<url>
		    				<url>$url_str</url>
		    				<mode>s</mode>
		    				<allowotherdomain>0</allowotherdomain>
		    				<onlyfirstcrawl>0</onlyfirstcrawl>
		    			</url>
		   			</urls>
		   		</url>
		   		<user_agent></user_agent>
		   		<url_ignore_fields></url_ignore_fields>
		   		<url_ignore_fields_no_session_id></url_ignore_fields_no_session_id>
		   		<crawl_childonly>2</crawl_childonly>
		   		<auth_mode>0</auth_mode><auth_login></auth_login><auth_passwd></auth_passwd><auth_param></auth_param>
		   	</params>
EOD;
		$source->addChild('params', base64_encode($params));
		return null;
	}
	
	function combine_arr($a, $b) {
		$acount = count($a);
		$bcount = count($b);
		$size = ($acount > $bcount) ? $bcount : $acount;
		$a = array_slice($a, 0, $size);
		$b = array_slice($b, 0, $size);
		return array_combine($a, $b);
	}
	//$combined = combine_arr($abbreviations, $states);
}


//$converter = new ImportConvert('/Users/bejean/CloudStation/Clients/Bpi/2014/04/websites.csv');
//$xml = $converter->getXml();
//$i=0;
?>