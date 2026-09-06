package com.universalmedia.model;

public class Media {

    private final int id;
    private final String title;
    private final String mediaType;
    private final String overview;
    private final String releaseDate;
    private final double rating;
    private final String posterPath;
    private final String providerId;

    public Media(
            int id,
            String title,
            String mediaType,
            String overview,
            String releaseDate,
            double rating,
            String posterPath,
	    String providerId
    ) {
        this.id = id;
        this.title = title;
        this.mediaType = mediaType;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.posterPath = posterPath;
	this.providerId =  providerId;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getOverview() {
        return overview;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public double getRating() {
        return rating;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public String getProviderId() {
	return providerId;
    }

    @Override
    public String toString() {
        return title;
    }
}
