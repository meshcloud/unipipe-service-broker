import { Command } from "../deps.ts";
import { Repository } from "../repository.ts";
import ProcessStatus = Deno.ProcessStatus;

interface GitOpts {
}

export function registerGitCmd(program: Command) {
  program
    .command("git <cmd> [repo]")
    .description("Runs Git and simplifies working with pull/push commands")
    .action(async (options: GitOpts, cmd: string, repo: string | undefined) => {

      const repository = new Repository(repo ? repo : ".");

      switch (cmd) {
        case "pull":
          await pull(repository, options);
          break;
      }
    });
}

export async function pull(repo: Repository, opts: GitOpts): Promise<boolean> {
  const status = await executeGit(repo.path, ["pull", "--ff-only"])

  return status.success
}

export async function executeGit(dir: string, args: string[]): Promise<ProcessStatus> {
  const cmd = ["git", ...args]

  console.log(`Running ${cmd.join(' ')}`)

  const process = Deno.run({
    cmd: cmd,
    cwd: dir
  });

  return await process.status();
}
