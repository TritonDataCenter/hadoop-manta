resource "triton_machine" "drill" {
  count = "${var.count_drill_workers}"

  # NOTE: the machine name is used by Triton CNS and in drill-env.sh in the drill install script
  name    = "${local.tag_role_drill}-${count.index}"
  package = "${var.machine_drill_package_zone}"
  image   = "${data.triton_image.ubuntu.id}"

  firewall_enabled = true

  networks = [
    "${data.triton_network.private.id}",
    "${data.triton_network.public.id}",
  ]

  tags {
    role = "${local.tag_role_drill}"
  }

  cns {
    services = ["${local.cns_service_drill}"]
  }

  metadata {
    version_zk_cli       = "${var.version_zk_cli}"
    version_drill        = "${var.version_drill}"
    md5_drill            = "${var.md5_drill}"
    version_hadoop_manta = "${var.version_hadoop_manta}"
    name_machine         = "${var.project_name}-drill-${count.index}"
    triton_account_uuid  = "${var.triton_account_uuid}"
    triton_region        = "${var.triton_region}"
    address_zookeeper    = "${local.address_zookeeper}"

    manta_url    = "${var.manta_url}"
    manta_user   = "${var.manta_user}"
    manta_key_id = "${var.manta_key_id}"
    manta_key    = "${var.manta_key}"
  }

  depends_on = [
    # explicitly depend on the zookeeper machine because we're using the CNS address
    # and not directly depending on the resource otherwise
    "triton_machine.zookeeper",
    "null_resource.zookeeper_install",

    "triton_firewall_rule.drill_to_drill",
    "triton_firewall_rule.drill_to_zookeeper",
  ]
}

# This is separate from the triton_machine resource, because the firewall ports
# need to be open first.
resource "null_resource" "drill_install" {
  count = "${var.count_drill_workers}"

  depends_on = [
    "triton_machine.drill",
    "triton_firewall_rule.ssh",
  ]

  connection {
    host        = "${triton_machine.drill.*.primaryip[count.index]}"
    user        = "root"
    private_key = "${file(var.key_path_private)}"
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir -p /tmp/drill_installer/",
    ]
  }

  provisioner "file" {
    source      = "./scripts/install_drill.sh"
    destination = "/tmp/drill_installer/install_drill.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod 0755 /tmp/drill_installer/install_drill.sh",
      "/tmp/drill_installer/install_drill.sh",
    ]
  }
}
