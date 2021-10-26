import { MeshMarketplaceContext } from './mesh.ts';
import { parse } from './yaml.ts';
import { mapBindings, mapInstances } from './commands/helpers.ts';

export interface CloudFoundryContext {
  // cloudfoundry context object, https://github.com/openservicebrokerapi/servicebroker/blob/master/profile.md#cloud-foundry-context-object  
  platform: "cloudfoundry";
  organization_name: string;
  space_name: string;
}

export interface OsbServiceInstance {
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
    plans: {
      id: string;
      name: string;
    }[];
  };

  metadata: {
    name: string;
  };
  
  deleted?: boolean;
  // todo: more fields
}

export interface OsbServiceBinding {
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

export interface OsbServiceInstanceStatus {
  status: "succeeded" | "failed";
  description: string;
}
export interface OsbServiceBindingStatus {
  status: "succeeded" | "failed";
  description: string;
}

export interface ServiceInstance {
  instance: OsbServiceInstance; // contents of instance.yml
  bindings: ServiceBinding[]; // contens of all bindings/$binding-id/binding.yml
  status: OsbServiceInstanceStatus | null; // contents of status.yml, null if not available
}

export interface ServiceBinding {
  binding: OsbServiceBinding; // content of bindings/$binding-id/binding.yml
  status: OsbServiceBindingStatus | null; // contents of status.yml, null if not available
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
  } catch (error) {
    return null;
  }
}

/**
 * Reads an OSB service instance from the git repo structure
 * @param path 
 * @returns 
 */
export async function readInstance(path: string): Promise<ServiceInstance> {
  const instance = await parseYamlFile(
    `${path}/instance.yml`,
  ) as OsbServiceInstance;

  const status = await tryParseYamlFile(`${path}/status.yml`) as
    | OsbServiceInstanceStatus
    | null;

  const bindings = await mapBindings(
    path,
    async (binding) => await binding,
  );

  return {
    instance: instance,
    bindings: bindings,
    status: status,
  };
}

/**
 * Reads an OSB service binding from the git repo structure
 * @param path 
 * @returns 
 */
 export async function readBinding(path: string): Promise<ServiceBinding> {
  const binding = await parseYamlFile(
    `${path}/binding.yml`,
  ) as OsbServiceBinding;

  const status = await tryParseYamlFile(`${path}/status.yml`) as
    | OsbServiceInstanceStatus
    | null;

    return {
      binding: binding,
      status: status,
    };
}
