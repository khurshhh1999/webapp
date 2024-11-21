variable "artifact_path" {}
variable "dev_account_id" {}
variable "demo_account_id" {}
variable "db_user" {}
variable "db_password" {}
variable "sendgrid_api_key" {}
variable "email_from" {}
variable "aws_sns_topic_arn" {}
variable "db_name" {}
variable "aws_bucket_name" {}
variable "db_host" {}

source "amazon-ebs" "ubuntu" {
  ami_name      = "webapp-ami-{{timestamp}}"
  instance_type = "t3.medium"
  region        = "us-east-1"
  source_ami_filter {
    filters = {
      virtualization-type = "hvm"
      name                = "ubuntu/images/hvm-ssd/ubuntu-focal-20.04-amd64-server-*"
      root-device-type    = "ebs"
    }
    owners      = ["099720109477"] # Canonical
    most_recent = true
  }
  ssh_username                 = "ubuntu"
  associate_public_ip_address  = true
}

build {
  sources = ["source.amazon-ebs.ubuntu"]

  provisioner "file" {
    source      = "myapp.service"
    destination = "/etc/systemd/system/myapp.service"
  }

  provisioner "file" {
    source      = "archive/webapp-0.0.1-SNAPSHOT.jar"
    destination = "/opt/myapp/app.jar"
  }

  provisioner "file" {
    source      = "cloudwatch-config.json"
    destination = "/opt/cloudwatch-config.json"
  }

  provisioner "shell" {
    inline = [
      "sudo systemctl daemon-reload",
      "sudo systemctl enable myapp.service",
      "sudo systemctl start myapp.service"
    ]
  }

}
