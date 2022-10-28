import { path } from "../deps.ts";
import { assertEquals, assertSpyCall, returnsNext, Stub, stub } from "../dev_deps.ts";
import { Repository } from "../repository.ts";
import { withTempDir } from "../test-util.ts";
import { run } from "./terraform.ts";

const SERVICE_DEFINITION_ID = "777";
const SERVICE_PLAN_ID = "plan456";
const SERVICE_PLAN_NAME = "My great test plan";
const SERVICE_INSTANCE_ID = "123";
const SERVICE_BINDING_ID = "456";

const CUSTOMER_IDENTIFIER = "my-customer";
const PROJECT_IDENTIFIER = "my-project";


const TF_MODULE = {
  variable: {
    platform_secret: {
      description:
        "The secret that will be used by Terraform to authenticate against the cloud platform.",
      type: "string",
      sensitive: true,
    },
  },
  module: {
    wrapper: {
      source: "../../../../terraform/" + SERVICE_DEFINITION_ID,
      platform_secret: "${var.platform_secret}",
      plan_id: SERVICE_PLAN_ID,
      plan_name: SERVICE_PLAN_NAME,
      platform: "dev.azure",
      project_id: "my-project",
      customer_id: "my-customer",
      myParam: "test",
      tenant_id: "my-tenant",
    },
  },
};

const TF_MODULE_CONTENT = JSON.stringify(
  TF_MODULE,
  null,
  2,
);

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
  "sets instance without bindings to successful as nothing needs to be executed",
  async () =>
    await withTempDir(async (tmp) => {
      createInstance(tmp);

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}: succeeded`,
      ]]);

      const expectedStatus =
        "status: succeeded\ndescription: Instance without binding processed successfully. No action executed.\n";

      await assertContent(
        [tmp, `/instances/${SERVICE_INSTANCE_ID}/status.yml`],
        expectedStatus,
      );
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

      assertSpyCall(stub, 2, {
        args: [{
          cmd: [
            "terraform",
            "apply",
            "-auto-approve",
          ],
          cwd:
            `${tmp}/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}`,
        }],
      });

      const expectedStatus =
        "status: succeeded\ndescription: Terraform applied successfully\n";

      await verifyStatusAndTfModuleFileContent(tmp, expectedStatus);

      stub.restore();
    }),
);

Deno.test(
  "copies backend.tf to binding folder if it exists",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp);
      createTerraformServiceFolder(tmp);
      createTerraformBackendFile(tmp);

      const stub = mockTerraformExecution();

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: successful`,
      ]]);

      const backendFile = Deno.statSync(
        `${tmp}/terraform/${SERVICE_DEFINITION_ID}/backend.tf`,
      );

      await (assertEquals(backendFile.isFile, true));

      stub.restore();
    }),
);

Deno.test(
  "selects correct terraform workspace",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp);
      createTerraformServiceFolder(tmp);

      const stub = mockTerraformExecution();

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: successful`,
      ]]);

      assertSpyCall(stub, 1, {
        args: [{
          cmd: [
            "terraform",
            "workspace",
            "select",
            SERVICE_BINDING_ID,
          ],
          cwd:
            `${tmp}/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}`,
        }],
      });

      stub.restore();
    }),
);

Deno.test(
  "creates a new workspace if terraform workspace select returned an error",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp);
      createTerraformServiceFolder(tmp);

      const successfulMockResult = createTfMockResult(true);

      const failedMockResult = createTfMockResult(false);

      const tfStub: Stub = stub(
        Deno,
        "run",
        returnsNext([
          successfulMockResult,
          failedMockResult,
          successfulMockResult,
          successfulMockResult,
        ]),
      );

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: successful`,
      ]]);

      assertSpyCall(tfStub, 2, {
        args: [{
          cmd: [
            "terraform",
            "workspace",
            "new",
            SERVICE_BINDING_ID,
          ],
          cwd:
            `${tmp}/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}`,
        }],
      });

      tfStub.restore();
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

