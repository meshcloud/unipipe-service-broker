import { assertSpyCall, assertSpyCalls, Stub, stub, returnsNext } from "../dev_deps.ts";
import { withTempDir } from "../test-util.ts";
import { Repository } from "../repository.ts";
import { commandPull, commandPush } from "./git.ts";

Deno.test(
  "can pull with fast forward with no local changes",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        successfulResult(),
        successfulResult(),
        successfulResult()
      ]);

      await commandPull(new Repository(tmp));

      assertSpyCall(strub, 0, { 
        args: [{ 
          cmd: [ 
            "git",
            "add",
            "."
          ], 
          cwd: tmp 
        }]
      });

      assertSpyCall(strub, 1, {
        args: [{
          cmd: [
            "git",
            "diff-index",
            "--quiet",
            "HEAD",
            "--"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 2, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--ff-only"
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 3);      
      strub.restore();      
    })
);

Deno.test(
  "can pull with fast forward with local changes",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(),
        successfulResult(),
        successfulResult()
      ]);

      await commandPull(new Repository(tmp));

      assertSpyCall(strub, 0, { 
        args: [{ 
          cmd: [ 
            "git",
            "add",
            "."
          ], 
          cwd: tmp 
        }]
      });

      assertSpyCall(strub, 1, {
        args: [{
          cmd: [
            "git",
            "diff-index",
            "--quiet",
            "HEAD",
            "--"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 2, {
        args: [{
          cmd: [
            "git",
            "stash"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 3, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--ff-only"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 4, {
        args: [{
          cmd: [
            "git",
            "stash",
            "pop"
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 5);      
      strub.restore();      
    })
);

Deno.test(
  "can pull with rebase with no local chnages",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        successfulResult(),
        successfulResult(),
        failedResult(),
        successfulResult(), 
        successfulResult(),
      ]);

      await commandPull(new Repository(tmp));

      assertSpyCall(strub, 0, { 
        args: [{ 
          cmd: [ 
            "git",
            "add",
            "."
          ], 
          cwd: tmp 
        }]
      });

      assertSpyCall(strub, 1, {
        args: [{
          cmd: [
            "git",
            "diff-index",
            "--quiet",
            "HEAD",
            "--"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 2, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--ff-only"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 3, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--rebase"
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 4);      
      strub.restore();      
    })
);

Deno.test(
  "can pull with rebase with local chnages",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(),
        failedResult(),
        successfulResult(), 
        successfulResult(),
        successfulResult()
      ]);

      await commandPull(new Repository(tmp));

      assertSpyCall(strub, 0, { 
        args: [{ 
          cmd: [ 
            "git",
            "add",
            "."
          ], 
          cwd: tmp 
        }]
      });

      assertSpyCall(strub, 1, {
        args: [{
          cmd: [
            "git",
            "diff-index",
            "--quiet",
            "HEAD",
            "--"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 2, {
        args: [{
          cmd: [
            "git",
            "stash"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 3, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--ff-only"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 4, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--rebase"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 5, {
        args: [{
          cmd: [
            "git",
            "stash",
            "pop"
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 6);      
      strub.restore();      
    })
);

Deno.test(
  "do not push if the repository is not initialized",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        failedResult()
      ]);

      await commandPush(new Repository(tmp), {});

      assertSpyCall(strub, 0, {
        args: [{
          cmd: [
            "git",
            "add",
            "."
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 1);
      strub.restore();
    })
);

Deno.test(
  "do not commit if there are no changes while pushing",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        successfulResult(),
        successfulResult()
      ]);

      await commandPush(new Repository(tmp), {});

      assertSpyCall(strub, 0, {
        args: [{
          cmd: [
            "git",
            "add",
            "."
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 1, {
        args: [{
          cmd: [
            "git",
            "diff-index",
            "--quiet",
            "HEAD",
            "--"
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 2);
      strub.restore();
    })
);

Deno.test(
  "can commit and push changes without upstream conflicts",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(),        
        successfulResult()
      ]);

      await commandPush(new Repository(tmp), {});

      assertSpyCall(strub, 0, {
        args: [{
          cmd: [
            "git",
            "add",
            "."
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 1, {
        args: [{
          cmd: [
            "git",
            "diff-index",
            "--quiet",
            "HEAD",
            "--"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 2, {
        args: [{
          cmd: [
            "git",
            "commit",
            "-a",
            "-m",
            "Unipipe CLI: Commit changes",
            "--author",
            "Uncoipipe CLI <unipipe-cli@meshcloud.io>",
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 3, {
        args: [{
          cmd: [
            "git",
            "push"
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 4);
      strub.restore();
    })
);

Deno.test(
  "can commit and push changes with upstream conflicts (fast-forward)",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(), 
        failedResult(),
        successfulResult(),
        successfulResult()
      ]);

      await commandPush(new Repository(tmp), {});

      assertSpyCall(strub, 0, {
        args: [{
          cmd: [
            "git",
            "add",
            "."
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 1, {
        args: [{
          cmd: [
            "git",
            "diff-index",
            "--quiet",
            "HEAD",
            "--"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 2, {
        args: [{
          cmd: [
            "git",
            "commit",
            "-a",
            "-m",
            "Unipipe CLI: Commit changes",
            "--author",
            "Uncoipipe CLI <unipipe-cli@meshcloud.io>",
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 3, {
        args: [{
          cmd: [
            "git",
            "push"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 4, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--ff-only"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 5, {
        args: [{
          cmd: [
            "git",
            "push"
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 6);
      strub.restore();
    })
);

Deno.test(
  "can commit and push changes with upstream conflicts (rebase)",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        successfulResult(),
        failedResult(),
        successfulResult(), 
        failedResult(),
        failedResult(),
        successfulResult(),
        successfulResult()
      ]);

      await commandPush(new Repository(tmp), {});

      assertSpyCall(strub, 0, {
        args: [{
          cmd: [
            "git",
            "add",
            "."
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 1, {
        args: [{
          cmd: [
            "git",
            "diff-index",
            "--quiet",
            "HEAD",
            "--"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 2, {
        args: [{
          cmd: [
            "git",
            "commit",
            "-a",
            "-m",
            "Unipipe CLI: Commit changes",
            "--author",
            "Uncoipipe CLI <unipipe-cli@meshcloud.io>",
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 3, {
        args: [{
          cmd: [
            "git",
            "push"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 4, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--ff-only"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 5, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--rebase"
          ],
          cwd: tmp
        }]
      });

      assertSpyCall(strub, 6, {
        args: [{
          cmd: [
            "git",
            "push"
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 7);
      strub.restore();
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