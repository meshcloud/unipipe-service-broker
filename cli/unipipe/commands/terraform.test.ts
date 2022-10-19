import { path } from "../deps.ts";
import { assertEquals, returnsNext, Stub, stub } from "../dev_deps.ts";
import { Repository } from "../repository.ts";
import { withTempDir } from "../test-util.ts";
import { run } from "./terraform.ts";

const SERVICE_DEFINITION_ID = "777";
const SERVICE_INSTANCE_ID = "123";
const SERVICE_BINDING_ID = "456";

const TF_MODULE_CONTENT = JSON.stringify(
  {
    variable: {
      platform_secret: {
        description: "The secret that will be used by Terraform to authenticate against the cloud platform.",
        type: "string",
        sensitive: true,
      },
    },
    module: {
      wrapper: {
        source: "../../../../terraform/" + SERVICE_DEFINITION_ID,
        platform_secret: "${var.platform_secret}",
        platform: "dev.azure",
        project_id: "my-project",
        customer_id: "my-customer",
        myParam: "test",
        tenant_id: "my-tenant",
      },
    },
  },
  null,
  2,
);

async function assertContent(pathComponents: string[], expected: string) {
  const result = await Deno.readTextFile(path.join(...pathComponents));

  assertEquals(result, expected);
}

Deno.test(
  "does nothing for empty repository",
  async () =>
    await withTempDir(async (tmp) => {
      const result = await run(new Repository(tmp), {});

      await assertEquals(result, []);
    }),
);

Deno.test(
  "skips instances with missing terraform folder for service definition",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp);

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: skipped`,
      ]]);
    }),
);

Deno.test(
  "can handle successful terraform execution",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp);
      createTerraformServiceFolder(tmp);

      const stub = mockTerraformExecution();

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: successful`,
      ]]);

      const expectedStatus =
        "status: succeeded\ndescription: Terraform applied successfully\n";

      await verifyStatusAndTfModuleFileContent(tmp, expectedStatus);

      stub.restore();
    }),
);

Deno.test(
  "can handle failed terraform execution",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp);
      createTerraformServiceFolder(tmp);

      const stub = mockTerraformExecution(false);

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: failed`,
      ]]);

      const expectedStatus =
        "status: failed\ndescription: Applying Terraform failed!\n";

      await verifyStatusAndTfModuleFileContent(tmp, expectedStatus);
      stub.restore();
    }),
);

Deno.test(
  "can handle error during processing",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp);
      createTerraformServiceFolder(tmp);

      const runStub = stub(
        Deno,
        "run",
        returnsNext([new Error("failed!")]),
      );

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: failed`,
      ]]);

      const expectedStatus =
        "status: failed\ndescription: Processing the binding failed!\n";

      await verifyStatusAndTfModuleFileContent(tmp, expectedStatus);
      runStub.restore();
    }),
);

function mockTerraformExecution(success = true): Stub {
  const mockResult = {
    status: () => {
      return {
        success: success,
        code: 0,
        signal: undefined,
      };
    },
  };

  return stub(
    Deno,
    "run",
    returnsNext([mockResult, mockResult]),
  );
}

function createTerraformServiceFolder(tmp: string) {
  Deno.mkdirSync(tmp + "/terraform/" + SERVICE_DEFINITION_ID, {
    recursive: true,
  });
}

function createInstanceAndBinding(tmp: string) {
  Deno.mkdirSync(
    tmp + `/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}`,
    { recursive: true },
  );
  Deno.writeTextFileSync(
    tmp + `/instances/${SERVICE_INSTANCE_ID}/instance.yml`,
    JSON.stringify(
      {
        serviceInstanceId: SERVICE_INSTANCE_ID,
        serviceDefinitionId: SERVICE_DEFINITION_ID,
        parameters: {
          myParam: "test",
        },
        context: {
          platform: "dev.azure",
          project_id: "my-project",
          customer_id: "my-customer",
          auth_url: "should-be-ignored"
        }
      },
      null,
      2,
    ),
  );
  Deno.writeTextFileSync(
    tmp +
      `/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}/binding.yml`,
    JSON.stringify(
      {
        bindingId: SERVICE_BINDING_ID,
        serviceInstanceId: SERVICE_INSTANCE_ID,
        serviceDefinitionId: SERVICE_DEFINITION_ID,
        parameters: {},
        bindResource: {
          tenant_id: "my-tenant",
          platform: "dev.azure",
        },
      },
      null,
      2,
    ),
  );
}

async function verifyStatusAndTfModuleFileContent(
  tmp: string,
  expectedStatus: string,
) {
  await assertContent(
    [tmp, `/instances/${SERVICE_INSTANCE_ID}/status.yml`],
    expectedStatus,
  );
  await assertContent(
    [
      tmp,
      `/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}/status.yml`,
    ],
    expectedStatus,
  );

  await assertContent(
    [
      tmp,
      `/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}/module.tf.json`,
    ],
    TF_MODULE_CONTENT,
  );
}
