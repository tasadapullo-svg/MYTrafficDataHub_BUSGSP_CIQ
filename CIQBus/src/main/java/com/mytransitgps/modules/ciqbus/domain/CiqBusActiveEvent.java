package com.mytransitgps.modules.ciqbus.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public class CiqBusActiveEvent {
    private String matchedEventId;
    private String routeNo;
    private String operatorCode;
    private String directionCode;
    private Instant firstSeenTime;
    private Instant lastSeenTime;
    private Instant lastEstimatedArrival;
    private Double lastLatitude;
    private Double lastLongitude;
    private String lastSourceStopCode;
    private int observationCount;
    private String entryStopCode;
    private Instant entryTime;
    private String exitStopCode;
    private Instant exitTime;
    private Set<String> visitedStops = new LinkedHashSet<>();
    private double confidence;
    private String status = "ACTIVE";

    public String getMatchedEventId() { return matchedEventId; }
    public void setMatchedEventId(String matchedEventId) { this.matchedEventId = matchedEventId; }
    public String getRouteNo() { return routeNo; }
    public void setRouteNo(String routeNo) { this.routeNo = routeNo; }
    public String getOperatorCode() { return operatorCode; }
    public void setOperatorCode(String operatorCode) { this.operatorCode = operatorCode; }
    public String getDirectionCode() { return directionCode; }
    public void setDirectionCode(String directionCode) { this.directionCode = directionCode; }
    public Instant getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(Instant firstSeenTime) { this.firstSeenTime = firstSeenTime; }
    public Instant getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Instant lastSeenTime) { this.lastSeenTime = lastSeenTime; }
    public Instant getLastEstimatedArrival() { return lastEstimatedArrival; }
    public void setLastEstimatedArrival(Instant lastEstimatedArrival) { this.lastEstimatedArrival = lastEstimatedArrival; }
    public Double getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(Double lastLatitude) { this.lastLatitude = lastLatitude; }
    public Double getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(Double lastLongitude) { this.lastLongitude = lastLongitude; }
    public String getLastSourceStopCode() { return lastSourceStopCode; }
    public void setLastSourceStopCode(String lastSourceStopCode) { this.lastSourceStopCode = lastSourceStopCode; }
    public int getObservationCount() { return observationCount; }
    public void setObservationCount(int observationCount) { this.observationCount = observationCount; }
    public String getEntryStopCode() { return entryStopCode; }
    public void setEntryStopCode(String entryStopCode) { this.entryStopCode = entryStopCode; }
    public Instant getEntryTime() { return entryTime; }
    public void setEntryTime(Instant entryTime) { this.entryTime = entryTime; }
    public String getExitStopCode() { return exitStopCode; }
    public void setExitStopCode(String exitStopCode) { this.exitStopCode = exitStopCode; }
    public Instant getExitTime() { return exitTime; }
    public void setExitTime(Instant exitTime) { this.exitTime = exitTime; }
    public Set<String> getVisitedStops() { return visitedStops; }
    public void setVisitedStops(Set<String> visitedStops) { this.visitedStops = visitedStops == null ? new LinkedHashSet<>() : visitedStops; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
