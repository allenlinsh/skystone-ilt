package org.firstinspires.ftc.teamcode.Autonomous;

import com.qualcomm.ftccommon.SoundPlayer;
import java.lang.reflect.Array;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.firstinspires.ftc.teamcode.PIDController;

import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

//
// References:
// -Template: https://github.com/FestiveInvader/ftc_app/blob/master/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/Auton/DeclarationsAutonomous.java
// -Vuforia: https://github.com/ftctechnh/ftc_app/blob/master/FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/ConceptVuforiaNavigationWebcam.java
// -Gyro Correction: https://stemrobotics.cs.pdx.edu/node/7265
// -PID Control: https://stemrobotics.cs.pdx.edu/node/7268
//

@Autonomous
public class MainAutonomous extends LinearOpMode {
    //
    // ==================================== DECLARE VARIABLES ======================================
    //
    // Declare hardware variables
    public BNO055IMU imu;
    public DcMotor leftBackMotor, rightBackMotor, leftFrontMotor, rightFrontMotor, armMotor, gripMotor;
    public Servo leftServo, rightServo;
    public DigitalChannel topLimit, bottomLimit;
    public WebcamName LogitechC310;

    // Declare general variables
    public ElapsedTime runtime = new ElapsedTime();
    public boolean initReady = false;
    public static float inPerBlock = 23.625f;
    public static float fullSkystoneDist = 8.0f;
    public static float halfSkystoneDist = 4.0f;

    // Declare movement variables
    public static final int ticksPerRev = 480;
    public Orientation lastAngles = new Orientation();
    public double globalAngle;
    public double correction;
    public double drivePower = 0.35;
    public double turnPower = 0.35;
    public PIDController pidRotate = new PIDController(0, 0, 0);
    public PIDController pidCorrection = new PIDController(.05, 0, 0);
    public PIDController pidDrive = new PIDController(0, 0, 0);

    // Declare vuforia variables
    public VuforiaLocalizer vuforia;
    public VuforiaLocalizer.Parameters parameters;
    public VuforiaTrackables visionTargets;
    public List<VuforiaTrackable> allTrackables;
    public VuforiaTrackable stoneTarget, blueRearBridge, redRearBridge, redFrontBridge,
            blueFrontBridge, red1, red2, front1, front2, blue1, blue2, rear1, rear2;
    public OpenGLMatrix lastKnownLocation = createMatrix(0, 0, 0, 0, 0, 0);
    public OpenGLMatrix latestLocation;
    public OpenGLMatrix webcamLocation;
    public String skystonePosition;
    public static final String VUFORIA_KEY = "AZ2DQXn/////AAABmV2NdKltaEv7nZA9fnEAYpONbuK/sGbJG" +
            "7tGyKNwNcaEPXyRq7V3WKOcmTwGwpTyl5Sm/2tJR6t5VFwarUda2dnW20yakyCThxpQcM4xXu5xnY3/HVPc" +
            "TCEloelyqgf0jSbw94/N7b2n7jdkdA/CYYvJOQo7/cQ3cnoa/3aZ1LpJgeYy8SHLDeLe2nwpARjaHokhhG8" +
            "35GzpFlTXa1IhHjo0Lsvm2qTM8WqgLIKYYep1urYPAPYYUsT+WXUSLCbw0TkQcIVLP6FdvQL6FtCeRoA29f" +
            "pTdq5L4RFsdqac2fELdXY8rjZpJDx4g/8KN6aw1iG4ZocJBzgzhELtCgQbqJppGGk7z/CRTvcXL1dhIunZ";
    public int cameraMonitorViewId;
    public boolean streamView                   = false;
    public boolean targetVisible                = false;
    public boolean vuforiaReady                 = false;
    public boolean skystoneFound                = false;
    public static final float mmPerInch         = 25.4f;
    public static final float mmTargetHeight    = 6.00f * mmPerInch;
    // Location of center of target with relation to center of field
    public static final float stoneZ            = 2.00f * mmPerInch;
    public static final float bridgeZ           = 6.42f * mmPerInch;
    public static final float bridgeY           = 23 * mmPerInch;
    public static final float bridgeX           = 5.18f * mmPerInch;
    public static final float bridgeRotY        = 59; // degrees
    public static final float bridgeRotZ        = 180; // degrees
    public static final float halfField         = 72 * mmPerInch;
    public static final float quadField         = 36 * mmPerInch;
    // Location of center of robot with relation to center of target
    public float robotX                         = 0;
    public float robotY                         = 0;
    public float robotAngle                     = 0;
    // Location of center of webcam with relation to center of robot
    public static final float webcamX           = 0;
    public static final float webcamY           = 6.50f * mmPerInch;
    public static final float webcamZ           = -3.00f * mmPerInch;
    public static final float webcamRotX        = 90; // degrees
    public static final float webcamRotY        = 0; // degrees
    public static final float webcamRotZ        = 180; // degrees
    // Dimension of robot
    public float robotWidth                     = 18.00f;
    public float robotLength                    = 18.00f;
    public float robotHeight                    = 14.00f;
    public float frontTranslation               = 112.0f / mmPerInch;
    // Distance to travel from front of robot to center of target
    public float travelX                        = 2.0f * fullSkystoneDist + halfSkystoneDist - (robotWidth / 2.0f);
    public float travelY                        = inPerBlock - frontTranslation;
    // Location of center of skystone placement with relation to wall
    public float firstSkystone                  = 13.25f;
    public float secondSkystone                 = 29.25f;
    public float centerSkystone                 = 21.25f;
    // Distance to travel from building site starting position to center of skystone placement (towards wall)
    public float firstPlacement                 = inPerBlock - firstSkystone + (robotWidth / 2);
    public float secondPlacement                = inPerBlock - secondSkystone + (robotWidth / 2);
    public float centerPlacement                = inPerBlock - centerSkystone + (robotWidth / 2);

