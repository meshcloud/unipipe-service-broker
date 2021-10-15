import { catalog } from '../blueprints/catalog.yml.js';
import { basicTransformHandler } from '../blueprints/basic-handler.js.js';
import { terraformTransformHandler } from '../blueprints/terraform-handler.js.js';
import { githubWorkflow } from '../blueprints/github-workflow.yml.js';
import { executionScript } from '../blueprints/execution-script.sh.js';
import { unipipeOsbAciTerraform } from '../blueprints/unipipe-osb-aci.tf.js';
import { unipipeOsbGCloudCloudRunTerraform } from '../blueprints/unipipe-osb-gcloud-cloudrun.js';
import { colors, Command, Input, Select, uuid, EnumType } from '../deps.ts';
import { Dir, File, write } from '../dir.ts';

const ALL_HANDLER_TYPES = [ "handler_b", "handler_tf" ] as const;
type HandlersTuple = typeof ALL_HANDLER_TYPES;
type Handler = HandlersTuple[number];

const handlersType = new EnumType(ALL_HANDLER_TYPES);

const ALL_DEPLOYMENT_TYPES = [ "aci_tf", "aci_az", "gcp_cloudrun_tf" ] as const;
type DeploymentsTuple = typeof ALL_DEPLOYMENT_TYPES;
type Deployment = DeploymentsTuple[number];

const deploymentsType = new EnumType(ALL_DEPLOYMENT_TYPES);

export interface CatalogOpts {
  destination?: string;
  handler?: Handler;
  deployment?: Deployment;
  uuid?: string
}

export function registerGenerateCmd(program: Command) {
  // the actual blueprint commands
  const blueprints = new Command()
    //
    .command("catalog")
    .description("Generate a sample OSB catalog for use by unipipe-broker.")
    .option("--destination <destination>", "Pick a destination directory for the generated catalog file.")
    .action(async (options: CatalogOpts) => await generateCatalog(options))
    //
    .command("uuid")
    .description(
      "Generate a UUID. This is useful for generating unique ids for OSB service definitions, plans etc.",
    )
    .action(() => generateUuid())
    //
    .command("transform-handler")
    .type("handler", handlersType)
    .description(
      "Generate a javascript transform-handler for `unipipe transform`.",
    )
    .option("--destination <destination>", "Pick a destination directory for the generated transform-handler file.")
    .option("--handler <handler_b|handler_tf>", "Pick a handler type for the generated transform-handler file.")
    .option("-u, --uuid <uuid>", "Pick an UUID for the generated transform-handler file.")
    .action(async (options: CatalogOpts) => await generateTransformHandler(options))
    //
    .command("github-workflow")
    .description(
      "Generate a github workflow for `Github Actions`.",
    )
    .option("--destination <destination>", "Pick a destination directory for the generated github-workflow file.")
    .action(async (options: CatalogOpts) => await generateGithubWorkflow(options))
    //
    .command("execution-script")
    .description(
      "Generate an execution shell script to apply your terraform templates.",
    )
    .option("--destination <destination>", "Pick a destination directory for the generated execution-script file.")
    .action(async (options: CatalogOpts) => await generateExecutionScript(options))

    .command("unipipe-service-broker-deployment")
    .type("deployment", deploymentsType)
    .description(
      "Generate infrastructure-as-code deployments for the UniPipe Service broker.",
    )
    .option("--destination <destination>", "Pick a destination directory for the generated unipipe-service-broker-deployment file.")
    .option("--deployment <aci_tf|aci_az|gcp_cloudrun_tf>", "Pick a deployment type for the generated unipipe-service-broker-deployment file.")
    .action(async (options: CatalogOpts) => await generateUniPipeDeployment(options));

  program
    .command("generate", blueprints)
    .description(
      "Generate useful artifacts for working with UniPipe OSB such as catalogs, transform handlers, CI pipelines and more.",
    );
}

function generateUuid() {
  console.log(uuid.generate());
}

async function generateCatalog(options: CatalogOpts) {
  console.log(catalog);
  writeDirectoryWithUserInput("catalog.yml", catalog, "Pick a destination directory for the generated catalog file:", (options.destination?options.destination:undefined))
}

async function generateExecutionScript(options: CatalogOpts) {
  console.log(executionScript);
  writeDirectoryWithUserInput("execute-terraform-templates.sh", executionScript, "Pick a destination directory for the generated execution script file:", (options.destination?options.destination:undefined))
}

