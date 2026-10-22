package pl.commercelink.pim.dev;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.commercelink.pim.api.Brand;
import pl.commercelink.pim.api.CategoryMatchRequest;
import pl.commercelink.pim.api.PIMEntryAddedEvent;
import pl.commercelink.pim.api.PIMEntryDeletedEvent;
import pl.commercelink.pim.api.PimCatalog;
import pl.commercelink.pim.api.PimCategory;
import pl.commercelink.pim.api.PimEntry;
import pl.commercelink.pim.api.PimEntryIndex;
import pl.commercelink.pim.api.PimEntryRequest;
import pl.commercelink.pim.api.PimView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class DevPimCatalog implements PimCatalog {

    private static final String INDEX_RESOURCE = "/pim-dev/index.json";
    private static final String CATEGORIES_RESOURCE = "/icecat/categories.json";
    private static final String LANG = "pl";

    private final PimEntryIndex index;
    private final List<PimCategory> categories;
    private final List<Brand> brands;

    public DevPimCatalog() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<PimEntry> entries = loadResource(objectMapper, INDEX_RESOURCE, new TypeReference<List<PimEntry>>() {});
        this.index = new PimEntryIndex(entries, String::trim, String::trim);
        this.categories = loadResource(objectMapper, CATEGORIES_RESOURCE, new TypeReference<List<BundledCategoryNode>>() {})
                .stream()
                .map(node -> new PimCategory(node.id(), node.parentId(), node.namePl(), LANG))
                .toList();
        this.brands = index.global().findAll().stream()
                .map(PimEntry::brand)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .map(name -> new Brand(name, List.of()))
                .toList();
    }

    @Override
    public PimView withGlobalData() {
        return index.global();
    }

    @Override
    public PimView withStoreData(String storeId) {
        return index.forStore(storeId);
    }

    @Override
    public List<Brand> allBrands() {
        return brands;
    }

    @Override
    public List<PimCategory> allCategories() {
        return categories;
    }

    @Override
    public void submit(PimEntryRequest request) {
    }

    @Override
    public void submitCategoryMatch(CategoryMatchRequest request) {
    }

    @Override
    public void refresh() {
    }

    @Override
    public void onEntryAdded(Consumer<PIMEntryAddedEvent> listener) {
    }

    @Override
    public void onEntryDeleted(Consumer<PIMEntryDeletedEvent> listener) {
    }

    private static <T> List<T> loadResource(ObjectMapper objectMapper, String resource, TypeReference<List<T>> type) {
        try (InputStream stream = DevPimCatalog.class.getResourceAsStream(resource)) {
            if (stream == null) {
                System.err.println("Bundled dev PIM resource not found: " + resource);
                return List.of();
            }
            return objectMapper.readValue(stream, type);
        } catch (IOException e) {
            System.err.println("Failed to load bundled dev PIM resource " + resource + ": " + e.getMessage());
            return List.of();
        }
    }

    private record BundledCategoryNode(String id, String parentId, String namePl, String nameEn) {
    }
}
