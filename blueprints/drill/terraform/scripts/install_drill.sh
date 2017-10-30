#!/bin/bash
#
# Installs Apache Drill with some customizations specific to the overall project.
#
# Note: Generally follows guidelines at https://web.archive.org/web/20170701145736/https://google.github.io/styleguide/shell.xml.
#

set -e

# check_prerequisites - exits if distro is not supported.
#
# Parameters:
#     None.
function check_prerequisites() {
  local distro
  if [[ -f "/etc/lsb-release" ]]; then
    distro="Ubuntu"
  fi

  if [[ -z "${distro}" ]]; then
    log "Unsupported platform. Exiting..."
    exit 1
  fi
}

# install_dependencies - installs dependencies
#
# Parameters:
#     -
function install_dependencies() {
  echo "Updating package index..."
  sudo apt-get -qq -y update
  echo "Installing prerequisites..."
  sudo apt-get -qq -y install wget openjdk-8-jdk
}

# check_arguments - exits if arguments are NOT satisfied
#
# Parameters:
#     $1: the version of drill
#     $2: the version of hadoop-manta
#     $3: the machine name
#     $4: the triton account uuid
#     $5: the triton region
#     $6: the zookeeper address
#     $7: the manta url
#     $8: the manta user
#     $9: the manta key id
#     $10: the manta private key
function check_arguments() {
  local -r version_drill=${1}
  local -r version_hadoop_manta=${2}
  local -r name_machine=${3}
  local -r triton_account_uuid=${4}
  local -r triton_region=${5}
  local -r address_zookeeper=${6}
  local -r manta_url=${7}
  local -r manta_user=${8}
  local -r manta_key_id=${9}
  local -r manta_key=${10}

  if [[ -z "${version_drill}" ]]; then
    log "Drill version NOT provided. Exiting..."
    exit 1
  fi

  if [[ -z "${version_hadoop_manta}" ]]; then
    log "Hadoop-Manta version NOT provided. Exiting..."
    exit 1
  fi

  if [[ -z "${name_machine}" ]]; then
    log "Machine name NOT provided. Exiting..."
    exit 1
  fi

  if [[ -z "${triton_account_uuid}" ]]; then
    log "Triton account uuid NOT provided. Exiting..."
    exit 1
  fi

  if [[ -z "${triton_region}" ]]; then
    log "Triton region NOT provided. Exiting..."
    exit 1
  fi

  if [[ -z "${address_zookeeper}" ]]; then
    log "Zookeeper address NOT provided. Exiting..."
    exit 1
  fi

  if [[ -z "${manta_url}" ]]; then
    log "No Manta URL provided. Exiting..."
    exit 1
  fi

  if [[ -z "${manta_user}" ]]; then
    log "No Manta user provided. Exiting..."
    exit 1
  fi

  if [[ -z "${manta_key_id}" ]]; then
    log "No Manta key id provided. Exiting..."
    exit 1
  fi

  if [[ -z "${manta_key}" ]]; then
    log "No Manta key path provided. Exiting..."
    exit 1
  fi

}

# install - downloads and installs the specified tool and version
#
# Parameters:
#     $1: the version of drill
#     $2: the version of hadoop-manta
#     $3: the machine name
#     $4: the triton account uuid
#     $5: the triton region
#     $6: the zookeeper address
#     $7: the manta url
#     $8: the manta user
#     $9: the manta key id
#     $10: the manta private key
function install_drill() {
  local -r version_drill=${1}
  local -r version_hadoop_manta=${2}
  local -r name_machine=${3}
  local -r triton_account_uuid=${4}
  local -r triton_region=${5}
  local -r address_zookeeper=${6}
  local -r manta_url=${7}
  local -r manta_user=${8}
  local -r manta_key_id=${9}
  local -r manta_key=${10}

  local -r user_drill='drill'

  local -r path_file="apache-drill-${version_drill}.tar.gz"
  local -r path_install="/usr/local/apache-drill-${version_drill}"

  log "Downloading Drill ${version_drill}..."
  wget -O ${path_file} "http://mirrors.sonic.net/apache/drill/drill-${version_drill}/apache-drill-${version_drill}.tar.gz"

  log "Installing Drill ${version_drill}..."

  useradd ${user_drill} || log "User [${user_drill}] already exists. Continuing..."

  install -d -o ${user_drill} -g ${user_drill} ${path_install}
  tar -xzf ${path_file} -C /usr/local/

  local -r manta_path_file="hadoop-manta-${version_hadoop_manta}-jar-with-dependencies.jar"
  local -r manta_path_install="${path_install}/jars/"

  log "Downloading hadoop-manta ${version_hadoop_manta}..."
  wget -O ${manta_path_file} "https://github.com/joyent/hadoop-manta/releases/download/hadoop-manta-${version_hadoop_manta}/hadoop-manta-${version_hadoop_manta}-jar-with-dependencies.jar"

  log "Installing hadoop-manta ${version_hadoop_manta}..."

  install -o ${user_drill} -g ${user_drill} ${manta_path_file} ${manta_path_install}

  log "Configuring Drill service..."

  install -d -o ${user_drill} -g ${user_drill} /etc/drill/conf
  install -d -o ${user_drill} -g ${user_drill} /var/lib/drill
  install -d -o ${user_drill} -g ${user_drill} /var/log/drill
  install -d -o ${user_drill} -g ${user_drill} /etc/manta/

  local -r pid_dir="/var/run/drill"
  local -r pid_file="${pid_dir}/drillbit.pid"

  /usr/bin/printf "
export DRILL_HOST_NAME='${name_machine}.inst.${triton_account_uuid}.${triton_region}.cns.joyent.com'
export DRILL_PID_DIR='${pid_dir}'
export DRILL_LOG_DIR='/var/log/drill'
" > /etc/drill/conf/drill-env.sh

  /usr/bin/printf "
drill.exec: {
  cluster-id: \"drillbits1\",
  zk.connect: \"${address_zookeeper}:2181\"
  rpc.bit.advertised.host: \"${name_machine}.inst.${triton_account_uuid}.${triton_region}.cns.joyent.com\"
  udf.directory.root: \"/var/lib/drill\"
}
" > /etc/drill/conf/drill-override.conf

  /usr/bin/printf "${manta_key}" > /etc/manta/manta_key

  /usr/bin/printf "
[Unit]
Description=Drill
Documentation=https://drill.apache.org/docs/configure-drill/
After=network-online.target

[Service]
User=${user_drill}
Type=forking
PIDFile=${pid_file}
RuntimeDirectory=drill
Environment=MANTA_URL=${manta_url}
Environment=MANTA_USER=${manta_user}
Environment=MANTA_KEY_ID=${manta_key_id}
Environment=MANTA_KEY_PATH=/etc/manta/manta_key
Environment=JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre/
ExecStart=${path_install}/bin/drillbit.sh start
ExecStop=${path_install}/bin/drillbit.sh stop

[Install]
WantedBy=default.target
" > /etc/systemd/system/drill.service

  log "Starting Drill..."
  systemctl daemon-reload

  systemctl enable drill.service
  systemctl start drill.service

  # TODO(clstokes): do this more gracefully - wait for open port 8047?
  log "Waiting a bit before configuring manta plugin..."
  sleep 30

  log "Configuring hadoop-manta storage plugin..."

  curl 'http://localhost:8047/storage/myplugin.json' \
    -X POST \
    -H 'Content-Type: application/json;charset=UTF-8' \
    -d "$(get_manta_plugin_config)"

}

