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

variable "ami_name" {
  type    = string
  default = "webapp-ami-{{timestamp}}"
}

variable "db_host" {
  type = string
}

variable "db_port" {
  type    = string
  default = "5432"
}

variable "db_name" {
  type = string
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

variable "aws_bucket_name" {
  type = string
  default= "your_bucket_name"
}

variable "aws_sns_topic_arn" {
  type        = string
  description = "SNS Topic ARN for user verification"
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}
variable "jwt_secret" {
  type    = string
  default = null
}

variable "s3_bucket" {
  type    = string
  default = null
}

source "amazon-ebs" "ubuntu" {
  profile                     = var.aws_profile
  region                      = var.aws_region
  instance_type               = var.instance_type
  ami_users                   = [var.demo_account_id]
  ssh_username                = "ubuntu"
  ami_name                    = var.ami_name
  associate_public_ip_address = true
  ssh_timeout                 = "10m"
  ssh_handshake_attempts      = "100"
  ssh_keep_alive_interval     = "10s"
  pause_before_connecting     = "30s"

  launch_block_device_mappings {
    device_name           = "/dev/sda1"
    volume_size           = 8
    volume_type           = "gp2"
    delete_on_termination = true
  }
  skip_create_ami                       = false
  temporary_key_pair_type               = "ed25519"
  temporary_security_group_source_cidrs = ["0.0.0.0/0"]
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
      "sudo apt-get install -y openjdk-17-jdk maven",
      "sudo apt-get install -y wget unzip net-tools",
      "sudo apt-get install -y awscli",
      "sudo apt-get install -y collectd",
      "sudo apt-get install -y tree",
      "sudo snap install jq"
    ]
  }
  provisioner "shell" {
    inline = ["mkdir -p /tmp/webapp-source"]
  }

  provisioner "file" {
    source      = "./"
    destination = "/tmp/webapp-source"
  }
  provisioner "shell" {
    inline = [
      "echo '=== Setting up Users and Directories ==='",
      "sudo groupadd csye6225 || true",
      "sudo useradd -s /bin/false -g csye6225 csye6225 || true",

      "sudo mkdir -p /opt/myapp",
      "sudo mkdir -p /var/log/myapp/archived",
      "sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc",
      "sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/logs",
      "sudo mkdir -p /tmp/webapp",

      "sudo touch /var/log/myapp/application.log",
      "sudo touch /var/log/myapp/access.log",
      "sudo chown csye6225:csye6225 /var/log/myapp/access.log",

      "sudo chown -R csye6225:csye6225 /opt/myapp",
      "sudo chown -R csye6225:csye6225 /var/log/myapp",
      "sudo chown -R root:root /opt/aws/amazon-cloudwatch-agent",

      "sudo chmod 755 /opt/myapp",
      "sudo chmod 755 /var/log/myapp",
      "sudo chmod 755 /var/log/myapp/archived",
      "sudo chmod 755 /opt/aws/amazon-cloudwatch-agent/etc",

      "sudo chmod 644 /var/log/myapp/application.log",
      "sudo chmod 644 /var/log/myapp/access.log",

      "sudo chown ubuntu:ubuntu /tmp/webapp"
    ]
  }
  provisioner "file" {
  source      = "archive/webapp-0.0.1-SNAPSHOT.jar"
  destination = "/tmp/webapp/app.jar"
}



  provisioner "shell" {
    inline = [
      "echo '=== Installing CloudWatch Agent ==='",
      "wget https://s3.amazonaws.com/amazoncloudwatch-agent/debian/amd64/latest/amazon-cloudwatch-agent.deb",
      "sudo dpkg -i -E ./amazon-cloudwatch-agent.deb",
      "sudo rm ./amazon-cloudwatch-agent.deb"
    ]
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

  provisioner "file" {
    source      = "src/main/resources/logback-spring.xml"
    destination = "/tmp/webapp/logback-spring.xml"
  }

  provisioner "shell" {
    environment_vars = [
      "DB_HOST=${var.db_host}",
      "DB_PORT=${var.db_port}",
      "DB_NAME=${var.db_name}",
      "DB_USER=${var.db_user}",
      "DB_PASSWORD=${var.db_password}",
      "AWS_REGION=${var.aws_region}",
      "AWS_SNS_TOPIC_ARN=${var.aws_sns_topic_arn}",
      "SENDGRID_API_KEY=${var.sendgrid_api_key}",
      "EMAIL_FROM=${var.email_from}",
      "JWT_SECRET=${var.jwt_secret}"
    ]
    inline = [
      "echo '=== Moving Application Files ==='",
      "sudo mkdir -p /opt/myapp",
      "sudo mv /tmp/webapp/app.jar /opt/myapp/",
      "sudo chown csye6225:csye6225 /opt/myapp/app.jar",
      "sudo chmod 644 /opt/myapp/app.jar",
      "sudo mv /tmp/webapp/application.yml /opt/myapp/",
      "sudo mv /tmp/webapp/logback-spring.xml /opt/myapp/",
      "sudo mv /tmp/webapp/myapp.service /etc/systemd/system/",

      "echo '=== Setting up CloudWatch ==='",
      "sudo cp /tmp/webapp/cloudwatch-config.json /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json",
      "sudo chown cwagent:cwagent /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json",
      "sudo chmod 644 /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json",

      "sudo sed -i 's|$${DB_HOST}|'\"$DB_HOST\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${DB_PORT}|'\"$DB_PORT\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${DB_NAME}|'\"$DB_NAME\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${DB_USER}|'\"$DB_USER\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${DB_PASSWORD}|'\"$DB_PASSWORD\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${AWS_REGION}|'\"$AWS_REGION\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${AWS_SNS_TOPIC_ARN}|'\"$AWS_SNS_TOPIC_ARN\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${AWS_BUCKET_NAME}|'\"$AWS_BUCKET_NAME\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${SENDGRID_API_KEY}|'\"$SENDGRID_API_KEY\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${EMAIL_FROM}|'\"$EMAIL_FROM\"'|g' /opt/myapp/application.yml",
      "sudo sed -i 's|$${JWT_SECRET}|'\"$JWT_SECRET\"'|g' /opt/myapp/application.yml",

      "echo '=== Setting File Permissions ==='",
      "sudo chown -R csye6225:csye6225 /opt/myapp",
      "sudo chmod 644 /opt/myapp/app.jar",
      "sudo chmod 644 /opt/myapp/application.yml",
      "sudo chmod 644 /opt/myapp/logback-spring.xml",
      "sudo chown root:root /etc/systemd/system/myapp.service",
      "sudo chmod 644 /etc/systemd/system/myapp.service",

      "echo '=== Verifying Log Files ==='",
      "sudo ls -la /var/log/myapp/",
      "sudo test -f /var/log/myapp/access.log && echo 'Access log exists' || echo 'Access log missing'",
      "sudo test -f /var/log/myapp/application.log && echo 'Application log exists' || echo 'Application log missing'",

      "echo '=== Starting Services ==='",
      "sudo systemctl daemon-reload",
      "sudo systemctl enable myapp.service",
      "sudo systemctl enable amazon-cloudwatch-agent",
      "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json -s",

      "echo '=== Cleanup ==='",
      "sudo rm -rf /tmp/webapp",
      "sudo rm -rf /tmp/webapp-source"
    ]
  }
}