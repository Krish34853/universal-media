package com.universalmedia;

import com.universalmedia.model.ResolvedStream;
import com.universalmedia.playback.MpvPlayer;

import java.util.Map;

public class MpvTest {

    public static void main(String[] args) {

        System.out.println("==============================");
        System.out.println("MPV TEST");
        System.out.println("==============================");

        ResolvedStream stream = new ResolvedStream(
                "https://www.w3schools.com/html/mov_bbb.mp4",
                null,
                "360p",
                "English",
                Map.of()
        );

        MpvPlayer player = new MpvPlayer();

        try {

            System.out.println("Starting MPV...");

            player.play(stream);

            System.out.println(
                    "Playing: " + player.isPlaying()
            );

        } catch (Exception e) {

            System.err.println(
                    "Playback failed: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}
