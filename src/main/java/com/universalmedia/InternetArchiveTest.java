package com.universalmedia;

import com.universalmedia.model.Media;
import com.universalmedia.model.ResolvedStream;
import com.universalmedia.providers.InternetArchiveProvider;

public class InternetArchiveTest {

    public static void main(String[] args) throws Exception {

        System.out.println("==============================");
        System.out.println("INTERNET ARCHIVE PROVIDER TEST");
        System.out.println("==============================");

        InternetArchiveProvider provider =
                new InternetArchiveProvider();

        Media movie = new Media(
                1,
                "Night of the Living Dead",
                "movie",
                "Public domain horror film.",
                "1968-01-01",
                7.0,
                null,
		""
        );

        System.out.println();
        System.out.println(
                "Provider: "
                        + provider.getName()
        );

        System.out.println(
                "Supports movie: "
                        + provider.supports(movie)
        );

        System.out.println();
        System.out.println("Resolving stream...");

        ResolvedStream stream =
                provider.getStream(
                        movie,
                        "720p"
                );

        System.out.println();
        System.out.println("STREAM FOUND");
        System.out.println("------------------------------");
        System.out.println(
                "URL: "
                        + stream.getUrl()
        );
        System.out.println(
                "Quality: "
                        + stream.getQuality()
        );
        System.out.println(
                "Audio: "
                        + stream.getAudioLanguage()
        );
        System.out.println(
                "Subtitles: "
                        + stream.getSubtitleUrl()
        );
        System.out.println("------------------------------");

        System.out.println();
        System.out.println("TEST PASSED");
    }
}
