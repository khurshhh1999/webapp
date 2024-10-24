packer {
  required_plugins {
    amazon = {
      version = ">= 1.3.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variable "aws_profile" {
  type    = string
  default = "dev"
}

variable "dev_account_id" {
  type = string
}

variable "demo_account_id" {
  type = string
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "ami_name" {
  type    = string
  default = "webapp-ami-{{timestamp}}"
}

variable "db_user" {
  type = string
}

variable "db_password" {
  type = string
}

source "amazon-ebs" "ubuntu" {
  profile                     = var.aws_profile
  region                      = var.aws_region
  instance_type              = "t3.medium"
  ami_users                  = []
  ssh_username               = "ubuntu"
  ami_name                   = var.ami_name
  associate_public_ip_address = true
  ssh_timeout                = "5m"

  launch_block_device_mappings {
    device_name           = "/dev/sda1"
    volume_size          = 8
    volume_type          = "gp2"
    delete_on_termination = true
  }

  source_ami_filter {
    filters = {
      name                = "ubuntu/images/hvm-ssd/ubuntu-focal-20.04-amd64-server-*"
      virtualization-type = "hvm"
      root-device-type    = "ebs"
    }
    owners      = ["099720109477"]
    most_recent = true
  }
}

build {
  sources = ["source.amazon-ebs.ubuntu"]

  provisioner "shell" {
    inline = [
      "export DEBIAN_FRONTEND=noninteractive",
      "sudo apt-get update --allow-unauthenticated",
      "sudo apt-get -y install software-properties-common --allow-unauthenticated",
      "sudo add-apt-repository ppa:openjdk-r/ppa",
      "sudo apt-get update --allow-unauthenticated",
      "sudo apt-get install -y openjdk-17-jdk --allow-unauthenticated",
      "sudo apt-get install -y postgresql postgresql-contrib --allow-unauthenticated",
      "sudo systemctl enable postgresql",
      "sudo systemctl start postgresql",
      "sleep 10"
    ]
  }

  provisioner "file" {
    source      = "db-setup.sh"
    destination = "/home/ubuntu/db-setup.sh"
  }

  provisioner "shell" {
    inline = [
      "sudo chmod +x /home/ubuntu/db-setup.sh",
      "sudo DB_USER='${var.db_user}' DB_PASSWORD='${var.db_password}' /home/ubuntu/db-setup.sh",
      "sudo rm /home/ubuntu/db-setup.sh"
    ]
  }

  provisioner "file" {
    source      = "app.jar"
    destination = "/home/ubuntu/app.jar"
  }

  provisioner "shell" {s
    inline = [
      "sudo touch /home/ubuntu/myapp.service",
      "echo '[Unit]' | sudo tee /home/ubuntu/myapp.service",
      "echo 'Description=Spring Boot WebApp Service' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'After=network.target postgresql.service' | sudo tee -a /home/ubuntu/myapp.service",
      "echo '' | sudo tee -a /home/ubuntu/myapp.service",
      "echo '[Service]' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'Type=simple' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'User=csye6225' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'ExecStart=/usr/bin/java -jar -Dserver.port=8080 -Dspring.profiles.active=prod /opt/myapp/app.jar' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'Restart=always' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'RestartSec=3' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'Environment=DB_USER=${var.db_user}' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'Environment=DB_PASSWORD=${var.db_password}' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'Environment=DB_HOST=localhost' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'Environment=DB_PORT=5432' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'Environment=DB_NAME=csye6225' | sudo tee -a /home/ubuntu/myapp.service",
      "echo '' | sudo tee -a /home/ubuntu/myapp.service",
      "echo '[Install]' | sudo tee -a /home/ubuntu/myapp.service",
      "echo 'WantedBy=multi-user.target' | sudo tee -a /home/ubuntu/myapp.service"
    ]
  }

  provisioner "shell" {
    inline = [
      "sudo groupadd csye6225 || true",
      "sudo useradd -M -s /usr/sbin/nologin -g csye6225 csye6225 || true",
      "sudo mkdir -p /opt/myapp",
      "sudo mv /home/ubuntu/app.jar /opt/myapp/app.jar",
      "sudo chown -R csye6225:csye6225 /opt/myapp",
      "sudo mv /home/ubuntu/myapp.service /etc/systemd/system/myapp.service",
      "sudo systemctl daemon-reload",
      "sudo systemctl enable myapp.service"
    ]
  }
}