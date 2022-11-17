import { assertSpyCall, assertSpyCalls, Stub, stub, returnsNext } from "../dev_deps.ts";
import { withTempDir } from "../test-util.ts";
import { Repository } from "../repository.ts";
import { commandPull } from "./git.ts";

Deno.test(
  "can pull with fast forward",
  async () =>
    await withTempDir(async (tmp) => {

      const strub = createMockedGit([
        createMockedResult()
      ]);

      await commandPull(new Repository(tmp));

      assertSpyCall(strub, 0, {
        args: [{
          cmd: [
            "git",
            "pull",
            "--ff-only"
          ],
          cwd: tmp
        }]
      });

      assertSpyCalls(strub, 1);      
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

function createMockedResult(success = true) {
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

