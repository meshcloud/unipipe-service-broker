import { Command } from "../deps.ts";
import { ServiceInstance } from "../handler.ts";
import { ServiceBinding } from "../osb.ts";
import { Repository } from "../repository.ts";

// currently unused, but we most likely will introduce options soon
interface TerraformOpts {
}

export function registerTerraformCmd(program: Command) {
  program
    .command("terraform [repo]")
    .description(
      "Runs Terraform modules located in the repository's terraform/<service_id> folder for all tenant service bindings. A" +
      "TF_VAR_client_secret env variable must be set to provide the secret of the service principal that will be used for applying " +
      "Terraform.",
    )
    .action(async (options: TerraformOpts, repo: string | undefined) => {
      const repository = new Repository(repo ? repo : ".");
      const out = await run(repository, options);
      console.log(out);
    });
}

export async function run(
  repo: Repository,
  opts: TerraformOpts,
): Promise<string[][]> {
  const instances = await repo.mapInstances(async (instance) => {
    return await instance;
  });

  return await Promise.all(
    instances.map((instance) => mapBindings(instance, repo)),
  );
}

async function mapBindings(
  instance: ServiceInstance,
  repo: Repository,
): Promise<string[]> {
  return await Promise.all(
    instance.bindings.map((binding) => processBinding(instance, binding, repo)),
  );
}

async function processBinding(
  instance: ServiceInstance,
  binding: ServiceBinding,
  repo: Repository,
): Promise<string> {
  const wrapper = {
    variable: {
      client_secret: {
        description: "Client Secret of the Azure App Registration.",
        type: "string",
        sensitive: true,
      },
    },
    module: {
      wrapper: {
        source:
          `../../../../terraform/${instance.instance.serviceDefinitionId}`,
        client_secret: "${var.client_secret}",
        ...instance.instance.parameters,
        ...binding.binding.bindResource,
        ...binding.binding.parameters
        // TODO We should also consider adding tags as parameters. Because currently for me meshStack or a policy that
        // is defined via a LZ always sets tags, that get removed again on terraform apply, as we don't define them here.
      },
    },
  };

  const bindingDir =
    `${repo.path}/instances/${instance.instance.serviceInstanceId}/bindings/${binding.binding.bindingId}`;

  Deno.writeTextFile(
    `${bindingDir}/module.tf.json`,
    JSON.stringify(wrapper, null, 2),
  );

  const tfResult = await executeTerraform(bindingDir);

  const bindingIdentifier = instance.instance.serviceInstanceId + "/" + binding.binding.bindingId + ": ";

  return  +
    tfResult.success ? bindingIdentifier + "successful" : bindingIdentifier + "failed";
}

// TODO write log to a service binding specific log file (can be overwritten on every new run)
async function executeTerraform(
  bindingDir: string,
): Promise<Deno.ProcessStatus> {
  console.log("Running Terraform Init for " + bindingDir);

  const tfInit = Deno.run({
    cmd: ["terraform", "init"],
    cwd: bindingDir,
  });

  await tfInit.status();

  console.log("Running Terraform Apply for " + bindingDir);

  const tfApply = Deno.run({
    cmd: ["terraform", "apply", "-auto-approve"],
    cwd: bindingDir,
  });

  return await tfApply.status();
}
