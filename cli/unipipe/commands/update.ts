import { Command, EnumType, path } from '../deps.ts';
import { stringify } from '../yaml.ts';

const ALL_STATUSES = ["succeeded", "failed", "in progress"] as const;
type StatusesTuple = typeof ALL_STATUSES;
type Status = StatusesTuple[number];

const statusesType = new EnumType(ALL_STATUSES);

interface UpdateOpts {
  instanceId: string;
  bindingId: string;
  status: Status;
  description: string;
}

export function registerUpdateCmd(program: Command) {
  program
    .command("update <repo>")
    .type("status", statusesType)
    .description(
      "Update status of a service instance or binding stored in a UniPipe OSB git repo.",
    )
    .option(
      "-i --instance-id <instance-id>",
      "Service instance id.", {
    }
    )
    .option(
      "-b --binding-id <binding-id>",
      "Service binding id.", {
      depends: ["instance-id"]
    }
    )
    .option(
      "--status <status:status>",
      "The status. Allowed values are 'in progress', 'succeeded' and 'failed'.",
    ) // todo use choices instead
    .option(
      "--description [description]",
      "Status description text.",
      {
        default: "",
      },
    )
    .action(async (options: UpdateOpts, repo: string) => {
      await update(repo, options);
    });
}

async function update(osbRepoPath: string, opts: UpdateOpts) {

  var statusYmlPath: string;

  if (!opts.bindingId) {
    const instanceStatusYmlPath = path.join(
      osbRepoPath,
      "instances",
      opts.instanceId,
      "status.yml",
    );
    statusYmlPath = instanceStatusYmlPath
  } else {
    const bindingStatusYmlPath = path.join(
      osbRepoPath,
      "instances",
      opts.instanceId,
      "bindings",
      opts.bindingId,
      "status.yml",
    );
    statusYmlPath = bindingStatusYmlPath
  }
    
  const yaml = stringify({
    status: opts.status,
    description: opts.description,
  });

  await Deno.writeTextFile(statusYmlPath, yaml);
}
