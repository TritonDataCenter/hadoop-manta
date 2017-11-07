terraform {
  required_version = "~> 0.10.6"
}

#
# Providers.
#
provider "triton" {
  version = "~> 0.2.0"
}

#
# Required variables.
#
variable "project_name" {
  description = "The name of this project. This value may be used for naming resources."
}

variable "triton_account_uuid" {
  description = "The Triton account UUID."
}

variable "manta_url" {
  default     = "https://us-east.manta.joyent.com/"
  description = "The URL of the Manta service endpoint."
}

variable "manta_user" {
  description = "The account name used to access the Manta service.."
}

variable "manta_key_id" {
  description = "The fingerprint for the public key used to access the Manta service."
}

variable "manta_key" {
  description = "The private key data for the Manta service credentials."
}

#
# Default Variables.
#
variable "triton_region" {
  default     = "us-east-1"
  description = "The region to provision resources within."
}

variable "network_name_private" {
  default     = "My-Fabric-Network"
  description = "The network name for private network access.."
}

variable "count_drill_workers" {
  default     = "3"
  description = "The number of Drill workers to provision."
}

variable "key_path_public" {
  default     = "~/.ssh/id_rsa.pub"
  description = "Path to the public key to use for connecting to machines."
}

variable "key_path_private" {
  default     = "~/.ssh/id_rsa"
  description = "Path to the private key to use for connecting to machines."
}

variable "machine_zookeeper_package_zone" {
  default     = "g4-general-4G"
  description = "Machine package size to use."
}

variable "machine_drill_package_zone" {
  default     = "g4-general-16G"
  description = "Machine package size to use."
}

variable "version_hadoop" {
  default     = "2.8.1"
  description = "The version of Hadoop to install. See https://hadoop.apache.org/releases.html."
}

variable "version_hadoop_manta" {
  default     = "1.0.7-snapshot"
  description = "The version of Hadoop Manta filesystem driver to install. See https://github.com/joyent/hadoop-manta."
}

variable "version_drill" {
  default     = "1.11.0"
  description = "The version of Drill to install. See https://drill.apache.org/download/."
}

variable "md5_drill" {
  default     = "04a6585e318e3a09ac17c41f9228f9ec"
  description = "The MD5 checksum of the Apache Drill tarball."
}

variable "version_zookeeper" {
  default     = "3.4.10"
  description = "The version of Zookeeper to install. See https://zookeeper.apache.org/releases.html."
}

variable "version_zk_cli" {
  default     = "1.4"
  description = "The version of Zookeeper CLI to install. See https://oss.sonatype.org/content/groups/public/com/loopfor/zookeeper/zookeeper-cli/."
}

#
# Data Sources
#
data "triton_image" "ubuntu" {
  name        = "ubuntu-16.04"
  type        = "lx-dataset"
  most_recent = true
}

# TODO(clstokes): this configuration will eventually be a module and networks
# will be provided as an input variable so that only private networks
# can be used.
data "triton_network" "public" {
  name = "Joyent-SDC-Public"
}

data "triton_network" "private" {
  name = "${var.network_name_private}"
}

#
# Locals
#
locals {
  cns_service_drill     = "drill"
  cns_service_zookeeper = "zookeeper"
  address_zookeeper     = "${local.cns_service_zookeeper}.svc.${var.triton_account_uuid}.${var.triton_region}.cns.joyent.com"

  tag_role_drill     = "${var.project_name}-drill"
  tag_role_zookeeper = "${var.project_name}-zookeeper"
}

#
# Outputs
#
output "drill_ip_public" {
  value = ["${triton_machine.drill.*.primaryip}"]
}

output "zookeeper_ip_public" {
  value = ["${triton_machine.zookeeper.*.primaryip}"]
}
