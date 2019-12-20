package org.firstinspires.ftc.teamcode.Autonomous.BlueAlliance.SkystonePark;

import org.firstinspires.ftc.teamcode.Autonomous.MainAutonomous;

public class BlueSkyPrkBridgeDep extends MainAutonomous {
    private String className = this.getClass().getSimpleName();
    @Override
    public void runOpMode() {
        // Initialize autonomous route
        getPreferences();
        checkPreferences();
        print("Alliance", teamColor);
        print("Foundation", doFoundation);
        print("Skystone", doSkystone);
        print("Parking Position", parking);
        print("Starting Position", starting);

        // Initialize hardware
        getHardwareMap();
        initCheck();
        print("Status", "Initialized");
        getPreferences();
        telemetry.update();

        waitForStart();
        print("Status", "Running");
        telemetry.update();

        /* Autonomous
         * Team Alliance:           Blue
         * Skystone:                Yes
         * Foundation:              No
         * Parking:                 Yes
         * Parking Position:        Bridge
         * Start Position:          Depot
         */
        if (opModeIsActive()) {
            runtime.startTime();
            while (runtime.milliseconds() < delayTime) {}
            // Transition
            encoderDrive("left", minPower, 0.25);
            encoderDrive("front", minPower, 1);
            // Recognize skystone
            recognizeTarget("Stone Target");
            // Grab 1st skystone
            switch (skystonePosition) {
                case "left":
                    encoderDrive("left", minPower, halfSkystoneDist);
                    grabSkystone(minPower);
                    encoderDrive("right", minPower, halfSkystoneDist);
                case "center":
                    encoderDrive("right", minPower, halfSkystoneDist);
                    grabSkystone(minPower);
                    encoderDrive("left", minPower, halfSkystoneDist);
                case "right":
                    encoderDrive("right", minPower, (fullSkystoneDist + halfSkystoneDist));
                    grabSkystone(minPower);
                    encoderDrive("left", minPower, (fullSkystoneDist + halfSkystoneDist));
            }
            // Deliver 1st skystone
            depotToBuildingsite("blue", minPower, minTurnPower);
            buildSkystone("blue", minPower, minTurnPower, 1);
            // Transition
            buildingsiteToDepot("blue", minPower, minTurnPower);
            encoderDrive("right", minPower, 1);
            // Grab 2nd skystone
            switch (skystonePosition) {
                case "left":
                    encoderDrive("left", minPower, halfSkystoneDist);
                    grabSkystone(minPower);
                    encoderDrive("right", minPower, halfSkystoneDist);
                case "center":
                    encoderDrive("right", minPower, halfSkystoneDist);
                    grabSkystone(minPower);
                    encoderDrive("left", minPower, halfSkystoneDist);
                case "right":
                    gyroTurn(90, minTurnPower);
                    encoderDrive("front", minPower, 0.25);
                    encoderDrive("left", minPower, 0.25);
                    captureLeftSkystone();
                    encoderDrive("right", minPower, 0.25);
                    encoderDrive("back", minPower, 0.25);
                    gyroTurn(-90, minTurnPower);
            }
            // Deliver 2nd skystone
            encoderDrive("left", minPower, 1);
            depotToBuildingsite("blue", minPower, minTurnPower);
            buildSkystone("blue", minPower, minTurnPower, 2);
            // Parking
            encoderDrive("right", minPower, 1.625);
        }

        resetMotor();
        visionTargets.deactivate();
    }
}