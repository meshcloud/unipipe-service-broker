import { Command, EnumType, path } from '../deps.ts';
import { readInstance } from '../osb.ts';
import { stringify } from '../yaml.ts';

const ALL_FORMATS = ["json", "yaml"] as const;
type FormatsTuple = typeof ALL_FORMATS;
type Format = FormatsTuple[number];

const formatsType = new EnumType(ALL_FORMATS);

export interface ShowOpts {
  instanceId: string;
  outputFormat: Format;
  pretty: boolean;
}

export function registerShowCmd(program: Command) {
  // show
  program
    .command("show <repo>")
    .type("format", formatsType)
    .description(
      "Shows the state stored service instance stored in a UniPipe OSB git repo.",
    )
    .option(
      "-i, --instance-id <id>",
      "Service instance id.",
    )
    .option(
      "-o, --output-format <output-format>",
      "Output format. Supported formats are yaml and json.",
      { default: "yaml" }
    )
    .option(
      "--pretty",
      "Pretty print",
    )
    .action(async (options: ShowOpts, repo: string) => {
      await show(repo, options);
    });
}

export async function show(osbRepoPath: string, opts: ShowOpts) {
  const instancesPath = path.join(
    osbRepoPath,
    "instances",
    opts.instanceId,
  );
  const instance = await readInstance(instancesPath);

  switch (opts.outputFormat) {
    case "json": {
      const p = opts.pretty ? 4 : undefined;
      console.log(JSON.stringify(instance, null, p));
      break;
    }
    case "yaml": {
      const p = opts.pretty ? { indent: 4 } : {};
      console.log(stringify(instance as any, p));
      break;
    }
  }
}
