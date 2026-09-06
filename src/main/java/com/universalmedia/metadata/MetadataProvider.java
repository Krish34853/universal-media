package com.universalmedia.metadata;

import com.universalmedia.model.Media;

import java.util.List;

public interface MetadataProvider {

    List<Media> search(String query) throws Exception;
}
