#!/bin/bash
#
# Installs Apache Drill with some customizations specific to the overall project.
#
# Note: Generally follows guidelines at https://web.archive.org/web/20170701145736/https://google.github.io/styleguide/shell.xml.
#

# Here we redirect the STDOUT and the STDERR to a log file so that we can debug
# things when they go wrong when starting up an instance.

# Close STDOUT file descriptor
exec 1<&-
# Close STDERR FD
exec 2<&-

# Open STDOUT as $LOG_FILE file for read and write.
exec 1<>/var/log/install-`date +%s`.log

# Redirect STDERR to STDOUT
exec 2>&1

set -o errexit
set -o pipefail
set -o nounset

export DEBIAN_FRONTEND=noninteractive

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
  log "Updating package index..."
  sudo apt-get -qq -y update
  log "Upgrading existing packages"
  sudo apt-get -qq -y upgrade
  log "Installing prerequisites..."
  sudo apt-get -qq -y install --no-install-recommends \
    wget openjdk-8-jdk-headless openjdk-8-dbg htop libnss3 dc \
    unattended-upgrades unzip jq netcat libsnappy1v5

  # Allow for unattended security updates
  log "Configuring unattended security patches"
  sudo cat << 'EOF' > /etc/apt/apt.conf.d/20auto-upgrades
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
EOF
}

# install_zk_cli - Installs a stand-alone zookeeper CLI utility.
# Parameters:
#     $1: the version of Zookeeper CLI
function install_zk_cli() {
  local -r version_zk_cli=${1}
  log "Installing Zookeeper CLI utility"
  wget -q -O /tmp/zookeeper-cli-${version_zk_cli}.zip https://oss.sonatype.org/content/groups/public/com/loopfor/zookeeper/zookeeper-cli/${version_zk_cli}/zookeeper-cli-${version_zk_cli}.zip
  echo "4f2de47d5cad9cf77dc43b8bf0613414  /tmp/zookeeper-cli-${version_zk_cli}.zip" | md5sum -c
  unzip /tmp/zookeeper-cli-${version_zk_cli}.zip -d /usr/local/
  chmod +x /usr/local/zookeeper-cli-${version_zk_cli}/bin/zk
  ln -s /usr/local/zookeeper-cli-${version_zk_cli}/bin/zk /usr/local/bin/zk
}

