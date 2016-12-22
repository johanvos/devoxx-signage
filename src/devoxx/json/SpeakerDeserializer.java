package devoxx.json;


import devoxx.model.Speaker;
import java.net.Proxy.Type;

/**
 *
 * @author stephan
 */
public class SpeakerDeserializer {}
//implements JsonDeserializer<Speaker> {
//    
//    private String imageCache;
//    
//    public SpeakerDeserializer(String imageCache) {
//        this.imageCache = imageCache;
//    }
//    
//    @Override
//    public Speaker deserialize(final JsonElement json, 
//            final java.lang.reflect.Type type, 
//            final JsonDeserializationContext jdc) throws JsonParseException {
//                
//        final JsonObject jsonObject = json.getAsJsonObject();
//
//        final String uuid = jsonObject.get("uuid").getAsString();
//        
//        final String firstname = jsonObject.get("firstName").getAsString();
//        final String lastname = jsonObject.get("lastName").getAsString();
//        
//        String downloadURL = "";
//        JsonElement avatarURL = jsonObject.get("avatarURL");
//        if (!avatarURL.isJsonNull()) {
//            downloadURL = avatarURL.getAsString();        
//        }
//        
//        Speaker speaker = new Speaker(uuid, firstname + " " + lastname, downloadURL, imageCache);    
//        speaker.cachePhoto();
//        return speaker;
//    }    
//}
