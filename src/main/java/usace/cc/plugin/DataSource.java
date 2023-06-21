package usace.cc.plugin;
import com.fasterxml.jackson.annotation.JsonProperty;
public class DataSource {
    @JsonProperty
    private String Name;
    @JsonProperty
    private String ID;
    @JsonProperty
    private String StoreName;
    @JsonProperty
    private String[] Paths;
    @JsonProperty
    private String[] DataPaths;
    public String getId(){
        return ID;
    }
    public String getName(){
        return Name;
    }
    public String[] getPaths(){
        return Paths;
    }
    public String[] getDataPaths(){
        return DataPaths;
    }
    public String getStoreName(){
        return StoreName;
    }
    public DataSource UpdatePaths(){
        DataSource dest = this;
        PluginManager pm = PluginManager.getInstance();
        for(int j=0; j<this.getPaths().length;j++){
                dest.getPaths()[j] = pm.SubstitutePath(this.getPaths()[j]);
        }
        for(int j=0; j<this.getDataPaths().length;j++){
            dest.getDataPaths()[j] = pm.SubstitutePath(this.getDataPaths()[j]);
        }
        return dest;
    }
}