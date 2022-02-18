import { path } from "./deps.ts";
import {
  OsbServiceBindingStatus,
  OsbServiceInstanceStatus,
  readInstance,
} from "./osb.ts";
import { ServiceInstance } from "./osb.ts";
import { stringify } from "./yaml.ts";

export class Repository {
  constructor(public readonly path: string) {}

  /**
   * Reads an OSB service instance from the git repo structure
   * @returns
   */
  async loadInstance(instanceId: string): Promise<ServiceInstance> {
    const instancePath = path.join(this.path, "instances", instanceId);
    return await readInstance(instancePath);
  }

  /**
   * A helper function to implement commands that need to perform a map operation on instances.
   *
   * NOTE: includes error handling, and will exit the process with an appropriate error message if an error occurs (e.g. directory not found, failed to process etc.)
   * @param osbRepoPath path to the osb repository
   * @param mapFn
   * @returns
   */
  async mapInstances<T>(
    mapFn: (serviceInstance: ServiceInstance) => Promise<T>,
    filterFn: (serviceInstance: ServiceInstance) => boolean = (
      _: ServiceInstance
    ) => {
      return true;
    }
  ): Promise<T[]> {
    const instancesPath = path.join(this.path, "instances");
    const results: T[] = [];

    // todo: should probably not Deno.exit from here, but throw a nice exception and handle that globally
    try {
      for await (const dir of Deno.readDir(instancesPath)) {
        if (!dir.isDirectory) {
          continue;
        }

        const ip = path.join(instancesPath, dir.name);

        try {
          const instance = await readInstance(ip);

          const instancePassesFilter = filterFn(instance);
          if (!instancePassesFilter) {
            continue;
          }

          try {
            const r = await mapFn(instance);
            results.push(r);
          } catch (error) {
            console.error(
              `Failed to process service instance "${ip}".\n`,
              error
            );
            Deno.exit(1);
          }
        } catch (error) {
          console.error(
            `Failed to apply filter to service instance "${ip}".\n`,
            error
          );
          Deno.exit(1);
        }
      }
    } catch (error) {
      console.error(
        `Failed to read instances directory "${instancesPath}".\n`,
        error
      );
      Deno.exit(1);
    }

    return results;
  }

  async updateInstanceStatus(
    instanceId: string,
    status: OsbServiceInstanceStatus
  ) {
    const statusYmlPath = path.join(
      this.path,
      "instances",
      instanceId,
      "status.yml"
    );

    const yaml = stringify(status);
    await Deno.writeTextFile(statusYmlPath, yaml);
  }

  async updateBindingStatus(
    instanceId: string,
    bindingId: string,
    status: OsbServiceBindingStatus
  ) {
    const statusYmlPath = path.join(
      this.path,
      "instances",
      instanceId,
      "bindings",
      bindingId,
      "status.yml"
    );

    const yaml = stringify(status);
    await Deno.writeTextFile(statusYmlPath, yaml);
  }

  async updateBindingCredentials(
    instanceId: string,
    bindingId: string,
    credentials: string
  ) {
    const credentialsYmlPath = path.join(
      this.path,
      "instances",
      instanceId,
      "bindings",
      bindingId,
      "credentials.yml"
    );

    await Deno.writeTextFile(credentialsYmlPath, credentials);
  }
}
