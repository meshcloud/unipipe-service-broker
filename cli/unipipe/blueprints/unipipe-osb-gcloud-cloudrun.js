export const unipipeOsbGCloudCloudRunTerraform = `
#############################################################
# UniPipe Service Broker Deployment
# - deploys on GCloud CloudRun Service
#
# Instructions
#   1. Replace all occurrences of "..." with proper values.
#   2. Ensure you have valid GCloud credentials to execute terraform \`gcloud auth login\` and \`gcloud auth configure-docker\`
#      2.1. you should also have \`resourcemanager.projects.setIamPolicy\` and \`resourcemanager.projects.getIamPolicy\` permissions on the GCloud Project to execute this terraform template otherwise google_iam_policy resource creation fails. Therefore \`roles/editor\` permissions are not sufficient.
#      2.2. the template will use docker pull/push commands for mirroring the unipipe-service-broker images. To do that you should install \`docker\` on your machine.
#   3. Run \`terraform init && terraform apply\`
#      3.1. You should add the unipipe_git_ssh_key to your repository as a Deploy Key and also give the write-access permission on it
#      (OPTIONAL STEPS)
#      3.2. You can also use your own private keys to secure git connection. In this case you should set the private_key_pem variable to your private key.
#      3.3. To provide your private key follow the instructions below:
#      3.3.1. Export your private key to your terminal environment variable without any new line. For example: \`export PRIVKEY='-----BEGIN EC PRIVATE KEY-----MIGkAgEBBDABjkhCM0UOWXXXXXX=-----END EC PRIVATE KEY-----'\`
#      3.3.2. Execute the apply command with extra private_key_pem variable \`terraform apply -var="create_cloudrun_service=true" -var="private_key_pem=\$PRIVKEY"\`
#   4. Add the deployed UniPipe OSB to your marketplace.
#      4.1. You will find all necessary info in the terraform output.
#      4.2. To view the OSB API password run \`terraform output unipipe_basic_auth_password\`
#      4.3. Ensure your git repo contains a valid catalog.yml. You can also generate an example catalog using \`unipipe generate catalog\`
#############################################################

terraform {
  # Removing the backend will output the terraform state in the local filesystem
  # See https://www.terraform.io/language/settings/backends for more details
  #
  # Remove/comment the backend block below if you are only testing the module.
  # Please be aware that you cannot destroy the created resources via terraform if you lose the state file.
  backend "gcs" {
    bucket = "..."
    prefix = "..."
  }
}

# UniPipe container on GCP
module "unipipe" {
  source     = "git@github.com:meshcloud/terraform-gcp-unipipe.git?ref=c758f8f27b0187da467fb8a9fd1dd583935036af"
  project_id = "..."

  unipipe_git_remote = "..."
}

output "unipipe_basic_auth_username" {
  value = module.unipipe.unipipe_basic_auth_username
}

output "unipipe_basic_auth_password" {
  value     = module.unipipe.unipipe_basic_auth_password
  description = "Execute the command to see the password => 'terraform output unipipe_basic_auth_password'"
  sensitive = true
}

output "url" {
  value       = module.unipipe.url
  description = "UniPipe OSB API URL. If you want access to the catalog page, you can add /v2/catalog at the end of the url."
}

output "unipipe_git_ssh_key" {
  value       = module.unipipe.unipipe_git_ssh_key
  description = "UniPipe will use this key to access the git repository. You have to give read+write access on the target repository for this key."
}
`;
