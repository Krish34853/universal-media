package com.universalmedia;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import com.universalmedia.metadata.TmdbProvider;
import com.universalmedia.metadata.MetadataManager;
import com.universalmedia.metadata.InternetArchiveMetadataProvider;
import com.universalmedia.model.Media;
import com.universalmedia.playback.MpvPlayer;
import com.universalmedia.providers.ProviderManager;
import com.universalmedia.providers.InternetArchiveProvider;
import com.universalmedia.providers.DemoMediaProvider;
import com.universalmedia.providers.BackupDemoProvider;
import com.universalmedia.providers.MediaProvider;

import com.universalmedia.model.ResolvedStream;

import java.util.Arrays;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    // ============================================================
    // APPLICATION FIELDS
    // ============================================================

    private MultiWindowTextGUI textGUI;
    private BasicWindow window;
    private TmdbProvider tmdbProvider;
    private ProviderManager providerManager;
    private InternetArchiveProvider internetArchiveProvider;
    private MpvPlayer mpvPlayer;
    private MetadataManager metadataManager;

    private static final Logger LOGGER =
	    Logger.getLogger(Main.class.getName());

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public Main() {

        String token =
                System.getenv("TMDB_ACCESS_TOKEN");

        if (token == null || token.isBlank()) {

            throw new IllegalStateException(
                    "TMDB_ACCESS_TOKEN environment variable is not set."
            );
        }

        tmdbProvider = new TmdbProvider(token);

	metadataManager = new MetadataManager();
	metadataManager.registerProvider(tmdbProvider);
	metadataManager.registerProvider(new InternetArchiveMetadataProvider());

	providerManager = new ProviderManager();
	internetArchiveProvider = new InternetArchiveProvider();
	providerManager.registerProvider(internetArchiveProvider);

	mpvPlayer = new MpvPlayer();
    }

    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) {

        Main app = new Main();

        app.start();
    }

    // ============================================================
    // START APPLICATION
    // ============================================================

    private void start() {

        try {

            DefaultTerminalFactory terminalFactory =
                    new DefaultTerminalFactory();

            Screen screen =
                    terminalFactory.createScreen();

            screen.startScreen();

            textGUI =
                    new MultiWindowTextGUI(screen);

            showHomeScreen();

            screen.close();

        } catch (Exception e) {

            LOGGER.log(
		    Level.SEVERE,
		    "Application failed to start.",
		    e
	    );
        }
    }

    // ============================================================
    // HOME SCREEN
    // ============================================================

    private void showHomeScreen() {

        window =
                new BasicWindow(
                        "Universal Media Center"
                );

        Panel mainPanel =
                new Panel(
                        new LinearLayout(
                                Direction.VERTICAL
                        )
                );

        // --------------------------------------------------------
        // TITLE
        // --------------------------------------------------------

        Label title =
                new Label(
                        "=== UNIVERSAL MEDIA CENTER ==="
                );

        title.setForegroundColor(
                TextColor.ANSI.CYAN
        );

        mainPanel.addComponent(title);

        mainPanel.addComponent(
                new EmptySpace(
                        new TerminalSize(1, 1)
                )
        );

        // --------------------------------------------------------
        // SEARCH
        // --------------------------------------------------------

	Button searchButton =
        new Button(
                "Search",
                this::showSearchScreen
        );

	mainPanel.addComponent(searchButton);

        // --------------------------------------------------------
        // TV SHOWS
        // --------------------------------------------------------

        Button tvButton =
                new Button(
                        "TV Shows",
                        () -> showComingSoon("TV Shows")
                );

        mainPanel.addComponent(tvButton);

        // --------------------------------------------------------
        // ANIME
        // --------------------------------------------------------

        Button animeButton =
                new Button(
                        "Anime",
                        () -> showComingSoon("Anime")
                );

        mainPanel.addComponent(animeButton);

        // --------------------------------------------------------
        // CONTINUE WATCHING
        // --------------------------------------------------------

        Button continueButton =
                new Button(
                        "Continue Watching",
                        () -> showComingSoon(
                                "Continue Watching"
                        )
                );

        mainPanel.addComponent(continueButton);

        // --------------------------------------------------------
        // FAVORITES
        // --------------------------------------------------------

        Button favoritesButton =
                new Button(
                        "Favorites",
                        () -> showComingSoon(
                                "Favorites"
                        )
                );

        mainPanel.addComponent(favoritesButton);

        // --------------------------------------------------------
        // EXIT
        // --------------------------------------------------------

        mainPanel.addComponent(
                new EmptySpace(
                        new TerminalSize(1, 1)
                )
        );

        Button exitButton =
                new Button(
                        "Exit",
                        window::close
                );

        mainPanel.addComponent(exitButton);

        // --------------------------------------------------------
        // WINDOW
        // --------------------------------------------------------

        window.setComponent(mainPanel);

        window.setHints(
                Arrays.asList(
                        Window.Hint.CENTERED
                )
        );

        textGUI.addWindowAndWait(window);
    }

    // ============================================================
    // SEARCH SCREEN
    // ============================================================

    private void showSearchScreen() {

        BasicWindow searchWindow =
                new BasicWindow(
                        "Search TMDB"
                );

        Panel mainPanel =
                new Panel(
                        new LinearLayout(
                                Direction.VERTICAL
                        )
                );

        // --------------------------------------------------------
        // TITLE
        // --------------------------------------------------------

        Label heading =
                new Label(
                        "Search Movies and TV Shows"
                );

        heading.setForegroundColor(
                TextColor.ANSI.CYAN
        );

        mainPanel.addComponent(heading);

        mainPanel.addComponent(
                new EmptySpace(
                        new TerminalSize(1, 1)
                )
        );

        // --------------------------------------------------------
        // SEARCH BOX
        // --------------------------------------------------------

        TextBox searchBox =
                new TextBox(
                        new TerminalSize(40, 1)
                );

        mainPanel.addComponent(searchBox);

        mainPanel.addComponent(
                new EmptySpace(
                        new TerminalSize(1, 1)
                )
        );

        // --------------------------------------------------------
        // STATUS
        // --------------------------------------------------------

        Label statusLabel =
                new Label(
                        "Enter a title and press Search."
                );

        mainPanel.addComponent(statusLabel);

        mainPanel.addComponent(
                new EmptySpace(
                        new TerminalSize(1, 1)
                )
        );

        // --------------------------------------------------------
        // RESULTS
        // --------------------------------------------------------

        ActionListBox resultsList =
                new ActionListBox(
                        new TerminalSize(60, 12)
                );

        mainPanel.addComponent(resultsList);

        mainPanel.addComponent(
                new EmptySpace(
                        new TerminalSize(1, 1)
                )
        );

        // --------------------------------------------------------
        // SEARCH BUTTON
        // --------------------------------------------------------

	Button searchButton =
        new Button(
                "Search",
                () -> {

                    String query =
                            searchBox
                                    .getText()
                                    .trim();

                    if (query.isBlank()) {

                        statusLabel.setText(
                                "Please enter a search term."
                        );

                        return;
                    }

                    statusLabel.setText(
                            "Searching TMDB and Internet Archive..."
                    );

                    resultsList.clearItems();

                    try {

                        List<Media> results =
                                metadataManager.search(query);

                        int totalResults =
                                results.size();

                        if (totalResults == 0) {

                            statusLabel.setText(
                                    "No results found."
                            );

                            return;
                        }

                        statusLabel.setText(
                                "Results found: "
                                        + totalResults
                        );

    List<Media> tmdbResults =
        results.stream()
                .filter(media ->
                        "tmdb".equalsIgnoreCase(
                                media.getProviderId()
                        )
                )
                .toList();

    List<Media> archiveResults =
        results.stream()
                .filter(media ->
                        "internet_archive".equalsIgnoreCase(
                                media.getProviderId()
                        )
                )
                .toList();

     if (!tmdbResults.isEmpty()) {

    resultsList.addItem(
            "========== TMDB ==========",
            () -> {}
    );

    for (Media media : tmdbResults) {

        String label =
                media.getTitle()
                        + " ["
                        + media.getMediaType()
                        + "]";

        resultsList.addItem(
                label,
                () -> showMediaDetails(
                        media,
                        searchWindow
                )
        );
    }
    }

    if (!archiveResults.isEmpty()) {

    resultsList.addItem(
            "===== INTERNET ARCHIVE =====",
            () -> {}
    );

    for (Media media : archiveResults) {

        String label =
                media.getTitle()
                        + " [Internet Archive]";

        resultsList.addItem(
                label,
                () -> showMediaDetails(
                        media,
                        searchWindow
                )
        );
    }
    }

                    } catch (Exception e) {

                        statusLabel.setText(
                                "Search request failed."
                        );

                        	MessageDialog.showMessageDialog(
                                	textGUI,
                                	"Search Error",
                                	e.getMessage(),
                        	        MessageDialogButton.OK
                	        );
        	            }
	                }
        	);

	mainPanel.addComponent(searchButton);

        // --------------------------------------------------------
        // BACK BUTTON
        // --------------------------------------------------------

        Button backButton =
                new Button(
                        "Back",
                        searchWindow::close
                );

        mainPanel.addComponent(backButton);

        // --------------------------------------------------------
        // WINDOW
        // --------------------------------------------------------

        searchWindow.setComponent(mainPanel);

        searchWindow.setHints(
                Arrays.asList(
                        Window.Hint.CENTERED
                )
        );

        textGUI.addWindowAndWait(searchWindow);
    }

    // ============================================================
    // MEDIA DETAILS SCREEN
    // ============================================================

    private void showMediaDetails(
        Media media,
        BasicWindow searchWindow
    ) {
    BasicWindow detailsWindow =
            new BasicWindow(
                    media.getTitle()
            );
    //Pressing ESC closes only this Window.
    detailsWindow.setCloseWindowWithEscape(true);

    Panel mainPanel =
            new Panel(
                    new LinearLayout(
                            Direction.VERTICAL
                    )
            );

    Label titleLabel =
            new Label(
                    media.getTitle()
            );

    titleLabel.setForegroundColor(
            TextColor.ANSI.CYAN
    );

    mainPanel.addComponent(titleLabel);

    mainPanel.addComponent(
            new EmptySpace(
                    new TerminalSize(1, 1)
            )
    );

    String releaseDate =
            media.getReleaseDate();

    if (releaseDate == null
            || releaseDate.isBlank()) {
        releaseDate = "Unknown";
    }

    String mediaType =
            media.getMediaType();

    String rating =
            String.format(
                    "%.2f",
                    media.getRating()
            );

    Label infoLabel =
            new Label(
                    "Type: "
                            + mediaType
                            + "    |    Rating: "
                            + rating
                            + "    |    Release: "
                            + releaseDate
            );

    mainPanel.addComponent(infoLabel);

    mainPanel.addComponent(
            new EmptySpace(
                    new TerminalSize(1, 1)
            )
    );

    Label descriptionLabel =
            new Label(
                    "Description"
            );

    descriptionLabel.setForegroundColor(
            TextColor.ANSI.YELLOW
    );

    mainPanel.addComponent(
            descriptionLabel
    );

    mainPanel.addComponent(
            new EmptySpace(
                    new TerminalSize(1, 1)
            )
    );

    String overview =
            media.getOverview();

    if (overview == null
            || overview.isBlank()) {
        overview =
                "No description available.";
    }

    TextBox overviewBox =
            new TextBox(
                    new TerminalSize(70, 7)
            );

    overviewBox.setText(overview);
    overviewBox.setReadOnly(true);
    overviewBox.setEnabled(false);

    mainPanel.addComponent(overviewBox);

    mainPanel.addComponent(
            new EmptySpace(
                    new TerminalSize(1, 1)
            )
    );

    Panel buttonPanel =
            new Panel(
                    new LinearLayout(
                            Direction.HORIZONTAL
                    )
            );

    Button playButton =
            new Button(
                    "Play",
                    () -> showQualitySelection(media)
            );

    buttonPanel.addComponent(playButton);

    Button favoriteButton =
            new Button(
                    "Favorite",
                    () -> showComingSoon(
                            "Favorites"
                    )
            );

    buttonPanel.addComponent(favoriteButton);

    Button backButton =
            new Button(
                    "Back to Search",
		    () -> {
			detailsWindow.close();
			searchWindow.setVisible(true);
                    }
            );

    buttonPanel.addComponent(backButton);

    mainPanel.addComponent(buttonPanel);

    detailsWindow.setComponent(mainPanel);

    detailsWindow.setHints(
            Arrays.asList(
                    Window.Hint.CENTERED
            )
    );

    playButton.takeFocus();

    /*
     * IMPORTANT:
     *
     * Do NOT use addWindowAndWait() here.
     *
     * The search window is already active.
     * We simply put the details window on top of it.
     */
    textGUI.addWindow(detailsWindow);
    }

    // ============================================================
    // COMING SOON
    // ============================================================

    private void showComingSoon(
            String feature
    ) {

        MessageDialog.showMessageDialog(
                textGUI,
                feature,
                feature
                        + " feature is coming soon.",
                MessageDialogButton.OK
        );
    }

    // ===========================================================
    //  PLAY MEDIA
    // ===========================================================

