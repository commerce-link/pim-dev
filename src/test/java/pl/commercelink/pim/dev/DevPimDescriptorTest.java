package pl.commercelink.pim.dev;

import org.junit.jupiter.api.Test;
import pl.commercelink.pim.api.PimCatalog;
import pl.commercelink.pim.api.PimCatalogDescriptor;

import java.util.Map;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class DevPimDescriptorTest {

    private final DevPimDescriptor descriptor = new DevPimDescriptor();

    @Test
    void isDiscoverableViaServiceLoader() {
        // when
        ServiceLoader<PimCatalogDescriptor> loader = ServiceLoader.load(PimCatalogDescriptor.class);

        // then
        assertThat(loader.stream().map(ServiceLoader.Provider::get))
                .anySatisfy(found -> assertThat(found).isInstanceOf(DevPimDescriptor.class));
    }

    @Test
    void marksItselfAsDevAdapter() {
        // when / then
        assertThat(descriptor.name()).isEqualTo("pim-dev");
        assertThat(descriptor.metadata()).containsEntry("dev", "true");
        assertThat(descriptor.configurationFields()).isEmpty();
        assertThat(descriptor.bindings()).isEmpty();
    }

    @Test
    void createsWorkingCatalogFromEmptyConfiguration() {
        // when
        PimCatalog catalog = descriptor.create(Map.of());

        // then
        assertThat(catalog.findAll()).isNotEmpty();
        assertThat(catalog.allCategories()).isNotEmpty();
    }
}
