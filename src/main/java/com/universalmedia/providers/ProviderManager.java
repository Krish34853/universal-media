package com.universalmedia.providers;

import com.universalmedia.model.Media;
import com.universalmedia.model.ResolvedStream;

import java.util.ArrayList;
import java.util.List;

public class ProviderManager {

    private final List<MediaProvider> providers =
            new ArrayList<>();

    public void registerProvider(
            MediaProvider provider
    ) {
        if (provider == null) {
            throw new IllegalArgumentException(
                    "Provider cannot be null."
            );
        }

        providers.add(provider);

        System.out.println(
                "Registered provider: "
                        + provider.getName()
        );
    }

    public MediaProvider findProvider(
            Media media
    ) {
        if (media == null) {
            return null;
        }

        String providerId =
                media.getProviderId();

        if (providerId == null || providerId.isBlank()) {
            return null;
        }

        for (MediaProvider provider : providers) {

            if (!provider.getProviderId()
                    .equalsIgnoreCase(providerId)) {
                continue;
            }

            if (provider.supports(media)) {
                return provider;
            }
        }

        return null;
    }

    public ResolvedStream resolveStream(
            Media media,
            String quality
    ) throws Exception {

        if (media == null) {
            throw new IllegalArgumentException(
                    "Media cannot be null."
            );
        }

        String providerId =
                media.getProviderId();

        if (providerId == null || providerId.isBlank()) {
            throw new RuntimeException(
                    "Media does not specify a provider."
            );
        }

        Exception lastException = null;

        for (MediaProvider provider : providers) {

            if (!provider.getProviderId()
                    .equalsIgnoreCase(providerId)) {
                continue;
            }

            if (!provider.supports(media)) {
                continue;
            }

            System.out.println(
                    "Trying provider: "
                            + provider.getName()
            );

            try {

                ResolvedStream stream =
                        provider.getStream(
                                media,
                                quality
                        );

                if (stream == null) {

                    System.out.println(
                            "Provider returned no stream: "
                                    + provider.getName()
                    );

                    continue;
                }

                System.out.println(
                        "Provider succeeded: "
                                + provider.getName()
                );

                return stream;

            } catch (Exception e) {

                lastException = e;

                System.out.println(
                        "Provider failed: "
                                + provider.getName()
                );

                System.out.println(
                        "Reason: "
                                + e.getMessage()
                );
            }
        }

        if (lastException != null) {

            throw new RuntimeException(
                    "Provider '"
                            + providerId
                            + "' failed.",
                    lastException
            );
        }

        throw new RuntimeException(
                "No registered provider supports provider ID: "
                        + providerId
        );
    }

    public List<MediaProvider> getProviders() {
        return List.copyOf(providers);
    }
}
