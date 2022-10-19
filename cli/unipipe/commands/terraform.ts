import { Command } from "../deps.ts";
import { ServiceInstance } from "../handler.ts";
import { OsbServiceInstanceStatus, ServiceBinding } from "../osb.ts";
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
  console.log("starting terraform command");

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
    instance.bindings.map(async(binding) => {
      try {
        return await processBinding(instance, binding, repo);
      } catch (e) {
        const bindingIdentifier = instance.instance.serviceInstanceId + "/" +
          binding.binding.bindingId;

        console.error(
          `processing binding ${bindingIdentifier} failed with error: ${e}`,
        );

        const status: OsbServiceInstanceStatus = {
          status: "failed",
          description: "Processing the binding failed!",
        };

        repo.updateInstanceStatus(
          instance.instance.serviceInstanceId,
          status,
        );

        repo.updateBindingStatus(
          instance.instance.serviceInstanceId,
          binding.binding.bindingId,
          status,
        );

        return bindingIdentifier + ": failed";
      }
    }),
  );
}

async function processBinding(
  instance: ServiceInstance,
  binding: ServiceBinding,
  repo: Repository,
): Promise<string> {
  const bindingIdentifier = instance.instance.serviceInstanceId + "/" +
    binding.binding.bindingId;

  console.log("starting to process " + bindingIdentifier);

  try {
    Deno.statSync(
      `${repo.path}/terraform/${instance.instance.serviceDefinitionId}`,
    );
  } catch (e) {
    if (e instanceof Deno.errors.NotFound) {
      console.log(
        `Skipping ${bindingIdentifier}, because no Terraform folder exists for service definition with id ${instance.instance.serviceInstanceId}`,
      );

      return bindingIdentifier + ": skipped";
    }
  }

  const bindingDir =
    `${repo.path}/instances/${instance.instance.serviceInstanceId}/bindings/${binding.binding.bindingId}`;

  createTerraformWrapper(instance, binding, bindingDir);

  const tfResult = await executeTerraform(bindingDir);

  updateStatusAfterTfApply(tfResult, repo, instance, binding);

  return tfResult.success
    ? bindingIdentifier + ": successful"
    : bindingIdentifier + ": failed";
}

function createTerraformWrapper(
  instance: ServiceInstance,
  binding: ServiceBinding,
  bindingDir: string,
) {
  const terraformWrapper = {
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
        ...binding.binding.parameters,
      },
    },
  };

  Deno.writeTextFileSync(
    `${bindingDir}/module.tf.json`,
    JSON.stringify(terraformWrapper, null, 2),
  );
}

function updateStatusAfterTfApply(
  tfResult: Deno.ProcessStatus,
  repo: Repository,
  instance: ServiceInstance,
  binding: ServiceBinding,
) {
  const status: OsbServiceInstanceStatus = tfResult.success
    ? {
      status: "succeeded",
      description: "Terraform applied successfully",
    }
    : {
      status: "failed",
      description: "Applying Terraform failed!",
    };

  repo.updateInstanceStatus(
    instance.instance.serviceInstanceId,
    status,
  );

  repo.updateBindingStatus(
    instance.instance.serviceInstanceId,
    binding.binding.bindingId,
    status,
  );
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
