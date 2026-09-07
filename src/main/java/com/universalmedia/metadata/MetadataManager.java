package com.universalmedia.metadata;

import com.universalmedia.model.Media;

import java.util.ArrayList;
import java.util.List;

public class MetadataManager {

    private final List<MetadataProvider> providers =
            new ArrayList<>();

    public void registerProvider(
            MetadataProvider provider
    ) {

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Metadata provider cannot be null."
            );
        }

        providers.add(provider);

        System.out.println(
                "Registered metadata provider: "
                        + provider.getClass().getSimpleName()
        );
    }

    public List<Media> search(String query)
            throws Exception {

        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<Media> results =
                new ArrayList<>();

        for (MetadataProvider provider : providers) {

            System.out.println(
                    "Searching with metadata provider: "
                            + provider.getClass().getSimpleName()
            );

            try {

                List<Media> providerResults =
                        provider.search(query);

                if (providerResults != null) {
                    results.addAll(providerResults);
                }

            } catch (Exception e) {

                System.out.println(
                        "Metadata provider failed: "
                                + provider.getClass().getSimpleName()
                );

                System.out.println(
                        "Reason: "
                                + e.getMessage()
                );
            }
        }

        return results;
    }

    public List<MetadataProvider> getProviders() {
        return List.copyOf(providers);
    }
}
