resource "triton_firewall_rule" "ssh" {
  rule    = "FROM any TO all vms ALLOW tcp PORT 22"
  enabled = true
}

# TODO(clstokes): Tighten this up.
resource "triton_firewall_rule" "all" {
  rule    = "FROM subnet 10.0.0.0/8 TO all vms ALLOW tcp PORT all"
  enabled = true
}
