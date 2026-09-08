package com.universalmedia.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universalmedia.model.Media;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InternetArchiveMetadataProvider
        implements MetadataProvider {

    private static final Logger LOGGER =
            Logger.getLogger(
                    InternetArchiveMetadataProvider.class.getName()
            );

    private static final String SEARCH_URL =
            "https://archive.org/advancedsearch.php";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public InternetArchiveMetadataProvider() {

        this.httpClient =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .build();

        this.objectMapper =
                new ObjectMapper();

        LOGGER.info("Internet Archive metadata provider initialized.");
    }

    @Override
    public List<Media> search(String query)
            throws Exception {

        if (query == null || query.isBlank()) {
            return List.of();
        }

        LOGGER.info(
                () -> "Searching Internet Archive metadata for: " + query
        );

        List<ArchiveCandidate> candidates =
                searchArchive(query);

        LOGGER.info(
                () -> "Internet Archive metadata candidates found: "
                        + candidates.size()
        );

        List<Media> results =
                new ArrayList<>();

        int id = -1;

        for (ArchiveCandidate candidate : candidates) {

            results.add(
                    new Media(
                            id--,
                            candidate.title,
                            "movie",
                            "Internet Archive public-domain/open-license movie",
                            candidate.year,
                            0.0,
                            "",
                            "internet_archive",
                            candidate.identifier
                    )
            );
        }

        LOGGER.info(
                () -> "Internet Archive metadata results created: "
                        + results.size()
        );

        return results;
    }

    private List<ArchiveCandidate> searchArchive(
            String title
    ) throws Exception {

        String normalizedTitle =
                normalizeTitle(title);

        String query =
                "collection:feature_films "
                        + "AND mediatype:movies "
                        + "AND title:("
                        + escapeArchiveQuery(title)
                        + ")";

        String url =
                SEARCH_URL
                        + "?q="
                        + URLEncoder.encode(
                        query,
                        StandardCharsets.UTF_8
                )
                        + "&fl[]=identifier"
                        + "&fl[]=title"
                        + "&fl[]=year"
                        + "&fl[]=date"
                        + "&rows=50"
                        + "&page=1"
                        + "&output=json";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .GET()
                        .build();

        HttpResponse<String> response;

        try {

            response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

        } catch (IOException | InterruptedException e) {

            LOGGER.log(
                    Level.WARNING,
                    "Internet Archive metadata request failed.",
                    e
            );

            throw e;
        }

        if (response.statusCode() != 200) {

            LOGGER.warning(
                    "Internet Archive metadata search failed. HTTP "
                            + response.statusCode()
            );

            throw new IOException(
                    "Internet Archive search failed. HTTP "
                            + response.statusCode()
            );
        }

        JsonNode root =
                objectMapper.readTree(
                        response.body()
                );

        JsonNode docs =
                root.path("response")
                        .path("docs");

        if (!docs.isArray()) {

            LOGGER.warning(
                    "Internet Archive metadata response contains no valid docs array."
            );

            return List.of();
        }

        List<ArchiveCandidate> candidates =
                new ArrayList<>();

        for (JsonNode doc : docs) {

            String identifier =
                    doc.path("identifier")
                            .asText("");

            String candidateTitle =
                    doc.path("title")
                            .asText("");

            String year =
                    doc.path("year")
                            .asText("");

            if (identifier.isBlank()
                    || candidateTitle.isBlank()) {
                continue;
            }


            int score =
                    calculateTitleScore(
                            normalizedTitle,
                            normalizeTitle(candidateTitle)
                    );

            candidates.add(
                    new ArchiveCandidate(
                            identifier,
                            candidateTitle,
                            year,
                            score
                    )
            );
        }

        return candidates;
    }

    private int calculateTitleScore(
            String requestedTitle,
            String candidateTitle
    ) {

        if (requestedTitle.equals(candidateTitle)) {
            return 90;
        }

        if (candidateTitle.contains(requestedTitle)
                || requestedTitle.contains(candidateTitle)) {
            return 70;
        }

        String[] requestedWords =
                requestedTitle.split(" ");

        int matchedWords = 0;

        for (String word : requestedWords) {

            if (word.length() < 2) {
                continue;
            }

            if (candidateTitle.contains(word)) {
                matchedWords++;
            }
        }

        if (matchedWords == 0) {
            return 0;
        }

        double wordMatchRatio =
                (double) matchedWords
                        / requestedWords.length;

        return (int) Math.round(
                wordMatchRatio * 50
        );
    }

    private String normalizeTitle(
            String title
    ) {

        if (title == null) {
            return "";
        }

        return title
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "\\[[^\\]]*\\]",
                        " "
                )
                .replaceAll(
                        "\\([^)]*\\)",
                        " "
                )
                .replaceAll(
                        "[^a-z0-9]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private String escapeArchiveQuery(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "(",
                        "\\("
                )
                .replace(
                        ")",
                        "\\)"
                );
    }

    private static class ArchiveCandidate {

        private final String identifier;
        private final String title;
        private final String year;
        private final int score;

        private ArchiveCandidate(
                String identifier,
                String title,
                String year,
                int score
        ) {

            this.identifier = identifier;
            this.title = title;
            this.year = year;
            this.score = score;
        }
    }
}
