package demo.task;

import demo.model.*;
import demo.service.PositionService;
import demo.support.NavUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//import demo.model.PositionInfo;
//import demo.support.NavUtils;

//Location Simulator is not a Spring managed bean
public class LocationSimulator implements Runnable {

    @Getter
    @Setter
    private long id;

//    private PositionService positionInfoService;

    private AtomicBoolean cancel = new AtomicBoolean();

    private Double speedInMps; // In meters/sec
    private boolean shouldMove;
    private boolean exportPositionsToMessaging = true;

    private Integer reportInterval = 500; // millisecs at which to send position reports

    @Getter
    @Setter
    private PositionInfo currentPosition = null; //Leg

    @Setter
    private List<Leg> legs;
    private RunnerStatus runnerStatus = RunnerStatus.NONE;
    private String runningId;

    @Setter
    private Point startPoint;
    private Date executionStartTime;

    @Setter
    private PositionService positionService;


    //@Nike Running
    private MedicalInfo medicalInfo;

    public LocationSimulator(GpsSimulatorRequest gpsSimulatorRequest) {
        this.shouldMove = gpsSimulatorRequest.isMove();
        this.exportPositionsToMessaging = gpsSimulatorRequest.isExportPositionsToMessaging();
        this.setSpeed(gpsSimulatorRequest.getSpeed());
        this.reportInterval = gpsSimulatorRequest.getReportInterval();
        this.runningId = gpsSimulatorRequest.getRunningId();
        this.runnerStatus = gpsSimulatorRequest.getRunnerStatus();
        //@Nike Running
        this.medicalInfo = gpsSimulatorRequest.getMedicalInfo();
    }

    public long getId() {
        return id;
    }

    @Override
    public void run() {

        try {
            if (cancel.get()) {
                destroy();
                return;
            }

            while (!Thread.interrupted()) {
                long startTime = new Date().getTime();
                if (currentPosition != null) {
                    if (shouldMove) {
                        moveRunningLocation();
                        currentPosition.setSpeed(speedInMps);
                    } else {
                        currentPosition.setSpeed(0.0);
                    }
                }

                currentPosition.setRunnerStatus(this.runnerStatus);

                MedicalInfo medicalInfoToUse;

                switch (this.runnerStatus) {
                    case SUPPLY_SOON:
                    case SUPPLY_NOW:
                    case STOP_NOW:
                        medicalInfoToUse = this.medicalInfo;
                        break;
                    default:
                        medicalInfoToUse = null;
                        break;
                }

                CurrentPosition currentPosition = new CurrentPosition(this.currentPosition.getRunningId(),
                        new Point(this.currentPosition.getPosition().getLatitude(), this.currentPosition.getPosition().getLongitude()),
                        this.currentPosition.getRunnerStatus(),
                        this.currentPosition.getSpeed(),
                        this.currentPosition.getLeg().getHeading(), medicalInfoToUse);

                positionService.processPositionInfo(id, currentPosition, this.exportPositionsToMessaging);
                sleep(startTime);
            }
        } catch (InterruptedException ie) {
            destroy();
            return;
        }
        destroy();
    }

    private void sleep(long startTime) throws InterruptedException {
        long endTime = new Date().getTime();
        long elapsedTime = endTime - startTime;
        long sleepTime = reportInterval - elapsedTime > 0 ? reportInterval - elapsedTime : 0;
        Thread.sleep(sleepTime);
    }

    void moveRunningLocation() {
        Double distance = speedInMps * reportInterval / 1000.0;
        Double distanceFromStart = currentPosition.getDistanceFromStart() + distance;
        Double excess = 0.0; // amount by which next postion will exceed end
        // point of present leg

        for (int i = currentPosition.getLeg().getId(); i < legs.size(); i++) {
            Leg currentLeg = legs.get(i);
            excess = distanceFromStart > currentLeg.getLength() ? distanceFromStart - currentLeg.getLength() : 0.0;

            if (Double.doubleToRawLongBits(excess) == 0) {
                // this means new position falls within current leg
                currentPosition.setDistanceFromStart(distanceFromStart);
                currentPosition.setLeg(currentLeg);
                Point newPosition = NavUtils.getPosition(currentLeg.getStartPosition(), distanceFromStart,
                        currentLeg.getHeading());
                currentPosition.setPosition(newPosition);
                return;
            }
            distanceFromStart = excess;
        }

        setStartPosition();
    }

    public void setStartPosition() {
        currentPosition = new PositionInfo();
        currentPosition.setRunningId(this.runningId);
        Leg leg = legs.get(0);
        currentPosition.setLeg(leg);
        currentPosition.setPosition(leg.getStartPosition());
        currentPosition.setDistanceFromStart(0.0);
    }

    public void setSpeed(Double speed) {
        this.speedInMps = speed;
    }

    public void cancel() {
        this.cancel.set(true);
    }

    private void destroy() {
        currentPosition = null;
    }
}
