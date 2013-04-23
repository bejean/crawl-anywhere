#!/bin/sh
#
# Crawl-Anywhere
#
# Author: Dominique Béjean <dominique.bejean@eolya.fr>
# Author: Jonathan Dray <jonathan.dray@gmail.com>
#
# Indexer launch script
#

# Get the path of crawl anywhere root directory
export HOME="$( cd "$( dirname "$0" )/.." && pwd )"

# Variables initialisation
. $HOME/scripts/init.inc.sh

JVMARGS="-Duser.timezone=Europe/Paris -Xmx512m"

#
# Script usage
#
# Display script usage and exit with an error code
#
usage() {
	echo "Usage : $HOME/scripts/indexer.sh {start|start_once|stop|status}"
	exit 1
}

# Check script arguments
if [ -z "$1" ]
then
  usage
fi

PROFILE=
PID_FILE="$LOG_DIR/indexer$PROFILE.pid"

#
# Get the indexer pid number if the process is running
#
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


# Indexer start operation
# It takes an optional argument to pass to the java program

# TODO
#  * is it necessary to cd to log directory ?
#  * write the pid file to the run directory instead of log directory
start() {
	#ARGS="-v"
	ARGS=
	PID=$(get_pid) || true
	if [ "${PID}" ]; then
		echo "Indexer is already running (pid $PID) !"
		exit 1
	else
		cd $LOG_DIR
		java $JVMARGS fr.eolya.indexer.Indexer -p "$CONF_DIR/indexer/indexer-mysolrserver$PROFILE.xml" $ARGS >> "$LOG_DIR/indexer$PROFILE.output" 2>&1  &
		exit 0
	fi
}

case $1 in
	start)
		echo "Starting indexer $PROFILE"
		start
	;;
	start_once)
		echo "Starting indexer once $PROFILE"
		start "-o"
	;;
	stop)
		PID=$(get_pid) || true
		if [ -n "$PID" ]; then
			echo "Indexer is running (pid $PID)"
		    echo "Stopping indexer"
		    rm $PID_FILE
			exit 0;
		else
			echo "Indexer is not running"
			exit 1;
		fi
		exit 0
	;;	status)
		PID=$(get_pid) || true
		if [ -n "$PID" ]; then
			echo "Indexer is running (pid $PID) $PROFILE"
			exit 0;
		else
			echo "Indexer is not running $PROFILE"
			exit 1;
		fi
		exit 0
	;;
	*)
		usage
	;;
esac
