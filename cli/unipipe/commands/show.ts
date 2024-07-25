import { Command, EnumType } from "../deps.ts";
import { Repository } from "../repository.ts";
import { stringify } from "../yaml.ts";

export enum OutputFormat {
  JSON = "json",
  YAML = "yaml",
}

export const OutputFormatType = new EnumType(Object.values(OutputFormat));

export interface ShowOpts {
  instanceId: string;
  outputFormat?: OutputFormat;
  pretty?: boolean;
}

export function registerShowCmd(program: Command) {
  // show
  program
    .command("show [repo]")
    .type("format", OutputFormatType)
    .description(
      "Shows the state stored service instance stored in a UniPipe OSB git repo.",
    )
    .option("-i, --instance-id <id>", "Service instance id.", {
      required: true,
    })
    .option(
      "-o, --output-format <output-format:format>",
      "Output format. Supported formats are yaml and json.",
      { default: OutputFormat.YAML },
    )
    .option("--pretty", "Pretty print")
    .action(async (options: ShowOpts, repo: string | undefined) => {
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
