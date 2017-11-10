resource "triton_firewall_rule" "ssh" {
  rule    = "FROM any TO all vms ALLOW tcp PORT 22"
  enabled = true
}

# see https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.2/bk_reference/content/zookeeper-ports.html
resource "triton_firewall_rule" "drill_to_zookeeper" {
  rule    = "FROM tag \"role\" = \"${local.tag_role_drill}\" TO tag \"role\" = \"${local.tag_role_zookeeper}\" ALLOW tcp PORT 2181"
  enabled = true
}

# see https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.2/bk_reference/content/zookeeper-ports.html
resource "triton_firewall_rule" "zookeeper_to_zookeeper" {
  rule    = "FROM tag \"role\" = \"${local.tag_role_zookeeper}\" TO tag \"role\" = \"${local.tag_role_zookeeper}\" ALLOW tcp (PORT 2888 AND PORT 3888)"
  enabled = true
}

# see https://drill.apache.org/docs/ports-used-by-drill/
resource "triton_firewall_rule" "external_to_drill" {
  rule    = "FROM all vms TO tag \"role\" = \"${local.tag_role_drill}\" ALLOW tcp PORT 8047"
  enabled = true
}

# see https://drill.apache.org/docs/ports-used-by-drill/
resource "triton_firewall_rule" "drill_to_drill" {
  rule    = "FROM tag \"role\" = \"${local.tag_role_drill}\" TO tag \"role\" = \"${local.tag_role_drill}\" ALLOW tcp PORTS 31010 - 31012"
  enabled = true
}
