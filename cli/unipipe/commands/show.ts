import { Command, EnumType } from "../deps.ts";
import { Repository } from "../repository.ts";
import { stringify } from "../yaml.ts";

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
    .command("show [repo]")
    .type("format", formatsType)
    .description(
      "Shows the state stored service instance stored in a UniPipe OSB git repo."
    )
    .option("-i, --instance-id <id>", "Service instance id.")
    .option(
      "-o, --output-format <output-format>",
      "Output format. Supported formats are yaml and json.",
      { default: "yaml" }
    )
    .option("--pretty", "Pretty print")
    .action(async (options: ShowOpts, repo: string|undefined) => {
      const repository = new Repository(repo ? repo : ".");
      await show(repository, options);
    });
}

export async function show(repository: Repository, opts: ShowOpts) {
  const instance = await repository.loadInstance(opts.instanceId);

  switch (opts.outputFormat) {
    case "json": {
      const p = opts.pretty ? 4 : undefined;
      console.log(JSON.stringify(instance, null, p));
      break;
    }
    case "yaml": {
      const p = opts.pretty ? { indent: 4 } : {};
      console.log(stringify(instance, p));
      break;
    }
  }
}
