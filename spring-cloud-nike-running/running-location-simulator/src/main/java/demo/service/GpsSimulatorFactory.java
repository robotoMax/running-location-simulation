package demo.service;

import demo.model.GpsSimulatorRequest;
import demo.task.LocationSimulator;

public interface GpsSimulatorFactory {

    LocationSimulator prepareGpsSimulator(GpsSimulatorRequest gpsSimulatorRequest);
}
