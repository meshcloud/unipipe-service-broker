import {
  colors,
  Command,
  Select,
  SelectValueOptions,
  prompt,
  Input,
} from "../deps.ts";
import { Repository } from "../repository.ts";
import { stringify } from "../yaml.ts";
import { show } from "./show.ts";
import { STATUSES, update } from "./update.ts";

export function registerBrowseCmd(program: Command) {
  program
    .command("browse <repo>")
    .description("Interactively browse and manipulate a UniPipe OSB git repo.")
    .action(async (_opts: Record<never, never>, repo: string) => {
      const repository = new Repository(repo);
      await browseInstances(repository);
    });
}

async function browseInstances(repo: Repository) {
  // We need to refresh the instance list select prompt before showing it.
  // Otherwise it's possible that e.g. a user updates an instance status but the instance list
  // does not reflect that when it's shown again. Replacing the select options array contents is not
  // exactly officially documented as supported by cliffy, but also not forbidden. It works though ;-)
  const instanceOptions: SelectValueOptions = [];
  async function refreshInstanceList() {
    // deno-lint-ignore require-await
    const opts = await repo.mapInstances(async (x) => {
      const i = x.instance;
      const plan = i.serviceDefinition.plans.filter(
        (x) => x.id === i.planId
      )[0];

      const name = [
        colors.dim("id: ") + colors.gray(i.serviceInstanceId),
        colors.dim("service: ") + colors.green(i.serviceDefinition.name),
        colors.dim("plan: ") + colors.yellow(plan.name),
        colors.dim("bindings: ") +
          colors.brightMagenta(x.bindings.length.toString()),
        colors.dim("status: ") +
          colors.blue(x.status?.status || "new") +
          colors.red(i.deleted ? " deleted" : ""),
      ].join(" ");

      return { name, value: i.serviceInstanceId };
    });

    instanceOptions.length = 0;
    instanceOptions.push(...opts);
  }

  const bindingOptions: SelectValueOptions = [];
  async function refreshBindingList(instanceId: string) {
    const instance = await repo.loadInstance(instanceId);
    const opts = instance.bindings.map((x) => {
      const name = [
        colors.dim("id: ") + colors.gray(x.binding.bindingId),
        // we could add binding paremeters in here,
        colors.dim("params: ") +
          colors.green(JSON.stringify(x.binding.parameters)),
        colors.dim("status: ") +
          colors.blue(x.status?.status || "new") +
          colors.red(x.deleted ? " deleted" : ""),
      ].join(" ");

      return { name, value: x.binding.bindingId };
    });

    bindingOptions.length = 0;
    bindingOptions.push(...opts);
  }

  await refreshInstanceList();

  await prompt([
    {
      name: "selectInstance",
      type: Select,
      message: "Pick a service instance",
      options: instanceOptions,
    },
    {
      name: "instanceCmd",
      message: "What do you want to do with this instance?",
      type: Select,
      options: ["show", "bindings", "update", "↑ instances"],
      after: async ({ selectInstance, instanceCmd }, next) => {
        const cmd = instanceCmd!!;
        const instanceId = selectInstance!!;
        switch (cmd!!) {
          case "show":
            await showInstance(repo, instanceId);
            next("instanceCmd"); // loop
            break;
          case "bindings":
            await refreshBindingList(instanceId);
            if (bindingOptions.length > 0) {
              next("selectBinding");
            }
            else {
              console.log("No bindings found for this instance!")
              next("instanceCmd");
            }
            break;
          case "update":
            await updateInstance(repo, instanceId);
            next("instanceCmd"); // loop
            break;
          case "↑ instances":
            await refreshInstanceList();
            next("selectInstance"); // back up
            break;
          default:
            break;
        }
      },
    },
    {
      name: "selectBinding",
      type: Select,
      message: "Pick a service binding",
      options: bindingOptions,
    },
    {
      name: "bindingCmd",
      message: "What do you want to do with this binding?",
      type: Select,
      options: ["show", "update", "↑ bindings", "↑ instances"],
      after: async ({ selectInstance, selectBinding, bindingCmd }, next) => {
        const instanceId = selectInstance!!;
        const bindingId = selectBinding!!;
        const cmd = bindingCmd!!;
        switch (cmd) {
          case "show":
            await showBinding(repo, instanceId, bindingId);
            next("bindingCmd"); // loop
            break;
          case "update":
            await updateBinding(repo, instanceId, bindingId);
            next("bindingCmd"); // loop
            break;
          case "↑ bindings":
            await refreshBindingList(instanceId);
            next("selectBinding"); // back up
            break;
          case "↑ instances":
            await refreshInstanceList();
            next("selectInstance"); // back up
            break;
          default:
            break;
        }
      },
    },
  ]);
}

async function showInstance(repository: Repository, instanceId: string) {
  await show(repository, {
    instanceId: instanceId,
    outputFormat: "yaml",
    pretty: true,
  });
}

async function updateInstance(repo: Repository, instanceId: string) {
  const { status, description } = await prompt([
    {
      name: "status",
      message: "What's the new status?",
      type: Select,
      options: STATUSES,
    },
    {
      name: "description",
      message: "Add a status descript status",
      type: Input,
    },
  ]);

  await update(repo, {
    instanceId,
    status: status,
    description: description,
  });

  console.log(`Updated status of instance ${instanceId} to '${status}'`);
}

async function updateBinding(
  repo: Repository,
  instanceId: string,
  bindingId: string
) {
  const { status, description } = await prompt([
    {
      name: "status",
      message: "What's the new status?",
      type: Select,
      options: STATUSES,
    },
    {
      name: "description",
      message: "Add a status descript status",
      type: Input,
    },
  ]);

  await update(repo, {
    instanceId,
    bindingId,
    status: status,
    description: description,
  });

  console.log(
    `Updated status of instance ${instanceId} binding ${bindingId} to '${status}'`
  );
}

async function showBinding(
  repo: Repository,
  instanceId: string,
  bindingId: string
) {
  const instance = await repo.loadInstance(instanceId);
  const binding = instance.bindings.find(
    (x) => x.binding.bindingId === bindingId
  );

  if (!binding) {
    throw new Error("Could not find binding with id: " + bindingId);
  }
  // todo: should probably centralize this code a bit
  // todo: also consider printing the file paths for any "show" style command?
  const p = { indent: 4 };
  console.log(stringify(binding, p));
}
