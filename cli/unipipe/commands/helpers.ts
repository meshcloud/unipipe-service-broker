import { path } from "../deps.ts";
import { readBinding, ServiceBinding } from "../osb.ts";

/**
 * A helper function to implement commands that need to perform a map operation on bindings.
 * If no bindings directory is found, an empty result is returned.
 *
 * NOTE: includes error handling, and will exit the process with an appropriate error message if an error occurs (e.g. failed to process etc.)
 * @param instancePath path to the osb repository
 * @param mapFn
 * @returns
 */
export async function mapBindings<T>(
  instancePath: string,
  mapFn: (serviceBinding: ServiceBinding) => Promise<T>,
): Promise<T[]> {
  const bindingsPath = path.join(instancePath, "bindings");
  const results: T[] = [];

  try {
    for await (const dir of Deno.readDir(bindingsPath)) {
      if (!dir.isDirectory) {
        continue;
      }

      const bp = path.join(bindingsPath, dir.name);

      try {
        const binding = await readBinding(bp);

        const r = await mapFn(binding);

        results.push(r);
      } catch (error) {
        console.error(`Failed to process service binding "${bp}".\n`, error);
        Deno.exit(1);
      }
    }
  } catch (_error) {
    // Do not error when bindings directory does not exist.
  }

  return results;
}
