class VpcHandler {
  name = "VPC Handler";

  handle(service) {
    const params = service.instance.parameters;

    const context = service.instance.context;

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

const handlers = {
  "d90c2b20-1d24-4592-88e7-6ab5eb147925": new VpcHandler(),
};

handlers;