package it.serex.ugs.manualheightmap;

/** Data that must survive a restart before acquisition is complete. */
public final class MeasurementSession {
    public final ManualGrid grid;
    public final String workFile;
    public final long workModified;
    public final double approachZ;
    public final double transferZ;
    public final double descentFeed;
    public final double workOffsetX, workOffsetY, workOffsetZ;

    public MeasurementSession(ManualGrid grid, String workFile, long workModified,
                              double approachZ, double transferZ, double descentFeed,
                              double workOffsetX, double workOffsetY, double workOffsetZ) {
        this.grid = grid;
        this.workFile = workFile;
        this.workModified = workModified;
        this.approachZ = approachZ;
        this.transferZ = transferZ;
        this.descentFeed = descentFeed;
        this.workOffsetX = workOffsetX;
        this.workOffsetY = workOffsetY;
        this.workOffsetZ = workOffsetZ;
    }
}
