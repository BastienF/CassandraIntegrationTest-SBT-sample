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

--=== deploy.sh ===--

#### Deploy integration tests cassandra container ####


* USAGE: ./deploy.sh [OPTIONS]

* OPTIONS:
  -h                        display this usage
  -v                        active verbose
  --uuid <UUID>             identifiant unique pour ce déploiement (ajouté aux noms des containers)
  --kill                    supprime les containers (annule et remplace --recreate)
  --recreate                supprime et recrée les containers (annule et remplace --kill)
  --port                    port pour un accès CQL à Cassandra

EOF
}

# Defaults
UUID=""
KILL="false"
RECREATE="false"
PORT="9042"
DOCKER_NETWORK=''

params="$(getopt -o hv -l uuid:,kill,recreate,port: --name "$(basename "$0")" -- "$@")"

if [ $? -ne 0 ]
then
    usage
    exit 1
fi

eval set -- "$params"
unset params

while true; do
    case "$1" in
        --uuid)
            case "$2" in
                "") shift 2;;
                *) UUID="$2"; shift 2;;
            esac
            ;;
        --kill)
            KILL="true"
            RECREATE="false"
            shift 1
            ;;
        --recreate)
            RECREATE="true"
            KILL="false"
            shift 1
            ;;
        --port)
            case "$2" in
                "") shift 2;;
                *) PORT="$2"; shift 2;;
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

BASEDIR="$(cd "$( dirname "$0" )" && cd '../../../' && pwd)"

echo "Base directory: $BASEDIR"
DOCKER_CASSANDRA_PORT="-p $PORT:9042"
CASSANDRA_DOCKER_IMAGE="test_dse"
CASSANDRA_HOSTNAME="${CASSANDRA_DOCKER_IMAGE}_${UUID}"
CASSANDRA_DSE_VERSION="5.1.10"

if [ "$KILL" == "false" ]; then
    echo "=== Build custom DSE ==="
    OLD_IMAGE="$(docker images -q ${CASSANDRA_DOCKER_IMAGE}:${CASSANDRA_DSE_VERSION})"
    docker build $BASEDIR/automation/docker/cassandra/ -t ${CASSANDRA_DOCKER_IMAGE}:${CASSANDRA_DSE_VERSION}
    NEW_IMAGE="$(docker images -q ${CASSANDRA_DOCKER_IMAGE}:${CASSANDRA_DSE_VERSION})"

    if [ "$OLD_IMAGE" != "$NEW_IMAGE" ]; then
        echo "INFO: the custom DSE image has been modified, forcing recreate"
        RECREATE="true"
    fi
    KILL="$RECREATE"
fi

if [ "$KILL" == "true" ]; then
    echo '=== Kill docker container (if exist) ==='
    if (docker ps -a --format '{{ .Names }}' | grep -q "^${CASSANDRA_HOSTNAME}\$"); then
        echo -n '  DSE (kill): '
        docker rm -f "${CASSANDRA_HOSTNAME}"
    fi
fi

if [ "$KILL" == "true" -a "$RECREATE" == "false" ]; then
    exit 0
fi

echo "=== Run or start DSE container ==="
if ! (docker ps -a --format '{{ .Names }}' | grep -q "^${CASSANDRA_HOSTNAME}\$"); then
    echo -n "  DSE (run): "
    docker run $DOCKER_NETWORK --name "${CASSANDRA_HOSTNAME}" \
      -v $BASEDIR/automation/docker/cassandra/cql:/etc/conf/cql \
      $DOCKER_CASSANDRA_PORT -d ${CASSANDRA_DOCKER_IMAGE}:${CASSANDRA_DSE_VERSION}
fi
if ! (docker ps --format '{{ .Names }}' | grep -q "^${CASSANDRA_HOSTNAME}\$"); then
    echo -n "  DSE (start): "
    docker start "${CASSANDRA_HOSTNAME}"
fi

echo -n "Wait until Cassandra is ready: "
while (docker exec "${CASSANDRA_HOSTNAME}" bash -c 'echo "DESCRIBE KEYSPACES" | cqlsh "127.0.0.1" -u cassandra -p cassandra 2>&1 | grep -q "Connection refused"'); do
    echo -n "."
    sleep 2
done
echo ""

echo "=== Init cassandra users and keyspaces ==="
docker exec "${CASSANDRA_HOSTNAME}" bash -c 'cat /etc/conf/cql/setup_cassandra.cql | cqlsh "127.0.0.1" -u cassandra -p cassandra'
echo
