<?php
/**
 * Online Web Application Builder Classes
 * @package 	OWAB
 */
/**
 * ConfigTool
 *
 * Part of OWAB (Online Web Application Builder) class set.
 *
 * ConfigTool reads normal text file to get all name value pairs, that are ment
 * to use on anykind of php application. In addition to read files, separate comments,
 * automatic variable type scanning through prefix in keys, ConfigTool can be used to
 * add, edit and delete key value pairs and save them to the file. Even new files can be
 * created from the scratch or based on earlier configuration text files.
 *
 * One additional usage with the class is to get language specific information from
 * the external text file. Then you can make straight object references to the
 * variables. Please see language_example.php for more information
 *
 * Special features
 *
 * By starting key name in a config.txt file with one of the five predefined prefixes
 * user can make automatic key conversion to different data types. Five prefixes are:
 * b_ like boolean, s_ like string, a_ like array, d_ like double and i_ like integer.
 *
 * {@internal	Now, if AUTOBOOLEAN, AUTOSTRING, AUTOARRAY, AUTODOUBLE and AUTOINTEGER
 * are set true in field variables (as they are by default), class converts the value
 * accordant to the key to proper data type.}}
 *
 * Normally all values are in string format on text file.
 * With boolean you can use one of the four values to get boolean work on automatic
 * conversion: true/false, on/off, yes/no, 1/0, y/n. So use these values on a text file.
 * More about configuration file format is described on {@link Config_definitions.txt} file.
 * Commenting is also discussed there more precisely.
 *
 * To debug and check key value pair, user can use getConfigLines() method to get
 * an array presentation of all lines with or without comments.
 *
 * Here is a small example, how to use this class:
 * <code>
 * <?php
 * include( "path/to/class/ConfigTool.php" );
 * $conf = new ConfigTool();
 * $conf->setConfigFromFile( "config.txt" );
 * // now you can get key value by direct reference
 * echo $conf->name_of_variable;
 * // to get all config lines for checking and debugging use:
 * $ar = $conf->getConfigLines();
 * // other useful methods are adding, updating, deleting and saving modified
 * // please see function descriptions, tests, and examples to know better, how
 * // they work.
 * ?>
 * </code>
 *
 * There are some known specialities on this class. First, it's not fully directed by OOP
 * form because it uses straight references to class field variables (not encapsulating).
 * And that's a main reason why developer must disable notice reporting if active in
 * php.ini. This is done either straight to the php.ini file (refer to http://www.php.net),
 * or by setting error_reporting( E_ERROR | E_WARNING | E_PARSE ) at runtime. Please see
 * further instructions from {@link http://www.php.net/error_reporting } and examples that
 * follows with the ConfigTool package {@link http://www.hmv-systems.fi/marko/ConfigTool/}.
 *
 * Another issue is to set write permission to the used configuration file(s). Procedure will
 * change on different servers and platforms, but mainly it's important to know, that many php
 * service providers has php in safe-mode. That means you cannot change/write to files from php
 * script. One possibility is to use ftp-tool/-application for file transfer, because most
 * ftp-tools has opportunity to change file permissions too. One good program is SmartFtp. You
 * may find program from {@link http://www.smartftp.com/ } and some figures at
 * {@link http://www.hmv-systems.fi/marko/ConfigTool/docs/smart.html }
 * how to change permissions with that program. Of course you will need ftp access to your
 * webserver to use it. But that's an another story if you don't have. ;)
 *
 * Changed from version 0.01:
 *
 * - Added AUTODOUBLE field variable and _toDouble type converter function. Added reserved word
 *   likewise.
 * - Set flags method has been changed from boolean parameter set to array set. This is
 *   !!!FUNDAMENTAL!!! change, so applications that changes flags with older method, has to be
 *   modified to newer proper one.
 * - Added _doubleToString(), _integerToString(), _checkType() & _checkAutoType() methods.
 * - Added setFileName() method for changing file name and creating new configuration file.
 * - Added _fileWrite() method for checking CHMOD at save and new file creation procedure. And
 *   that affects to ->
 * - Public saveToFile() method simplified to use above mentioned _fileWrite method!
 * - Added get() method to get safely variable name. This is due to notice error, if you refer to
 *   variable in object scope, if it's not defined. With get method you will get 'undefined' if
 *   variable does not exist
 *
 * @author		Marko Manninen <marko.manninen@hmv-systems.fi>
 * @copyright 	Copyright (c) 2004, Marko Manninen
 * @license		http://opensource.org/licenses/gpl-license.php GNU Public License
 * @version		0.02
 * @todo		- Allow multiline variables -> long string sentences
 * 				- Save config file to other known formats like XML, ini, apache,...
 * 				- Tie comments and their key value pairs together. Optimize & better comment code.
 * 				- Comments and variable add to the appropriate line!
 * @package 	OWAB
 */
