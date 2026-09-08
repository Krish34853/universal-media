package com.universalmedia.providers;

import com.universalmedia.model.Media;
import com.universalmedia.model.ResolvedStream;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProviderManagerTest {

    private static class TestProvider
            implements MediaProvider {

        private final String providerId;
        private final String name;
        private final boolean supportsMedia;

        TestProvider(
                String providerId,
                String name,
                boolean supportsMedia
        ) {
            this.providerId = providerId;
            this.name = name;
            this.supportsMedia = supportsMedia;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getProviderId() {
            return providerId;
        }

        @Override
        public boolean supports(Media media) {
            return supportsMedia && media != null;
        }

        @Override
        public ResolvedStream getStream(
                Media media,
                String quality
        ) {
            return new ResolvedStream(
                    "test://stream",
                    null,
                    quality,
                    "English",
                    Map.of()
            );
        }
    }

    private Media createMedia(
            String providerId,
            String mediaType
    ) {
        return new Media(
                1,
                "Test Movie",
                mediaType,
                "Test description",
                "2026-01-01",
                8.0,
                null,
                providerId
        );
    }

    @Test
    void findsProviderWhenProviderIdsMatch() {

        ProviderManager manager =
                new ProviderManager();

        TestProvider provider =
                new TestProvider(
                        "internet_archive",
                        "Internet Archive Test",
                        true
                );

        manager.registerProvider(provider);

        Media media =
                createMedia(
                        "internet_archive",
                        "movie"
                );

        MediaProvider found =
                manager.findProvider(media);

        assertSame(provider, found);
    }

    @Test
    void doesNotRouteWhenProviderIdsDoNotMatch() {

        ProviderManager manager =
                new ProviderManager();

        TestProvider provider =
                new TestProvider(
                        "internet_archive",
                        "Internet Archive Test",
                        true
                );

        manager.registerProvider(provider);

        Media media =
                createMedia(
                        "tmdb",
                        "movie"
                );

        MediaProvider found =
                manager.findProvider(media);

        assertNull(found);
    }

    @Test
    void returnsNullForUnknownProviderId() {

        ProviderManager manager =
                new ProviderManager();

        TestProvider provider =
                new TestProvider(
                        "internet_archive",
                        "Internet Archive Test",
                        true
                );

        manager.registerProvider(provider);

        Media media =
                createMedia(
                        "unknown_provider",
                        "movie"
                );

        MediaProvider found =
                manager.findProvider(media);

        assertNull(found);
    }

    @Test
    void returnsNullWhenMediaIsUnsupported() {

        ProviderManager manager =
                new ProviderManager();

        TestProvider provider =
                new TestProvider(
                        "internet_archive",
                        "Internet Archive Test",
                        false
                );

        manager.registerProvider(provider);

        Media media =
                createMedia(
                        "internet_archive",
                        "movie"
                );

        MediaProvider found =
                manager.findProvider(media);

        assertNull(found);
    }

    @Test
    void returnsNullForNullMedia() {

        ProviderManager manager =
                new ProviderManager();

        TestProvider provider =
                new TestProvider(
                        "internet_archive",
                        "Internet Archive Test",
                        true
                );

        manager.registerProvider(provider);

        MediaProvider found =
                manager.findProvider(null);

        assertNull(found);
    }

    @Test
    void returnsNullForBlankProviderId() {

        ProviderManager manager =
                new ProviderManager();

        TestProvider provider =
                new TestProvider(
                        "internet_archive",
                        "Internet Archive Test",
                        true
                );

        manager.registerProvider(provider);

        Media media =
                createMedia(
                        "",
                        "movie"
                );

        MediaProvider found =
                manager.findProvider(media);

        assertNull(found);
    }

    @Test
    void matchesProviderIdCaseInsensitively() {

        ProviderManager manager =
                new ProviderManager();

        TestProvider provider =
                new TestProvider(
                        "internet_archive",
                        "Internet Archive Test",
                        true
                );

        manager.registerProvider(provider);

        Media media =
                createMedia(
                        "INTERNET_ARCHIVE",
                        "movie"
                );

        MediaProvider found =
                manager.findProvider(media);

        assertSame(provider, found);
    }
    @Test
	void routesToInternetArchiveProvider() {

    	ProviderManager manager =
        	    new ProviderManager();

    	InternetArchiveProvider provider =
        	    new InternetArchiveProvider();

   	 manager.registerProvider(provider);

    	Media media =
        	    createMedia(
                	    "internet_archive",
                    	"movie"
           	 );

   	 MediaProvider found =
        	    manager.findProvider(media);

    	assertSame(provider, found);
    }

    @Test
    void doesNotRouteTmdbMediaToInternetArchiveProvider() {

        ProviderManager manager =
                new ProviderManager();

        InternetArchiveProvider provider =
                new InternetArchiveProvider();

        manager.registerProvider(provider);

        Media media =
                 createMedia(
                        "tmdb",
                        "movie"
                );

        MediaProvider found =
                manager.findProvider(media);

        assertNull(found);
	}

    @Test
    void internetArchiveSupportsMovieMedia() {

        InternetArchiveProvider provider =
                new InternetArchiveProvider();

        Media media =
                createMedia(
                        "internet_archive",
                        "movie"
                );

        assertTrue(provider.supports(media));
    }

    @Test
    void internetArchiveDoesNotSupportTvMedia() {

        InternetArchiveProvider provider =
                new InternetArchiveProvider();

        Media media =
                createMedia(
                        "internet_archive",
                        "tv"
                );

        assertFalse(provider.supports(media));
    }

}
