/*
 * Devoxx digital signage project
 */
package devoxx;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;

/**
 * The Devoxx control properties.
 * 
 * @author Simon Ritter (@speakjava)
 * @author @stephan007
 */
public class ControlProperties {

    public static final int MODE_REAL = 0;
    public static final int MODE_TEST = 1;

    private static final String MODE_REAL_NAME = "REAL";
    private static final String MODE_TEST_NAME = "TEST";

    private static final String TESTTIME = "test-time";
    private static final String TESTDAY = "test-day";
    private static final String TESTSCALE = "test-scale";
    private static final String OPERATINGMODE = "operating-mode";
    private static final String IMAGECACHE = "image-cache";
    private static final String DEVOXXSTARTDATE = "devoxx-start-date";
    private static final String DEVOXXDATAHOST = "devoxx-data-host";
    private static final String SCREENREFRESHTIME = "screen-refresh-time";
    private static final String DATAREFRESHTIME = "data-refresh-time";
    private static final String LOGGINGLEVEL = "logging-level";
    private static final String DEVOXXHOST = "devoxx-host";

    private final Properties properties = new Properties();

    /* Configurable properties and their defaults */
    private Level loggingLevel = Level.INFO;
    private int mode = MODE_REAL;
    private int dataRefreshTime = 30;
    private int screenRefreshTime = 60;
    // private String dataURL = "http://cfp.devoxx.be/api/conferences/DV15/";
    private String dataURL = "http://cfp.devoxx.co.uk/api/conferences/DevoxxUK2016/";
    private String imageCache = "/home/devoxx/speaker-images";
    private LocalDate startDate;
    private double testScale;
    private int testDay;
    private LocalTime testTime;

    /**
     * Constructor
     *
     * @param propertyFileName The file to load the properties from
     */
    public ControlProperties(final String propertyFileName) {
        
        loadProperties(propertyFileName);

        setProperties();

        printProperties();
    }

    private void setProperties() throws NumberFormatException {
        setDataURL();
        setLoggingLevel();
        setDataRefreshTime();
        setScreenRefreshTime();
        setStartDate();
        setImageCache();
        setRunMode();
        setTestScale();
        setTestDay();
        setTestTime();
    }

    private void loadProperties(final String propertyFileName) {
        /* Start by loading the properties */
        try {
            if (propertyFileName != null) {
                System.out.println("Loading parameters from FILE : " + propertyFileName);
                properties.load(new FileInputStream(propertyFileName));
            } else {
                // Load the properties using the file in the jar file
                System.out.println("Loading parameters from RESOURCES : resources/signage.properties");
                properties.load(Devoxx.class.getResourceAsStream("resources/signage.properties"));
            }
        } catch (IOException ex) {
            System.err.println("ControlProperties: Error reading properties file");
            System.err.println(ex.getMessage());
            System.exit(2);
        }
    }

    private void printProperties() {
        if (loggingLevel != Level.OFF) {
            System.out.println("\nSYSTEM CONFIGURATION");
            System.out.println("====================");
            System.out.println("logging-level       = " + loggingLevel.toString());
            System.out.println("data-refresh-time   = " + dataRefreshTime);
            System.out.println("screen-refresh-time = " + screenRefreshTime);
            System.out.println("devoxx-host         = " + dataURL);
            System.out.println("image-cache         = " + imageCache);
            System.out.println("mode                = " + (mode == MODE_TEST ? "TEST" : "REAL"));

            if (mode == MODE_TEST) {
                System.out.println("test-scale          = " + testScale);
                System.out.println("test-day            = " + testDay);
                System.out.println("test-time           = " + testTime);
            }

            System.out.println();
        }
    }

