variable "service_instance_id" {}
variable "flavor" {}
variable "public_key" {}
variable "external_network_id" {}
variable "external_ip_pool" {}
variable "image_id" {}
variable "dns_nameservers" {
  type="list"
}

provider "openstack" {
  version = "1.9.0"
}

# Create Network Resources

resource "openstack_networking_network_v2" "network_1" {
  name           = "network-${var.service_instance_id}"
  admin_state_up = "true"
}

resource "openstack_networking_subnet_v2" "subnet_1" {
  name            = "subnet-${var.service_instance_id}"
  network_id      = "${openstack_networking_network_v2.network_1.id}"
  cidr            = "192.168.199.0/24"
  dns_nameservers = "${var.dns_nameservers}"
  ip_version      = 4
  allocation_pools = {
    "start" = "192.168.199.100"
    "end"   = "192.168.199.200"
  }
}

resource "openstack_networking_secgroup_v2" "secgroup_1" {
  name = "secgroup-${var.service_instance_id}"
}

resource "openstack_networking_secgroup_rule_v2" "secgroup_rule_1" {
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "tcp"
  port_range_min    = 22
  port_range_max    = 22
  remote_ip_prefix  = "0.0.0.0/0"
  security_group_id = "${openstack_networking_secgroup_v2.secgroup_1.id}"
}

resource "openstack_networking_router_v2" "router_1" {
  name                = "router-${var.service_instance_id}"
  external_network_id = "${var.external_network_id}"
}

resource "openstack_networking_router_interface_v2" "router_interface_1" {
  router_id = "${openstack_networking_router_v2.router_1.id}"
  subnet_id = "${openstack_networking_subnet_v2.subnet_1.id}"
}

resource "openstack_networking_floatingip_v2" "fip_1" {
  pool = "${var.external_ip_pool}"
}

resource "openstack_networking_port_v2" "port_1" {
  network_id     = "${openstack_networking_network_v2.network_1.id}"
  admin_state_up = "true"

  security_group_ids = [
    "${openstack_networking_secgroup_v2.secgroup_1.id}",
  ]

  fixed_ip {
    "subnet_id"  = "${openstack_networking_subnet_v2.subnet_1.id}"
    "ip_address" = "192.168.199.11"
  }
}

# Create Compute Resources

resource "openstack_compute_keypair_v2" "keypair_1" {
  name       = "keypair-${var.service_instance_id}"
  public_key = "${var.public_key}"
}

resource "openstack_compute_instance_v2" "instance" {
  name      = "${var.service_instance_id}"
  image_id  = "${var.image_id}"
  flavor_id = "${var.flavor}"
  key_pair  = "${openstack_compute_keypair_v2.keypair_1.name}"

  network {
    port = "${openstack_networking_port_v2.port_1.id}"
  }
}

resource "openstack_compute_floatingip_associate_v2" "inst_fip_1" {
  floating_ip = "${openstack_networking_floatingip_v2.fip_1.address}"
  instance_id = "${openstack_compute_instance_v2.instance.id}"
}

output "ip" {
  value = "${openstack_networking_floatingip_v2.fip_1.address}"
}
