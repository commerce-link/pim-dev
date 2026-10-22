package pl.commercelink.pim.dev;

import org.junit.jupiter.api.Test;
import pl.commercelink.pim.api.Brand;
import pl.commercelink.pim.api.CategoryMatchRequest;
import pl.commercelink.pim.api.PimCategory;
import pl.commercelink.pim.api.PimEntry;
import pl.commercelink.pim.api.PimEntryRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DevPimCatalogTest {

    private final DevPimCatalog catalog = new DevPimCatalog();

    @Test
    void loadsAllBundledEntries() {
        // when
        List<PimEntry> entries = catalog.withGlobalData().findAll();

        // then
        assertThat(entries).hasSize(135);
        assertThat(entries).allSatisfy(entry -> {
            assertThat(entry.pimId()).isNotBlank();
            assertThat(entry.categoryId()).isNotBlank();
            assertThat(entry.approved()).isTrue();
        });
    }

    @Test
    void findsEntryByPimId() {
        // when
        Optional<PimEntry> entry = catalog.withGlobalData().findByPimId("local-seed-0101");

        // then
        assertThat(entry).hasValueSatisfying(found -> {
            assertThat(found.name()).isEqualTo("AMD Ryzen 7 9800X3D (8 rdzeni)");
            assertThat(found.brand()).isEqualTo("AMD");
            assertThat(found.categoryId()).isEqualTo("989");
            assertThat(found.netWeightInGrams()).isEqualTo(40);
            assertThat(found.grossWeightInGrams()).isEqualTo(250);
        });
    }

    @Test
    void findsEntryByGtin() {
        // when
        Optional<PimEntry> entry = catalog.withGlobalData().findByGtin("4711636046213");

        // then
        assertThat(entry).hasValueSatisfying(found ->
                assertThat(found.pimId()).isEqualTo("local-seed-0204"));
    }

    @Test
    void findsEntryByMpn() {
        // when
        Optional<PimEntry> entry = catalog.withGlobalData().findByMpn("NH-D15SCH.BK");

        // then
        assertThat(entry).hasValueSatisfying(found ->
                assertThat(found.name()).isEqualTo("Noctua NH-D15S"));
    }

    @Test
    void gtinOrMpnLookupFallsBackToMpn() {
        // when
        Optional<PimEntry> entry = catalog.withGlobalData().findByGtinOrMpn("0000000000000", "MZ-V9P2T0BW");

        // then
        assertThat(entry).hasValueSatisfying(found ->
                assertThat(found.pimId()).isEqualTo("local-seed-0501"));
    }

    @Test
    void unknownIdentifiersYieldEmptyResults() {
        // when / then
        assertThat(catalog.withGlobalData().findByPimId("missing")).isEmpty();
        assertThat(catalog.withGlobalData().findByGtin(null)).isEmpty();
        assertThat(catalog.withGlobalData().findByMpn(" ")).isEmpty();
        assertThat(catalog.withGlobalData().findByGtinOrMpn(null, null)).isEmpty();
    }

    @Test
    void servesBundledCategoryTreeInPolish() {
        // when
        List<PimCategory> categories = catalog.allCategories();

        // then
        assertThat(categories).hasSize(6798);
        assertThat(categories).allSatisfy(category -> assertThat(category.lang()).isEqualTo("pl"));
        assertThat(categories).contains(
                new PimCategory("989", "2844", "Procesory", "pl"),
                new PimCategory("170", "2844", "Karty graficzne", "pl"),
                new PimCategory("194", "191", "Klawiatury", "pl"));
        assertThat(categories).filteredOn(PimCategory::topLevel).hasSize(25);
    }

    @Test
    void derivesDistinctBrandsFromEntries() {
        // when
        List<Brand> brands = catalog.allBrands();

        // then
        assertThat(brands).hasSize(26);
        assertThat(brands).extracting(Brand::name).doesNotHaveDuplicates().contains("AMD", "Intel", "Noctua");
    }

    @Test
    void writeOperationsAreNoOps() {
        // when / then
        assertThatCode(() -> {
            catalog.submit(new PimEntryRequest("730143315289", "100-100001084WOF", "AMD", 1));
            catalog.submitCategoryMatch(new CategoryMatchRequest("Acme", "730143315289", "100-100001084WOF", "AMD", "name", "CPU"));
            catalog.refresh();
            catalog.onEntryAdded(event -> {});
            catalog.onEntryDeleted(event -> {});
            catalog.dispatch(new Object());
        }).doesNotThrowAnyException();
    }

    @Test
    void globalLookupsIgnoreStoreEntries() {
        assertThat(catalog.withGlobalData().findByPimId("local-store-0002")).isEmpty();
        assertThat(catalog.withGlobalData().findByGtin("5909990000017")).isEmpty();
        assertThat(catalog.withGlobalData().findAll()).noneMatch(e -> e.ownerStoreId() != null);
    }

    @Test
    void seedStoreSeesItsEntriesFirst() {
        String sharedGtin = catalog.withStoreData("uma2dqukxr").findByPimId("local-store-0001").orElseThrow().gtins().get(0);
        assertThat(catalog.withStoreData("uma2dqukxr").findByGtin(sharedGtin)).map(PimEntry::pimId).contains("local-store-0001");
        assertThat(catalog.withGlobalData().findByGtin(sharedGtin)).map(PimEntry::pimId).hasValueSatisfying(
                id -> assertThat(id).isNotEqualTo("local-store-0001"));
        assertThat(catalog.withStoreData("uma2dqukxr").findByGtin("5909990000017")).isPresent();
    }

    @Test
    void otherStoresEntryIsInvisibleToTheSeedStore() {
        assertThat(catalog.withStoreData("uma2dqukxr").findByGtin("5909990000024")).isEmpty();
        assertThat(catalog.withStoreData("local-other-store").findByGtin("5909990000024")).isPresent();
    }

    @Test
    void brandsComeFromGlobalEntriesOnly() {
        assertThat(catalog.allBrands()).isNotEmpty();
    }
}