# configure_jvm - configures cryptographic extensions for Manta and installs
#                 helpers that allow for Java to run efficiently on LX.
# Parameters:
#     None.
function configure_jvm() {
  echo "JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre" >> /etc/environment

  log "Adding libnss PKCS11 extensions to the JVM"

  # Add libnss PKCS11 configuration
  cat << 'EOF' > /etc/nss.cfg
name = NSS
nssLibraryDirectory = /usr/lib/x86_64-linux-gnu
nssDbMode = noDb
attributes = compatibility
EOF

  perl -0777 -i.original -pe \
    's/security.provider.1=sun.security.provider.Sun\nsecurity.provider.2=sun.security.rsa.SunRsaSign\nsecurity.provider.3=sun.security.ec.SunEC\nsecurity.provider.4=com.sun.net.ssl.internal.ssl.Provider\nsecurity.provider.5=com.sun.crypto.provider.SunJCE\nsecurity.provider.6=sun.security.jgss.SunProvider\nsecurity.provider.7=com.sun.security.sasl.Provider\nsecurity.provider.8=org.jcp.xml.dsig.internal.dom.XMLDSigRI\nsecurity.provider.9=sun.security.smartcardio.SunPCSC/security.provider.1=sun.security.pkcs11.SunPKCS11 \/etc\/nss.cfg\nsecurity.provider.2=sun.security.provider.Sun\nsecurity.provider.3=sun.security.rsa.SunRsaSign\nsecurity.provider.4=sun.security.ec.SunEC\nsecurity.provider.5=com.sun.net.ssl.internal.ssl.Provider\nsecurity.provider.6=com.sun.crypto.provider.SunJCE\nsecurity.provider.7=sun.security.jgss.SunProvider\nsecurity.provider.8=com.sun.security.sasl.Provider\nsecurity.provider.9=org.jcp.xml.dsig.internal.dom.XMLDSigRI\nsecurity.provider.10=sun.security.smartcardio.SunPCSC/igs' \
    /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/java.security

  log "Add CPU count spoofer library"
  mkdir -p /usr/local/numcpus
  wget -q -O /usr/local/numcpus/libnumcpus.so https://github.com/dekobon/libnumcpus/releases/download/1.0/libnumcpus-linux-x86_64.so
  echo '6bc838db493d70a83dba9bd3d34c013d368339f288efc4629830fdcb25fa08f7  /usr/local/numcpus/libnumcpus.so' | sha256sum -c

  log "Adding LX process tuning script"
  # Adds thread calculator script that allows you to tune JVM threadpools to
  # non-pathological values when running in a zone.
  cat <<'EOF' > /usr/local/bin/proclimit
#!/usr/bin/env sh

##
# When this script is invoked inside of a zone:
#
# This script returns a number representing a very conservative estimate of the
# maximum number of processes or threads that you want to run within the zone
# that invoked this script. Typically, you would take this value and define a
# multiplier that works well for your application.
#
# Otherwise:
# This script returns the number of cores reported by the OS.

# If we are on a LX Brand Zone calculation value using utilities only available in the /native
# directory

if [ -d /native ]; then
  PATH=/native/sbin:/native/usr/bin:/native/sbin:$PATH
fi

KSH="$(which ksh93)"
PRCTL="$(which prctl)"

if [ -n "${KSH}" ] && [ -n "${PRCTL}" ]; then
  CAP=$(${KSH} -c "echo \$((\$(${PRCTL} -n zone.cpu-cap -P \$\$ | grep privileged | awk '{ print \$3; }') / 100))")

  # If there is no cap set, then we will fall through and use the other functions
  # to determine the maximum processes.
  if [ -n "${CAP}" ]; then
    $KSH -c "echo \$((ceil(${CAP})))"
    exit 0
  fi
fi

# Linux calculation if you have nproc
if [ -n "$(which nproc)" ]; then
  nproc
  exit 0
fi

# Linux more widely supported implementation
if [ -f /proc/cpuinfo ] && [ -n $(which wc) ]; then
  grep processor /proc/cpuinfo | wc -l
  exit 0
fi

# OS X calculation
if [ "$(uname)" == "Darwin" ]; then
  sysctl -n hw.ncpu
  exit 0
fi

# Fallback value if we can't calculate
echo 1
EOF
  chmod +x /usr/local/bin/proclimit
}