private void playMedia(
        Media media,
        String quality
) {

    try {

        MediaProvider provider =
                providerManager.findProvider(media);

        if (provider == null) {

            MessageDialog.showMessageDialog(
                    textGUI,
                    "Playback Error",
                    "No provider available for this media.",
                    MessageDialogButton.OK
            );

            return;
        }

        System.out.println(
                "Requested quality: " + quality
        );

        ResolvedStream stream =
                providerManager.resolveStream(
                        media,
                        quality
                );

        System.out.println(
                "Provider: " + provider.getName()
        );

        System.out.println(
                "Stream: " + stream.getUrl()
        );

        System.out.println(
                "Quality: " + stream.getQuality()
        );

        System.out.println(
                "Audio: " + stream.getAudioLanguage()
        );

        System.out.println(
                "Subtitles: " + stream.getSubtitleUrl()
        );

        System.out.println(
                "Headers: " + stream.getHeaders()
        );

        mpvPlayer.play(stream);

        } catch (Exception e) {

            LOGGER.log(
		    Level.WARNING,
		    "Playback failed for media: "
			    + media.getTitle()
			    + " at quality: "
			    + quality,
		    e
	    );

	    String message =
                    e.getMessage();

		    if (message == null || message.isBlank()) {
			message =
				"Unable to play this media.";
		    }

		    MessageDialog.showMessageDialog(
				textGUI,
				"Playback Error",
				message,
				MessageDialogButton.OK
		    );
            }
        }
    // ======================================================
    // Quality Selection
    // ======================================================

    private void showQualitySelection(Media media) {

    BasicWindow qualityWindow =
            new BasicWindow("Select Quality");

    // Esc closes ONLY the quality window
    qualityWindow.setCloseWindowWithEscape(true);

    Panel mainPanel =
            new Panel(
                    new LinearLayout(Direction.VERTICAL)
            );

    Label heading =
            new Label(
                    "Select video quality for:\n"
                            + media.getTitle()
            );

    mainPanel.addComponent(heading);

    mainPanel.addComponent(
            new Label("")
    );

    ActionListBox qualityList =
            new ActionListBox();

    qualityList.addItem(
            "1080p",
            () -> {
                qualityWindow.close();
                playMedia(media, "1080p");
            }
    );

    qualityList.addItem(
            "720p",
            () -> {
                qualityWindow.close();
                playMedia(media, "720p");
            }
    );

    qualityList.addItem(
            "480p",
            () -> {
                qualityWindow.close();
                playMedia(media, "480p");
            }
    );

    qualityList.addItem(
            "360p",
            () -> {
                qualityWindow.close();
                playMedia(media, "360p");
            }
    );

    mainPanel.addComponent(qualityList);

    mainPanel.addComponent(
            new Label("")
    );

    // Back closes ONLY this window
    Button backButton =
            new Button(
                    "Back",
                    qualityWindow::close
            );

    mainPanel.addComponent(backButton);

    // Start keyboard focus on the quality list
    qualityList.takeFocus();

    qualityWindow.setComponent(mainPanel);

    /*
     * addWindowAndWait() keeps the Details window underneath.
     *
     * When qualityWindow closes:
     * - Esc → closes qualityWindow
     * - Back → closes qualityWindow
     * - Details window remains open
     */
    textGUI.addWindowAndWait(qualityWindow);
    }
}