    // Declare shared preference variables
    public SharedPreferences preferences;
    public String teamColor, parking, starting;
    public boolean doFoundation, doSkystone;
    public int delayTime;
    public StringBuilder autoName = new StringBuilder("");

    //
    // =================================== AUTONOMOUS PROGRAM ======================================
    //
    @Override
    public void runOpMode() {}
    //
    // ==================================== DECLARE FUNCTIONS ======================================
    //
    // General functions
    public void print(String caption, Object message) {
        telemetry.addData(caption, message);
    }
    private void update() { telemetry.update(); }
    private double round(double val, int roundTo) {
        return Double.valueOf(String.format("%." + roundTo + "f", val));
    }
    public void playSound(String soundName) {
        // List of available sound resources
        String [] sounds = {"ss_alarm", "ss_bb8_down", "ss_bb8_up", "ss_darth_vader", "ss_fly_by",
                "ss_mf_fail", "ss_laser", "ss_laser_burst", "ss_light_saber", "ss_light_saber_long", "ss_light_saber_short",
                "ss_light_speed", "ss_mine", "ss_power_up", "ss_r2d2_up", "ss_roger_roger", "ss_siren", "ss_wookie"};

        SoundPlayer.PlaySoundParams params = new SoundPlayer.PlaySoundParams();
        params.loopControl = 0;
        params.waitForNonLoopingSoundsToFinish = true;

        int sound = hardwareMap.appContext.getResources().getIdentifier(soundName, "raw", hardwareMap.appContext.getPackageName());
        SoundPlayer.getInstance().startPlaying(hardwareMap.appContext, sound, params, null, null);
    }