# check_arguments - exits if arguments are NOT satisfied
#
# Parameters:
#     $1: the version of ZK CLI
#     $2: the version of drill
#     $3: the drill tarball md5
#     $4: the version of hadoop-manta
#     $5: the machine name
#     $6: the triton account uuid
#     $7: the triton region
#     $8: the zookeeper address
#     $9: the manta url
#     $10: the manta user
#     $11: the manta key id
#     $12: the manta private key
function check_arguments() {
  local -r version_zk_cli=${1}
  local -r version_drill=${2}
  local -r md5_drill=${3}
  local -r version_hadoop_manta=${4}
  local -r name_machine=${5}
  local -r triton_account_uuid=${6}
  local -r triton_region=${7}
  local -r address_zookeeper=${8}
  local -r manta_url=${9}
  local -r manta_user=${10}
  local -r manta_key_id=${11}
  local -r manta_key=${12}

  if [[ -z "${version_zk_cli}" ]]; then
    log "ZK CLI version NOT provided. Exiting..."
    exit 1
  fi

  if [[ -z "${version_drill}" ]]; then
    log "Drill version NOT provided. Exiting..."
    exit 1
  fi

  if [[ -z "${md5_drill}" ]]; then
    log "Drill md5 NOT provided. Exiting..."
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
#     $2: the md5 checksum of the drill tarball
#     $3: the version of hadoop-manta
#     $4: the machine name
#     $5: the triton account uuid
#     $6: the triton region
#     $7: the zookeeper address
#     $8: the manta url
#     $9: the manta user
#     $10: the manta key id
#     $11: the manta private key
function install_drill() {
  local -r version_drill=${1}
  local -r md5_drill=${2}
  local -r version_hadoop_manta=${3}
  local -r name_machine=${4}
  local -r triton_account_uuid=${5}
  local -r triton_region=${6}
  local -r address_zookeeper=${7}
  local -r manta_url=${8}
  local -r manta_user=${9}
  local -r manta_key_id=${10}
  local -r manta_key=${11}

  local -r user_drill='drill'

  local -r path_file="apache-drill-${version_drill}.tar.gz"
  local -r path_install="/usr/local/apache-drill-${version_drill}"

  log "Downloading Drill ${version_drill}..."
  wget -q -O ${path_file} "http://mirrors.sonic.net/apache/drill/drill-${version_drill}/apache-drill-${version_drill}.tar.gz"
  echo "${md5_drill}  ${path_file}" | md5sum -c

  log "Installing Drill ${version_drill}..."

  useradd ${user_drill} || log "User [${user_drill}] already exists. Continuing..."

  install -d -o ${user_drill} -g ${user_drill} ${path_install}
  tar -xzf ${path_file} -C /usr/local/

  local -r manta_path_file="hadoop-manta-${version_hadoop_manta}-jar-with-dependencies.jar"
  local -r manta_path_install="${path_install}/jars/"

  log "Downloading hadoop-manta ${version_hadoop_manta}..."
  wget -q -O ${manta_path_file} "https://github.com/joyent/hadoop-manta/releases/download/hadoop-manta-${version_hadoop_manta}/hadoop-manta-${version_hadoop_manta}-jar-with-dependencies.jar"

  log "Installing hadoop-manta ${version_hadoop_manta}..."

  install -o ${user_drill} -g ${user_drill} ${manta_path_file} ${manta_path_install}

  log "Configuring Drill service..."

  install -d -o ${user_drill} -g ${user_drill} /etc/drill/conf
  install -d -o ${user_drill} -g ${user_drill} /etc/drill/conf/lib
  install -d -o ${user_drill} -g ${user_drill} /var/lib/drill
  install -d -o ${user_drill} -g ${user_drill} /var/log/drill
  install -d -o ${user_drill} -g ${user_drill} /etc/manta/

  # Add Hadoop native libraries
  curl -s "https://us-east.manta.joyent.com/elijah.zupancic/public/hadoop/hadoop-2.8.1-native-libs.tar.gz" \
    | tar -xz -C /etc/drill/conf/lib && chown -R ${user_drill}:${user_drill} /etc/drill/conf/lib

  # Linking in native compression libraries
  ln -s /usr/lib/x86_64-linux-gnu/libsnappy.so.1 /etc/drill/conf/lib/libsnappy.so
  ln -s /lib/x86_64-linux-gnu/libz.so.1 /etc/drill/conf/lib/libz.so
  ln -s /lib/x86_64-linux-gnu/libbz2.so.1 /etc/drill/conf/lib/libbz2.so

  local -r pid_dir="/var/run/drill"
  local -r pid_file="${pid_dir}/drillbit.pid"

  /usr/bin/printf "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>
<configuration>
 <!-- Enable the use of the Snappy compression codec -->
 <property>
  <name>io.compression.codecs</name>
  <value>org.apache.hadoop.io.compress.SnappyCodec</value>
 </property>
 <!-- Explicitly enable and define Manta filesystem support -->
 <property>
  <name>fs.manta.impl</name>
  <value>com.joyent.hadoop.fs.manta.MantaFileSystem</value>
 </property>
</configuration>
" > /etc/drill/conf/core-site.xml

  /usr/bin/printf "
export DRILL_HOST_NAME='${name_machine}.inst.${triton_account_uuid}.${triton_region}.cns.joyent.com'
export DRILL_PID_DIR='${pid_dir}'
export DRILL_LOG_DIR='/var/log/drill'
export HW_THREADS=\$(/usr/local/bin/proclimit)
export _NUM_CPUS=\$HW_THREADS
# This setting has been deprecated in the 1.8 JVM, so we don't set it
export DRILLBIT_JAVA_OPTS='-XX:+PrintFlagsFinal'

if [ -d /native ]; then
  # Tune GC thread pool to a non-pathological value
  if [ \$HW_THREADS -le 8 ]; then
      GC_THREADS=\$HW_THREADS
  else
      # ParallelGCThreads = (ncpus <= 8) ? ncpus : 3 + ((ncpus * 5) / 8)
      ADJUSTED=\$(echo \"8k \$HW_THREADS 5 * pq\" | dc)
      DIVIDED=\$(echo \"8k \$ADJUSTED 8 / pq\" | dc)
      GC_THREADS=\$(echo \"8k \$DIVIDED 3 + pq\" | dc | awk 'function ceil(valor) { return (valor == int(valor) && value != 0) ? valor : int(valor)+1 } { printf \"%%d\", ceil(\$1) }')
  fi

  if [ \$HW_THREADS -le 2 ]; then
      PARALLELISM=2
  else
      PARALLELISM=\$HW_THREADS
  fi;

  export DRILLBIT_JAVA_OPTS=\"\${DRILLBIT_JAVA_OPTS} -XX:-UseGCTaskAffinity -XX:-BindGCTaskThreadsToCPUs -XX:ParallelGCThreads=\${GC_THREADS} -Djava.util.concurrent.ForkJoinPool.common.parallelism=\${PARALLELISM}\"
  echo \"DRILLBIT_JAVA_OPTS=\${DRILLBIT_JAVA_OPTS}\"

  echo \"Loading drill with LD_PRELOAD=/usr/local/numcpus/libnumcpus.so to simulate \${_NUM_CPUS}\"
  export _DRILL_WRAPPER_=\"env LD_PRELOAD=/usr/local/numcpus/libnumcpus.so\"
fi

  # Heap size with drill is a fixed value because there is little variable usage.
  DRILL_HEAP_KB=\"4194304\"
  export DRILL_HEAP=\"\${DRILL_HEAP_KB}K\"

  # Tune memory to use most of the available memory on the zone
  TOTAL_MEMORY_KB=\$(cat /proc/meminfo | grep MemTotal | cut -d: -f2 | sed 's/^ *//' | cut -d' ' -f1)
  # We reserve 25%% of the total memory
  RESERVED_KB=\$(echo \"8k \$TOTAL_MEMORY_KB .25 * pq\" | dc)
  USABLE_MEMORY_KB=\$(echo \"8k \$TOTAL_MEMORY_KB \$RESERVED_KB - pq\" | dc)
  MINUS_HEAP=\$(echo \"8k \$USABLE_MEMORY_KB \$DRILL_HEAP_KB - pq\" | dc | awk 'function ceil(valor) { return (valor == int(valor) && value != 0) ? valor : int(valor)+1 } { printf \"%%d\", ceil(\$1) }')

  # We auto-calculate the amount of direct memory used on the zone because
  # it has variable usage with drill.
  export DRILL_MAX_DIRECT_MEMORY=\"\${MINUS_HEAP}K\"
  echo \"DRILL_MAX_DIRECT_MEMORY=\$DRILL_MAX_DIRECT_MEMORY\"
" > /etc/drill/conf/drill-env.sh

  /usr/bin/printf "
drill.exec: {
  cluster-id: \"drillbits1\",
  zk.connect: \"${address_zookeeper}:2181\",
  rpc.bit.advertised.host: \"${name_machine}.inst.${triton_account_uuid}.${triton_region}.cns.joyent.com\",
  udf.directory.root: \"/var/lib/drill\",
  drill.exec.http.ssl_enabled: \"true\"
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
Environment=JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre
ExecStart=${path_install}/bin/drillbit.sh start
ExecStop=${path_install}/bin/drillbit.sh stop
Restart=always
RestartSec=10

[Install]
WantedBy=default.target
" > /etc/systemd/system/drill.service

  # Add the Zookeeper CNS name to a global env var to aid with debugging
  echo "ZOOKEEPER=${address_zookeeper}:2181" >> /etc/environment

  while [ "$(echo 'stat zookeeper' | zk ${address_zookeeper}:2181 | grep 'czxid')" == "" ]; do
    log "Waiting for Zookeeper to come online before running Apache Drill"
    sleep 10
  done

  log "Starting Drill..."
  systemctl daemon-reload

  systemctl enable drill.service
  systemctl start drill.service

  # We wait for the first Drill instance to come online. It doesn't matter what
  # instance it is. All we care is that there was a single healthy instance that
  # boot up. This means that most of the drillbits should be coming online soon.
  while [ "$(echo 'stat drill/drillbits1' | zk ${address_zookeeper}:2181 | grep 'czxid')" == "" ]; do
    log "Waiting for a single Apache Drill instance to come online"
    sleep 10
  done

  # Wait until the Drillbit server port is online
  while ! nc -z localhost 8047; do
    sleep 5
  done

  # If the Drillbit plugin config hasn't been initialized, then we initialize
  # it to settings that we can handle data from Manta.
  if [[ "$(curl --retry 6 -S -s 'http://localhost:8047/storage/manta.json' | jq .config)" == "null" ]]; then
    log "Configuring hadoop-manta storage plugin..."
    curl --retry 6 -s -S -X POST \
      -H 'Content-Type: application/json;charset=UTF-8' \
      -d "$(get_manta_plugin_config)" \
      'http://localhost:8047/storage/myplugin.json'
  fi

  log "Finished installing Apache Drill"
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
      "default": {
        "location": "manta:/~~/stor/",
        "writable": true,
        "defaultInputFormat": null
      },
      "root": {
        "location": "manta:/",
        "writable": true,
        "defaultInputFormat": null
      },
      "tmp": {
        "location": "manta:/~~/stor/tmp",
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
          "ndjson", "json", "log", "gz", "snappy", "bz2"
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

  local -r arg_version_zk_cli=$(/native/usr/sbin/mdata-get 'version_zk_cli')
  local -r arg_version_drill=$(/native/usr/sbin/mdata-get 'version_drill')
  local -r arg_md5_drill=$(/native/usr/sbin/mdata-get 'md5_drill')
  local -r arg_version_hadoop_manta=$(/native/usr/sbin/mdata-get 'version_hadoop_manta')
  local -r arg_name_machine=$(/native/usr/sbin/mdata-get 'name_machine')
  local -r arg_cns_service_zookeeper=$(/native/usr/sbin/mdata-get 'cns_service_zookeeper')
  local -r arg_manta_url=$(/native/usr/sbin/mdata-get 'manta_url')
  local -r arg_manta_user=$(/native/usr/sbin/mdata-get 'manta_user')
  local -r arg_manta_key_id=$(/native/usr/sbin/mdata-get 'manta_key_id')
  local -r arg_manta_key=$(/native/usr/sbin/mdata-get 'manta_key')

  local -r arg_triton_account_uuid=$(/native/usr/sbin/mdata-get 'sdc:owner_uuid') # see https://eng.joyent.com/mdata/datadict.html
  local -r arg_triton_region=$(/native/usr/sbin/mdata-get 'sdc:datacenter_name') # see https://eng.joyent.com/mdata/datadict.html
  local -r arg_address_zookeeper="${arg_cns_service_zookeeper}.svc.${arg_triton_account_uuid}.${arg_triton_region}.cns.joyent.com"

  check_arguments \
    ${arg_version_zk_cli} ${arg_version_drill} ${arg_md5_drill} ${arg_version_hadoop_manta} \
    ${arg_name_machine} ${arg_triton_account_uuid} ${arg_triton_region} ${arg_address_zookeeper} \
    ${arg_manta_url} ${arg_manta_user} ${arg_manta_key_id} "${arg_manta_key}"

  install_dependencies
  configure_jvm
  install_zk_cli ${arg_version_zk_cli}
  install_drill \
    ${arg_version_drill} ${arg_md5_drill} ${arg_version_hadoop_manta} \
    ${arg_name_machine} ${arg_triton_account_uuid} ${arg_triton_region} ${arg_address_zookeeper} \
    ${arg_manta_url} ${arg_manta_user} ${arg_manta_key_id} "${arg_manta_key}"

  log "Done."
}

main
