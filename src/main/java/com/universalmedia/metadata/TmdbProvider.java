package com.universalmedia.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universalmedia.model.Media;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TmdbProvider implements MetadataProvider {

    private static final String BASE_URL =
            "https://api.themoviedb.org/3";

    private final String accessToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TmdbProvider(String accessToken) {

        this.accessToken = accessToken;

        this.httpClient =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .build();

        this.objectMapper =
                new ObjectMapper();
    }

    @Override
    public List<Media> search(String query) throws Exception {

        String encodedQuery =
                URLEncoder.encode(
                        query,
                        StandardCharsets.UTF_8
                );

        String url =
                BASE_URL
                        + "/search/multi"
                        + "?query="
                        + encodedQuery
                        + "&include_adult=false"
                        + "&language=en-US"
                        + "&page=1";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
                        .header(
                                "accept",
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

            throw new RuntimeException(
                    "TMDB API error: HTTP "
                            + response.statusCode()
                            + "\n"
                            + response.body()
            );
        }

        return parseResults(response.body());
    }

    private List<Media> parseResults(String json)
            throws Exception {

        List<Media> results =
                new ArrayList<>();

        JsonNode root =
                objectMapper.readTree(json);

        JsonNode resultArray =
                root.get("results");

        if (resultArray == null ||
                !resultArray.isArray()) {

            return results;
        }

        for (JsonNode node : resultArray) {

            String mediaType =
                    node.path("media_type")
                            .asText();

            // TMDB multi-search also returns people.
            // We only want movies and TV shows.
            if (!mediaType.equals("movie")
                    && !mediaType.equals("tv")) {

                continue;
            }

            int id =
                    node.path("id")
                            .asInt();

            String title;
            String releaseDate;

            if (mediaType.equals("movie")) {

                title =
                        node.path("title")
                                .asText("Unknown");

                releaseDate =
                        node.path("release_date")
                                .asText("");

            } else {

                title =
                        node.path("name")
                                .asText("Unknown");

                releaseDate =
                        node.path("first_air_date")
                                .asText("");
            }

            String overview =
                    node.path("overview")
                            .asText("");

            double rating =
                    node.path("vote_average")
                            .asDouble(0.0);

            String posterPath =
                    node.path("poster_path")
                            .asText("");

            results.add(
                    new Media(
                            id,
                            title,
                            mediaType,
                            overview,
                            releaseDate,
                            rating,
                            posterPath,
			    "tmdb"
                    )
            );
        }

        return results;
    }
}
