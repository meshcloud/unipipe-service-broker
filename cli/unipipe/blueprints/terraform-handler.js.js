import { uuid } from '../deps.ts';

export const terraformTransformHandler = `
/**
 Here's a full example for a handlers.js file that you can pass to unipipe transform --registry-of-handlers and that writes service instance parameters into a custom directory structure:
 It contains a Terraform template example for Azure VNET creation.

  /
  \`-- network
      \`-- customer-id
          \`-- project-id
              \`-- instance-id
                  \`-- terraform.tf

  For more information about handlers please review the official unipipe-cli documentation
  at https://github.com/meshcloud/unipipe-cli
*/

class VnetHandler {
  name = "Vnet Handler";

  handle(service) {
    const params = service.instance.parameters;
    const context = service.instance.context;
    const bindings = service.bindings;
    const deleted = service.instance.deleted;

    return {
      // Hierarchy level 1
      name: "network",
      entries: [{
        // Hierarchy level 2: Folder "<customer id>"
        name: context.customer_id,
        entries: [
          {
            // Hierarchy level 3: Folder "<project id>"
            name: context.project_id,
            entries: [
              {
                // Hierarchy level 4: Folder "<instance id>"
                // this prevents collisions for multiple instances under the same meshProject
                name: service.instance.serviceInstanceId,
                entries: [
                  { name: \`\${service.instance.serviceInstanceId}.main.tf\`, content:tf(params.VNetRegion, params.VNetIP, params.CidrBlock, service.instance.serviceInstanceId, deleted) },
                ],
              }
            ],
          },
        ],
      }],
    };
  }
}

function tf (VNetRegion, VNetIP, CidrBlock, serviceInstanceId, deleted) {
  return \`\${deleted?"#DELETED":""}
terraform {
  backend "local" {
  }
}
provider "azurerm" {
  features {}
}

locals {
  address_space = "\${VNetIP}/\${CidrBlock}"
}

resource "azurerm_resource_group" "main" {
  name     = "\${serviceInstanceId}"
  location = "\${VNetRegion}"

  lifecycle {
    ignore_changes = [tags]
  }
}

resource "azurerm_virtual_network" "main" {
  name                = "UP-\${serviceInstanceId}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  address_space       = [ local.address_space ]

  subnet {
    name           = "public"
    address_prefix = cidrsubnet(local.address_space, 1, 0)
    security_group = azurerm_network_security_group.main.id
  }

  subnet {
    name           = "private"
    address_prefix = cidrsubnet(local.address_space, 1, 1)
    security_group = azurerm_network_security_group.main.id
  }

  lifecycle {
    ignore_changes = [tags]
  }
}

resource "azurerm_network_security_group" "main" {
  name                = "SG-\${serviceInstanceId}"
  location            = "\${VNetRegion}"
  resource_group_name = azurerm_resource_group.main.name

  lifecycle {
    ignore_changes = [tags]
  }
}
\`
}

const handlers = {
  // Check your service id and replace the following key. You can find the service ids inside of your catalog.yml file.
  "$SERVICEID": new VnetHandler(),
};

handlers;
`