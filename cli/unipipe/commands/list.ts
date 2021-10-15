import { Command, EnumType, Table } from '../deps.ts';
import { MeshMarketplaceContext } from '../mesh.ts';
import { CloudFoundryContext, OsbServiceInstance, ServiceInstance } from '../osb.ts';
import { mapInstances } from './helpers.ts';

// see https://stackoverflow.com/questions/44480644/string-union-to-string-array for the trick used here
const ALL_FORMATS = ["text", "json"] as const;
type FormatsTuple = typeof ALL_FORMATS;
type Format = FormatsTuple[number];

const formatsType = new EnumType(ALL_FORMATS);

const ALL_PROFILES = ["meshmarketplace", "cloudfoundry"] as const;
type ProfilesTuple = typeof ALL_PROFILES;
type Profile = ProfilesTuple[number];

const profilesType = new EnumType(ALL_PROFILES);

interface ListOpts {
  profile?: Profile;
  outputFormat: Format;
  status?: Status;
  deleted?: boolean;
}

// TODO unify with statuses listed in ./update.ts
const ALL_STATUSES = ["succeeded", "failed", "in progress", "EMPTY"] as const;
type StatusesTuple = typeof ALL_STATUSES;
type Status = StatusesTuple[number];

const statusesType = new EnumType(ALL_STATUSES);

export function registerListCmd(program: Command) {
  program
    .command("list <repo>")
    .type("format", formatsType)
    .type("profile", profilesType)
    .type("status", statusesType)
    .option(
      "-p, --profile [profile:profile]",
      "include columns of context information according to the specified OSB API profile. Supported values are 'meshmarketplace' and 'cloudfoundry'. Ignored when '-o json' is set.",
    )
    .option(
      "-o, --output-format [format:format]",
      "Output format. Supported formats are json and text.",
      {
        default: "text",
      },
    )
    .option(
      "--status [status:status]",
      "Filters instances by status. Allowed values are 'in progress', 'succeeded', 'failed' and 'EMPTY' (no status file present for this instance)."
    )
    .option(
      "--deleted [deleted:boolean]",
      "Filters instances by deleted. Allowed values are 'true' and 'false'"
    )
    .description(
      "Lists service instances status stored in a UniPipe OSB git repo.",
    )
    .action(async (options: ListOpts, repo: string) => {
      await list(repo, options);
    });
}

export async function list(osbRepoPath: string, opts: ListOpts) {

  const filterFn = buildFilterFn(opts)

  switch (opts.outputFormat) {
    case "json":
      await listJson(osbRepoPath, filterFn);
      break;
    case "text":
      await listTable(osbRepoPath, filterFn, opts.profile);
      break;
  }
}

async function listJson(osbRepoPath: string, filterFn : (instance: ServiceInstance) => boolean) {
  const results = await mapInstances(
    osbRepoPath,
    async (instance) => await instance,
    filterFn
  );
  console.log(JSON.stringify(results));
}

async function listTable(osbRepoPath: string, filterFn : (instance: ServiceInstance) => boolean, profile?: Profile) {
  const results = await mapInstances(osbRepoPath, async (instance) => {
    const i = instance.instance;

    const plan = i.serviceDefinition.plans.filter((x) => x.id === i.planId)[0];

    const pcols: string[] = profileColValues(i, profile);

    return await [
      i.serviceInstanceId,
      ...pcols,
      plan?.name || "",
      i.serviceDefinition.name,
      instance.status?.status || "",
      i.deleted === undefined ? "" : i.deleted.toString(),
    ];
  }, filterFn);

  const pcols = profileColHeaders(profile);
  const header = [
    "id",
    ...pcols,
    "service",
    "plan",
    "status",
    "deleted",
  ];

  new Table()
    .header(header)
    .body(results)
    .render();
}

function profileColHeaders(profile?: Profile): string[] {
  switch (profile) {
    case undefined:
      return [];
    case "meshmarketplace":
      return ["customer", "project"];
    case "cloudfoundry":
      return ["organization", "space"];
  }
}

function profileColValues(
  i: OsbServiceInstance,
  profile?: Profile,
): string[] {
  switch (profile) {
    case undefined:
      return [];
    case "meshmarketplace": {
      const ctx = i.context as MeshMarketplaceContext;
      return [
        ctx.customer_id,
        ctx.project_id,
      ];
    }
    case "cloudfoundry": {
      const ctx = i.context as CloudFoundryContext;
      return [
        ctx.organization_name,
        ctx.space_name,
      ];
    }
  }
}

function buildFilterFn(opts: ListOpts ): (instance: ServiceInstance) => boolean {
  return ( (instance: ServiceInstance) => {

    const statusFilterMatches = !opts.status ||
      opts.status === instance.status?.status ||
      ((opts.status === "EMPTY" )&&(instance.status === null));

    const deletedFilterMatches =
      (!opts.deleted && opts.deleted !== false) ||
      opts.deleted === instance.instance.deleted;

    return deletedFilterMatches && statusFilterMatches;
    });
}