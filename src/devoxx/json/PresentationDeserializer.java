package devoxx.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import devoxx.ResourceUtil;
import devoxx.model.Presentation;
import devoxx.model.Speaker;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stephan
 */
public class PresentationDeserializer implements JsonDeserializer<Presentation> {

    private static final String SPEAKERJSON = "speaker.json";
    
    private final Map<String, Speaker> speakerMap;
    private final String imageCache;
    
    public PresentationDeserializer(final Map<String, Speaker> speakerMap,
                                    final String imageCache) {
        this.speakerMap = speakerMap;
        this.imageCache = imageCache;
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
                } else {
                    // This is a speaker which has not yet accepted the terms on the CFP !! 
                    // But if still scheduled we might as well load his/her details.
                    loadSpeakerDetails(link, speakers);
                }
            }
            
            // Schedule details
            String roomId = jsonObject.get("roomId").getAsString();
            String fromTime = jsonObject.get("fromTimeMillis").getAsString();
            String toTime = jsonObject.get("toTimeMillis").getAsString();
            String day = jsonObject.get("day").getAsString();

            // 
            // Not sure if this will need to get changed at Devoxx UK : ZoneOffset.ofTotalSeconds(3600)
            //
            // UTC+1
            LocalDateTime start = LocalDateTime.ofEpochSecond(Long.parseLong(fromTime) / 1000, 0, ZoneOffset.ofTotalSeconds(3600));    
            LocalDateTime end = LocalDateTime.ofEpochSecond(Long.parseLong(toTime) / 1000, 0, ZoneOffset.ofTotalSeconds(3600));    

            return new Presentation(id, title, roomId, start, end, 0, summary, speakers.toArray(new Speaker[speakers.size()]), track, talkType);
        } else {
            return null;
        }
    }

    /**
     * Loading a speaker which was not included in the public speakers list (yet).
     * 
     * @param link  the URL for the speaker details 
     * @param speakers  the list of existing speakers
     * @throws JsonParseException 
     */
    private void loadSpeakerDetails(final String link, 
                                    final List<Speaker> speakers) throws JsonParseException {
        try {
            ResourceUtil.download(link, SPEAKERJSON);
        } catch (IOException ex) {
            Logger.getLogger(PresentationDeserializer.class.getName()).log(Level.SEVERE, null, ex);
        }
        try(Reader reader = new InputStreamReader(new FileInputStream(new File(SPEAKERJSON)), "UTF-8")){
            JsonParser parser = new JsonParser();
            
            JsonElement root = parser.parse(reader);
            Speaker notAcceptedSpeaker = new SpeakerDeserializer(imageCache).deserialize(root, null, null);
            notAcceptedSpeaker.cachePhoto();
            speakers.add(notAcceptedSpeaker);
            speakerMap.put(notAcceptedSpeaker.uuid, notAcceptedSpeaker);
        } catch (IOException ex) {
            Logger.getLogger(PresentationDeserializer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
}
