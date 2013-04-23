#!/bin/sh
#
# Crawl-Anywhere
#
# Author: Dominique Béjean <dominique.bejean@eolya.fr>
# Author: Jonathan Dray <jonathan.dray@gmail.com>
#
# Pipeline launch script
#

# Get the path of crawl anywhere root directory
export HOME="$( cd "$( dirname "$0" )/.." && pwd )"

# Variables initialisation
. $HOME/scripts/init.inc.sh

JVMARGS="-Duser.timezone=Europe/Paris -Xmx1024m"
PID_FILE=$LOG_DIR/pipeline.pid

# TODO 
#  * Change this to a more reliable version
#  * need to add the real process id in the PID_FILE
get_pid() {
        # if there is a running process whose pid is in PID_FILE,
        # print it and return 0.
        if [ -e "$PID_FILE" ]; then
		cat $PID_FILE
		return 0
        fi
        return 1
}


# Pipeline start operation
# It takes an optional argument to pass to the java program

# TODO
#  * is it necessary to cd to log directory ?
#  * write the pid file to the run directory instead of log directory
start() {
	#ARGS="-v"
	ARGS=
	PID=$(get_pid) || true
	if [ "${PID}" ]; then
		echo "Pipeline is already running (pid $PID) !"
		exit 1
	else
		cd $LOG_DIR
		java $JVMARGS fr.eolya.simplepipeline.SimplePipeline -p $CONF_DIR/pipeline/simplepipeline-mysolrserver.xml $1 $ARGS >> $LOG_DIR/pipeline.output 2>&1  &
		exit 0
	fi
}

case $1 in
	start)
		echo "Starting pipeline processing"
		start
	;;
	start_once)
		echo "Starting pipeline processing once"
		start "-o"
	;;
	stop)
		PID=$(get_pid) || true
		if [ -n "$PID" ]; then
			echo "Pipeline is running (pid $PID)"
		    echo "Stopping pipeline"
		    rm $PID_FILE
			exit 0;
		else
			echo "Pipeline is not running"
			exit 1;
		fi
		exit 0
	;;
	status)
		PID=$(get_pid) || true
		if [ -n "$PID" ]; then
			echo "Pipeline is running (pid $PID)"
			exit 0;
		else
			echo "Pipeline is not running"
			exit 1;
		fi
		exit 0
	;;
	*)
		echo "Usage : $HOME/scripts/pipeline.sh {start|start_once|stop|status}"
		exit 1
	;;
esac
