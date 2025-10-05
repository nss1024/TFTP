package watchdog;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private ConcurrentHashMap<Runnable, Long> threadDataStore = new ConcurrentHashMap<>();


    public void addSessionToDataStore(Runnable r, Long systemTime){
        threadDataStore.put(r,systemTime);
    }

    public Long getStartTime(Runnable r){
        return threadDataStore.get(r);
    }

    public Set<Runnable> getSessionsInDataStore(){
        return threadDataStore.keySet();
    }
    


}
