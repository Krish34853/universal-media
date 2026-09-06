package com.universalmedia.model;

import java.util.Map;

public class ResolvedStream {

    private final String url;
    private final String subtitleUrl;
    private final String quality;
    private final String audioLanguage;
    private final Map<String, String> headers;

    public ResolvedStream(
            String url,
            String subtitleUrl,
            String quality,
            String audioLanguage,
            Map<String, String> headers
    ) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Stream URL cannot be empty.");
        }

        this.url = url;
        this.subtitleUrl = subtitleUrl;
        this.quality = quality;
        this.audioLanguage = audioLanguage;
        this.headers = headers == null
                ? Map.of()
                : Map.copyOf(headers);
    }

    public String getUrl() {
        return url;
    }

    public String getSubtitleUrl() {
        return subtitleUrl;
    }

    public String getQuality() {
        return quality;
    }

    public String getAudioLanguage() {
        return audioLanguage;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