async function generateGithubWorkflow(options: CatalogOpts) {
  console.log(githubWorkflow);
  writeDirectoryWithUserInput("github-workflow.yml", githubWorkflow, "Pick a destination directory for the generated github-workflow file:", (options.destination?options.destination:undefined))
}

async function generateTransformHandler(options: CatalogOpts) {
  if (options.handler == undefined){
      options.handler = await Select.prompt({
        message: "Pick the handler type:",
        options: [
          { name: "Basic Handler | handler_b", value: "handler_b" },
          { name: "Terraform Handler | handler_tf", value: "handler_tf" },
        ],
      }) as Handler;
  }

  if (options.uuid == undefined){
      options.uuid = await Input.prompt({
        message: "Write the Service Id that will be controlled by the handler. Press enter to continue.",
        default: "Default: Auto-generated UUID",
      });
  }

  var outputContent = ""
  switch (options.handler) {
    case "handler_b": {
      if (options.uuid == "Default: Auto-generated UUID"){
        outputContent = basicTransformHandler.replaceAll('$SERVICEID', `${uuid.generate()}` )
      }
      else {
        outputContent = basicTransformHandler.replaceAll('$SERVICEID', options.uuid )
      }
      console.log(outputContent);
      writeDirectoryWithUserInput("handler.js", outputContent, "Pick a destination directory for the generated transform file:", (options.destination?options.destination:undefined))
      break;
    }
    case "handler_tf": {
      if (options.uuid == "Default: Auto-generated UUID"){
        outputContent = terraformTransformHandler.replaceAll('$SERVICEID', `${uuid.generate()}` )
      }
      else {
        outputContent = terraformTransformHandler.replaceAll('$SERVICEID', options.uuid )
      }
      console.log(outputContent);
      writeDirectoryWithUserInput("handler.js", outputContent, "Pick a destination directory for the generated transform file:", (options.destination?options.destination:undefined))
      break;
    }
    default:
      throw new Error(`Received unexpected target ${options.handler} from input.`);
  }
}

async function generateUniPipeDeployment(options: CatalogOpts) {
  if (options.deployment == undefined){
    options.deployment = await Select.prompt({
      message: "Pick the target deployment environment:",
      options: [
        { name: "Azure ACI (terraform) | aci_tf", value: "aci_tf" },
        { name: "Azure ACI (azure-cli) | aci_az", value: "aci_az" },
        { name: "GCP CloudRun (terraform) | gcp_cloudrun_tf", value: "gcp_cloudrun_tf" },
      ],
    }) as Deployment;
  }

  switch (options.deployment) {
    case "aci_az":
      console.log(
        "Please open instructions at: https://github.com/meshcloud/unipipe-service-broker/wiki/2.-Deploy-a-Azure-Container-Group-for-Universal-Pipeline-Service-Broker---Caddy-(SSL)",
      );
      break;
    case "aci_tf": {
      console.log(unipipeOsbAciTerraform);
      writeDirectoryWithUserInput("unipipe-service-broker-deployment-azure.tf", unipipeOsbAciTerraform, "Pick a destination directory for the generated terraform file:", (options.destination?options.destination:undefined))
      writeTerraformInstructions(unipipeOsbAciTerraform);
      break;
    }
    case "gcp_cloudrun_tf": {
      console.log(unipipeOsbGCloudCloudRunTerraform);
      writeDirectoryWithUserInput("unipipe-service-broker-deployment-gcp.tf", unipipeOsbGCloudCloudRunTerraform, "Pick a destination directory for the generated terraform file:", (options.destination?options.destination:undefined))
      writeTerraformInstructions(unipipeOsbGCloudCloudRunTerraform);
      break;
    }
    default:
      throw new Error(`Received unexpected target ${options.deployment} from input.`);
  }
}

async function writeDirectoryWithUserInput(fileName: string, content: string, message: string, destination: string|undefined=undefined) {
  if (destination == undefined){
    destination = await Input.prompt({
      message: message,
      default: './',
    });
  }

  const dir: Dir = {
    name: destination,
    entries: [
      { name: fileName, content: content },
    ],
  };
  writeDirectory(dir);
}

async function writeDirectory(dir: Dir){
  await write(
    dir,
    "./",
    (file) => console.log(colors.green(`writing ${file}`)),
  );
}

function writeTerraformInstructions(terraform: string, initialText: string='Instructions:') {
  // KISS, just find where the actual terraform code starts and log the file header above it
  const instructions = terraform.substring(
    0,
    terraform.indexOf("terraform {"),
  );
  console.log(initialText);
  console.log(instructions);
}
