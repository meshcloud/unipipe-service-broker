
import { parseYamlFile, tryParseYamlFile } from "./osb.ts";
import { assertEquals } from "./dev_deps.ts";

Deno.test("parseYamlFile works", async () => {
  const tmp = await Deno.makeTempFile();
  await Deno.writeTextFile(tmp, "x: 1");

  await parseYamlFile(tmp);
});

Deno.test("tryParseYamlFile returns null when file does not exist", async () => {
  const path = "./idontexist";
  const result = await tryParseYamlFile(path);

  assertEquals(result, null);
});
