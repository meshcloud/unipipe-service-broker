# Example OSB CI/CD Pipeline
This is an example CI/CD Pipeline to be used in combination with the [UniPipe Service Broker](https://github.com/meshcloud/unipipe-service-broker). It shows
how the communication is done via a Git repo between the API and the pipeline.

The pipeline is divided up into 3 different jobs:

- Prepare: It prepares the instance. SSH keys are generated and all relevant information for a Terraform deployment and Ansible provisioning are extracted from the instance.yml and the environment and written to an instance.tfvars file.
- Deploy: The provisioning of the service instance is done in this job. It uses the prepared SSH key and instance.tfvars file to actually create the service instance via Terraform and configure it via Ansible. The instances are running in OpenStack.
- Bindings: Bindings are created in a separate step. Ansible is used to create a new user in the instance and the credentials are written back to the GIT repo. For production use, credentials should not be written to GIT, but something like Vault should be used for that. But for demo purposes, this approach is sufficient.

## Instances Git repository
Via the instances repository the communication with the UniPipe Service Broker is done. You can find details about the files to be exchanged in the [UniPipe Service Broker Readme](https://github.com/meshcloud/unipipe-service-broker).

## Configure pipeline
To configure the pipeline for your environment, you have to create a yaml file, that contains all properties custom to an environment. The following properties have to be defined:

```yaml
ci-repo-uri: https://github.com/Meshcloud/example-osb-ci.git # url to git repo that contains the tasks and scripts for the pipeline.
ci-repo-username: # if you need credentials for HTTPS access to your ci-repo, configure the username here
ci-repo-password: # if you need credentials for HTTPS access to your ci-repo, configure the password here
ci-repo-branch: master # if you are working with branches and want to run the pipeline on another branch, you can configure it here
private-key-ci-repo: # if you are using SSH to access your ci-repo, configure your key here, i.e. like this:
    -----BEGIN RSA PRIVATE KEY-----
    Hgiud8z89ijiojdobdikdosaa+hnjk789hdsanlklmladlsagasHOHAo7869+bcG
    x9tD2aI3ih+NJKnbikbdsaio97z9uijasnkjKJAmaölmö+eISBT8NykZuQJjcjpd
    6lTMAGod+5pIv0hWk9Us24IjTthx8K5blAACy/HsXNOH1EKSXCoqoKTehRwdXUaD
    bOclJ/U3FqswV/hjnks789za98sANoojoijoisaj/EHysKQfmAnDBdG4=
    -----END RSA PRIVATE KEY-----
instances-repo-uri: # url to git repo that is used for exchange of instance information with the UniPipe Service Broker
instances-repo-username: # if you need credentials for HTTPS access to your instances-repo, configure the username here
instances-repo-password: # if you need credentials for HTTPS access to your instances-repo, configure the password here
instances-repo-branch: master # if you are working with branches and want to use another branch, you can configure it here
private-key-instances-repo: # if you are using SSH to access your ci-repo, configure your key here, i.e. like this:
    -----BEGIN RSA PRIVATE KEY-----
    Hgiud8z89ijiojdobdikdosaa+hnjk789hdsanlklmladlsagasHOHAo7869+bcG
    x9tD2aI3ih+NJKnbikbdsaio97z9uijasnkjKJAmaölmö+eISBT8NykZuQJjcjpd
    6lTMAGod+5pIv0hWk9Us24IjTthx8K5blAACy/HsXNOH1EKSXCoqoKTehRwdXUaD
    bOclJ/U3FqswV/hjnks789za98sANoojoijoisaj/EHysKQfmAnDBdG4=
    -----END RSA PRIVATE KEY-----
os_user_name : # username to access your OpenStack project. I.e. username of a service user in meshcloud.
os_project_name : # project name of the OpenStack project to be used for provisioning the actual instances
os_password: # password of the OpenStack project
os_auth_url: # Auth URL of the OpenStack you are using
os_domain_id: # domain id where your OpenStack user and project are located in.
os_image_id: # OpenStack ID of the image to be used. Must be an Ubuntu Xenial image.
os_external_network_id: # The OpenStack ID of the external network to be used for getting internet or corporation-wide access to the created instances
os_dns_nameservers: "\"10.11.12.13\",\"10.11.12.14\",\"10.11.12.15\"" # can be a comma-separated list of nameservers to be used. It must use escaped quotation marks, because it is later on used in an array of strings.
small_flavor: # the example uses two different plans. Enter the OpenSTack ID of the flavor to be used for small instances
medium_flavor: # the example uses two different plans. Enter the OpenSTack ID of the flavor to be used for medium instances
external_ip_pool: # The name of the IP pool to be used for Floating IPs. I.e. "public00" at Meshcloud.
http_proxy: # if you have to define an http_proxy, you can do so here, otherwise leave it blank.
https_proxy: # if you have to define an https_proxy, you can do so here, otherwise leave it blank.
no_proxy: # if you have to define an http_proxy, you can do add no_proxy hosts here, otherwise leave it blank.
```

For production use credentials should not be configured via this file, but should be retrieved via Vault or similar. This is not part of this example.

For now you also have to do a minor adaption in the `pipeline.yml`, if you are using HTTPS to access the git repos. The `private-key` properties
have to be removed from the Git resources, because otherwise the Git Resource will fail due to an invalid SSH key.

## Deploy pipeline
In order to update the pipeline in Concourse, execute the following commands:

```bash
fly login -t ci -c <concourse-url> -n <team>
fly -t ci set-pipeline -c pipeline/pipeline.yml -p example-osb --load-vars-from pipeline/configs.yml
```
