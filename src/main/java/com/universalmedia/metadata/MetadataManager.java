package com.universalmedia.metadata;

import com.universalmedia.model.Media;

import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MetadataManager {

    private static final Logger LOGGER =
	    Logger.getLogger(MetadataManager.class.getName());

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

        LOGGER.info(
                () -> "Registered metadata provider: "
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

            LOGGER.info(
                    () -> "Searching with metadata provider: "
                            + provider.getClass().getSimpleName()
            );

            try {

                List<Media> providerResults =
                        provider.search(query);

                if (providerResults != null) {
                    results.addAll(providerResults);
                }

            } catch (Exception e) {

                LOGGER.log(
			Level.WARNING,
			"Mettadata provider failed: "
				+ provider.getClass().getSimpleName(),
                         e
                );
            }
        }

        return results;
    }

    public List<MetadataProvider> getProviders() {
        return List.copyOf(providers);
    }
}
