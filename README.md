# Motion Sensor Recorder

An Android application designed to capture, display, and log real-time telemetry data from a device's accelerometer and gyroscope sensors. This tool is ideal for researchers, developers, or anyone interested in analyzing motion data.

## Features

*   **Real-time Visualization:** Displays live X, Y, and Z axis data for both the Accelerometer and Gyroscope.
*   **Precise Data Logging:** Records sensor events with millisecond-accurate timestamps (`yyyy-MM-dd HH:mm:ss.SSS`) and sequential indexing.
*   **Structured CSV Export:** Saves recorded data into standard CSV format for easy analysis in tools like Excel, Python (Pandas), or MATLAB.
*   **Custom Save Locations:** Leverages the Android **Storage Access Framework (SAF)** to allow users to select their preferred storage directory on the device.
*   **Scenario Management:** Tag your recordings with custom scenario names (e.g., "walking", "running") for better data organization.
*   **File Management:** Built-in viewer to browse and manage previously saved sensor data files.

## Technical Details

*   **Language:** Kotlin
*   **Framework:** Android SDK
*   **Sensors:** `TYPE_ACCELEROMETER`, `TYPE_GYROSCOPE`
*   **Permissions:** 
    *   `WRITE_EXTERNAL_STORAGE` (Legacy support)
    *   Scoped Storage (via Storage Access Framework)
*   **Build System:** Gradle (Kotlin DSL)

## How to Use

1.  **Select Location:** Open the app and tap "Select Location" to choose a folder on your device where data should be saved.
2.  **Name Your Scenario:** Enter a descriptive name in the "Scenario name" field.
3.  **Start Recording:** Tap "Start Recording". The app will begin capturing sensor data.
4.  **Stop Recording:** Tap "Stop" when you have finished your motion activity.
5.  **Save Data:** Tap "Save Data". A CSV file named `[ScenarioName].csv` will be created in your selected folder.
6.  **View Files:** Use the "View Saved Files" button to see a list of recordings stored in the app's default directory.

## Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/your-username/MotionSensorRecorder.git
    ```
2.  Open the project in **Android Studio**.
3.  Build and run the project on a physical Android device (recommended for accurate sensor readings).