    // Init functions
    public void getPreferences() {
        preferences = PreferenceManager.getDefaultSharedPreferences(hardwareMap.appContext);
        teamColor = String.valueOf(preferences.getString("auto_team_color", "blue"));
        doFoundation = Boolean.valueOf(preferences.getBoolean("auto_do_foundation", true));
        doSkystone = Boolean.valueOf(preferences.getBoolean("auto_do_skystone", false));
        parking = String.valueOf(preferences.getString("auto_parking", "bridge"));
        starting = String.valueOf(preferences.getString("auto_starting", "depot"));
        delayTime = Integer.valueOf(preferences.getString("auto_delay_time", "0"));
    }
    public void checkPreferences(String className) {
        if ("blue".equals(teamColor)) autoName.append("Blue");
        if ("red".equals(teamColor)) autoName.append("Red");
        if (doFoundation) autoName.append("Fnd");
        if (doSkystone) autoName.append("Sky");
        autoName.append("Prk");
        if ("bridge".equals(parking)) autoName.append("Bridge");
        if ("wall".equals(parking)) autoName.append("Wall");
        if ("depot".equals(starting)) autoName.append("Dep");
        if ("buildingSite".equals(starting)) autoName.append("Build");

        if (!autoName.toString().equals(className) && !gamepad1.start && !gamepad2.start) {
            requestOpModeStop();
        } else {
            AppUtil.getInstance().showToast(UILocation.BOTH, "\'" + className + "\' selected.", 7500);
        }
    }
    public void getHardwareMap() {
        imu                 = hardwareMap.get(BNO055IMU.class, "imu");
        leftBackMotor       = hardwareMap.get(DcMotor.class, "leftBackMotor");
        rightBackMotor      = hardwareMap.get(DcMotor.class, "rightBackMotor");
        leftFrontMotor      = hardwareMap.get(DcMotor.class, "leftFrontMotor");
        rightFrontMotor     = hardwareMap.get(DcMotor.class, "rightFrontMotor");
        armMotor            = hardwareMap.get(DcMotor.class, "armMotor");
        gripMotor           = hardwareMap.get(DcMotor.class, "gripMotor");
        leftServo           = hardwareMap.get(Servo.class, "leftServo");
        rightServo          = hardwareMap.get(Servo.class, "rightServo");
        topLimit            = hardwareMap.get(DigitalChannel.class, "topLimit");
        bottomLimit         = hardwareMap.get(DigitalChannel.class, "bottomLimit");
        LogitechC310        = hardwareMap.get(WebcamName.class, "Logitech C310");
        cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
    }
    private void initMotor() {
        leftBackMotor.setDirection(DcMotorSimple.Direction.FORWARD);
        rightBackMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        leftFrontMotor.setDirection(DcMotorSimple.Direction.FORWARD);
        rightFrontMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        armMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        gripMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        leftBackMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightBackMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftFrontMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightFrontMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        armMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        gripMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Set up parameters for driving in a straight line.
        pidCorrection.setSetpoint(0);
        pidCorrection.setOutputRange(0, drivePower);
        pidCorrection.setInputRange(-90, 90);
        pidCorrection.enable();
    }
    private boolean checkMotor() {
        if (gripMotor.getZeroPowerBehavior() == DcMotor.ZeroPowerBehavior.BRAKE) {
            return true;
        } else {
            return false;
        }
    }
    private void initServo() {
        leftServo.setPosition(0);
        rightServo.setPosition(1);
        //leftSkystoneServo.setPosition(0.52);
        //rightSkystoneServo.setPosition(0.98);
    }
    private boolean checkServo() {
        /*
        if (round(leftServo.getPosition(), 2) == 0 &&
                round(rightServo.getPosition(), 2) == 1 &&
                round(leftSkystoneServo.getPosition(), 2) == 0.52 &&
                round(rightSkystoneServo.getPosition(), 2) == 0.98) {
            return true;
        }
        */
        if (round(leftServo.getPosition(), 2) == 0 &&
                round(rightServo.getPosition(), 2) == 1) {
            return true;
        } else {
            return false;
        }
    }
    private void initIMU() {
        BNO055IMU.Parameters imuParameters = new BNO055IMU.Parameters();
        imuParameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        imuParameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        imuParameters.loggingEnabled = false;
        imu.initialize(imuParameters);
        while(!imu.isGyroCalibrated()){}
    }
    private boolean checkIMU() {
        if (imu.isGyroCalibrated()) {
            return true;
        } else {
            return false;
        }
    }
    private void initVuforia() {
        parameters = (streamView)
                ? new VuforiaLocalizer.Parameters(cameraMonitorViewId)
                : new VuforiaLocalizer.Parameters();
        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = LogitechC310;
        parameters.useExtendedTracking = false;
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Load the data sets for trackable objects
        visionTargets = vuforia.loadTrackablesFromAsset("Skystone");
        VuforiaTrackable stoneTarget = visionTargets.get(0);
        stoneTarget.setName("Stone Target");
        VuforiaTrackable blueRearBridge = visionTargets.get(1);
        blueRearBridge.setName("Blue Rear Bridge");
        VuforiaTrackable redRearBridge = visionTargets.get(2);
        redRearBridge.setName("Red Rear Bridge");
        VuforiaTrackable redFrontBridge = visionTargets.get(3);
        redFrontBridge.setName("Red Front Bridge");
        VuforiaTrackable blueFrontBridge = visionTargets.get(4);
        blueFrontBridge.setName("Blue Front Bridge");
        VuforiaTrackable red1 = visionTargets.get(5);
        red1.setName("Red Perimeter 1");
        VuforiaTrackable red2 = visionTargets.get(6);
        red2.setName("Red Perimeter 2");
        VuforiaTrackable front1 = visionTargets.get(7);
        front1.setName("Front Perimeter 1");
        VuforiaTrackable front2 = visionTargets.get(8);
        front2.setName("Front Perimeter 2");
        VuforiaTrackable blue1 = visionTargets.get(9);
        blue1.setName("Blue Perimeter 1");
        VuforiaTrackable blue2 = visionTargets.get(10);
        blue2.setName("Blue Perimeter 2");
        VuforiaTrackable rear1 = visionTargets.get(11);
        rear1.setName("Rear Perimeter 1");
        VuforiaTrackable rear2 = visionTargets.get(12);
        rear2.setName("Rear Perimeter 2");
        allTrackables = new ArrayList<VuforiaTrackable>();
        allTrackables.addAll(visionTargets);

        // Set up field coordinate system

        // Field Coordinate System
        // If you are standing in the Red Alliance Station looking towards the center of the field:
        // -The X axis runs from your left to the right. (Positive is from the center to the right)
        // -The Y axis runs from the Red Alliance Station towards the Blue Alliance Station
        //  (Positive is from the center to the Blue Alliance Station)
        // -The Z axis runs from the floor to the ceiling. (Positive is above the floor)

        // Location of the Stone Target
        stoneTarget.setLocation(createMatrix(0, 0, stoneZ, 90, 0, -90));
        // Location of the bridge support targets with relation to the center of field
        blueFrontBridge.setLocation(createMatrix(-bridgeX, bridgeY, bridgeZ, 0, bridgeRotY, bridgeRotZ));
        blueRearBridge.setLocation(createMatrix(-bridgeX, bridgeY, bridgeZ, 0, -bridgeRotY, bridgeRotZ));
        redFrontBridge.setLocation(createMatrix(-bridgeX, -bridgeY, bridgeZ, 0, -bridgeRotY, 0));
        redRearBridge.setLocation(createMatrix(bridgeX, -bridgeY, bridgeZ, 0, bridgeRotY, 0));
        // Location of the perimeter targets with relation to the center of field
        red1.setLocation(createMatrix(quadField, -halfField, mmTargetHeight, 90, 0, 180));
        red2.setLocation(createMatrix(-quadField, -halfField, mmTargetHeight, 90, 0, 180));
        front1.setLocation(createMatrix(-halfField, -quadField, mmTargetHeight, 90, 0 , 90));
        front2.setLocation(createMatrix(-halfField, quadField, mmTargetHeight, 90, 0, 90));
        blue1.setLocation(createMatrix(-quadField, halfField, mmTargetHeight, 90, 0, 0));
        blue2.setLocation(createMatrix(quadField, halfField, mmTargetHeight, 90, 0, 0));
        rear1.setLocation(createMatrix(halfField, quadField, mmTargetHeight, 90, 0 , -90));
        rear2.setLocation(createMatrix(halfField, -quadField, mmTargetHeight, 90, 0, -90));
        // Location of the webcam with relation to the center of the robot
        webcamLocation = createMatrix(webcamX, webcamY, webcamZ, webcamRotX, webcamRotY, webcamRotZ);

        // Set up the listener
        for (VuforiaTrackable trackable : allTrackables) {
            ((VuforiaTrackableDefaultListener)trackable.getListener())
                    .setCameraLocationOnRobot(parameters.cameraName, webcamLocation);
        }

        visionTargets.activate();
        vuforiaReady = true;
    }
    private boolean checkVuforia() {
        if (vuforiaReady) {
            return true;
        } else {
            return false;
        }
    }
    public void initCheck() {
        while (!isStopRequested() && !initReady) {
            // Initialize motor
            if (!checkMotor()) {
                print("Motor","Initializing");
                update();
                initMotor();
            } else if (checkMotor()) {
                print("Motor","Initialized");
                update();
                // Initialize servo
                if (!checkServo()) {
                    print("Motor","Initialized");
                    print("Servo","Initializing");
                    update();
                    initServo();
                } else if (checkServo()) {
                    print("Motor","Initialized");
                    print("Servo","Initialized");
                    update();
                    // Initialize imu
                    if (!checkIMU()) {
                        print("Motor","Initialized");
                        print("Servo","Initialized");
                        print("IMU","Initializing...");
                        update();
                        initIMU();
                    } else if (checkIMU()) {
                        print("Motor","Initialized");
                        print("Servo","Initialized");
                        print("IMU","Initialized");
                        update();
                        // Initialize vuforia
                        if (!checkVuforia()) {
                            print("Motor","Initialized");
                            print("Servo","Initialized");
                            print("IMU","Initialized");
                            print("Vuforia","Initializing...");
                            update();
                            initVuforia();
                        } else if (checkVuforia()) {
                            print("Motor","Initialized");
                            print("Servo","Initialized");
                            print("IMU","Initialized");
                            print("Vuforia","Initialized");
                            update();
                            initReady = true;
                        }
                    }
                }
            }
        }
    }

