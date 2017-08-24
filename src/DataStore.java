import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore 
{
	public Map<String, String> mapTrips_ = new ConcurrentHashMap<String, String>(); 
	public Map<String, String> mapTS_ = new ConcurrentHashMap<String, String>(); 
	public Map<String, String> mapShed_ = new ConcurrentHashMap<String, String>(); 
	public Map<String, String> mapTime_ = new ConcurrentHashMap<String, String>(); 

	public static enum type_map { DS_Trips, DS_TS, DS_Shed, DS_Time};
	
	public void putAll (DataStore dsSrc)
	{
		if(dsSrc == null)
			return;
		
		mapTrips_.putAll (dsSrc.mapTrips_); 
		mapTS_.putAll (dsSrc.mapTS_); 
		mapShed_.putAll (dsSrc.mapShed_); 
		mapTime_.putAll (dsSrc.mapTime_); 
	}
}
