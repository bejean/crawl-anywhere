<?php

class Logger {
	
	private $logfile;
	private $f;
	private $enabled;
	private $debug;
	
	function Logger ($logfile)
    {
        $this->enabled = false;
    	if ($logfile=="") return;
    	$this->logfile = $logfile;
        $this->debug= false;
        $this->f=fopen($this->logfile,"a");
        if ($this->f) {
        	$this->enabled = true;
        }
    }
    
    function setEnabled($enabled)
    {
        $this->enabled = $enabled;
    }
    
    function setDebug($debug)
    {
        $this->debug = $debug;
    }
    
    function log($msg) 
    {
    	if ($this->enabled) {
    		$date = new DateTime();
			fwrite($this->f, $date->format('Y-m-d H:i:s') . " - ");
			fwrite($this->f,$msg . "\n");
    	}
    }
    
    function log_debug($msg) 
    {
    	if ($this->enabled && $this->debug) {
    		$this->log($msg);
    	}
    }
}

?>
