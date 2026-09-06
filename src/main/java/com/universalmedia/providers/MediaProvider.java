package com.universalmedia.providers;

import com.universalmedia.model.Media;
import com.universalmedia.model.ResolvedStream;

public interface MediaProvider {

    String getName();

    boolean supports(Media media);

    ResolvedStream getStream(Media media, String quality) throws Exception;
}
