package com.universalmedia;

import com.universalmedia.model.Media;
import com.universalmedia.model.ResolvedStream;
import com.universalmedia.providers.DemoMediaProvider;
import com.universalmedia.providers.MediaProvider;
import com.universalmedia.providers.ProviderManager;

public class ProviderTest {

    public static void main(String[] args) throws Exception {

        System.out.println("==============================");
        System.out.println("PROVIDER SYSTEM TEST");
        System.out.println("==============================");

        ProviderManager manager = new ProviderManager();

        MediaProvider provider =
                new DemoMediaProvider();

        manager.registerProvider(provider);

        Media media = new Media(
                123,
                "Test Movie",
                "movie",
                "Test description",
                "2026-01-01",
                8.5,
                null,
		""
        );

        System.out.println();
        System.out.println("Finding provider...");

        MediaProvider found =
                manager.findProvider(media);

        if (found == null) {
            throw new RuntimeException(
                    "No provider found."
            );
        }

        System.out.println(
                "Found provider: "
                        + found.getName()
        );

        System.out.println();
        System.out.println("Resolving stream...");

        ResolvedStream stream =
                manager.resolveStream(
                        media,
                        "720p"
                );

        System.out.println();
        System.out.println("Stream resolved successfully.");
        System.out.println("URL: " + stream.getUrl());
        System.out.println("Quality: " + stream.getQuality());
        System.out.println("Audio: " + stream.getAudioLanguage());
        System.out.println("Subtitles: " + stream.getSubtitleUrl());
        System.out.println("Headers: " + stream.getHeaders());

        System.out.println();
        System.out.println("==============================");
        System.out.println("TEST PASSED");
        System.out.println("==============================");
    }
}
