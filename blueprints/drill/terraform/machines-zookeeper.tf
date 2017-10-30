resource "triton_machine" "zookeeper" {
  name    = "${var.project_name}-zookeeper"
  package = "${var.machine_package_zone}"
  image   = "${data.triton_image.ubuntu.id}"

  firewall_enabled = true

  cns {
    services = ["${local.cns_service_zookeeper}"]
  }

  metadata {
    version_zookeeper = "${var.version_zookeeper}"
  }
}

# This is separate from the triton_machine resource, because the firewall ports
# need to be open first.
resource "null_resource" "zookeeper_install" {
  depends_on = [
    "triton_machine.zookeeper",
    "triton_firewall_rule.ssh",
  ]

  connection {
    host        = "${triton_machine.zookeeper.primaryip}"
    user        = "root"
    private_key = "${file(var.key_path_private)}"
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir -p /tmp/zookeeper_installer/",
    ]
  }

  provisioner "file" {
    source      = "./scripts/install_zookeeper.sh"
    destination = "/tmp/zookeeper_installer/install_zookeeper.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod 0755 /tmp/zookeeper_installer/install_zookeeper.sh",
      "/tmp/zookeeper_installer/install_zookeeper.sh",
    ]
  }
}
