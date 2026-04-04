# providers.tf — встроен в main.tf для простоты
terraform {
  required_version = ">= 1.0"

  required_providers {
    yandex = {
      source  = "yandex-cloud/yandex"
      version = "~> 0.130"
    }
  }
}

provider "yandex" {
  cloud_id                 = var.cloud_id
  folder_id                = var.folder_id
  zone                     = var.zone
}

# variables.tf — встроен в main.tf
variable "cloud_id" {
  type        = string
  description = "Yandex Cloud ID"
}

variable "folder_id" {
  type        = string
  description = "Folder ID for resources"
}

variable "zone" {
  type        = string
  description = "Default availability zone"
  default     = "ru-central1-a"
}

variable "ssh_public_key" {
  type        = string
  description = "Public SSH key for VM access"
}

variable "subnet_id" {
  type        = string
  description = "Existing subnet ID"
  default     = "fl80id702e4irnblcd63"
}

variable "security_group_id" {
  type        = string
  description = "Existing security group ID (optional)"
  default     = "enpkd9np0qbhc064o9mu"
}

variable "instance_name" {
  type    = string
  default = "poly-bot-vm"
}

# Data sources
data "yandex_vpc_subnet" "main" {
  subnet_id = var.subnet_id
}

data "yandex_compute_image" "ubuntu" {
  family = "ubuntu-2204-lts"
}


# Compute Instance
resource "yandex_compute_instance" "bot_server" {
  name        = var.instance_name
  platform_id = "standard-v3"
  zone        = data.yandex_vpc_subnet.main[0].zone

  resources {
    cores         = 2
    memory        = 2
    core_fraction = 20
  }

  boot_disk {
    initialize_params {
      image_id = data.yandex_compute_image.ubuntu.id
      size     = 10
      type     = "network-hdd"
    }
  }

  network_interface {
    subnet_id          = var.subnet_id
    nat                = true
    security_group_ids = [var.security_group_id]
  }

  metadata = {
    ssh-keys = "poly:${var.ssh_public_key}"
  }

  allow_stopping_for_update = true

  # Ждём готовности ВМ перед переходом к Ansible
  provisioner "local-exec" {
    command = "echo 'VM ${self.name} created: ${self.network_interface.0.nat_ip_address}'"
  }
}

# outputs.tf — встроен в main.tf
output "instance_id" {
  value = yandex_compute_instance.bot_server.id
}

output "instance_name" {
  value = yandex_compute_instance.bot_server.name
}

output "instance_public_ip" {
  description = "External (NAT) IP address for SSH access"
  value       = yandex_compute_instance.bot_server.network_interface[0].nat_ip_address
}

output "instance_private_ip" {
  description = "Internal IP address"
  value       = yandex_compute_instance.bot_server.network_interface[0].ip_address
}

output "ssh_command" {
  description = "Ready-to-use SSH command"
  value       = "ssh -i <private-key> ubuntu@${yandex_compute_instance.bot_server.network_interface[0].nat_ip_address}"
}