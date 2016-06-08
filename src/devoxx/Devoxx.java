/*
 * Devoxx digital signage project
 */
package devoxx;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import static javafx.animation.Animation.INDEFINITE;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 *
 * @author Angie
 */
public class Devoxx extends Application {

    private final static Logger LOGGER = Logger.getLogger(Devoxx.class.getName());
    private final static ConsoleHandler CONSOLDE_HANDLER = new ConsoleHandler();
    
    private ControlProperties controlProperties;
    private FXMLDocumentController screenController;

    private DataFetcher dataFetcher;
    private String roomNumber;
    
    private final List<Presentation> newPresentations = new ArrayList<>();
    private List<Presentation> presentations;
    private Presentation currentPresentation = null;
    private Presentation firstPresentation;
    private Presentation secondPresentation;
    private Presentation thirdPresentation;

    /**
     * Entry point for the JavaFX application life cycle.
     *
     * @param stage Where to present the scene
     * @throws Exception If there is an error
     */
    @Override
    public void start(Stage stage) throws Exception {
        List<String> parameters = getParameters().getRaw();
        String room = parameters.isEmpty() ? null : getParameters().getRaw().get(0).toLowerCase();

        if (room == null || room.isEmpty()) {
            System.out.println("Please specify a room to display");
            System.exit(1);
        }

        String propertiesFile = null;

        if (parameters.size() > 1) {
            propertiesFile = parameters.get(1);
        }

        controlProperties = new ControlProperties(propertiesFile);
        LOGGER.setLevel(controlProperties.getLoggingLevel());
        LOGGER.setUseParentHandlers(false);
        CONSOLDE_HANDLER.setLevel(controlProperties.getLoggingLevel());
        LOGGER.addHandler(CONSOLDE_HANDLER);

        LOGGER.fine("===================================================");
        LOGGER.log(Level.FINE, "=== DEVOXX DISPLAY APP for [{0}]", room);

        if (controlProperties.isTestMode()) {
            LOGGER.finest("=== RUNNING IN TEST MODE...");
        }

        LOGGER.fine("===================================================");

        /* Fetch the data from the Web service */
        dataFetcher = new DataFetcher(LOGGER, controlProperties, room);

        /* If the first read fails we don't really have any way to continue */
        if (!dataFetcher.updateData()) {
            System.err.println("Error retrieving initial data from server");
            System.err.println("Bailing out!");
            System.exit(1);
        }

        presentations = dataFetcher.getPresentationList();

        /**
         * IMPORTANT:
         *
         * If you use this code for a venue that uses different naming for the
         * rooms you will need to change this. Devoxx BE uses rooms with a
         * single digit number and two BOF rooms.
         */
        if (room.startsWith("room")) {
            roomNumber = room.substring("room".length());
        } else if (room.startsWith("bof")) {
            roomNumber = "BOF" + room.substring("bof".length());
        } else {
            LOGGER.severe("Room name not recognised (must be roomX or bofX)");
            System.exit(4);
        }

        FXMLLoader myLoader = new FXMLLoader(getClass().getResource("FXMLDocument.fxml"));
        Parent root = (Parent) myLoader.load();
        screenController = ((FXMLDocumentController) myLoader.getController());

        if (controlProperties.isTestMode()) {
            root.setScaleX(controlProperties.getTestScale());
            root.setScaleY(controlProperties.getTestScale());
        }

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> handleKeyPress(e));
        scene.setFill(null);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        // stage.setFullScreen(true);
        stage.show();

        screenController.setRoom(roomNumber);
        // update();

        /* Use a Timeline to periodically check for any updates to the published
         * data in case of last minute changes
         */
        Timeline downloadTimeline = new Timeline(new KeyFrame(
            Duration.minutes(controlProperties.getDataRefreshTime()),
            (ActionEvent t) -> updateData()));
        downloadTimeline.setCycleCount(INDEFINITE);
        downloadTimeline.play();

        /* Use another timeline to periodically update the screen display so 
         * the time changes and the session information correctly reflects what's
         * happening
         */
        Timeline updateTimeline = new Timeline(new KeyFrame(
            Duration.seconds(controlProperties.getScreenRefreshTime()),
            (ActionEvent t) -> update()));
        updateTimeline.setCycleCount(INDEFINITE);
        updateTimeline.getKeyFrames().get(0).getOnFinished().handle(null);
        updateTimeline.play();
    }

    private void updateData() {
        if (dataFetcher.updateData()) {
            screenController.setOnline();
            update();
        } else {
            screenController.setOffline();
        }
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {        
        launch(args);
    }

    /**
     * Update the display
     */
    private void update() {
        LocalDateTime now;

        if (controlProperties.isTestMode()) {
            now = LocalDateTime.of(
                controlProperties.getStartDate()
                .plusDays(controlProperties.getTestDay()),
                controlProperties.getTestTime());
        } else {
            now = LocalDateTime.now();
        }

        LOGGER.log(Level.FINER, "Date and time of update = {0}", now);
        newPresentations.clear();

        for (Presentation presentation : presentations) {
            if (now.isBefore(presentation.toTime)) {
                newPresentations.add(presentation);
            }

            if (newPresentations.size() >= 3) {
                break;
            }
        }

        firstPresentation = newPresentations.size() >= 1 ? newPresentations.get(0) : null;
        secondPresentation = newPresentations.size() >= 2 ? newPresentations.get(1) : null;
        thirdPresentation = newPresentations.size() >= 3 ? newPresentations.get(2) : null;
        LOGGER.log(Level.FINE, "Screen update @ ({0})", now);

        if (currentPresentation != firstPresentation) {
            currentPresentation = firstPresentation;
            screenController.setScreenData(firstPresentation, secondPresentation, thirdPresentation);
            LOGGER.log(Level.FINER, "New presentation: {0}", firstPresentation);

            if (secondPresentation != null) {
                LOGGER.log(Level.FINER, "Second presentation: {0}", secondPresentation);
            }

            if (thirdPresentation != null) {
                LOGGER.log(Level.FINER, "Third presentation: {0}", thirdPresentation);
            }
        }
    }

    /**
     * Add a simple way to exit the app. Obviously you need to plug a keyboard
     * in, but it's better than pulling out the power lead and hoping you don't
     * corrupt the file system or having to SSH in.
     *
     * @param keyEvent The details of which key was pressed
     */
    private void handleKeyPress(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();
        if (null != code) 
            switch (code) {
            case Q:
                System.exit(0);
            case LEFT:
                controlProperties.decrementTestTime();
                break;
            case RIGHT:
                controlProperties.incrementTestTime();
                break;
            case U:
                update();
                break;
            case D:
                updateData();
                break;
            default:
                break;
        }
    }
}
