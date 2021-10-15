export interface MeshMarketplaceOriginatingIdentity {
  user_id: string;
  user_euid?: string;
}

type DashboardContext = {
  auth_url: string;
  token_url: string;
  permission_url: string;
};

type MeshMarketplaceCoreContext = {
  platform: "meshmarketplace";
  customer_id: string;
  project_id: string;
};

export type MeshMarketplaceContext =
  | MeshMarketplaceCoreContext
  | MeshMarketplaceCoreContext & DashboardContext;
