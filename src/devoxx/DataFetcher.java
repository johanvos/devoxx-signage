/*
 * Devoxx digital signage project
 */
package devoxx;

import devoxx.JSONParserJP.CallbackAdapter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for loading all session data from devoxx server
 *
 * @author Jasper Potts
 */
public class DataFetcher {

    // JSON file names
    private static final String SPEAKERS_JSON = "speakers.json";

    // JSON depth level
    private static final int DEPTH_LEVEL_2 = 2;
    private static final int DEPTH_LEVEL_3 = 3;
    private static final int DEPTH_LEVEL_4 = 4;
    private static final int DEPTH_LEVEL_7 = 7;

    // JSON Keys values
    private static final String KEY_HREF = "href";
    private static final String KEY_TALK_TYPE = "talkType";
    private static final String KEY_TRACK = "track";
    private static final String KEY_SUMMARY = "summary";
    private static final String KEY_ID = "id";
    private static final String KEY_TO_TIME_MILLIS = "toTimeMillis";
    private static final String KEY_FROM_TIME_MILLIS = "fromTimeMillis";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ROOM_NAME = "roomName";
    private static final String KEY_AVATAR_URL = "avatarURL";
    private static final String KEY_COMPANY = "company";
    private static final String KEY_BIO = "bio";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_UUID = "uuid";

    private static final String[] DAYS = {"monday", "tuesday", "wednesday", "thursday", "friday"};

//  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm");
    private final Map<String, Speaker> speakerMap = new HashMap<>();
    private final Map<String, Presentation> presentationMap = new HashMap<>();
    private final List<Presentation> presentations = new ArrayList<>();
    private final Logger logger;
    private final String roomId;
    private final String devoxxHost;
    private final LocalDate startDate;
    private final String imageCache;

    /**
     * Constructor
     *
     * @param logger Where to log messages to
     * @param controlProperties control properties
     * @param roomId Which room to get data for
     */
    public DataFetcher(final Logger logger,
            final ControlProperties controlProperties,
            final String roomId) {
        this.logger = logger;
        this.roomId = roomId;
        devoxxHost = controlProperties.getDevoxxHost();
        imageCache = controlProperties.getImageCache();
        startDate = controlProperties.getStartDate();
    }

    /**
     * Get the list of presentations for the chosen room
     *
     * @return
     */
    public List<Presentation> getPresentationList() {
        return presentations;
    }

    /**
     * Try to update the data from the Devoxx CFP web service.
     *
     * @return Whether the update succeeded or failed
     */
    public boolean updateData() {
        logger.log(Level.FINE, "Retrieving data for room {0}", roomId);

        if (!retrieveSpeakerDetails()) {
            return false;
        }

        if (!retrieveScheduleDetails()) {
            return false;
        }

        sortPresentations();

        return true;
    }

    /* Sort the presentation by time.  I'm not sure this is really
     * necessary given the size of the data set (SR)
     */
    private void sortPresentations() {
        presentations.clear();
        presentations.addAll(presentationMap.values());
        Collections.sort(presentations, (s1, s2) -> s1.fromTime.compareTo(s2.fromTime));
    }

    /**
     * Retrieve all the session data for the week.
     *
     * @return true when successful
     */
    private boolean retrieveScheduleDetails() {
        for (int dayNbr = 0; dayNbr < DAYS.length; dayNbr++) {
            try {
                logger.log(Level.FINER, "Retrieving data for {0}", DAYS[dayNbr]);
                String dataUrl = devoxxHost + "rooms/" + roomId + "/" + DAYS[dayNbr];

                logger.log(Level.FINEST, "{0} URL = {1}", new Object[]{DAYS[dayNbr], dataUrl});
                String jsonString = "schedule-" + DAYS[dayNbr] + ".json";

                JSONParserJP.download(logger, dataUrl, jsonString);
                JSONParserJP.parse(logger, jsonString, new SessionCallcack());

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to retrieve schedule for {0}", DAYS[dayNbr]);
                logger.severe(e.getMessage());
            }
        }

        if (presentationMap.isEmpty()) {
            logger.severe("Error: No presentation data downloaded!");
            return false;
        }

        logger.log(Level.INFO, "Found [{0}] PRESENTATIONS\n", presentationMap.size());
        return true;
    }

    /**
     * Retrieve all the speakers data.
     *
     * @return true when successful
     */
    private boolean retrieveSpeakerDetails() {
        logger.finer("Retrieving speaker data...");

        try {
            JSONParserJP.download(logger, devoxxHost + "speakers", SPEAKERS_JSON);
            JSONParserJP.parse(logger, SPEAKERS_JSON, new SpeakerCallcack());

        } catch (Exception e) {
            logger.severe("Failed to retrieve speaker data!");
            logger.severe(e.getMessage());
            return false;
        }

        logger.log(Level.INFO, "Found [{0}] SPEAKERS", speakerMap.size());
        return true;
    }

    /**
     * Callback class for handling Devoxx speaker JSON data
     */
    private class SpeakerCallcack extends CallbackAdapter {

        private String uuid;
        private String firstName;
        private String lastName;
        private String company;
        private String bio;
        private String imageUrl;
        private String twitter;
        private boolean rockStar;

