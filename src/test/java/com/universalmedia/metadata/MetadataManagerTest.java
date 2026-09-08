package com.universalmedia.metadata;

import com.universalmedia.model.Media;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetadataManagerTest {

    private static class TestMetadataProvider
            implements MetadataProvider {

        private final List<Media> results;

        TestMetadataProvider(List<Media> results) {
            this.results = results;
        }

        @Override
        public List<Media> search(String query) {
            return results;
        }
    }

    private static class FailingMetadataProvider
            implements MetadataProvider {

        @Override
        public List<Media> search(String query)
                throws Exception {
            throw new RuntimeException(
                    "Test provider failure"
            );
        }
    }

    private Media createMedia(
            int id,
            String title
    ) {
        return new Media(
                id,
                title,
                "movie",
                "Test description",
                "2026-01-01",
                8.0,
                null,
                "test"
        );
    }

    @Test
    void registersMetadataProvider() {

        MetadataManager manager =
                new MetadataManager();

        MetadataProvider provider =
                new TestMetadataProvider(List.of());

        manager.registerProvider(provider);

        assertEquals(
                1,
                manager.getProviders().size()
        );

        assertSame(
                provider,
                manager.getProviders().get(0)
        );
    }

    @Test
    void rejectsNullMetadataProvider() {

        MetadataManager manager =
                new MetadataManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.registerProvider(null)
        );
    }

    @Test
    void returnsEmptyListForNullQuery()
            throws Exception {

        MetadataManager manager =
                new MetadataManager();

        manager.registerProvider(
                new TestMetadataProvider(List.of(
                        createMedia(1, "Test Movie")
                ))
        );

        List<Media> results =
                manager.search(null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void returnsEmptyListForBlankQuery()
            throws Exception {

        MetadataManager manager =
                new MetadataManager();

        manager.registerProvider(
                new TestMetadataProvider(List.of(
                        createMedia(1, "Test Movie")
                ))
        );

        List<Media> results =
                manager.search("   ");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void combinesResultsFromMultipleProviders()
            throws Exception {

        MetadataManager manager =
                new MetadataManager();

        Media movie1 =
                createMedia(1, "Movie One");

        Media movie2 =
                createMedia(2, "Movie Two");

        manager.registerProvider(
                new TestMetadataProvider(
                        List.of(movie1)
                )
        );

        manager.registerProvider(
                new TestMetadataProvider(
                        List.of(movie2)
                )
        );

        List<Media> results =
                manager.search("movie");

        assertEquals(2, results.size());
        assertTrue(results.contains(movie1));
        assertTrue(results.contains(movie2));
    }

    @Test
    void continuesWhenOneProviderFails()
            throws Exception {

        MetadataManager manager =
                new MetadataManager();

        Media movie =
                createMedia(1, "Working Movie");

        manager.registerProvider(
                new FailingMetadataProvider()
        );

        manager.registerProvider(
                new TestMetadataProvider(
                        List.of(movie)
                )
        );

        List<Media> results =
                manager.search("movie");

        assertEquals(1, results.size());
        assertSame(movie, results.get(0));
    }

    @Test
    void handlesProviderReturningNull()
            throws Exception {

        MetadataManager manager =
                new MetadataManager();

        MetadataProvider provider =
                new TestMetadataProvider(null);

        manager.registerProvider(provider);

        List<Media> results =
                manager.search("movie");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