Deno.test(
  "puts instance and binding to pending if manual instance parameters needed and not there yet",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp, true);

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: pending`,
      ]]);

      const expectedStatus =
        "status: in progress\ndescription: Waiting for manual input from a platform operator!\n";

      await verifyInstanceAndBindingStatus(tmp, expectedStatus);
    }),
);

Deno.test(
  "succeeds if manual instance parameters needed and they are available",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp, true);
      createTerraformServiceFolder(tmp);
      createManualInstanceParams(tmp);

      const stub = mockTerraformExecution();

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: successful`,
      ]]);

      const expectedStatus =
        "status: succeeded\ndescription: Terraform applied successfully\n";

      const tfModuleContent = JSON.parse(TF_MODULE_CONTENT);
      tfModuleContent.module.wrapper.manualParam = "test";

      await verifyStatusAndTfModuleFileContent(
        tmp,
        expectedStatus,
        JSON.stringify(tfModuleContent, null, 2),
      );
      stub.restore();
    }),
);

Deno.test(
  "does terraform destroy for a deleted service instance",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp, false, true);
      createTerraformServiceFolder(tmp);

      const stub = mockTerraformExecution();

      const result = await run(new Repository(tmp), {});

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: deleted`,
      ]]);

      assertSpyCall(stub, 2, {
        args: [{
          cmd: [
            "terraform",
            "destroy",
            "-auto-approve",
          ],
          cwd:
            `${tmp}/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}`,
        }],
      });

      const expectedStatus =
        "status: succeeded\ndescription: Service Instance successfully deleted!\n";

      await verifyInstanceAndBindingStatus(tmp, expectedStatus);

      stub.restore();
    }),
);

Deno.test(
  "does only terraform plan if --plan paramater given",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp);
      createTerraformServiceFolder(tmp);

      const stub = mockTerraformExecution();

      const result = await run(new Repository(tmp), { plan: true });

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: planned`,
      ]]);

      assertSpyCall(stub, 2, {
        args: [{
          cmd: [
            "terraform",
            "plan",
          ],
          cwd:
            `${tmp}/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}`,
        }],
      });

      await assertEquals(
        await fileExists([
          tmp,
          `/instances/${SERVICE_INSTANCE_ID}/status.yml`,
        ]),
        false,
      );

      await assertEquals(
        await fileExists([
          tmp,
          `/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}/status.yml`,
        ]),
        false,
      );

      stub.restore();
    }),
);

Deno.test(
  "does only terraform plan if --plan paramater given and instance is destroyed",
  async () =>
    await withTempDir(async (tmp) => {
      createInstanceAndBinding(tmp, false, true);
      createTerraformServiceFolder(tmp);

      const stub = mockTerraformExecution();

      const result = await run(new Repository(tmp), { plan: true });

      await assertEquals(result, [[
        `${SERVICE_INSTANCE_ID}/${SERVICE_BINDING_ID}: planned`,
      ]]);

      assertSpyCall(stub, 2, {
        args: [{
          cmd: [
            "terraform",
            "plan",
            "-destroy"
          ],
          cwd:
            `${tmp}/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}`,
        }],
      });

      await assertEquals(
        await fileExists([
          tmp,
          `/instances/${SERVICE_INSTANCE_ID}/status.yml`,
        ]),
        false,
      );

      await assertEquals(
        await fileExists([
          tmp,
          `/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}/status.yml`,
        ]),
        false,
      );

      stub.restore();
    }),
);

function mockTerraformExecution(
  tfApplySuccess = true,
): Stub {
  const successfulMockResult = createTfMockResult(true);

  const tfApplyMockResult = createTfMockResult(tfApplySuccess);

  return stub(
    Deno,
    "run",
    returnsNext([
      successfulMockResult,
      successfulMockResult,
      tfApplyMockResult,
    ]),
  );
}

