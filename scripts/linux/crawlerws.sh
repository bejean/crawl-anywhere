#! /bin/sh
#
# chkconfig: 2345 90 10
# description:  crawlerws daemon

if ( [ -f /lib/lsb/init-functions ] )
then
	# Debian / unbuntu
	. /lib/lsb/init-functions
else
	# Centos / Redhat
	. /etc/init.d/functions  
fi

# You will probably want to change only two following lines.
export BASEDIR="$( cd "$( dirname "$0" )/.." && pwd )"

PROG="crawlerws"
SETTINGSFILE="${BASEDIR}/config/crawlerws/settings.yml"
JARFILE="${BASEDIR}/bin/ws/eolya-crawlerws-4.0.0.jar"
PIDFILE="${BASEDIR}/log/crawlerws.pid"

CMD="java -jar ${JARFILE} server ${SETTINGSFILE}"
RETVAL=0
 
start () {
    echo "Starting ${PROG}"
    if ( [ -f ${PIDFILE} ] )
    then
        echo "${PROG} is already running."
        RETVAL=1
        return
    fi
    touch ${PIDFILE}
    cd ${BASEDIR}
    ${CMD} > /dev/null &
    echo $! > ${PIDFILE}
    echo "${PROG} started"
}

stop () {
    echo "Stopping ${PROG}"
    if ( [ ! -f ${PIDFILE} ] )
    then
        echo "${PROG} is not running."
        RETVAL=1
        return
    fi
    killproc -p ${PIDFILE}
    RETVAL=$?
    echo
    if [ $RETVAL -eq 0 ] ; then
        rm -f ${PIDFILE}
    fi
}

status () {
    if ( [ -f ${PIDFILE} ] )
    then
        echo "${PROG} is running."
	else
        echo "${PROG} is not running."
    fi
	echo
}

restart () {
    stop
    start
}


# See how we were called.
case "$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  status)
    status
    ;;
  restart)
    restart
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}"
    RETVAL=2
    ;;
esac

exit $RETVAL
