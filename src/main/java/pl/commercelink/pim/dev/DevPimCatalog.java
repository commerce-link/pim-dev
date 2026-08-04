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
import pl.commercelink.pim.api.PimEntryRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class DevPimCatalog implements PimCatalog {

    private static final String INDEX_RESOURCE = "/pim-dev/index.json";
    private static final String CATEGORIES_RESOURCE = "/icecat/categories.json";
    private static final String LANG = "pl";

    private final List<PimEntry> entries;
    private final Map<String, PimEntry> byPimId;
    private final Map<String, PimEntry> byGtin;
    private final Map<String, PimEntry> byMpn;
    private final List<PimCategory> categories;
    private final List<Brand> brands;

    public DevPimCatalog() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.entries = loadResource(objectMapper, INDEX_RESOURCE, new TypeReference<List<PimEntry>>() {});
        this.byPimId = index(entry -> entry.pimId() == null ? List.of() : List.of(entry.pimId()));
        this.byGtin = index(PimEntry::gtins);
        this.byMpn = index(PimEntry::mpns);
        this.categories = loadResource(objectMapper, CATEGORIES_RESOURCE, new TypeReference<List<BundledCategoryNode>>() {})
                .stream()
                .map(node -> new PimCategory(node.id(), node.parentId(), node.namePl(), LANG))
                .toList();
        this.brands = entries.stream()
                .map(PimEntry::brand)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .map(name -> new Brand(name, List.of()))
                .toList();
    }

    @Override
    public List<PimEntry> findAll() {
        return entries;
    }

    @Override
    public Optional<PimEntry> findByPimId(String pimId) {
        return find(byPimId, pimId);
    }

    @Override
    public Optional<PimEntry> findByGtin(String gtin) {
        return find(byGtin, gtin);
    }

    @Override
    public Optional<PimEntry> findByMpn(String mpn) {
        return find(byMpn, mpn);
    }

    @Override
    public Optional<PimEntry> findByGtinOrMpn(String gtin, String mpn) {
        return findByGtin(gtin).or(() -> findByMpn(mpn));
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

    private Map<String, PimEntry> index(Function<PimEntry, List<String>> keysOf) {
        Map<String, PimEntry> index = new HashMap<>();
        for (PimEntry entry : entries) {
            for (String key : keysOf.apply(entry)) {
                index.putIfAbsent(key, entry);
            }
        }
        return index;
    }

    private static Optional<PimEntry> find(Map<String, PimEntry> index, String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(index.get(key.trim()));
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