class ConfigTool
{
	/**
	 * Empty spaces between key string and value string
	 * Thought too obvious to mention, = mark is between them
	 * @access private
	 * @var int
	 */
	var $INDENT;
	/**
	 * Path and name of the configuration file
	 * @access private
	 * @var string
	 */
	var $FILENAME;
	/**#@+
	 * @access private
	 */
	/**
	 * All lines in array that are read from the configuration file
	 * @var array
	 */
	var $LINES;
	/**
	 * Reserved words list, that will be checked runtime
	 * These words cannot be used in configuration file!
	 * @var array
	 */
	var $RESERVED_WORDS;
	/**#@-*/
	/**#@+
	 * @access private
	 */
	/**
	 * Convert key type to boolean if key starts with b_
	 * @var bool
	 */
	var $AUTOBOOL;

	/**
	 * Convert key type to integer if key starts with i_
	 * @var bool
	 */
	var $AUTOINTEGER;
	/**
	 * Convert key type to integer if key starts with i_
	 * @var bool
	 */
	var $AUTODOUBLE;
	/**
	 * Convert key type to array if key starts with a_
	 * @var bool
	 */
	var $AUTOARRAY;
	/**
	 * Convert key type to string if key starts with s_
	 * @var bool
	 */
	var $AUTOSTRING;
	/**#@-*/
	/**
	 * ConfigTool constructor
	 *
	 * @internal	Constructor inits the class fields with wanted values.
	 * Reserved words should be changed by the developer only.
	 */
	function ConfigTool()
	{
		$this->LINES				= array();
		$this->FILENAME				= "";
		// if any new fields are inserted to the class
		// the name of the filed must be added to the RESERVED_WORD list!
		$this->RESERVED_WORDS   	= array( "LINES", "INDENT", "FILENAME", "RESERVED_WORDS", "AUTOBOOL", "AUTOINTEGER", "AUTODOUBLE", "AUTOARRAY", "AUTOSTRING" );
		$this->INDENT				= 25;
		$this->setFlags();
	}//END OF CONSTRUCTOR ConfigTool

	/**
	 * Set flags
	 * Internal key type conversion is activated throught these values
	 * If defauld flags are to be changed, flags must be set before calling
	 * setConfigFromFile() method
	 * Changed to include parameter as array type. That way it's more easier to
	 * set flags, because you don't need to remember the order of parameters.
	 * Array should be form of associative name values. If some of autotype is
	 * missing, then former autotype value will remain. All types will be true
	 * as default!
	 * @access public
	 * @param array $pAuto
	 */
	function setFlags( $pAuto = array( "AUTOBOOL" => true, "AUTOINTEGER" => true, "AUTODOUBLE" => true, "AUTOARRAY" => true, "AUTOSTRING" => true ) )
	{
		if( isset( $pAuto['AUTOBOOL'] ) )
		$this->AUTOBOOL = $pAuto['AUTOBOOL'];
		if( isset( $pAuto['AUTOINTEGER'] ) )
		$this->AUTOINTEGER = $pAuto['AUTOINTEGER'];
		if( isset( $pAuto['AUTODOUBLE'] ) )
		$this->AUTODOUBLE = $pAuto['AUTODOUBLE'];
		if( isset( $pAuto['AUTOARRAY'] ) )
		$this->AUTOARRAY = $pAuto['AUTOARRAY'];
		if( isset( $pAuto['AUTOSTRING'] ) )
		$this->AUTOSTRING = $pAuto['AUTOSTRING'];
	}//END OF FUNCTION setFlags

	/**
	 * Get indent, the number of empty spaces between key name and value
	 * With this value, you can set configuration file variables to look
	 * visually better. That means, it's easier to read.
	 * @access public
	 * @return integer
	 */
	function getIndent()
	{
		return $this->INDENT;
	}//END OF FUNCTION getIndent

	/**
	 * Set indent, the number of empty spaces between key name and value
	 * With this value, you can set configuration file variables to look
	 * visually better. That means, it's easier to read.
	 * @access public
	 * @param string $pIndent
	 */
	function setIndent( $pIndent )
	{
		$this->INDENT = $pIndent;
	}//END OF FUNCTION setIndent

