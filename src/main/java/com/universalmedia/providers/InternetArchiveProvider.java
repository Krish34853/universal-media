package com.universalmedia.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universalmedia.model.Media;
import com.universalmedia.model.ResolvedStream;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InternetArchiveProvider implements MediaProvider {

    private static final String SEARCH_URL =
            "https://archive.org/advancedsearch.php";

    private static final String METADATA_URL =
            "https://archive.org/metadata/";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public InternetArchiveProvider() {

        this.httpClient =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .build();

        this.objectMapper =
                new ObjectMapper();
    }

    @Override
    public String getName() {
        return "Internet Archive Public Domain";
    }

    @Override
    public boolean supports(Media media) {

        if (media == null) {
            return false;
        }

        return "movie".equalsIgnoreCase(
                media.getMediaType()
        );
    }

    @Override
    public ResolvedStream getStream(
            Media media,
            String quality
    ) throws Exception {

        if (media == null) {
            throw new IllegalArgumentException(
                    "Media cannot be null."
            );
        }

        if (!supports(media)) {
            throw new IllegalArgumentException(
                    "Internet Archive provider only supports movies."
            );
        }

        System.out.println();
        System.out.println(
                "=== Internet Archive Provider ==="
        );

        System.out.println(
                "Movie: "
                        + media.getTitle()
        );

        System.out.println(
                "Release date: "
                        + media.getReleaseDate()
        );

        System.out.println(
                "Requested quality: "
                        + quality
        );

        int requestedHeight =
                parseQualityHeight(quality);

        System.out.println(
                "Searching Archive..."
        );

        int releaseYear =
                extractYear(media.getReleaseDate());

        System.out.println(
                "Year used for matching: "
                        + releaseYear
        );

        List<ArchiveCandidate> candidates =
                searchArchive(
                        media.getTitle(),
                        releaseYear
                );

        System.out.println(
                "Archive candidates found: "
                        + candidates.size()
        );

        for (ArchiveCandidate candidate : candidates) {

            System.out.println(
                    "Candidate: "
                            + candidate.title
                            + " | year="
                            + candidate.year
                            + " | score="
                            + candidate.score
            );
        }

        if (candidates.isEmpty()) {
            throw new RuntimeException(
                    "No Internet Archive candidates found for: "
                            + media.getTitle()
            );
        }

        ArchiveCandidate selectedCandidate =
                candidates.stream()
                        .filter(candidate -> candidate.score >= 45)
                        .max(
                                Comparator
                                        .comparingInt(
                                                (ArchiveCandidate c)
                                                        -> c.score
                                        )
                                        .thenComparing(
                                                c -> c.year
                                        )
                        )
                        .orElse(null);

        if (selectedCandidate == null) {
            throw new RuntimeException(
                    "No sufficiently matching Internet Archive movie found for: "
                            + media.getTitle()
            );
        }

        System.out.println(
                "Selected Archive item: "
                        + selectedCandidate.identifier
        );

        JsonNode metadata =
                getMetadata(
                        selectedCandidate.identifier
                );

        JsonNode metadataNode =
                metadata.path("metadata");

        String archiveTitle =
                metadataNode
                        .path("title")
                        .asText("");

        System.out.println(
                "Archive title: "
                        + archiveTitle
        );

        /*
         * ------------------------------------------------------------
         * RIGHTS / LICENSE VALIDATION
         * ------------------------------------------------------------
         */

        RightsInfo rights =
                inspectRights(metadataNode);

        System.out.println();
        System.out.println("Rights validation");
        System.out.println("------------------------------");

        System.out.println(
                "License: "
                        + rights.license
        );

        System.out.println(
                "Rights: "
                        + rights.rights
        );

        System.out.println(
                "Description: "
                        + rights.description
        );

        System.out.println(
                "Rights status: "
                        + (
                        rights.authorized
                                ? "AUTHORIZED"
                                : "NOT AUTHORIZED / AMBIGUOUS"
                )
        );

        System.out.println(
                "------------------------------"
        );

        if (!rights.authorized) {

            throw new RuntimeException(
                    "Internet Archive item does not contain sufficiently clear "
                            + "public-domain or open-license information: "
                            + selectedCandidate.identifier
            );
        }

        /*
         * ------------------------------------------------------------
         * VIDEO FILE DISCOVERY
         * ------------------------------------------------------------
         */

        List<VideoFile> videoFiles =
                extractMp4Files(
                        metadata.path("files")
                );

        if (videoFiles.isEmpty()) {
            throw new RuntimeException(
                    "No MP4 video files found in Archive item: "
                            + selectedCandidate.identifier
            );
        }

        System.out.println();
        System.out.println(
                "Available MP4 files:"
        );

        for (VideoFile file : videoFiles) {

            System.out.println(
                    "  "
                            + file.name
                            + " | "
                            + file.height
                            + "p"
                            + " | "
                            + file.size
                            + " bytes"
            );
        }

        /*
         * ------------------------------------------------------------
         * QUALITY SELECTION
         * ------------------------------------------------------------
         */

        VideoFile selectedFile =
                selectBestQuality(
                        videoFiles,
                        requestedHeight
                );

        if (selectedFile == null) {
            throw new RuntimeException(
                    "Could not determine a suitable video quality."
            );
        }

        System.out.println();
        System.out.println(
                "Requested: "
                        + quality
                        + " ("
                        + requestedHeight
                        + "p)"
        );

        System.out.println(
                "Selected: "
                        + selectedFile.name
                        + " ("
                        + selectedFile.height
                        + "p)"
        );

        String streamUrl =
                buildDownloadUrl(
                        selectedCandidate.identifier,
                        selectedFile.name
                );

        System.out.println(
                "Selected stream: "
                        + streamUrl
        );

        return new ResolvedStream(
                streamUrl,
                null,
                selectedFile.height + "p",
                "English",
                Map.of()
	);
    }

/*
 * ================================================================
 * ARCHIVE SEARCH
 * ================================================================
 */

public List<Media> search(String query) throws Exception {

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
			"internet_archive"
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

    /*
     * ================================================================
     * TITLE MATCHING
     * ================================================================
     */

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

    /*
     * ================================================================
     * METADATA
     * ================================================================
     */

    private JsonNode getMetadata(
            String identifier
    ) throws Exception {

        String url =
                METADATA_URL
                        + URLEncoder.encode(
                        identifier,
                        StandardCharsets.UTF_8
                );

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
                    "Internet Archive metadata request failed. HTTP "
                            + response.statusCode()
            );
        }

        return objectMapper.readTree(
                response.body()
        );
    }

    /*
     * ================================================================
     * RIGHTS VALIDATION
     * ================================================================
     */

    private RightsInfo inspectRights(
            JsonNode metadata
    ) {

        String license =
                getMetadataValue(
                        metadata,
                        "licenseurl"
                );

        if (license.isBlank()) {
            license =
                    getMetadataValue(
                            metadata,
                            "license"
                    );
        }

        String rights =
                getMetadataValue(
                        metadata,
                        "rights"
                );

        String description =
                getMetadataValue(
                        metadata,
                        "description"
                );

        String publicDomain =
                getMetadataValue(
                        metadata,
                        "publicdomain"
                );

        String notes =
                getMetadataValue(
                        metadata,
                        "notes"
                );

        String combined =
                (
                        license
                                + " "
                                + rights
                                + " "
                                + description
                                + " "
                                + publicDomain
                                + " "
                                + notes
                ).toLowerCase(
                        Locale.ROOT
                );

        boolean explicitPublicDomain =
                containsAny(
                        combined,
                        "public domain",
                        "public-domain",
                        "publicdomain",
                        "pd"
                );

        boolean creativeCommons =
                containsAny(
                        combined,
                        "creativecommons.org",
                        "creativecommons",
                        "creative commons",
                        "cc by",
                        "cc0",
                        "cc-by",
                        "cc-by-sa",
                        "cc by-sa"
                );

        boolean explicitOpenLicense =
                containsAny(
                        combined,
                        "open license",
                        "open licence",
                        "free license",
                        "free licence"
                );

        boolean authorized =
                explicitPublicDomain
                        || creativeCommons
                        || explicitOpenLicense;

        return new RightsInfo(
                license.isBlank()
                        ? "Not specified"
                        : license,
                rights.isBlank()
                        ? "Not specified"
                        : rights,
                description.isBlank()
                        ? "Not specified"
                        : shorten(description),
                authorized
        );
    }

    private boolean containsAny(
            String value,
            String... terms
    ) {

        for (String term : terms) {

            if (value.contains(
                    term.toLowerCase(
                            Locale.ROOT
                    )
            )) {
                return true;
            }
        }

        return false;
    }

    private String getMetadataValue(
            JsonNode metadata,
            String field
    ) {

        JsonNode value =
                metadata.path(field);

        if (value.isMissingNode()
                || value.isNull()) {
            return "";
        }

        if (value.isArray()) {

            StringBuilder result =
                    new StringBuilder();

            for (JsonNode item : value) {

                if (result.length() > 0) {
                    result.append(" ");
                }

                result.append(
                        item.asText("")
                );
            }

            return result.toString();
        }

        return value.asText("");
    }

    private String shorten(
            String value
    ) {

        value =
                value.replaceAll(
                        "\\s+",
                        " "
                ).trim();

        if (value.length() <= 200) {
            return value;
        }

        return value.substring(
                0,
                197
        ) + "...";
    }

    /*
     * ================================================================
     * VIDEO FILE EXTRACTION
     * ================================================================
     */

    private List<VideoFile> extractMp4Files(
            JsonNode files
    ) {

        List<VideoFile> result =
                new ArrayList<>();

        if (!files.isArray()) {
            return result;
        }

        for (JsonNode file : files) {

            String name =
                    file.path("name")
                            .asText("");

            if (name.isBlank()) {
                continue;
            }

            String format =
                    file.path("format")
                            .asText("")
                            .toLowerCase(
                                    Locale.ROOT
                            );

            boolean mp4 =
                    name.toLowerCase(
                            Locale.ROOT
                    ).endsWith(".mp4")
                            || format.contains("mpeg4")
                            || format.contains("mp4");

            if (!mp4) {
                continue;
            }

            long size =
                    file.path("size")
                            .asLong(0);

            int height =
                    detectHeight(
                            file,
                            name
                    );

            if (height <= 0) {
                continue;
            }

            result.add(
                    new VideoFile(
                            name,
                            height,
                            size
                    )
            );
        }

        return result;
    }

    /*
     * ================================================================
     * RESOLUTION DETECTION
     * ================================================================
     */

    private int detectHeight(
            JsonNode file,
            String filename
    ) {

        int height =
                file.path("height")
                        .asInt(0);

        if (height > 0) {
            return normalizeHeight(
                    height
            );
        }

        String width =
                file.path("width")
                        .asText("");

        String fileHeight =
                file.path("height")
                        .asText("");

        if (!fileHeight.isBlank()) {

            try {

                int parsed =
                        Integer.parseInt(
                                fileHeight
                        );

                if (parsed > 0) {
                    return normalizeHeight(
                            parsed
                    );
                }

            } catch (NumberFormatException ignored) {
            }
        }

        /*
         * Try common filename patterns.
         */

        String lower =
                filename.toLowerCase(
                        Locale.ROOT
                );

        if (lower.matches(
                ".*(^|[^0-9])2160p([^0-9]|$).*"
        )) {
            return 2160;
        }

        if (lower.matches(
                ".*(^|[^0-9])1440p([^0-9]|$).*"
        )) {
            return 1440;
        }

        if (lower.matches(
                ".*(^|[^0-9])1080p([^0-9]|$).*"
        )) {
            return 1080;
        }

        if (lower.matches(
                ".*(^|[^0-9])720p([^0-9]|$).*"
        )) {
            return 720;
        }

        if (lower.matches(
                ".*(^|[^0-9])576p([^0-9]|$).*"
        )) {
            return 576;
        }

        if (lower.matches(
                ".*(^|[^0-9])480p([^0-9]|$).*"
        )) {
            return 480;
        }

        if (lower.matches(
                ".*(^|[^0-9])360p([^0-9]|$).*"
        )) {
            return 360;
        }

        if (lower.matches(
                ".*(^|[^0-9])240p([^0-9]|$).*"
        )) {
            return 240;
        }

        /*
         * If Archive provides dimensions such as
         * width/height in metadata but height was not directly
         * available above, attempt to use the width field only
         * as a final fallback.
         */

        if (!width.isBlank()) {

            try {

                int parsedWidth =
                        Integer.parseInt(
                                width
                        );

                if (parsedWidth >= 3000) {
                    return 2160;
                }

                if (parsedWidth >= 1900) {
                    return 1080;
                }

                if (parsedWidth >= 1200) {
                    return 720;
                }

                if (parsedWidth >= 800) {
                    return 480;
                }

            } catch (NumberFormatException ignored) {
            }
        }

        return 0;
    }

    private int normalizeHeight(
            int height
    ) {

        /*
         * Preserve common actual video heights.
         * Archive metadata sometimes reports values that are
         * slightly different from the marketing resolution.
         */

        if (height >= 2000) {
            return 2160;
        }

        if (height >= 1300) {
            return 1440;
        }

        if (height >= 1000) {
            return 1080;
        }

        if (height >= 650) {
            return 720;
        }

        if (height >= 500) {
            return 576;
        }

        if (height >= 400) {
            return 480;
        }

        if (height >= 300) {
            return 360;
        }

        return 240;
    }

    /*
     * ================================================================
     * QUALITY SELECTION
     * ================================================================
     */

    private VideoFile selectBestQuality(
        List<VideoFile> files,
        int requestedHeight
    ) {

    if (files == null
            || files.isEmpty()) {
        return null;
    }

    /*
     * First preference:
     * exact requested resolution.
     */
    VideoFile exact =
            files.stream()
                    .filter(
                            file ->
                                    file.height
                                            == requestedHeight
                    )
                    .max(
                            Comparator.comparingLong(
                                    (VideoFile file) -> file.size
                            )
                    )
                    .orElse(null);

    if (exact != null) {
        return exact;
    }

    /*
     * Second preference:
     * highest resolution below requested.
     *
     * Example:
     * requested 720p
     * available 480p, 360p, 1080p
     *
     * -> choose 480p
     */
    VideoFile below =
            files.stream()
                    .filter(
                            file ->
                                    file.height
                                            < requestedHeight
                    )
                    .max(
                            Comparator
                                    .comparingInt(
                                            (VideoFile file)
                                                    -> file.height
                                    )
                                    .thenComparingLong(
                                            (VideoFile file)
                                                    -> file.size
                                    )
                    )
                    .orElse(null);

    if (below != null) {
        return below;
    }

    /*
     * Third preference:
     * lowest resolution above requested.
     *
     * This is only used when there is no version
     * at or below the requested quality.
     */
    return files.stream()
            .filter(
                    file ->
                            file.height
                                    > requestedHeight
            )
            .min(
                    Comparator
                            .comparingInt(
                                    (VideoFile file)
                                            -> file.height
                            )
                            .thenComparingLong(
                                    (VideoFile file)
                                            -> file.size
                            )
            )
            .orElse(
                    files.stream()
                            .max(
                                    Comparator.comparingInt(
                                            (VideoFile file)
                                                    -> file.height
                                    )
                            )
                            .orElse(null)
                );
    }
    /*
     * ================================================================
     * URL
     * ================================================================
     */

    private String buildDownloadUrl(
            String identifier,
            String filename
    ) {

        return "https://archive.org/download/"
                + encodePath(identifier)
                + "/"
                + encodePath(filename);
    }

    private String encodePath(
            String value
    ) {

        return URLEncoder
                .encode(
                        value,
                        StandardCharsets.UTF_8
                )
                .replace(
                        "+",
                        "%20"
                );
    }

    /*
     * ================================================================
     * QUALITY PARSING
     * ================================================================
     */

    private int parseQualityHeight(
            String quality
    ) {

        if (quality == null
                || quality.isBlank()) {
            return 720;
        }

        String normalized =
                quality
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        switch (normalized) {

            case "2160p":
            case "4k":
                return 2160;

            case "1440p":
                return 1440;

            case "1080p":
                return 1080;

            case "720p":
                return 720;

            case "576p":
                return 576;

            case "480p":
                return 480;

            case "360p":
                return 360;

            case "240p":
                return 240;

            default:

                String digits =
                        normalized.replaceAll(
                                "[^0-9]",
                                ""
                        );

                if (!digits.isBlank()) {

                    try {

                        int parsed =
                                Integer.parseInt(
                                        digits
                                );

                        if (parsed > 0) {
                            return parsed;
                        }

                    } catch (NumberFormatException ignored) {
                    }
                }

                return 720;
        }
    }

    /*
     * ================================================================
     * YEAR
     * ================================================================
     */

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

    /*
     * ================================================================
     * INTERNAL DATA CLASSES
     * ================================================================
     */

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

    private static class VideoFile {

        private final String name;
        private final int height;
        private final long size;

        private VideoFile(
                String name,
                int height,
                long size
        ) {

            this.name = name;
            this.height = height;
            this.size = size;
        }
    }

    private static class RightsInfo {

        private final String license;
        private final String rights;
        private final String description;
        private final boolean authorized;

        private RightsInfo(
                String license,
                String rights,
                String description,
                boolean authorized
        ) {

            this.license = license;
            this.rights = rights;
            this.description = description;
            this.authorized = authorized;
        }
    }
}
