import { uuid } from '../deps.ts';

export const basicTransformHandler = `
/**
 Here's a full example for a handlers.js file that you can pass to unipipe transform --registry-of-handlers and that writes service instance parameters into a custom directory structure:

  /
  \`-- customers
      \`-- unipipe-osb-dev
          \`-- osb-dev
              \`-- params.json

  For more information about handlers please review the official unipipe-cli documentation
  at https://github.com/meshcloud/unipipe-cli
*/

class MyHandler {
  name = "My Handler";

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
  // Check your service id and replace the following key. You can find the service ids inside of your catalog.yml file.
  "$SERVICEID": new MyHandler(),
};

handlers;
`