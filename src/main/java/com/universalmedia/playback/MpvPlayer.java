package com.universalmedia.playback;

import com.universalmedia.model.ResolvedStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MpvPlayer {

    private Process process;

    public void play(ResolvedStream stream) throws IOException {

        if (stream == null) {
            throw new IllegalArgumentException("Resolved stream cannot be null.");
        }

        List<String> command = new ArrayList<>();

        command.add("mpv");
        command.add("--force-window=yes");

        // --------------------------------------------------
        // Audio Language
        // --------------------------------------------------

        if (stream.getAudioLanguage() != null
                && !stream.getAudioLanguage().isBlank()) {

            command.add(
                    "--alang=" + stream.getAudioLanguage()
            );
        }

        // --------------------------------------------------
        // Subtitle
        // --------------------------------------------------

        if (stream.getSubtitleUrl() != null
                && !stream.getSubtitleUrl().isBlank()) {

            command.add(
                    "--sub-file=" + stream.getSubtitleUrl()
            );
        }

        // --------------------------------------------------
        // HTTP Headers
        // --------------------------------------------------

        Map<String, String> headers = stream.getHeaders();

        if (headers != null && !headers.isEmpty()) {

            StringBuilder headerFields =
                    new StringBuilder();

            for (Map.Entry<String, String> entry
                    : headers.entrySet()) {

                if (headerFields.length() > 0) {
                    headerFields.append(",");
                }

                headerFields
                        .append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue());
            }

            command.add(
                    "--http-header-fields="
                            + headerFields
            );
        }

        // --------------------------------------------------
        // Media URL
        // --------------------------------------------------

        command.add(stream.getUrl());

        // --------------------------------------------------
        // Debug information
        // --------------------------------------------------

        System.out.println("Starting MPV...");
        System.out.println("URL: " + stream.getUrl());

        System.out.println(
                "Quality: " + stream.getQuality()
        );

        System.out.println(
                "Audio: " + stream.getAudioLanguage()
        );

        System.out.println(
                "Subtitle: " + stream.getSubtitleUrl()
        );

        System.out.println(
                "Headers: " + stream.getHeaders()
        );

        // --------------------------------------------------
        // Start MPV
        // --------------------------------------------------

        process = new ProcessBuilder(command)
                .inheritIO()
                .start();
    }

    public boolean isPlaying() {
        return process != null && process.isAlive();
    }

    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }
}