	/**
	 * Set file name of the configuration file. If file name is other than
	 * defined in setConfigFromFile( $pFileName ) and user is saving
	 * configuration variables with saveToFile(), then new file is tried to
	 * create.
	 * @see _fileWrite()
	 * @access public
	 * @param string $pfileName
	 */
	function setFileName( $pfileName )
	{
		$this->FILENAME = $pfileName;
	}//END OF FUNCTION getConfigLines

	/**
	 * cookies support
	 */
	function setCookiePath($path) {
		$this->cookiePath=$path;
	}
	function setCookieDomain($domain) {
		$this->cookieDomain=$domain;
	}
	function setCookieExpire($expire) {
		$this->cookieExpire=time() + $expire;
	}
	function isCookieEnabled() {
		return (isset($this->cookiePath)  && isset($this->cookieExpire)); // && isset($this->cookieDomain)
	}
	function setCookie( $pKey, $val )
	{
		$cookie_key= str_replace ( "." , "_" , $pKey);
		setcookie ( $cookie_key, $val, $this->cookieExpire, $this->cookiePath); //, $this->cookieDomain
	}

	/**
	 * Get configuration file name
	 * @access public
	 * @return string
	 */
	function getConfigFileName()
	{
		return $this->FILENAME;
	}//END OF FUNCTION getConfigFileName
	
	
	/**
	 * Get safely variable value. If variable is not defined, method will return 'undefined'
	 * @access public
	 * @param string $pKey
	 * @return mixed
	 */
	function get( $pKey, $checkCookie = false )
	{
		global $_COOKIE;
		if( $this->isDefined( $pKey ) ) {
			$cookie_key= str_replace ( "." , "_" , $pKey);
			if ($checkCookie && $this->isCookieEnabled() && isset($_COOKIE[$cookie_key])) {
				$val = $_COOKIE[$cookie_key];
			}
			else {
				$val = $this->$pKey;
			}
			if ($checkCookie && $this->isCookieEnabled())
			setcookie ( $cookie_key, $val, $this->cookieExpire, $this->cookiePath); //, $this->cookieDomain
		}
		else
		$val = "undefined";

		return $val;
	}//END OF FUNCTION get

	/**
	 * Get safely variable value. If variable is not defined, method will return default value
	 * @access public
	 * @param string $pKey
	 * @param string $default
	 * @return mixed
	 */

	function getDefault( $pKey, $default, $checkCookie = false)
	{
		if( $this->isDefined( $pKey ) ) {
			$cookie_key= str_replace ( "." , "_" , $pKey);
			if ($checkCookie && $this->isCookieEnabled() && isset($_COOKIE[$cookie_key])) {
				$val = $_COOKIE[$cookie_key];
			}
			else {
				$val = $this->$pKey;
			}
		} else {
			$val = $default;
		}
		if ($checkCookie && $this->isCookieEnabled())
		setcookie ( $cookie_key, $val, $this->cookieExpire, $this->cookiePath); //, $this->cookieDomain

		return $val;
	}//END OF FUNCTION get

	function set( $pKey, $val )
	{
		$this->$pKey = $val;
	}//END OF FUNCTION set



	/**
	 * Get configuration file lines.
	 * Method gives all the read lines from the configuration file. This is useful
	 * when debugging and checking all the name value pairs from the file. Additional parameter
	 * is used to get either all lines with comments, or lines without comments, or only comment lines.
	 * So parameters can be: 1. all 2. "" that is same as 3. configs 4. comment
	 * @access public
	 * @param string $pStr
	 * @return array
	 */
	function getConfigLines( $pStr = "" )
	{
		$temp_array = array();
		// return all rows
		if( $pStr == "all" )
		$temp_array = $this->LINES;
		// return only configuration rows
		$count = count( $this->LINES );
		if( $pStr == "configs" || $pStr == "" )
		{
			for( $i=0; $i < $count; $i++ )
			{
				//$val = ereg_replace( "[ 	]", "", $val );
				//$val = preg_replace( "/\s+/",	"", $val );

				//if ( eregi( "^[A-Z]", $this->LINES[$i] ) )
				if ( preg_match( "/^[A-Z]/i", $this->LINES[$i] ) )
				{
					array_push( $temp_array, $this->LINES[$i] );
				}
			}
		}
		// return only comment rows
		if( $pStr == "comments" )
		{
			for( $i=0; $i < $count; $i++ )
			{
				//if ( eregi( "^[A-Z]", $this->LINES[$i] ) )
				if ( preg_match( "/^[A-Z]/i", $this->LINES[$i] ) )
				{
					array_push( $temp_array, $this->LINES[$i] );
				}
			}
		}
		//
		return $temp_array;
			
	}//END OF FUNCTION getConfigLines

