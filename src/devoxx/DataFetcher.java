/*
 * Devoxx digital signage project
 */
package devoxx;

import devoxx.JSONParserJP.CallbackAdapter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Class for loading all session data from devoxx server
 *
 * @author Jasper Potts
 */
public class DataFetcher {

  private static final String[] DAY = {
    "monday", "tuesday", "wednesday", "thursday", "friday"
  };

//  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm");
  private final Map<String, Speaker> speakerMap = new HashMap<>();
  private final Map<String, Presentation> presentationMap = new HashMap<>();
  private final List<Presentation> presentations = new ArrayList<>();
  private final Logger logger;
  private final String room;
  private final String devoxxHost;
  private final LocalDate startDate;
  private final String imageCache;

  /**
   * Constructor
   *
   * @param logger Where to log messages to
   * @param controlProperties control properties
   * @param room Which room to get data for
   */
  public DataFetcher(final Logger logger, 
                     final ControlProperties controlProperties, 
                     final String room) {
    this.logger = logger;
    this.room = room;
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
   * Try to update the data from the Devoxx CFP web service
   *
   * @return Whether the update suceeded or failed
   */
  public boolean updateData() {
    logger.log(Level.FINE, "Retrieving data for room {0}", room);

    /* First get all the speakers data */
    String dataUrl;

    try {
      logger.finer("Retrieving speaker data...");
      dataUrl = devoxxHost + "speakers";
      JSONParserJP.download(logger, dataUrl, "speakers.json");
      JSONParserJP.parse(logger, "speakers.json", new SpeakerCallcack());
    } catch (Exception e) {
      logger.severe("Failed to retrieve speaker data!");
      logger.severe(e.getMessage());
      return false;
    }

    logger.log(Level.INFO, "Found [{0}] SPEAKERS", speakerMap.size());

    /* Now retrieve all the session data for the week */
    for (int i = 0; i < 5; i++) {
      try {
        logger.log(Level.FINER, "Retrieving data for {0}", DAY[i]);
        dataUrl = devoxxHost + "rooms/" + room + "/" + DAY[i];
        logger.log(Level.FINEST, "{0} URL = {1}", new Object[]{DAY[i], dataUrl});
        String jsonString = "schedule-" + DAY[i] + ".json";
        JSONParserJP.download(logger, dataUrl, jsonString);
        JSONParserJP.parse(logger, jsonString, new SessionCallcack());
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to retrieve schedule for {0}", DAY[i]);
        logger.severe(e.getMessage());
      }
    }

    if (presentationMap.isEmpty()) {
      logger.severe("Error: No presentation data downloaded!");
      return false;
    }

    logger.log(Level.INFO, "Found [{0}] PRESENTATIONS\n", presentationMap.size());

    /* Sort the presentation by time.  I'm not sure this is really
     * necessary given the size of the data set (SR)
     */
    presentations.clear();
    presentations.addAll(presentationMap.values());
    Collections.sort(presentations,
        (s1, s2) -> s1.fromTime.compareTo(s2.fromTime));
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
    public void keyValue(String key, String value, int depth) {
      if (depth == 2) {
        if (null != key) {
          switch (key) {
            case "uuid":
              uuid = value;
              break;
            case "lastName":
              lastName = value;
              break;
            case "firstName":
              firstName = value;
              break;
            case "bio":
              bio = value;
              break;
            case "company":
              company = value;
              break;
            case "avatarURL":
              imageUrl = value;
              break;
          }
        }
      }
    }

    /**
     * Indicates that the parser ran into end of object '}'
     *
     * @param objectName if this object is value of key/value pair the this is
     * the key name, otherwise its null
     * @param depth The current depth, number of parent objects and arrays that
     * contain this object
     */
    @Override
    public void endObject(String objectName, int depth) {
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
        
      if (depth == 4 && "id".equals(key)) {
        id = value;
      } else if (depth == 4 && "summary".equals(key)) {
        summary = value;
      } else if (depth == 4 && "track".equals(key)) {
        track = value;
      } else if (depth == 4 && "talkType".equals(key)) {
        type = value;
      } else if (depth == 7 && "href".equals(key)) {
        Speaker speaker
            = speakerMap.get(value.substring(value.lastIndexOf('/') + 1));

        if (speaker == null) {
          logger.log(Level.FINER, "Failed to load: {0}", value.substring(value.lastIndexOf('/') + 1));
        } else {
          speakers.add(speaker);
        }
      } else if (depth == 3 && "roomName".equals(key)) {
        room = value;
      } else if (depth == 4 && "title".equals(key)) {
        title = value;
        logger.log(Level.FINEST, "Title = {0}", title);
      } else if (depth == 3 && "fromTimeMillis".equals(key)) {                    
          start = LocalDateTime.ofEpochSecond(Long.parseLong(value) / 1000, 0, ZoneOffset.ofTotalSeconds(3600));    // UTC+1
      } else if (depth == 3 && "toTimeMillis".equals(key)) {
          end = LocalDateTime.ofEpochSecond(Long.parseLong(value) / 1000, 0, ZoneOffset.ofTotalSeconds(3600));      // UTC+1
      } 
    }

    /**
     * Indicates that the parser ran into end of object '}'
     *
     * @param objectName if this object is value of key/value pair the this is
     * the key name, otherwise its null
     * @param depth The current depth, number of parent objects and arrays that
     * contain this object
     */
    @Override
    public void endObject(final String objectName, 
                          final int depth) {
      if (depth == 2 && title != null) {
        /* XXX LETS COME BACK AND FIGURE THIS OUT LATER */
        length = 0;

        Presentation presentation = new Presentation(logger, id, title, room, start, end, length);
        presentationMap.put(id, presentation);
        
        System.out.println("presentation = " + presentation.title + " (" + presentation.fromTime + " - " + presentation.toTime + ")");
        
          presentation.setExtended(
            summary,
            speakers.toArray(new Speaker[speakers.size()]),
            track,
            type);
      }
    }

    /**
     * Indicates that the parser ran into start of object '{'
     *
     * @param objectName if this object is value of key/value pair the this is
     * the key name, otherwise its null
     * @param depth The current depth, number of parent objects and arrays that
     * contain this object
     */
    @Override
    public void startObject(String objectName, int depth) {
      if (depth == 2) {
        speakers.clear();
      }
    }
  }
}
