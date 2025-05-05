#ssh key
data "digitalocean_ssh_key" "www-1" {
    name = var.do_ssh_key
}

# Create an "app-server" droplet
resource "digitalocean_droplet" "server" {
    name   = "app-server"
    image  = var.do_image
    region = var.do_region
    size   = var.do_size
    ssh_keys = [data.digitalocean_ssh_key.www-1.id]
}


resource "local_file" "ssh_alias" {
    filename        = "root@${digitalocean_droplet.server.ipv4_address}"
    content         = ""
    file_permission = "0444"
}


# Render Ansible inventory from template
resource "local_file" "inventory" {
        filename        = "inventory.yaml"
        content         = templatefile("inventory.yaml.tftpl", {
        droplet_ip        = digitalocean_droplet.server.ipv4_address
        ssh_private_key   = var.ssh_private_key
  })
  file_permission = "0444"
}

# Expose the droplet IP for CI/CD to consume
output "droplet_ip" {
    value = digitalocean_droplet.server.ipv4_address
}