	/**
	 * Set configurations from the file
	 * This is the main method for getting and setting configuration variables
	 * Parameter given is the relative path and exact name of the configuration file
	 * Method checks if file name is specified, exists and contains valueable data.
	 * Comments are not counted as valuable data, so to keep method working, you need
	 * at least one name value pair in your configuration file. If everything goes well
	 * method returns boolean true. Else false is returned
	 *
	 * @access public
	 * @param string $pFileName
	 * @return bool
	 */
	function setConfigFromFile( $pFileName )
	{
		$this->FILENAME = $pFileName;
		//
		if( empty( $this->FILENAME  ) )
		{
			trigger_error( "File not specified!", E_USER_ERROR );
			return false;
		}
		if( !file_exists( $this->FILENAME  ) )
		{
			trigger_error( "File [" . $this->FILENAME . "] does not exist!", E_USER_ERROR );
			return false;
		}
		//
		$fp = fopen( $this->FILENAME, "r" );
		//
		if( ( !$fp) || ( empty( $fp ) ) )
		{
			trigger_error( "File opening error!", E_USER_ERROR );
			return false;
		}
		//
		while( !feof( $fp ) )
		{
			$line = fgets( $fp, 4096 );
			// trim lines from leading and trailing whitespaces and line-endings
			// now, if line starts with empty spaces and then follows with
			// variable name, config will be fine with that name, but remember
			// when you save back to file, the white space will be removed!
			$this->LINES[] = trim( $line );
		}
		//
		if( count( $this->getConfigLines() ) == 0 )
		{
			trigger_error( "Config file [" . $this->FILENAME . "] doesn't contain any key value pairs!", E_USER_WARNING );
			return false;
		}

		return $this->_parseConfig();
	}//END OF FUNCTION _setConfigFromFile

	/**
	 * Parse configuration lines from the LINES array
	 * If line start with alphabeth, it is tranported to the
	 * _parseConfig() method. Line that start with anything else
	 * like # / ; or other marks are not touched.
	 * If everything goes well, return true
	 * @see _setConfig()
	 * @access private
	 * @return boolean
	 */
	function _parseConfig()
	{
		$count = count( $this->LINES );
		for( $i = 0; $i < $count; $i++ )
		{
			// if line starts with a-Z alphabeth, it is supposed to be
			// a configuration name value pair
			//if ( eregi( "^[A-Z]", $this->LINES[$i] ) )
			if ( preg_match( "/^[A-Z]/i", $this->LINES[$i] ) )
			{
				$this->_setConfig( $this->LINES[$i] );
			}
		}
		return true;

	}//END OF FUNCTION _parseConfig

	/**
	 * Set configuration line. With this function line is separated to the
	 * key value pairs and class scope is filled with keys. Automatic key type
	 * concersion is made now. Return true, if everything is done right
	 * @see _parseConfig()
	 * @access private
	 * @param string $line
	 * @return boolean
	 */
	function _setConfig( $line )
	{
		// check, if there are any occurence of = marks in the line
		// at least one must be founded to make name value to work
		if( substr_count( $line, "=" ) < 1 )
		trigger_error( "There was no = -marks on the line: " . $line . " in file: " . $this->FILENAME . ". Please set at least one = -mark to separate name from value.", E_USER_WARNING );
		// separate line with =
		$pos = strpos($line, "=");
		$key = substr($line, 0, $pos);
		$val = substr($line, $pos+1);
		//list ( $key, $val ) = explode( "=", $line );
		// remove all white spaces from the key
		//$key = ereg_replace( "[ 	]",	"", $key );
		$key = preg_replace( "/\s+/",	"", $key );
		// remove white spaces before and after val
		$val = trim( $val );
		// then check '-marks at line start
		//if ( !ereg( "^'", $val ) && !preg_match( "/a_/i", $key ) )
		if ( !preg_match( "/^'/", $val ) && !preg_match( "/a_/i", $key ) )
		{
			//$val = ereg_replace( "[ 	]", "", $val );
			$val = preg_replace( "/\s+/",	"", $val );
		}
		else
		{
			// remove the leading ' and trailing '
			//$val = ereg_replace( "^'", "", $val );
			//$val = ereg_replace( "'$", "", $val );
			$val = preg_replace( "/^'/", "", $val );
			$val = preg_replace( "/'$/", "", $val );
		}
		// if key is not defined in the object scope
		// set up new key with new value
		if( !isset( $this->$key ) )
		{
			$this->$key	= $val;
		}
		else
		{
			// if dublicate configuration variable is found, lift user warning!
			trigger_error( "Initialization error. Dublicate variable (key: " . $key . ") was found from the configuration file! Please change the name in the file: " .$this->FILENAME, E_USER_WARNING );
		}
		if( $this->isReserved( $key ) )
		{
			trigger_error( "Key word is in reserved word list (key: " . $key . ")! Please change the name in the file: " .$this->FILENAME, E_USER_WARNING );
		}
		// check autotypes: boolean, integer, string and array
		$this->_checkAutoTypes( $key );
		// set key value pair to the debug array
		return true;
	}//END OF FUNCTION _setConfig

