import { Command } from '../deps.ts';
import { write } from '../dir.ts';
import { InstanceHandler } from '../handler.ts';
import { ServiceInstance } from '../osb.ts';
import { mapInstances } from './helpers.ts';

interface TransformOpts {
  xportRepo: string;
  registryOfHandlers: string;
}

export function registerTransformCmd(program: Command) {
  program
    .command(
      "transform <repo>",
    )
    .description(
      "Transform service instances stored in a UniPipe OSB git repo using the specified handlers.",
    )
    .option(
      "-r, --registry-of-handlers <file>",
      "A registry of handlers for processing service instance transformation. These can be defined in javascript, see `unipipe generate transform-handler` for an example.",
    )
    .option(
      "-x, --xport-repo [path:string]",
      "Path to the target git repository. If not specified the transform runs in place on the OSB git repo.",
    )
    .action(async (options: TransformOpts, repo: string) => {
      await transform(repo, options);
    });
}

async function transform(
  osbRepoPath: string,
  opts: TransformOpts,
) {
  const handlers = await loadHandlers(opts.registryOfHandlers);
  const outRepoPath = opts.xportRepo || osbRepoPath;

  mapInstances(osbRepoPath, async (instance: ServiceInstance) => {
    const handler = handlers[instance.instance.serviceDefinitionId];
    const handledBy = (handler && handler.name) || "(no handler found)";

    console.log(
      `- instance id: ${instance.instance.serviceInstanceId} | definition id: ${instance.instance.serviceDefinitionId} -> ${handledBy}`,
    );

    if (!handler) {
      return;
    }

    const tree = handler.handle(instance);
    if (tree) {
      await write(tree, outRepoPath);
    }
  });
}

async function loadHandlers(
  handlerSrc: string,
): Promise<Record<string, InstanceHandler>> {
  if (handlerSrc.endsWith(".ts")) {
    console.log(
      `Loading handlers as default export from typescript module ${handlerSrc}.`,
    );

    return await loadTypeScriptHandlers(handlerSrc);
  } else if (handlerSrc.endsWith(".js")) {
    console.log(
      `Loading handlers as javascript via eval() from ${handlerSrc}.`,
    );

    return await loadJavaScriptHandlers(handlerSrc);

  } else {
    throw Error(
      "could not land handlers, unsupport handler type (needs to be a '.ts' or '.js' file).",
    );
  }
}

/**
 * This is currently a "hidden" feature of unipipe-cli. 
 * Loading a typescript registry only works when running via `deno run` with the `--allow-net` permission set and is
 * therefore not available in compiled builds of unipipe-cli.
 */
async function loadTypeScriptHandlers(
  handlerSrc: string,
): Promise<Record<string, InstanceHandler>> {
  const handlersModule = await import(handlerSrc);

  console.debug(`loaded handler modules`, handlersModule);

  return handlersModule.default;
}

async function loadJavaScriptHandlers(
  handlerSrc: string,
): Promise<Record<string, InstanceHandler>> {
  const js = await Deno.readTextFile(handlerSrc);
  const handlersModule = eval(js);

  console.debug(`loaded handler modules`, handlersModule);
  
  return handlersModule;
}
