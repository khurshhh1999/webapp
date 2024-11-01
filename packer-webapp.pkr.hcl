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

variable "instance_type" {
  type    = string
  default = "t3.medium"
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

variable "sendgrid_api_key" {
  type    = string
  default = null
}

variable "email_from" {
  type    = string
  default = null
}

source "amazon-ebs" "ubuntu" {
  profile                     = var.aws_profile
  region                      = var.aws_region
  instance_type               = var.instance_type
  ami_users                   = []
  ssh_username                = "ubuntu"
  ami_name                    = var.ami_name
  associate_public_ip_address = true
  ssh_timeout                 = "5m"

  launch_block_device_mappings {
    device_name           = "/dev/sda1"
    volume_size           = 8
    volume_type           = "gp2"
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
      "sudo apt-get install -y wget unzip"
    ]
  }

  provisioner "shell" {
    inline = [
      "wget https://s3.amazonaws.com/amazoncloudwatch-agent/debian/amd64/latest/amazon-cloudwatch-agent.deb",
      "sudo dpkg -i -E ./amazon-cloudwatch-agent.deb",
      "sudo systemctl enable amazon-cloudwatch-agent"
    ]
  }

  provisioner "file" {
    source      = "app.jar"
    destination = "/home/ubuntu/app.jar"
  }

  provisioner "file" {
    source      = "myapp.service"
    destination = "/home/ubuntu/myapp.service"
  }

  provisioner "file" {
    source      = "cloudwatch-config.json"
    destination = "/tmp/cloudwatch-config.json"
  }

  provisioner "shell" {
    inline = [
      "sudo groupadd csye6225 || true",
      "sudo useradd -M -s /usr/sbin/nologin -g csye6225 csye6225 || true",
      "sudo mkdir -p /opt/myapp",
      "sudo mv /home/ubuntu/app.jar /opt/myapp/app.jar",
      "sudo chown -R csye6225:csye6225 /opt/myapp",
      
      "sudo tee /opt/myapp/application.properties <<EOL",
      "spring.datasource.url=jdbc:postgresql://localhost:5432/csye6225",
      "spring.datasource.username=${var.db_user}",
      "spring.datasource.password=${var.db_password}",
      "spring.jpa.hibernate.ddl-auto=update",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "SENDGRID_API_KEY=${var.sendgrid_api_key}",
      "EMAIL_FROM=${var.email_from}",
      "EOL",
      
      "sudo mv /home/ubuntu/myapp.service /etc/systemd/system/myapp.service",
      "sudo mv /tmp/cloudwatch-config.json /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json",
      "sudo chown root:root /etc/systemd/system/myapp.service",
      "sudo chmod 644 /etc/systemd/system/myapp.service",
      
      "sudo systemctl daemon-reload",
      "sudo systemctl enable myapp.service",
      "sudo systemctl enable amazon-cloudwatch-agent",
      "sudo systemctl start amazon-cloudwatch-agent || true",
      "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json -s"
    ]
  }
}