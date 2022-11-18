import { assertSpyCall, assertSpyCalls, Stub, stub, returnsNext } from "../dev_deps.ts";
import { withTempDir } from "../test-util.ts";
import { Repository } from "../repository.ts";
import { commandPull, commandPush, GitOpts } from "./git.ts";

Deno.test(
  "can pull with fast forward with no local changes",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        successfulResult(),
        successfulResult(),
        successfulResult()
      ]);

      await commandPull(new Repository(dir));

      assertGit(dir, stub, 0, ["git", "add", "."]);
      assertGit(dir, stub, 1, ["git", "diff-index", "--quiet", "HEAD", "--"])
      assertGit(dir, stub, 2, ["git", "pull", "--ff-only"])      
      assertSpyCalls(stub, 3);      

      stub.restore();      
    })
);

Deno.test(
  "can pull with fast forward with local changes",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(),
        successfulResult(),
        successfulResult()
      ]);

      await commandPull(new Repository(dir));

      assertGit(dir, stub, 0, [ "git", "add", "." ]);
      assertGit(dir, stub, 1, [ "git", "diff-index", "--quiet", "HEAD", "--" ]);
      assertGit(dir, stub, 2, [ "git", "stash" ]);
      assertGit(dir, stub, 3, [ "git", "pull", "--ff-only" ]);
      assertGit(dir, stub, 4, [ "git", "stash", "pop" ]);
      assertSpyCalls(stub, 5);

      stub.restore();      
    })
);

Deno.test(
  "can pull with rebase with no local chnages",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        successfulResult(),
        successfulResult(),
        failedResult(),
        successfulResult(), 
        successfulResult(),
      ]);

      await commandPull(new Repository(dir));

      assertGit(dir, stub, 0, [ "git", "add", "." ]);
      assertGit(dir, stub, 1, [ "git", "diff-index", "--quiet", "HEAD", "--" ]);      
      assertGit(dir, stub, 2, [ "git", "pull", "--ff-only" ]);
      assertGit(dir, stub, 3, [ "git", "pull", "--rebase" ]);
      assertSpyCalls(stub, 4);      

      stub.restore();      
    })
);

Deno.test(
  "can pull with rebase with local chnages",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(),
        failedResult(),
        successfulResult(), 
        successfulResult(),
        successfulResult()
      ]);

      await commandPull(new Repository(dir));

      assertGit(dir, stub, 0, [ "git", "add", "." ]);
      assertGit(dir, stub, 1, [ "git", "diff-index", "--quiet", "HEAD", "--" ]);
      assertGit(dir, stub, 2, [ "git", "stash" ]);
      assertGit(dir, stub, 3, [ "git", "pull", "--ff-only" ]);
      assertGit(dir, stub, 4, [ "git", "pull", "--rebase" ]);
      assertGit(dir, stub, 5, [ "git", "stash", "pop" ]);
      assertSpyCalls(stub, 6);      

      stub.restore();      
    })
);

Deno.test(
  "do not push if the repository is not initialized",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        failedResult()
      ]);

      await commandPush(new Repository(dir), {});

      assertGit(dir, stub, 0, [ "git", "add", "." ]);
      assertSpyCalls(stub, 1);

      stub.restore();
    })
);

Deno.test(
  "do not commit if there are no changes while pushing",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        successfulResult(),
        successfulResult()
      ]);

      await commandPush(new Repository(dir), {});

      assertGit(dir, stub, 0, [ "git", "add", "." ]);
      assertGit(dir, stub, 1, [ "git", "diff-index", "--quiet", "HEAD", "--" ]);
      assertSpyCalls(stub, 2);

      stub.restore();
    })
);