function get_manta_plugin_config() {
  local -r config_manta=$(cat <<EOF
{
  "name": "manta",
  "config": {
    "type": "file",
    "enabled": true,
    "connection": "manta:///",
    "config": null,
    "workspaces": {
      "root": {
        "location": "/",
        "writable": false,
        "defaultInputFormat": null
      },
      "tmp": {
        "location": "/tmp",
        "writable": true,
        "defaultInputFormat": null
      }
    },
    "formats": {
      "psv": {
        "type": "text",
        "extensions": [
          "tbl"
        ],
        "delimiter": "|"
      },
      "csv": {
        "type": "text",
        "extensions": [
          "csv"
        ],
        "delimiter": ","
      },
      "tsv": {
        "type": "text",
        "extensions": [
          "tsv"
        ],
        "delimiter": "\t"
      },
      "parquet": {
        "type": "parquet"
      },
      "json": {
        "type": "json",
        "extensions": [
          "json"
        ]
      },
      "avro": {
        "type": "avro"
      },
      "sequencefile": {
        "type": "sequencefile",
        "extensions": [
          "seq"
        ]
      },
      "csvh": {
        "type": "text",
        "extensions": [
          "csvh"
        ],
        "extractHeader": true,
        "delimiter": ","
      }
    }
  }
}
EOF
);

  echo "${config_manta}"
}

# log - prints an informational message
#
# Parameters:
#     $1: the message
function log() {
  local -r message=${1}
  local -r script_name=$(basename ${0})
  echo -e "==> ${script_name}: ${message}"
}

# main
function main() {
  check_prerequisites

  local -r arg_version_drill=$(/native/usr/sbin/mdata-get 'version_drill')
  local -r arg_version_hadoop_manta=$(/native/usr/sbin/mdata-get 'version_hadoop_manta')
  local -r arg_name_machine=$(/native/usr/sbin/mdata-get 'name_machine')
  local -r arg_triton_account_uuid=$(/native/usr/sbin/mdata-get 'triton_account_uuid')
  local -r arg_triton_region=$(/native/usr/sbin/mdata-get 'triton_region')
  local -r arg_address_zookeeper=$(/native/usr/sbin/mdata-get 'address_zookeeper')
  local -r arg_manta_url=$(/native/usr/sbin/mdata-get 'manta_url')
  local -r arg_manta_user=$(/native/usr/sbin/mdata-get 'manta_user')
  local -r arg_manta_key_id=$(/native/usr/sbin/mdata-get 'manta_key_id')
  local -r arg_manta_key=$(/native/usr/sbin/mdata-get 'manta_key')
  check_arguments \
    ${arg_version_drill} ${arg_version_hadoop_manta} \
    ${arg_name_machine} ${arg_triton_account_uuid} ${arg_triton_region} ${arg_address_zookeeper} \
    ${arg_manta_url} ${arg_manta_user} ${arg_manta_key_id} "${arg_manta_key}"

  install_dependencies
  install_drill \
    ${arg_version_drill} ${arg_version_hadoop_manta} \
    ${arg_name_machine} ${arg_triton_account_uuid} ${arg_triton_region} ${arg_address_zookeeper} \
    ${arg_manta_url} ${arg_manta_user} ${arg_manta_key_id} "${arg_manta_key}"

  log "Done."
}

main
