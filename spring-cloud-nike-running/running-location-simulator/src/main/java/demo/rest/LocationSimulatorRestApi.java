package demo.rest;

import demo.model.GpsSimulatorRequest;
import demo.model.SimulatorInitLocations;
import demo.service.GpsSimulatorFactory;
import demo.service.PathService;
import demo.task.LocationSimulator;
import demo.task.LocationSimulatorInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api")
public class LocationSimulatorRestApi {

    @Autowired
    private AsyncTaskExecutor taskExecutor;

    @Autowired
    private PathService pathService;

    @Autowired
    private GpsSimulatorFactory gpsSimulatorFactory;

    private Map<Long, LocationSimulatorInstance> taskFutures = new HashMap<>();



    // 1. Load simulation json data polyline,   speed
    // 2. Transform domain model simulator request to a class that can be executed by taskExecutor
    // 2.1 Scheduler is a class/interface  that can schedule a task to execute.
    // 2.2 AsyncTaskExecutor
    // 3. taskExecutor.submit(simulator)
    // 4. simulation starts

    @RequestMapping("/simulation")
    public List<LocationSimulatorInstance> simulation(){
        SimulatorInitLocations fixture = this.pathService.loadSimulatorInitLocations();
        List<LocationSimulatorInstance> instances = new ArrayList<>();

        for (GpsSimulatorRequest gpsSimulatorRequest : fixture.getGpsSimulatorRequests()){
            LocationSimulator locationSimulator = gpsSimulatorFactory.prepareGpsSimulator(gpsSimulatorRequest);

            Future<?> future  = taskExecutor.submit(locationSimulator);
            LocationSimulatorInstance instance = new LocationSimulatorInstance(locationSimulator.getId(), locationSimulator, future);
            taskFutures.put(locationSimulator.getId(), instance);
            instances.add(instance);
        }

        return instances;
    }

    @RequestMapping("/cancel")
    public int cancel(){
        int numberOfCancelledTasks = 0;
        for (Map.Entry<Long, LocationSimulatorInstance> entry: taskFutures.entrySet()){
            LocationSimulatorInstance instance = entry.getValue();
            instance.getLocationSimulator().cancel();
            boolean wasCancelled = instance.getLocationSimulatorTask().cancel(true);
            if(wasCancelled){
                numberOfCancelledTasks++;
            }
        }

        taskFutures.clear();
        return numberOfCancelledTasks;
    }
}