function createTfMockResult(success = true) {
  return {
    status: () => {
      return {
        success: success,
        code: 0,
        signal: undefined,
      };
    },
  };
}

function createTerraformServiceFolder(repoDir: string) {
  Deno.mkdirSync(`${repoDir}/terraform/${SERVICE_DEFINITION_ID}`, {
    recursive: true,
  });
}

function createTerraformBackendFile(repoDir: string) {
  Deno.writeTextFileSync(
    `${repoDir}/terraform/${SERVICE_DEFINITION_ID}/backend.tf`,
    "backend-config...",
  );
}

function createInstanceAndBinding(
  repoDir: string,
  manualInstanceInputNeeded = false,
  deleted = false,
) {
  createInstance(repoDir, manualInstanceInputNeeded, deleted);
  createBinding(repoDir, deleted);
}

function createInstance(
  repoDir: string,
  manualInstanceInputNeeded = false,
  deleted = false,
) {
  Deno.mkdirSync(
    `${repoDir}/instances/${SERVICE_INSTANCE_ID}`,
    { recursive: true },
  );
  Deno.writeTextFileSync(
    `${repoDir}/instances/${SERVICE_INSTANCE_ID}/instance.yml`,
    JSON.stringify(
      {
        serviceInstanceId: SERVICE_INSTANCE_ID,
        serviceDefinitionId: SERVICE_DEFINITION_ID,
        planId: SERVICE_PLAN_ID,
        serviceDefinition: {
          plans: [{
            id: "plan123",
            name: "Another Plan",
            metadata: {
              manualInstanceInputNeeded: true,
            },
          }, {
            id: SERVICE_PLAN_ID,
            name: SERVICE_PLAN_NAME,
            metadata: {
              manualInstanceInputNeeded: manualInstanceInputNeeded,
            },
          }],
        },
        parameters: {
          myParam: "test",
        },
        context: {
          platform: "dev.azure",
          project_id: PROJECT_IDENTIFIER,
          customer_id: CUSTOMER_IDENTIFIER,
          auth_url: "should-be-ignored",
        },
        deleted: deleted,
      },
      null,
      2,
    ),
  );
}

function createManualInstanceParams(repoDir: string) {
  Deno.writeTextFileSync(
    `${repoDir}/instances/${SERVICE_INSTANCE_ID}/params.yml`,
    JSON.stringify(
      {
        manualParam: "test",
      },
      null,
      2,
    ),
  );
}

function createBinding(repoDir: string, deleted = false) {
  Deno.mkdirSync(
    `${repoDir}/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}`,
    { recursive: true },
  );
  Deno.writeTextFileSync(
    repoDir +
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
        deleted: deleted,
      },
      null,
      2,
    ),
  );
}

async function verifyStatusAndTfModuleFileContent(
  repoDir: string,
  expectedStatus: string,
  expectedTfModuleContent = TF_MODULE_CONTENT,
) {
  await verifyInstanceAndBindingStatus(repoDir, expectedStatus);

  await assertContent(
    [
      repoDir,
      `/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}/module.tf.json`,
    ],
    expectedTfModuleContent,
  );
}

async function verifyInstanceAndBindingStatus(
  repoDir: string,
  expectedStatus: string,
) {
  await assertContent(
    [repoDir, `/instances/${SERVICE_INSTANCE_ID}/status.yml`],
    expectedStatus,
  );

  await assertContent(
    [
      repoDir,
      `/instances/${SERVICE_INSTANCE_ID}/bindings/${SERVICE_BINDING_ID}/status.yml`,
    ],
    expectedStatus,
  );
}

async function assertContent(pathComponents: string[], expected: string) {
  const result = await Deno.readTextFile(path.join(...pathComponents));

  assertEquals(result, expected);
}

async function fileExists(pathComponents: string[]): Promise<boolean> {
  try {
    await Deno.statSync(path.join(...pathComponents));
    return true;
  } catch (_) {
    return false;
  }
}