	/**
	 * Deletes key and value from the config array.
	 * Key name is checked, if it is defined in ConfigTool object. Then it's deleted.
	 * You must call saveToFile() method, if you need to save changes to the text file.
	 * @see saveToFile()
	 * @access public
	 * @param string $pKey
	 * @return boolean
	 */
	function deleteKey( $pKey )
	{
		if( $pKey == "" || $pKey == NULL )
		{
			trigger_error( "Delete error. Key was not defined.", E_USER_WARNING );
			return false;
		}
		if( $this->isReserved( $pKey ) )
		{
			trigger_error( "Delete error. Key is on reserved word list and cannot be deleted.", E_USER_WARNING );
			return false;
		}
		//
		$temp_array = array();
		//
		if( isset( $this->$pKey ) )
		{
			$ar = $this->getConfigLines( "all" );
			$count = count( $ar );
			for( $i = 0; $i < $count; $i++ )
			{
				// we need to check only lines, that may contain variables
				// comment lines are avoided. split function throws notice
				// if line doesn't contain any = mark
				$add = true;
				//if ( eregi( "^[A-Z]", $ar[$i] ) )
				if ( preg_match( "/^[A-Z]/i", $ar[$i] ) )
				{
					list ( $key, $val ) = explode( "=", $ar[$i] );
					//$key = ereg_replace( "[ 	]",	"", $key );
					$key = preg_replace( "/\s+/",	"", $key );
					if ( $key == $pKey )
					{
						$add = false;
					}
					else
					$add = true;
				}
				if( $add )
				{
					$temp_array[] = $ar[$i];
				}
			}
			$this->LINES = $temp_array;
			//
			unset( $this->$pKey );
			unset( $temp_array );
			return true;
		}
		else
		{
			trigger_error( "Delete error. Name ($pKey) is was not found from the configuration file ($this->FILENAME) and couldn't be deleted!", E_USER_WARNING );
			return false;
		}

	}//END OF FUNCTION deleteKey

	/**
	 * Saves all config variables to the file
	 * All variables and comments are saved to the file. File name is defined
	 * automatic when you construct the object and call setConfigFromFile( $pFileName )
	 * Name can be changed with setFileName() method. Also fi you make new configuration
	 * file from the scratch, you need to define file name with setFileName()
	 * @see setConfigFromFile( $pFileName )
	 * @see setFileName( $pFileName )
	 * @access public
	 * @return boolean
	 */
	function saveToFile()
	{
		$contents = $this->_arrayToString( $this->LINES, "\r\n" );
		// write to file prosess checks CHMOD and current file name
		if( $this->_fileWrite( 'w', $contents ) )
		return true;
		else
		{
			trigger_error( "Save operation failed!", E_USER_WARNING );
			return false;
		}
	}//END OF FUNCTION saveToFile

