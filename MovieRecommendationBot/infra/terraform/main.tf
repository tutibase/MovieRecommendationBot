terraform {
  required_providers {
    yandex = {
      source  = "yandex-cloud/yandex"
      version = "~> 0.130"
    }
  }
}

provider "yandex" {
  token     = var.yc_token
  cloud_id  = var.cloud_id
  folder_id = var.folder_id
  zone      = var.zone
}

# ==========================================
# Переменные
# ==========================================
variable "yc_token" {
  type        = string
  description = "IAM токен Yandex Cloud"
  sensitive   = true
}

variable "cloud_id" {
  type        = string
  description = "ID облака Yandex Cloud"
}

variable "folder_id" {
  type        = string
  description = "ID каталога (Folder)"
}

variable "zone" {
  type        = string
  description = "Зона доступности"
  default     = "ru-central1-d"
}

variable "ssh_public_key" {
  type        = string
  description = "Публичный SSH ключ для доступа к ВМ"
}

# ID существующей подсети
variable "subnet_id" {
  type        = string
  description = "ID существующей подсети"
  default     = "fl80id702e4irnblcd63"
}

# ID существующей группы безопасности (теперь обязательная переменная)
variable "security_group_id" {
  type        = string
  description = "ID существующей группы безопасности (должна иметь открытый порт 22)"
  default     = "enpkd9np0qbhc064o9mu"
}

# ==========================================
# Образ Ubuntu (жесткий ID)
# ==========================================
locals {
  ubuntu_image_id = "fd8r71tg4mg5b3uiholm"
}

# ==========================================
# Создание виртуальной машины
# ==========================================
resource "yandex_compute_instance" "bot_vm" {
  name        = "movie-recommendation-bot-vm"
  platform_id = "standard-v3"
  zone        = var.zone

  resources {
    cores         = 2
    memory        = 2
    core_fraction = 20
  }

  boot_disk {
    initialize_params {
      image_id = local.ubuntu_image_id
      size     = 15
      type     = "network-hdd"
    }
  }

  network_interface {
    subnet_id          = var.subnet_id
    nat                = true
    # Привязываем СУЩЕСТВУЮЩУЮ группу безопасности
    security_group_ids = [var.security_group_id]
  }

  metadata = {
    ssh-keys = "ubuntu:${var.ssh_public_key}"
  }

  allow_stopping_for_update = true
}

# ==========================================
# Выводы
# ==========================================
output "vm_public_ip" {
  description = "Публичный IP адрес виртуальной машины"
  value       = yandex_compute_instance.bot_vm.network_interface[0].nat_ip_address
}

output "vm_private_ip" {
  description = "Внутренний IP адрес"
  value       = yandex_compute_instance.bot_vm.network_interface[0].ip_address
}

output "ssh_command" {
  description = "Команда для подключения по SSH"
  value       = "ssh -i <private_key> ubuntu@${yandex_compute_instance.bot_vm.network_interface[0].nat_ip_address}"
}