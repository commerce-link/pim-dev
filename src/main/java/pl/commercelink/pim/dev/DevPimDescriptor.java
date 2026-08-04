package pl.commercelink.pim.dev;

import pl.commercelink.pim.api.PimCatalog;
import pl.commercelink.pim.api.PimCatalogDescriptor;
import pl.commercelink.provider.api.ProviderField;

import java.util.List;
import java.util.Map;

public class DevPimDescriptor implements PimCatalogDescriptor {

    @Override
    public String name() {
        return "pim-dev";
    }

    @Override
    public String displayName() {
        return "Dev PIM (bundled data)";
    }

    @Override
    public List<ProviderField> configurationFields() {
        return List.of();
    }

    @Override
    public PimCatalog create(Map<String, String> configuration) {
        return new DevPimCatalog();
    }

    @Override
    public Map<String, String> metadata() {
        return Map.of("dev", "true");
    }
}
