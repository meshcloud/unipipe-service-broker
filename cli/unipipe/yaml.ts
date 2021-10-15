import { yaml, YamlSchema, YamlType } from "./deps.ts";

const PlatformContextType = new YamlType("PlatformContext", {
  kind: "mapping",
});

const OSB_SCHEMA = new YamlSchema({
  explicit: [PlatformContextType],
  include: [yaml.DEFAULT_SCHEMA],
});

export function parse(content: string, options?: yaml.ParseOptions): unknown {
  return yaml.parse(content, options || { schema: OSB_SCHEMA });
}

export function stringify(
  obj: Record<string, unknown>,
  options?: yaml.StringifyOptions,
): string {
  return yaml.stringify(obj, options);
}
