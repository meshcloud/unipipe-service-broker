// this module re-exports type definitions for handlers

import { Dir } from "./dir.ts";
export type { Dir } from "./dir.ts";

import { ServiceInstance } from "./osb.ts";
export type { ServiceInstance } from "./osb.ts";

export type { MeshMarketplaceContext } from "./mesh.ts";

export interface InstanceHandler {
  readonly name: string;
  handle(instance: ServiceInstance): Dir | null;
}