    // Vuforia functions
    private OpenGLMatrix createMatrix(float x, float y, float z, float u, float v, float w) {
        return OpenGLMatrix
                .translation(x, y, z)
                .multiplied(Orientation.getRotationMatrix(
                        AxesReference.EXTRINSIC, AxesOrder.XYZ, AngleUnit.DEGREES, u, v, w));
    }
    public String formatMatrix(OpenGLMatrix matrix) {return matrix.formatAsTransform();}
    public void recognizeTarget(String target) {
        runtime.reset();
        runtime.startTime();
        boolean targetFound = false;
        while(opModeIsActive() && checkVuforia()) {
            String targetName = "";
            targetVisible = false;
            skystoneFound = false;
            // Check if any trackable target is visible
            for(VuforiaTrackable trackable : allTrackables) {
                if (((VuforiaTrackableDefaultListener)trackable.getListener()).isVisible()) {
                    targetVisible = true;
                    targetName = trackable.getName();
                    print("Visible Target", targetName);
                    if (targetName == "Stone Target") skystoneFound = true;

                    latestLocation = ((VuforiaTrackableDefaultListener)trackable.getListener())
                            .getUpdatedRobotLocation();
                    if (latestLocation != null) lastKnownLocation = latestLocation;
                    break;
                }
            }

            if (targetVisible) {
                float [] coordinates = lastKnownLocation.getTranslation().getData();
                robotX      = coordinates[1] / mmPerInch;
                robotY      = coordinates[0] / mmPerInch;
                robotAngle  = Orientation.getOrientation(lastKnownLocation, AxesReference.EXTRINSIC,
                        AxesOrder.XYZ, AngleUnit.DEGREES).thirdAngle;

                // Update robot's location with relation to center of target
                print("Robot Coordinates", "(" + round(robotX, 0) + "in , " + round(robotY, 0) + "in)");
                print("Robot Heading", round(robotAngle, 2));

                // Update travelling distance
                if (robotX != 0) travelX = robotX;
                if (robotY != 0) travelY = -robotY - (robotLength / 2) - frontTranslation;

                if (skystoneFound) {
                    if (robotX < -0.4) {
                        skystonePosition = "left";
                    } else {
                        skystonePosition = "center";
                    }
                }
            } else {
                print("Visible Target", "none");
                skystonePosition = "right";
                if (runtime.milliseconds() > 1500) break;
            }

            // Update the skystone's position in terms of "left", "center", and "right"
            print("Skystone Position", skystonePosition);
            update();

            if (targetName == target) break;
        }
    }

