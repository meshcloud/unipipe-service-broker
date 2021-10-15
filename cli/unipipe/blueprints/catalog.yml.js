import { uuid } from '../deps.ts';

export const catalog =
`services:
  # define a simple service offering
  # for field documentation see https://github.com/openservicebrokerapi/servicebroker/blob/v2.15/spec.md#service-offering-object
  - id: ${uuid.generate()} # this uuid was randomly generated using "unipipe generate uuid"
    name: "Webservice-api"
    description: "This service spins up a webservice in aws."
    bindable: true
    tags:
      - "example"
    plans:
      # service offerings can come in one or multiple plans
      # here we define plans in different t-shirt sizes
      - id: ${uuid.generate()} # this uuid was randomly generated using "unipipe generate uuid"
        name: "S"
        description: "A t2.nano instance"
        free: true
        bindable: true
        schemas: # schemas define the configuration options offered by a plan
          service_instance:
            create:
              parameters:
                "$schema": http://json-schema.org/draft-04/schema#
                properties:
                  username:
                    title: username
                    type: string

      - id: ${uuid.generate()} # this uuid was randomly generated using "unipipe generate uuid"
        name: "M"
        description: "A t2.micro instance"
        free: true
        bindable: true
        schemas:
          service_instance:
            create:
              parameters:
                "$schema": http://json-schema.org/draft-04/schema#
                properties:
                  username:
                    title: username
                    type: string

  # a more complex service example
  - id: ${uuid.generate()} # this uuid was randomly generated using "unipipe generate uuid"
    name: "Azure DevOps"
    description: "Onboard your team to Azure DevOps services"
    bindable: false
    plan_updateable: true
    instances_retrievable: true
    bindings_retrievable: false
    metadata:
      displayName: "Azure DevOps"
      imageUrl: "https://docs.microsoft.com/en-us/azure/devops/media/index/devopsiconpipelines96.svg?view=azure-devops"
      longDescription: "Azure DevOps services"
      providerDisplayName: "Azure"
      documentationUrl: "https://docs.microsoft.com/en-us/azure/devops/?view=azure-devops"
      supportUrl: "https://azure.microsoft.com/en-us/services/devops/"
    tags:
      - "azure"
      - "devops"
    plans:
      - id: ${uuid.generate()} # this uuid was randomly generated using "unipipe generate uuid"
        name: "Full Team Onboarding plan"
        description: "Enable all user in your meshProject on Azure DevOps."
        free: false
        metadata:
          bullets:
            - bulletpoint 1
            - bulletpoint 2
          costs:
            - amount:
                eur: 99.99
              unit: MONTHLY
            - amount:
                eur: 100.00
              unit: SETUP FEE # setup fees as supported by meshMarketplace https://docs.meshcloud.io/docs/meshstack.meshmarketplace.metering.html#setup-fees
          displayName: Fullteam plan
        schemas:
          service_instance:
            create:
              parameters:
                $schema: http://json-schema.org/draft-04/schema#
                properties:
                  projectname:
                    description: "Project name"
                    type: "string"
                  projectdescription:
                    description: "Project describtion"
                    type: "string"
                  msteamsad:
                    type: string
                    title: MS Teams AD
                    description: Select the correct ms teams ad
                    enum:
                      - DEV
                      - TEST
                      - PROD
                    widget: select
                  email:
                    description: "E-mail address"
                    type: "string"
                type: object
            update:
              parameters:
                $schema: http://json-schema.org/draft-04/schema#
                properties:
                  projectname:
                    description: "Project name"
                    type: "string"
                  projectdescription:
                    description: "Project describtion"
                    type: "string"
                  msteamsad:
                    description: "MS Teams AD group"
                    type: "string"
                  email:
                    description: "E-mail address"
                    type: "string"
                type: object

  # a example for a network tenant service
  - id: ${uuid.generate()} # this uuid was randomly generated using "unipipe generate uuid"
    name: Azure VNet
    description: Provides a configurable default VNET in your Azure subscription.
    bindable: false
    plan_updateable: false
    instances_retrievable: true
    bindings_retrievable: false
    metadata:
      displayName: "Azure VNet Service"
      imageUrl: "https://symbols.getvecta.com/stencil_28/71_virtual-network.4fe1e0cdfa.svg"
    plans:
      - id: ${uuid.generate()} # this uuid was randomly generated using "unipipe generate uuid"
        name: Standard
        description: Standard vNet generates 1 Private and 1 Public Subnet
        free: true
        schemas:
          service_instance:
            create:
              parameters:
                "$schema": http://json-schema.org/draft-04/schema#
                properties:
                  VNetIP:
                    title: vNet IP
                    type: string
                    widget:
                      id: string
                    default: "10.X.X.X"
                  CidrBlock:
                    title: CIDR Block
                    type: string
                    widget:
                      id: select
                    default: "28"
                    oneOf:
                      - description: /28 vNet with 6 addresses (+5 addresses reserved by Azure per each subnet)
                        enum:
                          - "28"
                      - description: /27 vNet with 22 addresses (+5 addresses reserved by Azure per each subnet)
                        enum:
                          - "27"
                      - description: /26 vNet with 54 addresses (+5 addresses reserved by Azure per each subnet)
                        enum:
                          - "26"
                      - description: /25 vNet with 118 addresses (+5 addresses reserved by Azure per each subnet)
                        enum:
                          - "25"
                      - description: /24 vNet with 246 addresses (+5 addresses reserved by Azure per each subnet)
                        enum:
                          - "24"
                  VNetRegion:
                    title: vNet Region
                    type: string
                    widget:
                      id: select
                    default: "GermanyWestCentral"
                    oneOf:
                      - description: GermanyWestCentral (default choice)
                        enum:
                          - "GermanyWestCentral"
                      - description: WestEurope
                        enum:
                          - "WestEurope"
`;