    private void setTestTime() {
        String value = properties.getProperty(TESTTIME);
        testTime = LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME);
    }
    
    private void setTestDay() throws NumberFormatException {
        String value = properties.getProperty(TESTDAY);
        testDay = Integer.parseInt(value);
    }

    private void setTestScale() throws NumberFormatException {
        String value = properties.getProperty(TESTSCALE);
        testScale = Double.parseDouble(value);
    }
    
    public void toggleRunMode() {
        if (isTestMode()) {
            mode = MODE_REAL;
        } else {
            mode = MODE_TEST;
        }
    }

    private String setRunMode() {
        /* What mode to run in: real or test */
        String value = properties.getProperty(OPERATINGMODE);
        String modeName = null;
        if (value != null) {
            switch (value) {
                case MODE_REAL_NAME:
                    mode = MODE_REAL;
                    modeName = "REAL";
                    break;
                case MODE_TEST_NAME:
                    mode = MODE_TEST;
                    modeName = "TEST";
                    break;
                default:
                    System.out.println("ControlProperties: Unrecognized mode: " + value);
                    break;
            }
        }
        return modeName;
    }

    private void setImageCache() {
        /* Where to store the speaker images for caching */
        String value = properties.getProperty(IMAGECACHE);

        if (value != null) {
            imageCache = value;
        } else {
            imageCache = System.getProperty("user.home") + "/.devoxx-signage";
        }
    }

    private void setStartDate() {

        String value = properties.getProperty(DEVOXXSTARTDATE);

        /**
         * If there is no start date specified then we can go no further
         */
        if (value == null) {
            System.err.println("ERROR: No start date found in config file");
            System.exit(3);
        }

        startDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        System.out.println("Start date is: " + startDate);
    }

    /** 
     * How often do we want to refresh the data on the screen 
     */
    private void setScreenRefreshTime() {
        
        String value = properties.getProperty(SCREENREFRESHTIME);

        try {
            int i = Integer.parseInt(value);
            screenRefreshTime = i;
        } catch (NumberFormatException nfe) {
            System.out.println("ControlProperties: screen-refresh-time is not a number");
        }
    }

    /**
     * How often do we want to refresh the session data (measured in minutes) 
     */
    private void setDataRefreshTime() {
        
        String value = properties.getProperty(DATAREFRESHTIME);

        try {
            int i = Integer.parseInt(value);
            dataRefreshTime = i;
        } catch (NumberFormatException nfe) {
            System.out.println("ControlProperties: data-refresh-time is not a number");
        }
    }

    /**
     * What level of debug messages to log 
     */
    private void setLoggingLevel() {
        
        String value = properties.getProperty(LOGGINGLEVEL);

        switch (value) {
            case "ALL":
                loggingLevel = Level.ALL;
                break;
            case "CONFIG":
                loggingLevel = Level.CONFIG;
                break;
            case "FINE":
                loggingLevel = Level.FINE;
                break;
            case "FINER":
                loggingLevel = Level.FINER;
                break;
            case "FINEST":
                loggingLevel = Level.FINEST;
                break;
            case "INFO":
                loggingLevel = Level.INFO;
                break;
            case "OFF":
                loggingLevel = Level.OFF;
                break;
            case "SEVERE":
                loggingLevel = Level.SEVERE;
                break;
            case "WARNING":
                loggingLevel = Level.WARNING;
                break;
        }
    }

    private void setDataURL() {
        // Host URL
        String hostURL = properties.getProperty(DEVOXXHOST);
        if (!hostURL.isEmpty()) {
            dataURL = hostURL;
        }
    }

    /**
     * Convert a property that represents a boolean into an actual boolean
     *
     * @param key The key for the property
     * @return Whether its value is true or false
     */
    private boolean processBooleanProperty(String key) {
        String value = properties.getProperty(key);

        if (value != null) {
            if (value.compareTo("TRUE") == 0 || value.compareTo("true") == 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get what level of messages should be printed
     *
     * @return the logging level
     */
    public Level getLoggingLevel() {
        return loggingLevel;
    }

    /**
     * Get how long we want to wait between data refreshes
     *
     * @return Time (in minutes) between data refreshes
     */
    public int getDataRefreshTime() {
        return dataRefreshTime;
    }

    /**
     * Get how long we want to wait between updates to the screen display
     *
     * @return Time (in seconds) between screen refreshes
     */
    public int getScreenRefreshTime() {
        return screenRefreshTime;
    }

    /**
     * Get the host name of where to retrieve data from
     *
     * @return The host name of where to retrieve data from
     */
    public String getDevoxxHost() {
        return dataURL;
    }

    public boolean isDevoxxBelgium() {
        return dataURL.contains("devoxx.be");
    }

    public boolean isDevoxxUK() {
        return dataURL.contains("devoxx.co.uk");
    }

    /**
     * Get the start date of Devoxx
     *
     * @return The date when Devoxx starts
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Get the directory where we want to store the speaker photos for caching
     *
     * @return The directory for photo caching
     */
    public String getImageCache() {
        return imageCache;
    }

    /**
     * Are we in test or real mode
     *
     * @return True for test mode
     */
    public boolean isTestMode() {
        return mode == MODE_TEST;
    }

    /**
     * Get the scaling factor to use when testing
     *
     * @return The display scaling factor to use
     */
    public double getTestScale() {
        return testScale;
    }

    /**
     * Get the test day
     *
     * @return The day to use for testing
     */
    public int getTestDay() {
        return testDay;
    }

    /**
     * Get the test time
     *
     * @return The time to use for testing
     */
    public LocalTime getTestTime() {
        return testTime;
    }

    public void incrementTestTime(int minutes) {
        testTime = getTestTime().plusMinutes(minutes);
        if (getTestTime().isBefore(LocalTime.of(0, minutes))) {
            testDay++;
        }
    }

    public void decrementTestTime(int minutes) {
        if (getTestTime().isBefore(LocalTime.of(0, minutes))) {
            testDay--;
        }
        testTime = getTestTime().minusMinutes(minutes);
    }
}
