import { registerGenerateCmd } from './commands/generate.ts';
import { registerListCmd } from './commands/list.ts';
import { registerShowCmd } from './commands/show.ts';
import { registerTransformCmd } from './commands/transform.ts';
import { registerUpdateCmd } from './commands/update.ts';
import { Command, CompletionsCommand } from './deps.ts';

const program = new Command()
  .name("unipipe")
  .version("0.6.4")
  .description("UniPipe CLI - supercharge your GitOps OSB service pipelines");

program
  .command("completions", new CompletionsCommand());

registerListCmd(program);
registerShowCmd(program);
registerTransformCmd(program);
registerUpdateCmd(program);
registerGenerateCmd(program);

const hasArgs = Deno.args.length > 0;
if (hasArgs)
  await program.parse(Deno.args);
else
  program.showHelp();