import { parse } from "./yaml.ts";

Deno.test("can parse !<PlatformContext> tag", () => {
  const yaml = `
  originatingIdentity: !<PlatformContext>
  platform: "meshmarketplace"
  user_id: "502c9d14-db19-4e20-b71a-e3a0717bc0db"
  user_euid: "partner@meshcloud.io"
  `;

  parse(yaml);
});
