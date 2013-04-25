<?php
/**
 * Class FeedFinder 
 *
 * @author 
 * @package 
 */
  
require_once("magpierss-0.72/rss_fetch.inc");  
  
/**
 * Class FeedFinder
 *
 */
Class FeedFinder {
    
    var $_feedlist = NULL;
    var $_processedPages = NULL;
    var $_processedPages2 = NULL;  
    var $_depth2Pages = NULL;
    var $_timeout = 0;
    var $_timestart;
    var $_pagereadcount = 0;
    var $_proxy_host = "";
    var $_proxy_port = "";
    var $_proxy_exclude = "";
    var $_proxy_user = "";
    var $_proxy_passwd = "";
    
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function FeedFinder() {}
    
    function SetProxy ($proxy_host, $proxy_port, $proxy_exclude, $proxy_user, $proxy_passwd) {
    	$this->_proxy_host = $proxy_host;
    	$this->_proxy_port = $proxy_port;
    	$this->_proxy_exclude = $proxy_exclude;
    	$this->_proxy_user = $proxy_user;
    	$this->_proxy_passwd = $proxy_passwd;
    }
    
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function ListAdd ($rss, $url)
    {
        if ($this->_feedlist == NULL)
            $this->_feedlist = array();
            
        $channel = $rss->channel;
        $channel['url'] = $url;
        /*
        $feed_title = $channel['title'];
        $feed_description = $channel['description'];
        $feed_link = $channel['link'];
        $feed_language = $channel['language'];            
        */
                                
        if (!in_array($channel, $this->_feedlist))
        {
            
            $bExists = FALSE;
            foreach ($this->_feedlist as $feed) {
                if ($channel['url'] == $feed['url'])
                {
                    $bExists = TRUE; 
                    break;
                }
            }
            if (!$bExists)
            {
                array_push($this->_feedlist, $channel);        
            }
        }
    }
    
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function isProcessedPage ($url)
    {
        if ($this->_processedPages == NULL)
            $this->_processedPages = array();
                                            
        if (!in_array($url, $this->_processedPages))
        {
            array_push($this->_processedPages, $url);   
            return false;      
        }
        else
        {
            return true;
        }
    }    
    
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function isProcessedPage2 ($url)
    {
        if ($this->_processedPages2 == NULL)
            $this->_processedPages2 = array();
                                            
        if (!in_array($url, $this->_processedPages2))
        {
            array_push($this->_processedPages2, $url);   
            return false;      
        }
        else
        {
            return true;
        }
    }        
    
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function GetList ()
    {
        return $this->_feedlist;
    }    
    
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function GetPageReadCount ()
    {
        return $this->_pagereadcount;
    }    
        
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function readurl($url) {

        if (trim($url) == "") return "";
        if (!parse_url ($url)) return "";

        //$url = urlencode ($url);
        $url = str_replace(" ", "%20", $url);
        // Création d'une ressource CURL
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, false);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER,true);
        curl_setopt($ch, CURLOPT_TIMEOUT,300); 
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION,true);

        if ($this->_proxy_host!="" && $this->_proxy_port!="") {
        	$proxy_host = $this->_proxy_host;
        	if ($this->_proxy_exclude!="") {
        		$aExclude = explode(',', $this->_proxy_exclude);
        		for ($i=0;$i<count();$i++) {
        			$aExclude[$i]=trim(strtolower($aExclude[$i]));
        		}
        		$host = strtolower(parse_url($url, PHP_URL_HOST));
        		if (in_array($host, $aExclude)) 
        			$proxy_host = "";
        	}
        	if ($proxy_host!="") {
		        //curl_setopt($ch, CURLOPT_HTTPPROXYTUNNEL, true);
		        curl_setopt($ch, CURLOPT_PROXY, $this->_proxy_host);
		        curl_setopt($ch, CURLOPT_PROXYPORT, $this->_proxy_port);
		        if ($this->_proxy_user!="" && $this->_proxy_passwd!="") {
		        	curl_setopt($ch, CURLOPT_PROXYUSERPWD, "$this->_proxy_user:$this->_proxy_passwd");
		        }
        	}
        }
        
        // Récuperation de la page
        $response = curl_exec($ch);
        $errmsg = "";
        if (curl_errno($ch)) {
            $errmsg = curl_error($ch);
            curl_close($ch);
            return "";
        }
        curl_close($ch);
        
        $this->_pagereadcount++;
        
        return $response;
    }
        
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/      
    function isTimeOut()
    {
        if ($this->_timeout!=0) 
        {
            $timenow = microtime(TRUE);
            if (($timenow - $this->_timestart) > $this->_timeout)
            {
                return true; 
            }
        }
        return false;
    }
       
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function Find($url,$useHeaderOnly=false,$stopIfInHeader=false,$timeout=0)
    { 
        $this->_timeout = $timeout;
        if ($this->_timeout!=0) $this->_timestart=microtime(TRUE);
        
        // Est-ce que l'url est un flux ?
        $rss = $this->fetchRSS($url);
        if ($rss)
        {
            $this->ListAdd($rss, $url);
            return;
        } 
        
        $this->Find_Internal($url,$useHeaderOnly,$stopIfInHeader,false);
        return;  
    }
        
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function Find_Internal($url,$useHeaderOnly=false,$stopIfInHeader=false,$final=true)
    {       
        if ($this->isTimeOut()) return; 

        if ($this->isProcessedPage ($url)) return;

        // Lire la page pour y trouver les définitions de flux
        $data = $this->readurl($url);        
        if ($data=="") return;
                
        // Recherche un élément <link ...>
        //<link type="application/rss+xml" rel="alternate" title="Flux RSS" href="http://ambafrance-jp.org/backend.php3?id_secteur=1"/>                
        $pattern = "/<link[^>]+>/i";  
        if (preg_match_all($pattern, $data, $matches, PREG_SET_ORDER)>0)
        {
            $found_in_header = FALSE;
            
            // Un ou plusieurs Ã©lÃ©ments <link ...>                                                                                                               
            foreach ($matches as $val) {
                if (preg_match("/rel=[\"']alternate[\"']/i", $val[0])>0 && (preg_match("/type=[\"']application\/rss\+xml[\"']/i", $val[0])>0 || preg_match("/type=[\"']application\/atom\+xml[\"']/i", $val[0])>0))
                {         
                    //preg_match( "/title=[\"']([^\"']*)[\"']/i", $val[0], $title_matches);                // !!! Ne fonctionne pas si " ou ' present dans le title
                    preg_match( "/href=[\"']([^\"']*)[\"']/i", $val[0], $href_matches);
                    $link = $this->getAbsoluteURL($url, $href_matches[1]);  
                    $link = str_replace("&amp;", "&", $link);
                    $rss = $this->fetchRSS($link);
                    if ($rss)
                    {
                        // Cette url correspond a un flux
                        $this->ListAdd($rss, $link); 
                        $found_in_header = TRUE;              
                    }
                }
            }

            if ($found_in_header && $stopIfInHeader) return;
        }
        
        if ($useHeaderOnly) return;  
        
        $rss_count = 0;
        // Recherche tous les <a href..>
        //$pattern = "/<(a.*) href=[\"'](.*?)[\"'](.*)<\/a>/i";  
        $pattern = "/(href)\s*=\s*[\"'](.*?)[\"']/i";
        if (preg_match_all($pattern, $data, $matches, PREG_SET_ORDER)>0)
        {

            /*
            foreach ($matches as $val) 
            {
                $link = $val[2];
                $handle = fopen("c:/temp/_links.txt", "a");
                fwrite($handle, $link . "\n");
                fclose($handle);
            } 
            */
        
            $rss_count = $this->doLinks($url, $matches, $final);
        }  
        
        if ($rss_count==0)
        {  
            // a tous les coups les attributs ne sont pas entourés de " ou '    
            $pattern = "/<(a.*) href=(.*?)[\s+\>](.*)<\/a>/i";  
            if (preg_match_all($pattern, $data, $matches, PREG_SET_ORDER)>0)
            {
                $rss_count = $this->doLinks($url, $matches, $final);
            }
        }        
        
        /* Désactivé pour la version en ligne 
        //if (!$final && count($this->_feedlist) <= 2 && $this->_depth2Pages!=NULL)
        if (!$final && count($this->_feedlist) == 0 && $this->_depth2Pages!=NULL)
        {
            foreach ($this->_depth2Pages as $link)
            {
                $this->Find_Internal($link,true,true,true);         
            }
        }
        */
    }
    
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function doLinks($url, $matches, $final)
    {        
        if ($this->isTimeOut()) return;       

        if ($this->isProcessedPage2 ($url))
        {
            return;
        }
                
        $rss_count = 0;
        
        // Un ou plusieurs éléments <a href..>                                                                                                               
        foreach ($matches as $val) {
            $link = $val[2];

            if (preg_match("/(rss|xml|feed|atom|rdf)/i", $link)>0) 
            {
                $i=1;
            }           

            if (preg_match("/\.(png|gif|jpg|jpeg|pdf|xls|ppt|ico|mp3)$/i", $link)>0)
                continue;

            if (preg_match("/\.(swf)\?/i", $link)>0)
                continue;
                
            if (preg_match("/^[#'\"]/", $link)>0)
                continue;

            if (preg_match("/javascript/i", $link)>0)
                continue;

            if (preg_match("/^http:\/\/$/i", $link)>0)
                continue;

            if (preg_match("/^http:\/\/\/$/i", $link)>0)
                continue;

            if (preg_match("/(url|feedurl)=/i", $link)>0)
            {
                preg_match( "/[\\?&](url|feedurl)=([^&#]*)/i", $link, $href_matches);
                $link = $this->getAbsoluteURL($url, $href_matches[2]);  
            }    
                
            $link = $this->getAbsoluteURL($url, $link);
            $url_items = parse_url($link);
                            
            if (strtolower($url_items["host"]) == "feeds.feedburner.com")
            {
                $rss = $this->fetchRSS($link);
                if ($rss)
                {
                    // Cette url correspond a un flux
                    $this->ListAdd($rss, $link); 
                    $rss_count++;
                }
            }
            else
            {
                //if (preg_match("/(rss|xml|flux|channel|feed|atom|rdf)/i", $link)>0)
                if (preg_match("/(rss|xml|feed|atom|rdf)/i", $link)>0)
                {
                    $rss = $this->fetchRSS($link);
                    if ($rss)
                    {
                        // Cette url correspond a un flux
                        $this->ListAdd($rss, $link);
                        $rss_count++;
                    } 
                    else
                    {
                        if ($this->belongSameDomain($url, $link))
                        {
                            // Cette page liste peut-être des flux
                            if (!$final)
                            {
                                $count_start = count($this->_feedlist);
                                $this->Find_Internal($link,false,false,true);
                                $count_stop = count($this->_feedlist);
                                if (($count_stop - $count_start) >=3)
                                {
                                    return $rss_count;
                                }
                            }
                        }
                    }
                }
                else
                {
                    if (!$final && $this->belongSameDomain($url, $link))
                    {
                        if ($this->_depth2Pages==NULL)
                            $this->_depth2Pages = array();         
                                            
                        if (!in_array($link, $this->_depth2Pages))
                            array_push($this->_depth2Pages, $link);   
                        
                        //$this->Find_Internal($link,true,true,true);
                    }
                }
            }
        }  
        return $rss_count;  
    }
    
    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/   
    function getAbsoluteURL($urlReferer, $urlHref) {

        if ($urlHref=="")
            return "";    
            
        // Case 1 : urlHref starts with "http://"        
        if (preg_match("/^http:\/\//i", $urlHref) > 0)
            return $urlHref;

        $url_items = parse_url($urlReferer);
        $urlRefererHost = $url_items["scheme"] . "://" . $url_items["host"]; 
        
        // Case 2 : urlHref looks like "/path/file.html..."
        if (preg_match("/^\//i", $urlHref) > 0)
            return $urlRefererHost . $urlHref;
        
        // Case 3 : urlHref looks like "path/file.html..."
        $offset = strrpos($url_items["path"], "/");
        if (is_bool($offset) && !$offset) {
            $urlRefererPath = "";
        } else {
            $urlRefererPath = substr($url_items["path"], 0, $offset);
        }
        return $urlRefererHost . $urlRefererPath . "/" . $urlHref;        
    }  

    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function belongSameDomain($urlReferer, $urlHref) {

        $urlR_items = parse_url($urlReferer);
        $urlH_items = parse_url($urlHref); 
        
        $hostR = strtolower($urlR_items["host"]);
        if (strpos($hostR,".") != strrpos($hostR,"."))
            $hostR = substr($hostR, strpos($hostR,".") + 1);

        $hostH = strtolower($urlH_items["host"]);
        if (strpos($hostH,".") != strrpos($hostH,"."))
            $hostH = substr($hostH, strpos($hostH,".") + 1);

        return ($hostR==$hostH);
    }    

    /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
    function fetchRSS($url) {
        $rss = @fetch_rss($url);
        if (isset($rss->feed_type))
            return $rss;
        
        return NULL;
    }

    
}    
?>
