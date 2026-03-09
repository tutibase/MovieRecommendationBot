#!/bin/bash
set -e  # Останавливаемся при первой ошибке

# Логирование
exec > /var/log/install-deps.log 2>&1

echo "=== Install Dependencies Started at $(date) ==="

# Обновление пакетов
echo "Updating packages..."
apt-get update
apt-get upgrade -y

# Установка Docker
echo "Installing Docker..."
curl -fsSL https://get.docker.com -o /tmp/get-docker.sh
sh /tmp/get-docker.sh

# Добавление пользователя ubuntu в группу docker (чтобы работать без sudo)
usermod -aG docker ubuntu

# Проверка установки
echo "Docker version:"
docker --version

# Создание директории для приложения
echo "📁 Creating app directory..."
mkdir -p /opt/movie-bot
chown ubuntu:ubuntu /opt/movie-bot

echo "=== Install Dependencies Completed at $(date) ==="
echo "Server is ready for Docker deployment"