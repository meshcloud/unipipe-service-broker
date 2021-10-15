import { path } from './deps.ts';
import { assertEquals } from './dev_deps.ts';
import { Dir, write } from './dir.ts';
import { withRestoreCwd, withTempDir } from './test-util.ts';

const dir: Dir = {
  name: "x",
  entries: [
    {
      name: "y",
      entries: [
        { name: "z", content: "1" },
      ],
    },
  ],
};

async function assertContent(pathComponents: string[], expected: string) {
  const result = await Deno.readTextFile(path.join(...pathComponents));

  assertEquals(result, expected);
}

Deno.test(
  "can write new trees",
  async () =>
    await withTempDir(async (tmp) => {
      await write(dir, tmp);

      await assertContent([tmp, "x", "y", "z"], "1");
    }),
);

Deno.test(
  "can merge a tree into existing dir",
  async () =>
    await withTempDir(async (tmp) => {
      await write(dir, tmp);

      const merged: Dir = {
        name: "x",
        entries: [
          {
            name: "y",
            entries: [
              { name: "z", content: "-1" }, // update file content
              { name: "zz", content: "2" }, // a new file in an existing dir
            ],
          },
          {
            name: "yy",
            entries: [
              { name: "zzz", content: "3" }, // a new dir and a new file
            ],
          },
        ],
      };

      await write(merged, tmp);

      await assertContent([tmp, "x", "y", "z"], "-1");
      await assertContent([tmp, "x", "y", "zz"], "2");
      await assertContent([tmp, "x", "yy", "zzz"], "3");
    }),
);

Deno.test(
  "can write into relative dir",
  async () =>
    await withRestoreCwd(async () => {
      await withTempDir(async (tmp) => {
        Deno.chdir(tmp);

        await write(dir, "././"); // repeatedly referring to the same dir works as well
        
        // assertContent uses an absolute dir
        await assertContent([tmp, "x", "y", "z"], "1");
      });
    }),
);
