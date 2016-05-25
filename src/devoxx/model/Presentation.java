/*
 * Devoxx digital signage project
 */
package devoxx.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * Model object for conference Speaker.
 *
 * @author Jasper Potts
 */
public class Presentation {

    public String id;
    public String title;
    public String room;
    public LocalDateTime fromTime;
    public LocalDateTime toTime;
    public int length;

    public String summary;
    public Speaker[] speakers;
    public String track;
    public String type;

    public Presentation() {
        super();   
    }           
    
    /**
     * Constructor
     *
     * @param id The id of the presentation
     * @param title The title of the presentation
     * @param room Which room the presentation is in
     * @param fromTime What time the presentation starts
     * @param toTime What time the presentation ends
     * @param length How long the presentation is
     */
    public Presentation(final String id, 
                        final String title, 
                        final String room,
                        final LocalDateTime fromTime, 
                        final LocalDateTime toTime, 
                        final int length,
                        final String summary, 
                        final Speaker[] speakers,
                        final String track, 
                        final String type) {
        this.id = id;
        this.title = title;
        this.room = room;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.length = length;
        this.summary = summary;
        this.speakers = speakers;
        this.track = track;
        this.type = type;
    }
    
    public String getSpeakerList() {
        final StringJoiner sj = new StringJoiner(", ");            
            for (Speaker speaker : speakers) {                    
                sj.add(speaker.fullName);
            }
        return "by " + sj.toString();
    }

    /**
     * Create a nice readable string representation of the object
     *
     * @return A string containing the model data
     */
    @Override
    public String toString() {
        return "Presentation{" + "id=" + id
            + ", room=" + room
            + ", fromTime=" + fromTime
            + ", toTime=" + toTime
            + ", speakers=" + Arrays.toString(speakers)
            + '}';
    }
}
