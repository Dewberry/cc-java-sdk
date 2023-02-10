package usace.wat.plugin;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
public class DataStore {
    @JsonProperty
    private String name;
    @JsonProperty
    private String id;
    @JsonProperty
    private Map<String, String> parameters;
    @JsonProperty
    private StoreType store_type;
    @JsonProperty
    private String dsProfile;
    @JsonProperty
    private Object session;
    
    public String getName(){
        return name;
    }
    public String getId(){
        return id;
    }
    public StoreType getStoreType(){
        return store_type;
    }
    public Map<String, String> getParameters(){
        return parameters;
    }
    public String getDsProfile(){
        return dsProfile;
    }
    public Object getSession(){
        return session;
    }

}