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
#     $1: the version of zookeeper
function check_arguments() {
  local -r version_zookeeper=${1}

  if [[ -z "${version_zookeeper}" ]]; then
    log "Zookeeper version NOT provided. Exiting..."
    exit 1
  fi

}

# install - downloads and installs the specified tool and version
#
# Parameters:
#     $1: the version of zookeeper
#     $2: the version of hadoop-manta
function install_zookeeper() {
  local -r version_zookeeper=${1}

  local -r user_zookeeper='zookeeper'

  local -r path_file="apache-zookeeper-${version_zookeeper}.tar.gz"
  local -r path_install="/usr/local/zookeeper-${version_zookeeper}"

  log "Downloading zookeeper ${version_zookeeper}..."
  wget -O ${path_file} "http://mirrors.sonic.net/apache/zookeeper/zookeeper-${version_zookeeper}/zookeeper-${version_zookeeper}.tar.gz"

  log "Installing zookeeper ${version_zookeeper}..."

  useradd ${user_zookeeper} || log "User [${user_zookeeper}] already exists. Continuing..."

  install -d -o ${user_zookeeper} -g ${user_zookeeper} ${path_install}
  tar -xzf ${path_file} -C /usr/local/

  log "Configuring Zookeeper service..."

  install -d -o ${user_zookeeper} -g ${user_zookeeper} /etc/zookeeper/conf
  install -d -o ${user_zookeeper} -g ${user_zookeeper} /var/lib/zookeeper
  install -d -o ${user_zookeeper} -g ${user_zookeeper} /var/log/zookeeper

  local -r pid_dir="/var/run/zookeeper"
  local -r pid_file="${pid_dir}/zookeeper.pid"

  /usr/bin/printf "
tickTime=2000
dataDir=/var/lib/zookeeper/
clientPort=2181
" > /etc/zookeeper/conf/zoo.cfg

  /usr/bin/printf "
[Unit]
Description=Zookeeper
Documentation=https://zookeeper.apache.org/doc/r3.1.2/zookeeperAdmin.html
After=network-online.target

[Service]
User=zookeeper
Type=forking
RuntimeDirectory=zookeeper
Environment=JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre/
Environment=ZOOPIDFILE=${pid_file}
Environment=ZOO_LOG_DIR=/var/log/zookeeper/
ExecStart=${path_install}/bin/zkServer.sh start /etc/zookeeper/conf/zoo.cfg
ExecStop=${path_install}/bin/zkServer.sh stop /etc/zookeeper/conf/zoo.cfg

[Install]
WantedBy=default.target
" > /etc/systemd/system/zookeeper.service

  log "Starting Zookeeper..."
  systemctl daemon-reload

  systemctl enable zookeeper.service
  systemctl start zookeeper.service

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

  local -r arg_version_zookeeper=$(/native/usr/sbin/mdata-get 'version_zookeeper')
  check_arguments ${arg_version_zookeeper}

  install_dependencies
  install_zookeeper ${arg_version_zookeeper}

  log "Done."
}

main
