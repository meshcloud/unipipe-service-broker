import { Command } from "../deps.ts";
import { Repository } from "../repository.ts";

interface GitOpts {
  name?: string;
  email?: string;
  message?: string;
}

export function registerGitCmd(program: Command) {
  program
    .command("git <cmd> [repo]")
    .option(
      "-n, --name [name:string]",
      "Git author username. Default is `Unipipe CLI`."
    )
    .option(
      "-e, --email [email:string]",
      "Git author email. Default is `unipipe-cli@meshcloud.io`."
    )
    .option(
      "-m, --message [message:string]",
      "Commit message. Default is `Commit changes`."
    )
    .description("Runs Git pull/push commands resiliently. It takes care of retrying and rebasing if needed to make sure a push will be successful.")
    .action(async (opts: GitOpts, cmd: string, repo: string | undefined) => {

      const repository = new Repository(repo ? repo : ".");

      switch (cmd) {
        case "pull":
          await commandPull(repository);
          break;
        case "push":
          await commandPush(repository, opts);
          break;
        default:
          console.log(`Git command '${cmd}' is not found`);
      }
    });
}

export async function commandPull(repo: Repository) {
  const add = await gitAdd(repo.path);
  if (!add) return;

  const hasChanges = await gitHasChanges(repo.path);

  if(hasChanges) {
    const stash = await gitStash(repo.path);
    if (!stash) return;
  }

  const pullFastForward = await gitPullFastForward(repo.path);

  if (!pullFastForward) {
    await gitPullRebase(repo.path);
  }

  if(hasChanges) {
    await gitStashPop(repo.path);
  }
}

export async function commandPush(repo: Repository, opts: GitOpts) {
  const name = opts.name || "Uncoipipe CLI";
  const email = opts.email || "unipipe-cli@meshcloud.io";
  const message = opts.message || "Commit changes";
  
  const add = await gitAdd(repo.path);
  if (!add) return;

  const hasChanges = await gitHasChanges(repo.path);

  if (hasChanges) {
    const commit = await gitCommit(repo.path, name, email, message);
    if (!commit) return;

    const push = await gitPush(repo.path);
    if (!push) {
      await commandPull(repo);
      await gitPush(repo.path);
    }
  }
  else {
    console.log("No chnages to commit")
  }  
}

async function gitPullFastForward(repoPath: string): Promise<boolean> {
  return await executeGit(repoPath, ["pull", "--ff-only"]);
}

async function gitPullRebase(repoPath: string): Promise<boolean> {
  return await executeGit(repoPath, ["pull", "--rebase"]);
}

async function gitPush(repoPath: string): Promise<boolean> {
  return await executeGit(repoPath, ["push"]);
}

async function gitAdd(repoPath: string): Promise<boolean> {
  return await executeGit(repoPath, ["add", "."]);
}

async function gitHasChanges(repoPath: string): Promise<boolean> {  
  const diffIndex = await executeGit(repoPath, ["diff-index", "--quiet", "HEAD", "--"], false);
  return !diffIndex;
}

async function gitStash(repoPath: string): Promise<boolean> {
  return await executeGit(repoPath, ["stash"]);
}

async function gitStashPop(repoPath: string): Promise<boolean> {
  return await executeGit(repoPath, ["stash", "pop"]);
}

async function gitCommit(repoPath: string, name: string, email: string, message: string): Promise<boolean> {
  return await executeGit(repoPath, [
    "commit", "-a", "-m", `Unipipe CLI: ${message}`, "--author", `${name} <${email}>`]);
}

async function executeGit(dir: string, args: string[], displayStatus = true): Promise<boolean> {
  const cmd = ["git", ...args];
  const cmdLine = cmd.join(' ');

  console.log(`Running ${cmdLine}`);

  const process = Deno.run({
    cmd: cmd,
    cwd: dir
  });

  const status = await process.status();

  if(displayStatus) {
    console.log(status.success ? `Command ${cmdLine} succeeded` : `Command ${cmdLine} failed`);
  } 

  return status.success;
}