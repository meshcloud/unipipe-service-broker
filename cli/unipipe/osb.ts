import { mapBindings } from "./commands/helpers.ts";
import { MeshMarketplaceContext } from "./mesh.ts";
import { parse } from "./yaml.ts";

export interface CloudFoundryContext {
  // cloudfoundry context object, https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#cloud-foundry-context-object
  platform: "cloudfoundry";
  organization_name: string;
  space_name: string;
}

export interface OsbServiceInstance extends Record<string, unknown> {
  serviceInstanceId: string;
  serviceDefinitionId: string;
  planId: string;

  // todo: technically we know a bit better
  originatingIdentity: Record<string, unknown>;
  parameters: Record<string, unknown>;
  context: MeshMarketplaceContext | CloudFoundryContext;

  serviceDefinition: {
    id: string;
    name: string;
    plans: OsbServicePlan[];
  };

  deleted?: boolean;
  // todo: more fields
}

export interface OsbServicePlan {
  id: string;
  name: string;
  metadata: {
    manualInstanceInputNeeded?: boolean;
  };
}

export interface OsbServiceBinding extends Record<string, unknown> {
  bindingId: string;
  serviceInstanceId: string;
  serviceDefinitionId: string;
  planId: string;

  // todo: technically we know a bit better
  originatingIdentity: Record<string, unknown>;
  parameters: Record<string, unknown>;
  bindResource: Record<string, unknown>;
  context: Record<string, unknown>;

  deleted: boolean;
}

export type OsbStatusValue = "succeeded" | "in progress" | "failed";

export interface OsbServiceInstanceStatus extends Record<string, unknown> {
  status: OsbStatusValue;
  description: string;
}
export interface OsbServiceBindingStatus extends Record<string, unknown> {
  status: OsbStatusValue;
  description: string;
}

export interface ServiceInstance extends Record<string, unknown> {
  instance: OsbServiceInstance; // contents of instance.yml
  bindings: ServiceBinding[]; // contens of all bindings/$binding-id/binding.yml
  status: OsbServiceInstanceStatus | null; // contents of status.yml, null if not available
  servicePlan: OsbServicePlan;
  manualParameters: ManualServiceInstanceParameters | null;
}

export interface ServiceBinding extends Record<string, unknown> {
  binding: OsbServiceBinding; // content of bindings/$binding-id/binding.yml
  status: OsbServiceBindingStatus | null; // contents of status.yml, null if not available
  credentials: Record<string, unknown> | null; // contents of credentials.yml
}

// In this case an empty interface makes sense as the name of it provides semantic meaning.
// Using Record<string, unknown> directly instead would be too generic and therefore harder to understand.
// deno-lint-ignore no-empty-interface
export interface ManualServiceInstanceParameters
  extends Record<string, unknown> {
}

/**
 * Parse a yaml file, throwing an error if it fails.
 * @param path
 * @returns
 */
export async function parseYamlFile(path: string) {
  const yml = await Deno.readTextFile(path);

  return parse(yml);
}
/**
 * Attempts to parse a yaml file, returning null if the file does not exist or the YAML fails to parse.
 * @param path
 * @returns
 */
export async function tryParseYamlFile(path: string) {
  try {
    const yml = await Deno.readTextFile(path);
    return parse(yml);
  } catch (_error) {
    return null;
  }
}

/**
 * Reads an OSB service instance from the git repo structure
 * @param path
 * @returns
 */
export async function readInstance(path: string): Promise<ServiceInstance> {
  const instance = (await parseYamlFile(
    `${path}/instance.yml`,
  )) as OsbServiceInstance;

  const status = (await tryParseYamlFile(
    `${path}/status.yml`,
  )) as OsbServiceInstanceStatus | null;

  const manualParams = (await tryParseYamlFile(
    `${path}/params.yml`,
  )) as ManualServiceInstanceParameters | null;

  const bindings = await mapBindings(path, async (binding) => await binding);

  const plan = instance.serviceDefinition.plans.find(
    (x) => x.id === instance.planId,
  );

  if (!plan) {
    throw Error(
      `Service Plan with id ${instance.planId} for instance ${instance.serviceInstanceId} not found in attached service definition!`,
    );
  }

  return {
    instance: instance,
    bindings: bindings,
    status: status,
    servicePlan: plan,
    manualParameters: manualParams,
  };
}

/**
 * Reads an OSB service binding from the git repo structure
 * @param path
 * @returns
 */
export async function readBinding(path: string): Promise<ServiceBinding> {
  const binding = (await parseYamlFile(
    `${path}/binding.yml`,
  )) as OsbServiceBinding;

  const status = (await tryParseYamlFile(
    `${path}/status.yml`,
  )) as OsbServiceInstanceStatus | null;

  const credentials = (await tryParseYamlFile(
    `${path}/credentials.yml`,
  )) as Record<string, unknown> | null;

  return {
    binding: binding,
    status: status,
    credentials: credentials,
  };
}
