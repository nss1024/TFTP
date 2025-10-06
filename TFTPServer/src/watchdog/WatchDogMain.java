package watchdog;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WatchDogMain {

    private static final ScheduledExecutorService se = Executors.newSingleThreadScheduledExecutor();

    public void start(){
        se.scheduleAtFixedRate(new Runnable(){
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Set<WatchDogMonitoredSession> dataStoreKeys = DataStore.getSessionsInDataStore();
                if(dataStoreKeys.isEmpty())return;
                for(WatchDogMonitoredSession r : dataStoreKeys){
                    if(r==null){continue;}
                    long sessionLifeTime = now-DataStore.getStartTime(r);
                    if(sessionLifeTime>now+600_000){
                        if(!r.isAlive()){
                            DataStore.removeSession(r);
                            r.stopSession();
                        }
                    }
                    if(sessionLifeTime>now+1_200_000){
                        DataStore.removeSession(r);
                        r.stopSession();
                    }
                }

            }
        },0,5, TimeUnit.MINUTES);
    }

    public void stop(){

    }


}
