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
  ssh_timeout                 = "20m"
  ssh_handshake_attempts      = "100"
  ssh_keep_alive_interval     = "30s"
  pause_before_connecting     = "90s"

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

  # System setup and package installation
  provisioner "shell" {
    inline = [
      "while [ ! -f /var/lib/cloud/instance/boot-finished ]; do echo 'Waiting for cloud-init...'; sleep 1; done",
      "sudo rm -rf /var/lib/apt/lists/*",
      "sudo apt-get clean",
      "export DEBIAN_FRONTEND=noninteractive",
      "echo 'Waiting for 30 seconds before apt operations...'",
      "sleep 30",
      "sudo apt-get update",
      "sudo apt-get upgrade -y",
      "sudo apt-get install -y software-properties-common",
      "sudo add-apt-repository -y ppa:openjdk-r/ppa",
      "sudo apt-get update",
      "sudo apt-get install -y openjdk-17-jdk",
      "sudo apt-get install -y wget unzip net-tools",
      "sudo apt-get install -y awscli"
    ]
  }

  # Initial setup: Create user, directories, and log files
  provisioner "shell" {
    inline = [
      # Create user and group
      "sudo groupadd csye6225 || true",
      "sudo useradd -s /bin/false -g csye6225 csye6225 || true",

      # Create directories
      "sudo mkdir -p /opt/myapp",
      "sudo mkdir -p /var/log/myapp",
      "sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc",

      # Create log files
      "sudo touch /var/log/myapp/application.log",
      "sudo touch /var/log/myapp/myapp.log",

      # Set permissions
      "sudo chown -R csye6225:csye6225 /opt/myapp",
      "sudo chown -R csye6225:csye6225 /var/log/myapp",
      "sudo chmod 755 /opt/myapp",
      "sudo chmod 755 /var/log/myapp",
      "sudo chmod 664 /var/log/myapp/application.log",
      "sudo chmod 664 /var/log/myapp/myapp.log",

      # Create a temporary directory for file uploads
      "sudo mkdir -p /tmp/webapp",
      "sudo chown ubuntu:ubuntu /tmp/webapp"
    ]
  }

  # Install CloudWatch Agent
  provisioner "shell" {
    inline = [
      "wget https://s3.amazonaws.com/amazoncloudwatch-agent/debian/amd64/latest/amazon-cloudwatch-agent.deb",
      "sudo dpkg -i -E ./amazon-cloudwatch-agent.deb",
      "sudo systemctl enable amazon-cloudwatch-agent"
    ]
  }

  # Copy application files to temp location first
  provisioner "file" {
    source      = "app.jar"
    destination = "/tmp/webapp/app.jar"
    max_retries = 5
  }

  provisioner "file" {
    source      = "myapp.service"
    destination = "/tmp/webapp/myapp.service"
  }

  provisioner "file" {
    source      = "cloudwatch-config.json"
    destination = "/tmp/webapp/cloudwatch-config.json"
  }
  provisioner "file" {
    source      = "src/main/resources/application.yml"
    destination = "/tmp/webapp/application.yml"
  }

  # Move files to final locations and set permissions
  provisioner "shell" {
    inline = [
      # Move files to their final locations
      "sudo mv /tmp/webapp/app.jar /opt/myapp/app.jar",
      "sudo mv /tmp/webapp/myapp.service /etc/systemd/system/myapp.service",
      "sudo mv /tmp/webapp/application.yml /opt/myapp/application.yml",
      "sudo mv /tmp/webapp/cloudwatch-config.json /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json",

      # Set file permissions
      "sudo chown csye6225:csye6225 /opt/myapp/app.jar",
      "sudo chown csye6225:csye6225 /opt/myapp/application.yml",
      "sudo chmod 644 /opt/myapp/app.jar",
      "sudo chmod 644 /opt/myapp/application.yml",
      "sudo chmod 644 /etc/systemd/system/myapp.service",
      "sudo chmod 644 /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json",

      # Create application.properties
      "sudo tee /opt/myapp/application.properties <<EOL",
      "spring.datasource.url=jdbc:postgresql://localhost:5432/csye6225",
      "spring.datasource.username=${var.db_user}",
      "spring.datasource.password=${var.db_password}",
      "spring.jpa.hibernate.ddl-auto=update",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "SENDGRID_API_KEY=${var.sendgrid_api_key}",
      "EMAIL_FROM=${var.email_from}",
      "logging.file.path=/var/log/myapp",
      "logging.file.name=/var/log/myapp/application.log",
      "logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n",
      "logging.level.root=INFO",
      "logging.level.org.springframework=INFO",
      "logging.level.com.example=DEBUG",
      "metrics.statsd.host=localhost",
      "metrics.statsd.port=8125",
      "metrics.prefix=csye6225",
      "EOL",

      # Set permissions for application.properties
      "sudo chmod 644 /opt/myapp/application.properties",
      "sudo chown csye6225:csye6225 /opt/myapp/application.properties",

      # Configure systemd
      "sudo systemctl daemon-reload",
      "sudo systemctl enable myapp.service",
      "sudo systemctl enable amazon-cloudwatch-agent",

      # Clean up temp directory
      "sudo rm -rf /tmp/webapp"
    ]
  }
  provisioner "shell" {
    inline = [
      "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json",
      "sudo systemctl enable amazon-cloudwatch-agent",
      "sudo systemctl start amazon-cloudwatch-agent"
    ]
  }
}