Deno.test(
  "can commit and push changes without upstream conflicts",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(),        
        successfulResult()
      ]);

      await commandPush(new Repository(dir), {});

      assertGit(dir, stub, 0, [ "git", "add", "." ]);
      assertGit(dir, stub, 1, [ "git", "diff-index", "--quiet", "HEAD", "--" ]);
      assertGit(dir, stub, 2, [ "git", "commit", "-a", "-m", "Unipipe CLI: Commit changes", "--author", "Uncoipipe CLI <unipipe-cli@meshcloud.io>"]);
      assertGit(dir, stub, 3, [ "git", "push" ]);
      assertSpyCalls(stub, 4);

      stub.restore();
    })
);

Deno.test(
  "can commit with custom author and commit message",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(),        
        successfulResult()
      ]);

      const opts: GitOpts = {
        name: "John Doe",
        email: "john@doe.loc",
        message: "Some changes"

      }
      await commandPush(new Repository(dir), opts);

      assertGit(dir, stub, 0, [ "git", "add", "." ]);
      assertGit(dir, stub, 1, [ "git", "diff-index", "--quiet", "HEAD", "--" ]);
      assertGit(dir, stub, 2, [ "git", "commit", "-a", "-m", "Unipipe CLI: Some changes", "--author", "John Doe <john@doe.loc>"]);
      assertGit(dir, stub, 3, [ "git", "push" ]);
      assertSpyCalls(stub, 4);

      stub.restore();
    })
);

Deno.test(
  "can commit and push changes with upstream conflicts (fast-forward)",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(), 
        failedResult(),
        successfulResult(),
        successfulResult()
      ]);

      await commandPush(new Repository(dir), {});

      assertGit(dir, stub, 0, [ "git", "add", "." ]);
      assertGit(dir, stub, 1, [ "git", "diff-index", "--quiet", "HEAD", "--" ]);
      assertGit(dir, stub, 2, [ "git", "commit", "-a", "-m", "Unipipe CLI: Commit changes", "--author", "Uncoipipe CLI <unipipe-cli@meshcloud.io>"]);
      assertGit(dir, stub, 3, [ "git", "push" ]);
      assertGit(dir, stub, 4, [ "git", "pull", "--ff-only" ]);
      assertGit(dir, stub, 5, [ "git", "push" ]);
      assertSpyCalls(stub, 6);

      stub.restore();
    })
);

Deno.test(
  "can commit and push changes with upstream conflicts (rebase)",
  async () =>
    await withTempDir(async (dir) => {

      const stub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(), 
        failedResult(),
        failedResult(),
        successfulResult(),
        successfulResult()
      ]);

      await commandPush(new Repository(dir), {});

      assertGit(dir, stub, 0, [ "git", "add", "." ]);
      assertGit(dir, stub, 1, [ "git", "diff-index", "--quiet", "HEAD", "--" ]);
      assertGit(dir, stub, 2, [ "git", "commit", "-a", "-m", "Unipipe CLI: Commit changes", "--author", "Uncoipipe CLI <unipipe-cli@meshcloud.io>", ]);
      assertGit(dir, stub, 3, [ "git", "push" ]);
      assertGit(dir, stub, 4, [ "git", "pull", "--ff-only" ]);
      assertGit(dir, stub, 5, [ "git", "pull", "--rebase" ]);
      assertGit(dir, stub, 6, [ "git", "push" ]);
      assertSpyCalls(stub, 7);

      stub.restore();
    })
);

function createMockedGit(mockedResults: unknown[]): Stub {
  return stub(
    Deno,
    "run",        
    returnsNext(mockedResults)
  );
}

function successfulResult() {
  return createMockedResult(true)
}

function failedResult() {
  return createMockedResult(false)
}

function createMockedResult(success: boolean) {
  return {
    status: () => {
      return {
        success: success,
        code: 0,
        signal: undefined
      }
    }
  };
}

function assertGit(dir: string, stub: Stub, callIndex: number, args: string[]) {
  assertSpyCall(stub, callIndex, {
    args: [{
      cmd: args,
      cwd: dir
    }]
  });
}