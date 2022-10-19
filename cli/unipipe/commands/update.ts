import { Command, EnumType } from "../deps.ts";
import { Repository } from "../repository.ts";

const ALL_STATUSES = ["succeeded", "failed", "in progress"] as const;
type StatusesTuple = typeof ALL_STATUSES;
type Status = StatusesTuple[number];

export const STATUSES = ALL_STATUSES.slice();

const statusesType = new EnumType(ALL_STATUSES);

interface UpdateOpts {
  instanceId: string;
  bindingId?: string;
  credentials?: string[]
  status: Status;
  description: string;
}

export function registerUpdateCmd(program: Command) {
  program
    .command("update [repo]")
    .type("status", statusesType)
    .description(
      "Update status of a service instance or binding stored in a UniPipe OSB git repo."
    )
    .option("-i --instance-id <instance-id>", "Service instance id.", {})
    .option("-b --binding-id <binding-id>", "Service binding id.", {
      depends: ["instance-id"],
    })
    .option("-c --credentials <credentials>", "Credential format `key: value`", {
      collect: true,
      depends: ["binding-id"],
    })
    .option(
      "--status <status:status>",
      "The status. Allowed values are 'in progress', 'succeeded' and 'failed'."
    ) // todo use choices instead
    .option("--description [description]", "Status description text.", {
      default: "",
    })
    .action(async (options: UpdateOpts, repo: string|undefined) => {
      const repository = new Repository(repo ? repo : ".");
      await update(repository, options);
    });
}

export async function update(repository: Repository, opts: UpdateOpts) {
  const status = {
    status: opts.status,
    description: opts.description,
  };

  let credentialsYaml = "";
  opts.credentials?.forEach(credential => {
    credentialsYaml += `${credential}\n`;
  })

  if (!opts.bindingId) {
    await repository.updateInstanceStatus(opts.instanceId, status);
  } else {
    if (credentialsYaml != "") {
      await repository.updateBindingCredentials(opts.instanceId, opts.bindingId, credentialsYaml);
    }
    await repository.updateBindingStatus(
      opts.instanceId,
      opts.bindingId,
      status
    );
  }
}
