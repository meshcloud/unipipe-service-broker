export const unipipeOsbGCloudCloudRunTerraform = `
#############################################################
# UniPipe Service Broker Deployment
# - deploys on GCloud CloudRun Service
#
# Instructions
#   1. Customize the variable blocks below to configure your deployment and consider configuring a terraform backend
#   2. Ensure you have valid GCloud credentials to execute terraform \`gcloud auth login\` and \`gcloud auth configure-docker\`
#      2.1. you have to be OWNER on Gcloud Project to execute this terraform template otherwise google_iam_policy resource creation fails
#      2.2. the template will use docker pull/push commands for mirroring the unipipe-service-broker images. To do that you should install \`docker\` on your machine.
#   3. Run \`terraform init && terraform apply\`
#      3.1. Set create_cloudrun_service variable as false on your first setup. We should add our auto generated ssh deploy key into the github repository and also you should commit your first catalog.yml
#      3.2. You should add the unipipe_git_ssh_key to your repository as a Deploy Key and also give the write-access permission on it
#      3.3. After you set your Deploy key and Catalog.yml, execute \`terraform apply\` once more with create_cloudrun_service variable as true
#   4. Add the deployed UniPipe OSB to your marketplace.
#      4.1. You will find all necessary info in the terraform output.
#      4.2. To view the OSB API password run \`terraform output unipipe_basic_auth_password\`
#      4.3. Ensure your git repo contains a valid catalog.yml. You can also generate an example catalog using \`unipipe generate catalog\`
#############################################################

terraform {
  required_version = ">= 0.14"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "3.72.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "3.1.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "3.1.0"
    }
  }

  # use local state files, if you want you can put your state files to your remote storage as well
  backend "local" {
  }
}

variable "create_cloudrun_service" {
  description = "You should set it as 'True' after you configure your Github repository and apply the terraform template again."
  type        = bool
  default     = false
}

variable "project_id" {
  description = "The project ID to deploy resource into"
  default     = "GCLOUDPROJECT"
}

variable "region" {
  description = "The Region to deploy resource into"
  default     = "europe-west3"
}

variable "cloudrun_service_name" {
  description = "The CloudRun service name"
  default     = "unipipe-service-broker-demo"
}

variable "unipipe_version" {
  description = "Unipipe version, see https://github.com/meshcloud/unipipe-service-broker/releases"
  default     = "v1.2.0"
}

variable "gcloud_container_registry_prefix" {
  description = "GCloud Container Registry address. Format: <region>.gcr.io/<project_id>"
  default     = "eu.gcr.io/GCLOUDPROJECT"
}

variable "unipipe_git_remote" {
  description = "Git repo URL. Use a deploy key (GitHub) or similar to setup an automation user SSH key for unipipe."
  default     = "git@github.com:ORGANIZATION/REPO.git"
}

variable "unipipe_git_branch" {
  description = "Git branch name"
  default     = "master"
}

variable "unipipe_basic_auth_username" {
  description = "OSB API basic auth username"
  default     = "user"
}

provider "google" {
  project = var.project_id
  region  = var.region
}
provider "tls" {
}

provider "random" {
}

module "mirror" {
  source  = "neomantra/mirror/docker"
  version = "0.4.0"
  # insert the 2 required variables here
  image_name    = "unipipe-service-broker"
  image_tag     = var.unipipe_version
  source_prefix = "ghcr.io/meshcloud"
  dest_prefix   = var.gcloud_container_registry_prefix
}

resource "tls_private_key" "unipipe_git_ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

# setup a random password for the OSB instance
resource "random_password" "unipipe_basic_auth_password" {
  length  = 16
  special = false
}

resource "google_cloud_run_service" "default" {
  count    = var.create_cloudrun_service ? 1 : 0
  name     = var.cloudrun_service_name
  location = var.region

  template {
    spec {
      containers {
        image = module.mirror.dest_full
        ports {
          name           = "http1"
          container_port = 8075
        }
        env {
          name  = "GIT_REMOTE"
          value = var.unipipe_git_remote
        }
        env {
          name  = "GIT_REMOTE_BRANCH"
          value = var.unipipe_git_branch
        }
        env {
          name  = "GIT_SSH_KEY"
          value = tls_private_key.unipipe_git_ssh_key.private_key_pem
        }
        env {
          name  = "APP_BASIC_AUTH_USERNAME"
          value = var.unipipe_basic_auth_username
        }
        env {
          name  = "APP_BASIC_AUTH_PASSWORD"
          value = random_password.unipipe_basic_auth_password.result
        }
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
  autogenerate_revision_name = true
}

data "google_iam_policy" "noauth" {
  count = var.create_cloudrun_service ? 1 : 0
  binding {
    role = "roles/run.invoker"
    members = [
      "allUsers",
    ]
  }
}

resource "google_cloud_run_service_iam_policy" "noauth" {
  count    = var.create_cloudrun_service ? 1 : 0
  location = google_cloud_run_service.default[0].location
  project  = google_cloud_run_service.default[0].project
  service  = google_cloud_run_service.default[0].name

  policy_data = data.google_iam_policy.noauth[0].policy_data
}

output "unipipe_basic_auth_username" {
  value = var.unipipe_basic_auth_username
}

output "unipipe_basic_auth_password" {
  value     = random_password.unipipe_basic_auth_password.result
  description = "Execute the command to see the password => 'terraform output unipipe_basic_auth_password'"
  sensitive = true
}

output "url" {
  value       = google_cloud_run_service.default[*].status[*].url
  description = "UniPipe OSB API URL. If you want access to the catalog page, you can add /v2/catalog at the end of the url."
  depends_on = [
    google_cloud_run_service.default
  ]
}

output "unipipe_git_ssh_key" {
  value       = tls_private_key.unipipe_git_ssh_key.public_key_openssh
  description = "UniPipe will use this key to access the git repository. You have to give read+write access on the target repository for this key."
}
`
