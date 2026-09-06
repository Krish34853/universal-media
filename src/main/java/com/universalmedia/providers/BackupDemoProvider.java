package com.universalmedia.providers;

import com.universalmedia.model.Media;
import com.universalmedia.model.ResolvedStream;

import java.util.Map;

public class BackupDemoProvider implements MediaProvider {

    private static final String TEST_STREAM =
            "https://www.w3schools.com/html/mov_bbb.mp4";

    @Override
    public String getName() {
        return "Backup Demo Provider";
    }

    @Override
    public boolean supports(Media media) {
        return media != null;
    }

    @Override
    public ResolvedStream getStream(
            Media media,
            String quality
    ) {

        System.out.println(
                "Backup provider resolving: "
                        + media.getTitle()
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