	/**
	 * Add key value pair.
	 *
	 * Method checks if key is already defined or is in reserved word list and then
	 * adds key value pair{@internal as a string presentation to the global line array}}.
	 * Long string values must be started and ended with ' marks!
	 * See config_definitions to learn, how to make ConfigTool compatible configurations
	 * name value pairs.
	 * You must call saveToFile() method, if you need to save changes to the text file.
	 * @see saveToFile()
	 * @access public
	 * @param string $pKey
	 * @param string $pVal
	 * @return boolean
	 */
	function addKeyValue( $pKey, $pVal )
	{
		if( $pKey == "" || $pKey == NULL )
		{
			trigger_error( "Add error. Key was not defined.", E_USER_WARNING );
			return false;
		}
		if( $this->isReserved( $pKey ) )
		{
			trigger_error( "Add error. Key is on reserved word list. Change the key name.", E_USER_WARNING );
			return false;
		}
		if( !$this->isDefined( $pKey ) )
		{
			$pVal = $this->_checkType( $pKey, $pVal );
			$line = $pKey . str_pad( " ", $this->INDENT - strlen( $pKey )  ) . " = " . $pVal;
			array_push( $this->LINES, $line );
			// if value starts with ', remove them from object value
			// however, ' marks are left in array, because
			// array is saved to the file, and long strings
			// must be surrounded by ' marks to work properly
			//if ( ereg( "^'", $pVal ) )
			if ( preg_match( "/^'/", $pVal ) )
			{
				//$pVal = ereg_replace( "^'", "", $pVal );
				//$pVal = ereg_replace( "'$", "", $pVal );
				$pVal = preg_replace( "/^'/", "", $pVal );
				$pVal = preg_replace( "/'$/", "", $pVal );
			}
			$this->$pKey = $pVal;
			$this->_checkAutoTypes( $pKey );
			return true;
		}
		else
		{
			trigger_error( "Add error. Name ($pKey) is already defined in configuration file ($this->FILENAME). Please choose another name!", E_USER_WARNING );
			return false;
		}
	}//END OF FUNCTION addKeyValue

	/**
	 * Check type of key and value and return correct value back as a string type
	 * Conversion is needed
	 * @see addKeyValue()
	 * @access private
	 * @param string $pKey
	 * @param string $pVal
	 * @return string
	 */
	function _checkType( $pKey, $pVal )
	{
		// boolean must start with b_
		if ( preg_match( "/^b_/i", $pKey ) )
		{
			if( gettype( $pVal ) == "boolean" )
			return $this->_booleanToString( $pVal );
			else
			{
				$pattern = array( "true","yes","1","on","y","false","no","0","off","n" );
				$count = count( $pattern );
				if( in_array( strtolower( $pVal ), $pattern ) )
				return $pVal;
				trigger_error( "Boolean error. Specified boolean value ($pVal) is not accepted. Use (true|yes|1|on|y) or (false|no|0|off|n) values only.", E_USER_WARNING );
			}
		}
		// array must start with a_
		else if ( preg_match( "/a_/i", $pKey ) )
		{
			if( gettype( $pVal ) == "array" )
			return $this->_arrayToString( $pVal );
			else
			return $pVal;
		}
		// integer must start with i_
		// do we have to check gettype( $pVal ) in any case???
		// i don't think so...
		else if ( preg_match( "/i_/i", $pKey ) )
		{
			return $this->_integerToString( $pVal );
		}
		// double must start with d_
		else if ( preg_match( "/d_/i", $pKey ) )
		{
			return $this->_doubleToString( $pVal );
		}
		// string must start with s_
		else if ( preg_match( "/s_/i", $pKey ) )
		{
			return $this->_stringToString( $pVal );
		}
		// everything else is handled as a string
		else
		{
			return $this->_stringToString( $pVal );
		}
	}//END OF FUNCTION _checkType

	/**
	 * Checks, if key word is defined
	 *
	 * This is more presice method than trying to refer straight to
	 * the class scope keys. Expecially boolean keys and values
	 * may cause problems with straight object referings. Another
	 * knows issue comes, when userr tries to point keyname on object
	 * that doesn't exist. It causes notice of undefined property.
	 * One solution is to check with isDefined before trying to
	 * catch key value. Another solution may come if new method
	 * secureGet will be announced. That function could return undefined
	 * if key is not defined...
	 * @access public
	 * @param string $pKey
	 * @return bool
	 */
	function isDefined( $pKey )
	{
		$ar = $this->getConfigLines();
		$count = count( $ar );
		for( $i = 0; $i < $count; $i++ )
		{
			$line = $ar[$i];
			list ( $key, $val ) = explode( "=", $line );
			//$key = ereg_replace( "[ 	]",	"", $key );
			$key = preg_replace( "/\s+/",	"", $key );
			if( $pKey == $key )
			return true;
		}
		return false;
	}//END OF FUNCTION isDefined

	/**
	 * Checks, if key word is in reserved word list
	 * @access public
	 * @param string $pKey
	 * @return bool
	 */
	function isReserved( $pKey )
	{
		for( $i=0; $i< count( $this->RESERVED_WORDS ); $i++ )
		if( $pKey == $this->RESERVED_WORDS )
		return true;
		return false;
	}//END OF FUNCTION isReserved

