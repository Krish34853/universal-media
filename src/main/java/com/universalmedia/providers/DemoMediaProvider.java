package com.universalmedia.providers;

import com.universalmedia.model.Media;
import com.universalmedia.model.ResolvedStream;

import java.util.Map;

public class DemoMediaProvider implements MediaProvider {

    private static final String TEST_STREAM =
            "https://www.w3schools.com/html/mov_bbb.mp4";

    @Override
    public String getName() {
        return "Demo Stream Provider";
    }

    @Override
    public boolean supports(Media media) {
        return media != null;
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

        if (quality == null || quality.isBlank()) {
            throw new IllegalArgumentException(
                    "Quality cannot be empty."
            );
        }

        System.out.println(
                "Resolving stream for: "
                        + media.getTitle()
        );

        System.out.println(
                "Requested quality: "
                        + quality
        );

        return new ResolvedStream(
                TEST_STREAM,
                null,
                quality,
                "English",
                Map.of()
        );
    }
}
