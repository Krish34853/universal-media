package com.universalmedia;

import com.universalmedia.metadata.TmdbProvider;
import com.universalmedia.model.Media;

import java.util.List;

public class TmdbTest {

    public static void main(String[] args) {

    	System.out.println("############################");
    	System.out.println("TMDB TEST CLASS IS RUNNING");
    	System.out.println("############################");

        String token =
                System.getenv("TMDB_ACCESS_TOKEN");

        if (token == null || token.isBlank()) {

            System.out.println(
                    "ERROR: TMDB_ACCESS_TOKEN is NOT set."
            );

            return;
        }

        System.out.println(
                "TMDB token found."
        );

        try {

            TmdbProvider tmdb =
                    new TmdbProvider(token);

            System.out.println(
                    "Created TmdbProvider."
            );

            System.out.println(
                    "Searching TMDB for: Batman"
            );

            List<Media> results =
                    tmdb.search("Batman");

            System.out.println(
                    "TMDB request completed."
            );

            System.out.println(
                    "Number of results: "
                            + results.size()
            );

            System.out.println();

            for (Media media : results) {

                System.out.println(
                        media.getTitle()
                                + " | "
                                + media.getMediaType()
                                + " | Rating: "
                                + media.getRating()
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "ERROR WHILE CONNECTING TO TMDB:"
            );

            e.printStackTrace();
        }

        System.out.println();
        System.out.println("================================");
        System.out.println("TMDB TEST FINISHED");
        System.out.println("================================");
    }
}
