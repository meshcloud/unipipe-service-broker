import { InstanceHandler } from "../../unipipe/handler.ts";
import { VpcHandler } from "./vpc.ts";

const handlers: Record<string, InstanceHandler> = {
  "d90c2b20-1d24-4592-88e7-6ab5eb147925": new VpcHandler(),
};

export default handlers;