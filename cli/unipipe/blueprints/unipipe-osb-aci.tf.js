export const unipipeOsbAciTerraform = `
#####################################
# UniPipe Service Broker Deployment on Azure ACI including caddy for SSL.
#
# How to use this terraform file:
#   1. Replace all occurrences of "..." with proper values.
#   2. Ensure you have valid azure credentials to execute terraform \`az login\`
#   3. Run \`terraform init && terraform apply\`
#   4. Add the deployed UniPipe OSB to your marketplace.
#      1. You will find all necessary info in the terraform output.
#      1.1 You should add the unipipe_git_ssh_key to your repository as a Deploy Key and also give the write-access permission on it
#      2. To view the OSB API password run \`terraform output unipipe_basic_auth_password\`
#      3. Ensure your git repo contains a valid catalog.yml. You can also generate an example catalog using \`unipipe generate catalog\`
#
#####################################

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

# UniPipe container on Azure
module "unipipe" {
  source = "git::https://github.com/meshcloud/terraform-azure-unipipe.git/?ref=b824997f0b71ba6829832039f9e6b0309253553a"

  subscription_id    = "..." # The subscription the container lives in.
  unipipe_git_remote = "..."
  unipipe_git_branch = "main"
}

output "url" {
  value       = module.unipipe.url
  description = "UniPipe OSB API URL. If you want access to the catalog page, you can add /v2/catalog at the end of the url."
}

output "unipipe_basic_auth_username" {
  value = module.unipipe.unipipe_basic_auth_username
}

output "unipipe_basic_auth_password" {
  value     = module.unipipe.unipipe_basic_auth_password
  sensitive = true
}

output "unipipe_git_ssh_key" {
  value       = module.unipipe.unipipe_git_ssh_key
  description = "UniPipe will use this key to access the git repository. You have to give read+write access on the target repository for this key."
}

output "info" {
  value = "UniPipe OSB is starting now. This may take a couple of minutes on Azure ACI. You can use Azure Portal to view logs of the container starting up and debug any issues. Also note that for newly deployed domains Azure ACI can take a few minutes to provide DNS."
}
`;
