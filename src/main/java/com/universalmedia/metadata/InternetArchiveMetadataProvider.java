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

public class InternetArchiveMetadataProvider
        implements MetadataProvider {

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
    }

    @Override
    public List<Media> search(String query)
            throws Exception {

        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<ArchiveCandidate> candidates =
                searchArchive(query, 0);

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

        return results;
    }

    private List<ArchiveCandidate> searchArchive(
            String title,
            int releaseYear
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

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
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

            int candidateYear =
                    extractYear(year);

            int score =
                    calculateTitleScore(
                            normalizedTitle,
                            normalizeTitle(candidateTitle),
                            releaseYear,
                            candidateYear
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
            String candidateTitle,
            int requestedYear,
            int candidateYear
    ) {

        if (requestedTitle.equals(candidateTitle)) {

            if (requestedYear > 0
                    && candidateYear == requestedYear) {
                return 100;
            }

            return 90;
        }

        if (candidateTitle.contains(requestedTitle)
                || requestedTitle.contains(candidateTitle)) {

            if (requestedYear > 0
                    && candidateYear == requestedYear) {
                return 80;
            }

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

        int score =
                (int) Math.round(
                        wordMatchRatio * 50
                );

        if (requestedYear > 0
                && candidateYear == requestedYear) {
            score += 20;
        }

        return score;
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

    private int extractYear(
            String value
    ) {

        if (value == null
                || value.isBlank()) {
            return 0;
        }

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile(
                                "(19|20)\\d{2}"
                        )
                        .matcher(value);

        if (matcher.find()) {

            try {

                return Integer.parseInt(
                        matcher.group()
                );

            } catch (NumberFormatException ignored) {
            }
        }

        return 0;
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
