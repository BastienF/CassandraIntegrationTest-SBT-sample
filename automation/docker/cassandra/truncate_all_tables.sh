#!/bin/bash
# Protect from errors when iterating on empty directories
shopt -s nullglob
set -e

###
### PARAMETERS HANDLING
###

usage()
{
cat << EOF

--=== truncate_all_tables.sh ===--

#### truncate all tables of a specific keyspace ####


* USAGE: ./truncate_all_tables.sh [OPTIONS]

* OPTIONS:
  -h                        display this usage
  -v                        active verbose
  --docker-hostname <name>  name of dse container
  --keyspace <ks_name>      name of keyspace to flush

EOF
}

# Defaults

params="$(getopt -o hv -l docker-hostname:,keyspace: --name "$(basename "$0")" -- "$@")"

if [ $? -ne 0 ]
then
    usage
    exit 1
fi

eval set -- "$params"
unset params

while true; do
    case "$1" in
        --docker-hostname)
            case "$2" in
                "") shift 2;;
                *) DOCKER_HOSTNAME="$2"; shift 2;;
            esac
            ;;
        --keyspace)
            case "$2" in
                "") shift 2;;
                *) KEYSPACE="$2"; shift 2;;
            esac
            ;;
        -v)
            VERBOSE="true"
            set -x
            shift
            ;;
        -h)
            usage
            exit 0
            ;;
        --)
            shift
            break
            ;;
        *)
            echo $1
            usage
            exit 1
            ;;
    esac
done

if [ -z "$DOCKER_HOSTNAME" ]; then
  >&2 echo 'Please provide the DSE docker instance hostname'
  usage
  exit 1
fi

if [ -z "$KEYSPACE" ]; then
  >&2 echo 'Please provide the keyspace to flush name'
  usage
  exit 1
fi


DOCKER_EXEC="docker exec $DOCKER_HOSTNAME bash -c"
CQLSH_CONNECT="cqlsh 127.0.0.1 -u cassandra -p cassandra"
PIDS=()

echo "=== Get all tables of keyspace ==="
TABLES=$($DOCKER_EXEC "$CQLSH_CONNECT -k $KEYSPACE -e 'DESCRIBE TABLES;'" | xargs)

echo "tables: ${TABLES}"

echo "=== Truncate all tables ==="
for table in ${TABLES}; do
    if [[ $table != *"<empty>"* ]]; then
        echo "TRUNCATE TABLE $KEYSPACE.$table"
        $DOCKER_EXEC "$CQLSH_CONNECT -k $KEYSPACE -e 'TRUNCATE TABLE $table;'" &
        PIDS+=($!)
    fi
done

for pid in "${PIDS[@]}";do
    wait $pid
    ret=$?
    if [ $ret -ne 0 ]; then
        echo "Fail to TRUNCATE some tables" >&2
        exit $ret
    fi
done
