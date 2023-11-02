import { Command } from "../deps.ts";
import { ServiceInstance } from "../handler.ts";
import { OsbServiceInstanceStatus, ServiceBinding } from "../osb.ts";
import { Repository } from "../repository.ts";

// currently unused, but we most likely will introduce options soon
interface TerraformOpts {
  plan?: boolean;
}

type TerraformCommand = "apply" | "plan" | "destroy";

export function registerTerraformCmd(program: Command) {
  program
    .command("terraform [repo]")
    .option(
      "-p, --plan",
      "With this option only a terraform plan instead of an apply will be executed.",
    )
    .description(
      "Runs Terraform modules located in the repository’s terraform/<service_id> folder for all tenant service bindings. A " +
        "TF_VAR_platform_secret env variable must be set to provide the secret of the service principal that will be used for applying " +
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
    instances.map((instance) => mapBindings(instance, repo, opts)),
  );
}

async function mapBindings(
  instance: ServiceInstance,
  repo: Repository,
  opts: TerraformOpts,
): Promise<string[]> {
  if (instance.bindings.length == 0) {
    const status: OsbServiceInstanceStatus = {
      status: "succeeded",
      description:
        "Instance without binding processed successfully. No action executed.",
    };
    repo.updateInstanceStatus(
      instance.instance.serviceInstanceId,
      status,
    );

    return [instance.instance.serviceInstanceId + ": succeeded"];
  }

  return await Promise.all(instance.bindings.map(async (binding) => {
    try {
      return await processBinding(instance, binding, repo, opts);
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
  }));
}

async function processBinding(
  instance: ServiceInstance,
  binding: ServiceBinding,
  repo: Repository,
  opts: TerraformOpts,
): Promise<string> {
  const bindingIdentifier = instance.instance.serviceInstanceId + "/" +
    binding.binding.bindingId;

  console.log("starting to process " + bindingIdentifier);

  if (
    instance.servicePlan.metadata.manualInstanceInputNeeded === true &&
    instance.manualParameters === null
  ) {
    return handlePendingManualParameters(
      bindingIdentifier,
      repo,
      instance,
      binding,
    );
  }

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
  tryCopyBackendTf(repo, instance.instance.serviceDefinitionId, bindingDir);

  const isDeleted = instance.instance.deleted || binding.binding.deleted;
  let terraformCommand: TerraformCommand = "plan";
  if (!opts.plan) {
    terraformCommand = isDeleted ? "destroy" : "apply";
  }

  const tfResult = await executeTerraform(
    terraformCommand,
    bindingDir,
    binding.binding.bindingId,
    isDeleted,
  );

  if (!opts.plan) {
    updateStatusAfterTfApply(tfResult, repo, instance, binding, isDeleted);
  }

  const successMessage = opts.plan
    ? "planned"
    : isDeleted
    ? "deleted"
    : "successful";

  return tfResult.success
    ? `${bindingIdentifier}: ${successMessage}`
    : `${bindingIdentifier}: failed`;
}

function handlePendingManualParameters(
  bindingIdentifier: string,
  repo: Repository,
  instance: ServiceInstance,
  binding: ServiceBinding,
) {
  console.log(
    `Service Binding ${bindingIdentifier} is pending, because manual parameters have not been provided yet. ` +
      "params.yml is missing in the service instance",
  );

  const status: OsbServiceInstanceStatus = {
    status: "in progress",
    description: "Waiting for manual input from a platform operator!",
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

  return bindingIdentifier + ": pending";
}

function createTerraformWrapper(
  instance: ServiceInstance,
  binding: ServiceBinding,
  bindingDir: string,
) {
  // we reduce the context object here as for the Terraform execution those auth urls are not relevant
  // deno-lint-ignore no-explicit-any
  const reducedContext: any = {
    ...instance.instance.context,
  };
  delete (reducedContext.auth_url);
  delete (reducedContext.token_url);
  delete (reducedContext.permission_url);

  const terraformWrapper = {
    variable: {
      platform_secret: {
        description:
          "The secret that will be used by Terraform to authenticate against the cloud platform.",
        type: "string",
        sensitive: true,
      },
    },
    module: {
      wrapper: {
        source:
          `../../../../terraform/${instance.instance.serviceDefinitionId}`,
        platform_secret: "${var.platform_secret}",
        plan_id: instance.servicePlan.id,
        plan_name: instance.servicePlan.name,
        ...reducedContext,
        ...instance.instance.parameters,
        ...binding.binding.bindResource,
        ...binding.binding.parameters,
        ...instance.manualParameters,
      },
    },
  };

  Deno.writeTextFileSync(
    `${bindingDir}/module.tf.json`,
    JSON.stringify(terraformWrapper, null, 2),
  );
}

function tryCopyBackendTf(
  repo: Repository,
  serviceDefinitionId: string,
  bindingDir: string,
) {
  try {
    Deno.statSync(
      `${repo.path}/terraform/${serviceDefinitionId}/backend.tf`,
    );
  } catch (_) {
    console.debug(
      `No backend file exists for service definition ${serviceDefinitionId}. So we are not using a backend for binding ${bindingDir}`,
    );

    return;
  }

  Deno.copyFileSync(
    `${repo.path}/terraform/${serviceDefinitionId}/backend.tf`,
    `${bindingDir}/backend.tf`,
  );
}

function updateStatusAfterTfApply(
  tfResult: Deno.ProcessStatus,
  repo: Repository,
  instance: ServiceInstance,
  binding: ServiceBinding,
  isDeleted = false,
) {
  const status: OsbServiceInstanceStatus = tfResult.success
    ? {
      status: "succeeded",
      description: isDeleted
        ? "Service Instance successfully deleted!"
        : "Terraform applied successfully",
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
  terraformCommand: TerraformCommand,
  bindingDir: string,
  bindingId: string,
  isDeleted: boolean,
): Promise<Deno.ProcessStatus> {
  console.log("Running Terraform Init for " + bindingDir);

  const tfInit = Deno.run({
    cmd: ["terraform", "init"],
    cwd: bindingDir,
  });

  await tfInit.status();

  await selectOrCreateTerraformWorkspace(bindingId, bindingDir);

  console.log(`Running Terraform ${terraformCommand} for ${bindingDir}`);

  const planCommand = ["terraform", "plan"];
  if (isDeleted) {
    planCommand.push("-destroy");
  }

  const cmd = terraformCommand == "plan"
    ? planCommand
    : ["terraform", terraformCommand, "-auto-approve"];

  const tfCommand = Deno.run({
    cmd: cmd,
    cwd: bindingDir,
  });

  return await tfCommand.status();
}

async function selectOrCreateTerraformWorkspace(
  bindingId: string,
  bindingDir: string,
) {
  const workspaceName = bindingId;

  console.log(
    `Selecting Terraform Workspace ${workspaceName} for binding ${bindingDir}.`,
  );

  const existingWorkspace = Deno.run({
    cmd: ["terraform", "workspace", "select", workspaceName],
    cwd: bindingDir,
  });

  const existingWorkspaceResult = await existingWorkspace.status();

  if (!existingWorkspaceResult.success) {
    console.log("Running Terraform Workspace new for " + bindingDir);

    const newWorkspace = Deno.run({
      cmd: ["terraform", "workspace", "new", workspaceName],
      cwd: bindingDir,
    });

    const newWorkspaceResult = await newWorkspace.status();

    if (!newWorkspaceResult.success) {
      throw new Error(
        `Could not create workspace ${workspaceName} for binding ${bindingDir}.`,
      );
    }
  }
}