        /**
         * Key value pair detected in the JSON data
         *
         * @param key The key
         * @param value The value
         * @param depth The depth of the key/value
         */
        @Override
        public void keyValue(final String key,
                final String value,
                final int depth) {
            if (DEPTH_LEVEL_2 == depth) {
                if (null != key) {
                    switch (key) {
                        case KEY_UUID:
                            uuid = value;
                            break;
                        case KEY_LAST_NAME:
                            lastName = value;
                            break;
                        case KEY_FIRST_NAME:
                            firstName = value;
                            break;
                        case KEY_BIO:
                            bio = value;
                            break;
                        case KEY_COMPANY:
                            company = value;
                            break;
                        case KEY_AVATAR_URL:
                            imageUrl = value;
                            break;
                    }
                }
            }
        }

        /**
         * Indicates that the parser ran into end of object '}'
         *
         * @param objectName if this object is value of key/value pair the this
         * is the key name, otherwise its null
         * @param depth The current depth, number of parent objects and arrays
         * that contain this object
         */
        @Override
        public void endObject(final String objectName,
                final int depth) {

            logger.log(Level.FINEST, "End speaker object found: {0} {1}", new Object[]{firstName, lastName});

            if (depth == 1) {
                Speaker speaker = speakerMap.get(uuid);

                if (speaker == null) {
                    logger.finest("Speaker is null, adding new speaker");
                    speaker = new Speaker(logger, uuid, firstName + " " + lastName, imageUrl, imageCache);
                    speaker.cachePhoto();
                    speakerMap.put(uuid, speaker);
                }
            }
        }
    }

    /**
     * Callback class for handling Devoxx session JSON data
     */
    private class SessionCallcack extends CallbackAdapter {

        private final List<Speaker> speakers = new ArrayList<>();
        public String id;
        private String summary;
        private String type;
        private String track;
        private String title;
        private String room;
        private LocalDateTime start;
        private LocalDateTime end;
        private int length;

        /**
         * Key value pair detected in the JSON data
         *
         * @param key The key
         * @param value The value
         * @param depth The depth of the key/value
         */
        @Override
        public void keyValue(final String key,
                final String value,
                final int depth) {

            if (depth == DEPTH_LEVEL_4 && KEY_ID.equals(key)) {
                id = value;
            } else if (depth == DEPTH_LEVEL_4 && KEY_SUMMARY.equals(key)) {
                summary = value;
            } else if (depth == DEPTH_LEVEL_4 && KEY_TRACK.equals(key)) {
                track = value;
            } else if (depth == DEPTH_LEVEL_4 && KEY_TALK_TYPE.equals(key)) {
                type = value;
            } else if (depth == DEPTH_LEVEL_7 && KEY_HREF.equals(key)) {
                Speaker speaker
                        = speakerMap.get(value.substring(value.lastIndexOf('/') + 1));

                if (speaker == null) {
                    logger.log(Level.FINER, "Failed to load: {0}", value.substring(value.lastIndexOf('/') + 1));
                } else {
                    speakers.add(speaker);
                }
            } else if (depth == DEPTH_LEVEL_3 && KEY_ROOM_NAME.equals(key)) {
                room = value;
            } else if (depth == DEPTH_LEVEL_4 && KEY_TITLE.equals(key)) {
                title = value;
                logger.log(Level.FINEST, "Title = {0}", title);
            } else if (depth == DEPTH_LEVEL_3 && KEY_FROM_TIME_MILLIS.equals(key)) {
                start = LocalDateTime.ofEpochSecond(Long.parseLong(value) / 1000, 0, ZoneOffset.ofTotalSeconds(3600));    // UTC+1
            } else if (depth == DEPTH_LEVEL_3 && KEY_TO_TIME_MILLIS.equals(key)) {
                end = LocalDateTime.ofEpochSecond(Long.parseLong(value) / 1000, 0, ZoneOffset.ofTotalSeconds(3600));      // UTC+1
            }
        }

        /**
         * Indicates that the parser ran into end of object '}'
         *
         * @param objectName if this object is value of key/value pair the this
         * is the key name, otherwise its null
         * @param depth The current depth, number of parent objects and arrays
         * that contain this object
         */
        @Override
        public void endObject(final String objectName,
                final int depth) {
            if (DEPTH_LEVEL_2 == depth && title != null) {
                /* XXX LETS COME BACK AND FIGURE THIS OUT LATER */
                length = 0;

                Presentation presentation = new Presentation(logger, id, title, room, start, end, length);
                presentationMap.put(id, presentation);

                presentation.setExtended(
                        summary,
                        speakers.toArray(new Speaker[speakers.size()]),
                        track,
                        type);
                
                // Show presentation details to stdout
                System.out.print("presentation = " + presentation.title + " by ");
                for (Speaker speaker : presentation.speakers) {
                    System.out.print(speaker.fullName + " ");
                }
                System.out.println(" (" + presentation.fromTime + " - " + presentation.toTime + ")");
            }
        }

        /**
         * Indicates that the parser ran into start of object '{'
         *
         * @param objectName if this object is value of key/value pair the this
         * is the key name, otherwise its null
         * @param depth The current depth, number of parent objects and arrays
         * that contain this object
         */
        @Override
        public void startObject(final String objectName,
                final int depth) {
            if (DEPTH_LEVEL_2 == depth) {
                speakers.clear();
            }
        }
    }
}
