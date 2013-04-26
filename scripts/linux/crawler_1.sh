#!/bin/sh
#
# Crawl-Anywhere
#
# Author: Dominique Béjean <dominique.bejean@eolya.fr>
# Author: Jonathan Dray <jonathan.dray@gmail.com>
#
# Crawler launch script
#

# Get the path of crawl anywhere root directory
export HOME="$( cd "$( dirname "$0" )/.." && pwd )"

# Variables initialisation
. $HOME/scripts/init.inc.sh

JVMARGS="-Duser.timezone=Europe/Paris -Xmx1024m"
PID_FILE=$LOG_DIR/crawler.pid
JAVA_CLASS="fr.eolya.crawler.crawler.Crawler"

# TODO 
#  * Change this to a more reliable version
#  * need to add the real process id in the PID_FILE
get_pid() {
        # if there is a running process whose pid is in PIDFILE,
        # print it and return 0.
        if [ -e "$PID_FILE" ]; then
                # if pidof $JAVA_CLASS | tr ' ' '\n' | grep -w $(cat $PID_FILE); then
                #         return 0
                # fi
		cat $PID_FILE
		return 0
        fi
        return 1
}


# Crawler start operation
# It takes an optional argument to pass to the java program

# TODO
#  * is it necessary to cd to log directory ?
#  * write the pid file to the run directory instead of log directory
start() {
	ARGS="-c 1 -a 1 -v"
	#ARGS=
	PID=$(get_pid) || true
	if [ "${PID}" ]; then
		echo "Web Crawler is already running (pid $PID) !"
		exit 1
	else
		cd $LOG_DIR
		java $JVMARGS fr.eolya.crawler.Crawler -p $CONF_DIR/crawler/crawler.xml $1 $ARGS >> $LOG_DIR/crawler.output 2>&1  &
		exit 0
	fi
}

case $1 in
	start)
		echo "Starting Web Crawler"
		start
	;;
	start_once)
		echo "Starting Web Crawler once"
		start "-o"
	;;
	stop)
		PID=$(get_pid) || true
		if [ -n "$PID" ]; then
			echo "Web Crawler is running (pid $PID)"
		    echo "Stopping Web Crawler"
		    rm $PID_FILE
			exit 0;
		else
			echo "Web Crawler is not running"
			exit 1;
		fi
		exit 0
	;;	status)
		PID=$(get_pid) || true
		if [ -n "$PID" ]; then
			echo "Web Crawler is running (pid $PID)"
			exit 0;
		else
			echo "Web Crawler is not running"
			exit 1;
		fi
		exit 0
	;;
	*)
		echo "Usage : $HOME/scripts/crawler.sh {start|start_once|stop|status}"
		exit 1
	;;
esac
