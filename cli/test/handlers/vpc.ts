import {
  Dir,
  InstanceHandler,
  MeshMarketplaceContext,
  ServiceInstance,
} from "../../unipipe/handler.ts";

interface VpcServiceParameters {
  cidr: string;
  region: string;
}

export class VpcHandler implements InstanceHandler {
  readonly name = "VPC Handler";

  handle(service: ServiceInstance): Dir | null {
    const params: VpcServiceParameters = service.instance
      .parameters as unknown as VpcServiceParameters;

    const context: MeshMarketplaceContext = service.instance
      .context as MeshMarketplaceContext;

    return {
      name: "customers",
      entries: [{
        name: context.customer_id,
        entries: [
          {
            name: context.project_id,
            entries: [
              { name: "params.json", content: JSON.stringify(params, null, 2) },
            ],
          },
        ],
      }],
    };
  }
}
