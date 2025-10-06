package watchdog;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private static final ConcurrentHashMap<WatchDogMonitoredSession, Long> threadDataStore = new ConcurrentHashMap<>();

    private DataStore(){}

    public static void addSessionToDataStore(WatchDogMonitoredSession r){
        threadDataStore.put(r,System.currentTimeMillis());
    }

    public static Long getStartTime(WatchDogMonitoredSession r){
        return threadDataStore.get(r);
    }

    public static Set<WatchDogMonitoredSession> getSessionsInDataStore(){
        return threadDataStore.keySet();
    }

    public static void removeSession(WatchDogMonitoredSession r){
        threadDataStore.remove(r);
    }


}