	/**
	 * Update excisting key value pair
	 * Long string values must be started and ended with ' marks!
	 * See config_definitions to learn, how to make ConfigTool
	 * compatible configurations name value pairs.
	 * @access public
	 * @param string $pKey
	 * @param string $pVal
	 * @return bool
	 */
	function updateKeyValue( $pKey, $pVal )
	{
		$temp_array = array();
		//
		$pVal = $this->_checkType( $pKey, $pVal );
		//
		if( $this->isDefined( $pKey ) )
		{
			$ar = $this->getConfigLines( "all" );
			$count = count( $ar );
			for( $i=0; $i < $count; $i++ )
			{
				// we need to check only lines, that may contain variables
				// comment lines are avoided. split function throws notice
				// if line doesn't contain any = mark
				//if ( eregi( "^[A-Z]", $ar[$i] ) )
				if ( preg_match( "/^[A-Z]/i", $ar[$i] ) )
				{
					list ( $key, $val ) = explode( "=", $ar[$i] );
					//$key = ereg_replace( "[ 	]",	"", $key );
					$key = preg_replace( "/\s+/",	"", $key );
					if ( $key == $pKey )
					$line = $pKey . str_pad( " ", $this->INDENT - strlen( $pKey )  ) . " = " . $pVal;
					else
					$line = $ar[$i];
				}
				else
				$line = $ar[$i];
				//
				$temp_array[$i] = $line;
			}
			$this->LINES = $temp_array;
			// if value starts with ', remove them from object value
			// however, ' marks are left in array, because
			// array is saved to the file, and long strings
			// must be surrounded by ' marks to work properly
			if ( preg_match( "/^'/i", $pVal ) )
			//if ( ereg( "^'", $pVal ) )
			{
				//$pVal = ereg_replace( "^'", "", $pVal );
				//$pVal = ereg_replace( "'$", "", $pVal );
				$pVal = preg_replace( "/^'/", "", $pVal );
				$pVal = preg_replace( "/'$/", "", $pVal );
			}
			$this->$pKey = $pVal;
			$this->_checkAutoTypes( $pKey );
			unset( $temp_array );
			return true;
		}
		else
		{
			unset( $temp_array );
			trigger_error( "Update error. Name ($pKey) was not found from the configuration file ($this->FILENAME). Please choose another name!", E_USER_WARNING );
			return false;
		}
	}//END OF FUNCTION updateKeyValue

	/**
	 * Check, if string starts with specified symbol (b_, i_, a_, s_)
	 * @access private
	 * @param string $key
	 */
	function _checkAutoTypes( $key )
	{
		// boolean must start with b_
		if ( $this->AUTOBOOL && preg_match( "/^b_/i", $key ) )
		{
			$this->_toBoolean( $key );
		}
		// array must start with a_
		else if ( $this->AUTOARRAY && preg_match( "/a_/i", $key ) )
		{
			$this->_toArray( $key );
		}
		// integer must start with i_
		else if ( $this->AUTOINTEGER && preg_match( "/i_/i", $key ) )
		{
			$this->_toInteger( $key );
		}
		// double must start with d_
		else if ( $this->AUTODOUBLE && preg_match( "/d_/i", $key ) )
		{
			$this->_toDouble( $key );
		}
		// string must start with s_
		else if ( $this->AUTOSTRING && preg_match( "/s_/i", $key ) )
		{
			$this->_toString( $key );
		}
		// everything else is handled as a string
		else
		{
			$this->_toString( $key );
		}
	}//END OF FUNCTION _checkAutoTypes

	/**
	 * Set key to boolean from string presentation
	 * @access private
	 * @param string $key
	 * @return boolean
	 */
	function _toBoolean( $key )
	{
		$val = $this->$key;

		//if( eregi( "(true|yes|1|on|y)", $val ) )
		if( preg_match( "/(true|yes|1|on|y)/i", $val ) )
		{
			$this->$key = true;
			return true;
		}
		//else if( eregi( "(false|no|0|off|n)", $val ) )
		else if( preg_match( "/(false|no|0|off|n)/i", $val ) )
		{
			$this->$key = false;
			return true;
		}
		else
		{
			trigger_error( "_toBoolean operation failed. Value couldn't be detected (" . $key . " = " . $val . ").Please check the value in the configuration file.", E_USER_WARNING );
			unset( $this->$key );
			return false;
		}
	}//END OF FUNCTION _toBoolean

