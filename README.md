<h1 align="center">UniPipe Service Broker</h1>

<p align="center">
  <img src=".github/unipipe-logo.png" width="250">
</p>

<p align="center">
  <i>Offering Services the GitOps Way</i>
</p>

<p align="center">
  <a href="https://github.com/meshcloud/unipipe-service-broker/actions/workflows/build.yml">
    <img src="https://github.com/meshcloud/unipipe-service-broker/actions/workflows/build-workflow.yml/badge.svg">
  </a>
  <a href="https://github.com/meshcloud/unipipe-service-brokergraphs/contributors">
    <img src="https://img.shields.io/badge/maintained-true-green">
  </a>
  <a href="https://github.com/meshcloud/unipipe-service-broker/blob/develop/LICENSE">
    <img src="https://img.shields.io/github/license/meshcloud/unipipe-service-broker">
  </a>
  <a href="https://github.com/meshcloud/unipipe-service-broker/releases">
    <img src="https://img.shields.io/github/v/release/meshcloud/unipipe-service-broker?sort=semver">
  </a>
</p> 

UniPipe service broker is a tool for offering services on [OSB API](https://www.openservicebrokerapi.org/) compliant marketplaces. It's key features are:
- **Fast deployment using terraform:** [Get up and running with your first service broker in < 30 Minutes](https://github.com/meshcloud/unipipe-service-broker/wiki/How-To-Guides#deploy-unipipe-service-broker-with-terraform)
- **Easy operations:** The service broker container is stateless. Service Instances and bindings are stored in a git repository. The container is broken? Just `terraform apply` again!
- **GitOps workflow:** Don't have all the automation yet? Just read out service requests via `unipipe list` and `unipipe show` from the git repository and provision the service manually, then update the status via `unipipe update`.
- **Built for automation:** The interface for integrating automation is the git repository and `unipipe` CLI makes working with it easy. Use whatever automation tooling you are most productive with!

# 🎬 Getting Started

Pretty much everything in the UniPipe universe can be done with unipipe cli. [🕹️ How to install unipipe cli](https://github.com/meshcloud/unipipe-service-broker/wiki/How-To-Guides#%EF%B8%8F-how-to-install-unipipe-cli).

After installing unipipe cli, dive into the [Tutorials](https://github.com/meshcloud/unipipe-service-broker/wiki/Tutorials) for getting hands-on experience.

For understanding how everything fits together check out [Understanding the UniPipe universe](https://github.com/meshcloud/unipipe-service-broker/wiki/Understanding#understanding-the-unipipe-universe).

[![Or watch this video!](https://img.youtube.com/vi/Vasaz8JvdeE/hqdefault.jpg))](https://youtu.be/Vasaz8JvdeE)

# 📚 Wiki

All documentation is available in [our Wiki](https://github.com/meshcloud/unipipe-service-broker/wiki#home) 

# 💡 Why UniPipe service broker?

At [meshcloud](https://meshcloud.io/) we have years of experience in building cloud foundations in large organizations with our cloud governance platform [meshStack](https://meshcloud.io/).

We see that teams often struggled to offer services for self service consumption. UniPipe service broker was developed to make iterating on service offerings easy. By using a git repository as the main interface, teams can offer services directly after defining the [service catalog](https://github.com/meshcloud/unipipe-service-broker/wiki/Reference#instanceyml). The first few orders of the service don't involve any automation, but are simply executed by hand. This way, service owners build up a good understanding of the problem at hand while already providing value to their customers. After a few orders, service owners might start to automate common steps and work with a Pull-Request workflow that allows them to work with partial automation, while still doing some tasks by hand. Eventually the remaining manual tasks might be automated and the service is 100% automated.

After building the first 100% automated service (say on-prem connectivity), teams often start to help other teams to offer their services (managed CI/CD platform, managed Key Vault, kubernetes cluster as a service, you name it ...) the same way, thus moving the whole organization closer to the cloud operating model.

This way, UniPipe service broker becomes the starting point for the transition from IT services to a cloud foundation.

If you want to understand where your are on your cloud journey, try out our [Cloud Foundation Maturity Model](https://cloudfoundation.meshcloud.io/). It is a tool that you can use to identify gaps in your cloud setup, define a roadmap and understand where you can save cost by improving efficiency.

<img align="center" src=".github/cfmm.png">



<p align="center"><b>Made with ❤️ by <a href="https://meshcloud.io/?ref=gh-collie">meshcloud</a></b></p>