    // Movement functions
    private void run(double leftBackPower, double rightBackPower, double leftFrontPower, double rightFrontPower) {
        leftBackMotor.setPower(leftBackPower);
        rightBackMotor.setPower(rightBackPower);
        leftFrontMotor.setPower(leftFrontPower);
        rightFrontMotor.setPower(rightFrontPower);
    }
    private void set(int leftBackDistance, int rightBackDistance, int leftFrontDistance, int rightFrontDistance) {
        leftBackMotor.setTargetPosition(leftBackDistance);
        rightBackMotor.setTargetPosition(rightBackDistance);
        leftFrontMotor.setTargetPosition(leftFrontDistance);
        rightFrontMotor.setTargetPosition(rightFrontDistance);
    }
    private void pause(String mode) {
        int duration = 0;
        runtime.reset();
        runtime.startTime();
        switch (mode) {
            case "servo":
                duration = 850;
                break;
            case "motor":
                duration = 500;
                break;
            case "short motor":
                duration = 100;
                break;
        }
        while (opModeIsActive() && runtime.milliseconds() < duration){}
    }
    private void mode(String mode) {
        switch (mode) {
            case "reset":
                leftBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                rightBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                leftFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                rightFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                break;
            case "position":
                leftBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                rightBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                leftFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                rightFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                break;
            case "no encoder":
                leftBackMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                rightBackMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                leftFrontMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                rightFrontMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                break;
        }
    }
    public void stopMotor() {
        run(0, 0, 0, 0);
    }
    public void stopAllMotors() {
        run(0, 0, 0, 0);
        armMotor.setPower(0);
        gripMotor.setPower(0);
    }
    private boolean topPressed() {
        return !topLimit.getState();
    }
    private boolean bottomPressed() {
        return !bottomLimit.getState();
    }
    private double getTick(String direction) {
        double firstTick = 0;
        double secondTick = 0;
        if (direction == "front") {
            firstTick = Math.abs(leftFrontMotor.getCurrentPosition());
            secondTick = Math.abs(rightFrontMotor.getCurrentPosition());
        }
        if (direction == "back") {
            firstTick = Math.abs(leftBackMotor.getCurrentPosition());
            secondTick = Math.abs(rightBackMotor.getCurrentPosition());
        }
        if (direction == "left") {
            firstTick = Math.abs(rightBackMotor.getCurrentPosition());
            secondTick = Math.abs(rightFrontMotor.getCurrentPosition());
        }
        if (direction == "right") {
            firstTick = Math.abs(leftBackMotor.getCurrentPosition());
            secondTick = Math.abs(leftFrontMotor.getCurrentPosition());
        }

        return (firstTick + secondTick)/2.0;
    }
    public void driveDist(String direction, double inches, double power) {
        double circumference    = Math.PI * 3.75;
        double inPerRev         = circumference / ticksPerRev;
        int ticks               = (int)(inches / inPerRev);

        pidDrive.reset();
        double p                = Math.abs(power/ticks);
        double i                = p/100.0;
        pidDrive.setPID(p, i, 0);
        pidDrive.setSetpoint(ticks);
        pidDrive.setInputRange(0, ticks);
        pidDrive.setOutputRange(0, power);
        pidDrive.setTolerance(1.0 / Math.abs(ticks) * 100.0);
        pidDrive.enable();

        do {
            correction = pidCorrection.performPID(getAngle());
            power = pidDrive.performPID(getTick(direction));
            if (direction == "front") {
                run(power, power, power-correction, power+correction);
            } else if (direction == "back") {
                run(-power-correction, -power+correction, -power, -power);
            } else if (direction == "left") {
                run(power-correction, -power, -power-correction, power);
            } else if (direction == "right") {
                run(-power, power+correction, power, -power+correction);
            }
        } while (opModeIsActive() && !pidDrive.onTarget());

        stopMotor();
        pause("motor");
    }
    public void drive(String direction, double blocks, double power) {
        double inches = blocks * inPerBlock;
        driveDist(direction, inches, power);
    }
    public void resetAngle() {
        lastAngles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        globalAngle = 0;
    }
    private double getAngle() {
        Orientation angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        double deltaAngle = angles.firstAngle - lastAngles.firstAngle;

        if (deltaAngle < -180)
            deltaAngle += 360;
        else if (deltaAngle > 180)
            deltaAngle -= 360;

        globalAngle += deltaAngle;
        lastAngles = angles;
        return globalAngle;
    }
    public void rotate(int degrees, double power) {
        resetAngle();
        if (Math.abs(degrees) > 359) degrees = (int) Math.copySign(359, degrees);

        pidRotate.reset();
        double p                = Math.abs(power/degrees);
        double i                = p/100.0;
        pidRotate.setPID(p, i, 0);
        pidRotate.setSetpoint(degrees);
        pidRotate.setInputRange(0, degrees);
        pidRotate.setOutputRange(0, power);
        pidRotate.setTolerance(1);
        pidRotate.enable();

        if (degrees < 0) {
            while (opModeIsActive() && getAngle() == 0) {
                run(power, -power, power, -power);
                pause("short motor");
            }

            do {
                power = pidRotate.performPID(getAngle());
                run(-power, power, -power, power);
            } while (opModeIsActive() && !pidRotate.onTarget());
        } else {
            do {
                power = pidRotate.performPID(getAngle());
                run(-power, power, -power, power);
            } while (opModeIsActive() && !pidRotate.onTarget());
        }

        stopMotor();
        pause("motor");
        resetAngle();
    }
    public void armExtend() {
        armMotor.setPower(-0.35);
        while (opModeIsActive() && !bottomPressed()) {}
        armMotor.setPower(0);
        pause("motor");
    }
    public void armCollapse() {
        armMotor.setPower(0.35);
        while (opModeIsActive() && !topPressed()) {}
        armMotor.setPower(0);
        pause("motor");
    }
    public void armRaise(int duration) {
        runtime.reset();
        runtime.startTime();
        armMotor.setPower(0.35);
        while(opModeIsActive() && !topPressed() && runtime.milliseconds() < duration) {}
        armMotor.setPower(0);
        pause("motor");
    }
    public void armDrop(int duration) {
        runtime.reset();
        runtime.startTime();
        armMotor.setPower(-0.35);
        while(opModeIsActive() && !bottomPressed() && runtime.milliseconds() < duration) {}
        armMotor.setPower(0);
        pause("motor");
    }
    public void gripHold(int duration) {
        runtime.reset();
        runtime.startTime();
        if (!topPressed()) gripMotor.setPower(0.35);
        while(opModeIsActive() && !topPressed() && runtime.milliseconds() < duration) {}
        pause("motor");
    }
    public void gripRelease(int duration) {
        runtime.reset();
        runtime.startTime();
        if (!topPressed()) gripMotor.setPower(-0.25);
        while(opModeIsActive() && !topPressed() && runtime.milliseconds() < duration) {}
        gripMotor.setPower(0);
        pause("motor");
    }
    public void hookOn() {
        leftServo.setPosition(1);
        rightServo.setPosition(0);
        pause("servo");
        pause("servo");
    }
    public void hookOff() {
        leftServo.setPosition(0);
        rightServo.setPosition(1);
        pause("servo");
        pause("servo");
    }
    public void depotToBuildingSite(String alliance, double blocks, double drivePower, double turnPower) {
        switch (alliance) {
            case "blue":
                rotate(-90, turnPower);
                drive("front", blocks, drivePower);
                rotate(90, turnPower);
                break;
            case "red":
                rotate(90, turnPower);
                drive("front", blocks, drivePower);
                rotate(-90, turnPower);
                break;
        }
    }
    public void buildingSiteToDepot(String alliance, double blocks, double drivePower, double turnPower) {
        switch (alliance) {
            case "blue":
                rotate(90, turnPower);
                drive("front", blocks, drivePower);
                rotate(-90, turnPower);
                break;
            case "red":
                rotate(-90, turnPower);
                drive("front", blocks, drivePower);
                rotate(90, turnPower);
                break;
        }
    }
    public void grabFoundation(String alliance) {
        rotate(90, turnPower);
        rotate(90, turnPower);
        drive("back", drivePower, 0.5);
        hookOn();
        drive("front", drivePower, 1);
        switch (alliance) {
            case "blue": rotate(-90, turnPower); break;
            case "red": rotate(90, turnPower); break;
        }
        drive("back", drivePower, 0.25);
        hookOff();
    }
    public void grabSkystone(double power) {
        armExtend();
        armRaise(250);
        gripRelease(500);
        driveDist("front", power, travelY);
        armDrop(250);
        gripHold(500);
        armRaise(500);
        driveDist("back", power, travelY-0.5*inPerBlock);
    }
    public void buildSkystone(double power, int height) {
        int duration = 500 * (height - 1);
        int doubleDuration = duration * 2;
        armRaise(doubleDuration);
        driveDist("front", power, travelY-0.5*inPerBlock);
        armDrop(duration);
        gripRelease(500);
        armRaise(150);
        driveDist("back", power, travelY-0.5*inPerBlock);
        armExtend();
    }
}