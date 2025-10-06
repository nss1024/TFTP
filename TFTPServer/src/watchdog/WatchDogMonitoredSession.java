package watchdog;

public interface WatchDogMonitoredSession{

    boolean isAlive();

    void stopSession();

}
