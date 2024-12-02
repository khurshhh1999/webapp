# Web Application Deployment with Packer for Terraform Infra

This repository contains the configuration files and source code for deploying a web application using Packer. The `packer-webapp.pkr.hcl` file is the core configuration file used to define the AMI build process, which includes provisioning the required software and setting up the application environment. The `packer-vars.json` file is used for parameterizing the build with specific values.

## How to Use

To build the AMI using Packer, ensure you have Packer installed and the necessary environment variables configured. Then, run the following command:

```bash
packer build -var-file=packer-vars.json packer-webapp.pkr.hcl
```
This output's an AMI which can be used, which is created in dev profile
