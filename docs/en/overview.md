# Welcome to the meshMarketplace
The meshStack marketplace enables you to serve and manage your services in the meshPanel.

Offering your services in the central cloud foundation portal has the following benefits:
- easier reach customers which already are approching cloud services
- out-of-the-box billing capabilities
- (optional) user assignment through the cloud foundation portal

We suggest you to setup your Service Broker using meshcloud open-source project "Unipipe-Service-Broker". The Unipipe-Service-Broker will enable you fast and quick demo setup, quick iterative service development and a quick time-to-market.
The Unipipe-Service-Broker will reduce your own code development efforts as it provides the basic functionality requied by the Open Service Broker API reference.

# Requirements
Service Owner need to implement their service according to the Open Service Broker API (https://www.openservicebrokerapi.org/).
The reference for the Open Service Broker API can be found here: https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md

If you want to offer a servcie in the marketplace you need meet the following requirements:
- Your own meshCustomer in the meshPanel where you can add your Open Service Broker
- Your Open Service Broker implementation including a catalog definition

# Contact
If you need support regarding your Service Broker setup in the meshMarketplace contact us via support@meshcloud.io

If you need support regarding configuration in the meshStack for your Service Broker contact support@meshcloud.io

# How to start using Unipipe-Service-Broker
## Unipipe-Service-Broker Wiki
The Unipipe-Service-Broker Wiki will provide you most infromation to start and setup your own service broker in a short time.
https://github.com/meshcloud/unipipe-service-broker/wiki

## How to deploy the Unipipe-Service-Broker
How to deploy Unipipe-Service-Broker
https://github.com/meshcloud/unipipe-service-broker/wiki/How-To-Guides#-how-to-deploy-unipipe-service-broker

## How to setup your Service Broker in the meshMarketplace
Service development in the meshMarketplace is documented in the meschloud official documents.
https://docs.meshcloud.io/docs/meshstack.meshmarketplace.development.html#docsNav

To setup your service broker in the meshMarketpalce you need to login into the meshPanel.
In the Customer Overview select the tab Marketplace and the sub-tab Service Brokers. Click on '+ Register'
<p align="center">
  <img src="docs/assets/add-service-broker.png" width="250">
</p>

Register the Service Broker by entering the required parameters.
<p align="center">
  <img src="docs/assets/register-service-broker.png" width="250">
</p>

# Further information regarding the meshMarketplace
Official meshcloud documentation regarding the meshMarketpalce:
https://docs.meshcloud.io/docs/marketplace.index.html
