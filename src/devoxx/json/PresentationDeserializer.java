package devoxx.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import devoxx.model.Presentation;
import devoxx.model.Speaker;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author stephan
 */
public class PresentationDeserializer implements JsonDeserializer<Presentation> {

    Map<String, Speaker> speakerMap;
    
    public PresentationDeserializer(final Map<String, Speaker> speakerMap) {
        this.speakerMap = speakerMap;
    }
    
    @Override
    public Presentation deserialize(final JsonElement json, 
            final Type type, 
            final JsonDeserializationContext jdc) throws JsonParseException {
                
        final JsonObject jsonObject = json.getAsJsonObject();
        
        JsonElement talk = jsonObject.get("talk");
        if (!talk.isJsonNull()) {
            
            JsonObject theTalk = ((JsonObject)talk);
            
            // Presentation details
            String id = theTalk.get("id").getAsString();
            String title = theTalk.get("title").getAsString();
            String summary = theTalk.get("summary").getAsString();
            String track = theTalk.get("track").getAsString();
            String talkType = theTalk.get("talkType").getAsString();
            
            JsonArray speakersArray = theTalk.get("speakers").getAsJsonArray();            
            
            List<Speaker> speakers = new ArrayList<>();
            
            for (int i = 0; i < speakersArray.size(); i++) {
                JsonObject speakerObj = speakersArray.get(i).getAsJsonObject();
                JsonObject speakerLink = speakerObj.get("link").getAsJsonObject();
                String link = speakerLink.get("href").getAsString();
                Speaker speaker = speakerMap.get(link.substring(link.lastIndexOf('/') + 1));
                if (speaker != null) {
                    speakers.add(speaker);
                }
            }
            
            // Schedule details
            String roomId = jsonObject.get("roomId").getAsString();
            String fromTime = jsonObject.get("fromTimeMillis").getAsString();
            String toTime = jsonObject.get("toTimeMillis").getAsString();
            String day = jsonObject.get("day").getAsString();

            LocalDateTime start = LocalDateTime.ofEpochSecond(Long.parseLong(fromTime) / 1000, 0, ZoneOffset.ofTotalSeconds(3600));    // UTC+1
            LocalDateTime end = LocalDateTime.ofEpochSecond(Long.parseLong(toTime) / 1000, 0, ZoneOffset.ofTotalSeconds(3600));    // UTC+1

            return new Presentation(id, title, roomId, start, end, 0, summary, speakers.toArray(new Speaker[speakers.size()]), track, talkType);
        } else {
            return null;
        }
    }
}
