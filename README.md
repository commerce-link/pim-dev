# pim-dev

Offline, in-memory implementation of `pim-api` for development and demo environments. It lets the
main application run with a fully working PIM catalog without deploying the PIM microservice —
the counterpart of `pim-commercelink`, which talks to the real service.

## What it serves

- **Index** (`findAll`, `findByPimId`, `findByGtin`, `findByMpn`, `findByGtinOrMpn`): 135 entries
  bundled in `src/main/resources/pim-dev/index.json`. They mirror the app's local seed
  (`local-init/seed/catalog.csv` plus the store-owned feed of the second AcmeB connection, joined
  with the taxonomy seed for weights), so every product seeded by the local Acme/AcmeB feeds except
  the `SIM-*` scenario rows resolves to a PIM entry with `categoryId`, weights and `approved = true`.
- **Categories** (`allCategories`): the bundled Icecat snapshot `src/main/resources/icecat/categories.json`
  (6798 nodes, 25 top-level), served as `PimCategory` with `lang = "pl"` — the same snapshot the PIM
  service and the app's `OfflinePimCatalog` use.
- **Brands** (`allBrands`): distinct brand names derived from the bundled entries (no aliases).
- **Writes and events** (`submit`, `submitCategoryMatch`, `refresh`, listeners): no-ops. The
  descriptor declares no queue bindings, so no SQS infrastructure is required.

## Discovery

Registered via `META-INF/services/pl.commercelink.pim.api.PimCatalogDescriptor` as descriptor
`pim-dev` with metadata `dev=true`. The app's `PimCatalogRegistry` prefers a non-dev descriptor
when several are on the classpath, so running with both `pim-dev` and `pim-commercelink`
(`-Pdev,pim`) deterministically picks the real adapter.

## Usage

Included by the app's Maven `dev` profile (like `supplier-acme`):

```bash
mvn spring-boot:run -Pdev -Dspring-boot.run.profiles=localdev,local
```

No configuration or secrets are needed; an empty configuration map is valid.

## License

MIT — see [LICENSE](LICENSE).

## Refreshing the bundled data

- `icecat/categories.json` — copy verbatim from the PIM service repo (`pim/src/main/resources/icecat/categories.json`).
- `pim-dev/index.json` — regenerate from the app seed: join `local-init/seed/catalog.csv` (without the
  `SIM-*` rows) and `local-init/s3/stores/uma2dqukxr/supplier-feeds/acmeb-h6rxcqtb-feed.csv`
  (pimId, identifiers, brand, name, category, `pimCategoryId`) with
  `local-init/s3/datalake/taxonomy/*.csv` (weights) on EAN; `approved = true`, `subcategory = null`.