	/**
	 * Set key to array from string presentation
	 * @access private
	 * @param string $key
	 */
	function _toArray( $key )
	{
		$ar = array();
		//$this->$key = str_replace( " ",	"", $this->$key );
		$ar = explode( ",", $this->$key );
		$this->$key = $ar;
		unset ( $ar );
	}//END OF FUNCTION _toArray

	/**
	 * Set key to string from string presentation
	 * @access private
	 * @param string $key
	 */
	function _toString( $key )
	{
		settype( $this->$key, "string" );
	}//END OF FUNCTION _toString

	/**
	 * Set key to integer from string presentation
	 * @access private
	 * @param string $key
	 */
	function _toInteger( $key )
	{
		settype( $this->$key, "integer" );
	}//END OF FUNCTION _toInteger

	/**
	 * Set key to integer from string presentation
	 * @access private
	 * @param string $key
	 */
	function _toDouble( $key )
	{
		settype( $this->$key, "double" );
	}//END OF FUNCTION _toDouble

	/**
	 * Set value to boolean from string presentation
	 * @access private
	 * @param bool $b
	 * @return string
	 */
	function _booleanToString( $b )
	{
		if( $b )
		return "true";
		else
		return "false";
	}//END OF FUNCTION _booleanToString

	/**
	 * Set value from string to string presentation
	 * @access private
	 * @param string $str
	 * @return string $str
	 */
	function _stringToString( $str )
	{
		return "" . $str;
	}//END OF FUNCTION _stringToString


	/**
	 * Set value from integer to string presentation
	 * @access private
	 * @param integer $int
	 * @return string $str
	 */
	function _integerToString( $int )
	{
		return "" . $int;
	}//END OF FUNCTION _integerToString

	/**
	 * Set value from double to string presentation
	 * @access private
	 * @param double $dbl
	 * @return string $str
	 */
	function _doubleToString( $dbl )
	{
		return "" . $dbl;
	}//END OF FUNCTION _doubleToString

	/**
	 * Convert array to comma separated string. Other separator can be
	 * defined in parameter
	 * @access private
	 * @param array $array
	 * @param string $separ
	 * @return string $str
	 */
	function _arrayToString( $array, $separ = "," )
	{
		$str = "";
		$count = count( $array );
		for( $i=0; $i < $count - 1; $i++ )
		{
			$str .= $array[$i] . $separ;
		}
		$str .= $array[$i];
		return $str;
	}//END OF FUNCTION _arrayToString

	/**
	 * Write to file
	 *
	 * Overwrite to file, if exists and is writable.
	 * Try to create new if file name doesn't exists.
	 * If new is about to write, then directory has to have CHMOD 0666 or 0777 also!
	 *
	 * @access private
	 * @param string $pFlag
	 * @param string $pContent
	 * @return boolean
	 */
	function _fileWrite( $pFlag, &$pContent )
	{
		if( file_exists( $this->FILENAME ) )
		{
			if( !is_writable( $this->FILENAME ) )
			{
				if( !chmod( $this->FILENAME, 0666 ) )
				{
					trigger_error( "Cannot change the mode of file (" . $this->FILENAME . "). Php may work on safe mode... You should change file permission to CHMOD 0666 or 0777 manually by using ftp tool or shell commands.", E_USER_WARNING );
					return false;
				};
			}
		}
		//
		$path_parts = pathinfo( $this->FILENAME );
		//echo $this->FILENAME;
		$directory = $path_parts['dirname'];
		//
		if( !is_writable( $directory ) )
		{
			if( !chmod( $directory, 0666 ) )
			{
				trigger_error( "Cannot change the mode of directory (" . $directory . "). Php may work on safe mode... You should change directory permission to CHMOD 0666 or 0777 manually by using ftp tool or shell commands.", E_USER_WARNING );
				return false;
			};
		}
		//
		if( !$fp = @fopen( $this->FILENAME, $pFlag ) )
		{
			trigger_error( "Cannot open file (" . $this->FILENAME . ")", E_USER_WARNING );
			return false;
		}
		if( fwrite( $fp, $pContent ) === FALSE )
		{
			trigger_error( "Cannot write to file (" . $this->FILENAME . ")", E_USER_WARNING );
			return false;
		}
		if( !fclose( $fp ) )
		{
			trigger_error( "Cannot close file (" . $this->FILENAME . ")", E_USER_WARNING );
			return false;
		}
		return true;
	}//END OF FUNCTION _fileWrite

}//END OF CLASS ConfigTool
?>