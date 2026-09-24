package com.example.motionsensorapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // UI Elements
    private lateinit var tvAccelX: TextView
    private lateinit var tvAccelY: TextView
    private lateinit var tvAccelZ: TextView
    private lateinit var tvGyroX: TextView
    private lateinit var tvGyroY: TextView
    private lateinit var tvGyroZ: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDataCount: TextView
    private lateinit var etScenarioName: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnSelectLocation: Button
    private lateinit var btnSave: Button
    private lateinit var btnViewSaved: Button

    // Data storage
    private val sensorDataList = mutableListOf<SensorData>()
    private var isRecording = false
    private var startTime: Long = 0
    private var saveDirectoryUri: Uri? = null

    private val PERMISSION_REQUEST_CODE = 100

    data class SensorData(
        val timestamp: Long,
        val accelX: Float,
        val accelY: Float,
        val accelZ: Float,
        val gyroX: Float,
        val gyroY: Float,
        val gyroZ: Float
    )

    private var lastAccelData = FloatArray(3) { 0f }
    private var lastGyroData = FloatArray(3) { 0f }

    private val selectDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                saveDirectoryUri = it
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                Toast.makeText(this, "Save location selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeSensors()
        setupButtonListeners()
        checkPermissions()
    }

    private fun initializeViews() {
        tvAccelX = findViewById(R.id.tvAccelX)
        tvAccelY = findViewById(R.id.tvAccelY)
        tvAccelZ = findViewById(R.id.tvAccelZ)
        tvGyroX = findViewById(R.id.tvGyroX)
        tvGyroY = findViewById(R.id.tvGyroY)
        tvGyroZ = findViewById(R.id.tvGyroZ)
        tvStatus = findViewById(R.id.tvStatus)
        tvDataCount = findViewById(R.id.tvDataCount)
        etScenarioName = findViewById(R.id.etScenarioName)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnSelectLocation = findViewById(R.id.btnSelectLocation)
        btnSave = findViewById(R.id.btnSave)
        btnViewSaved = findViewById(R.id.btnViewSaved)
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (accelerometer == null) {
            Toast.makeText(this, "Accelerometer not available!", Toast.LENGTH_LONG).show()
        }
        if (gyroscope == null) {
            Toast.makeText(this, "Gyroscope not available!", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtonListeners() {
        btnStart.setOnClickListener {
            startRecording()
        }

        btnStop.setOnClickListener {
            stopRecording()
        }

        btnSelectLocation.setOnClickListener {
            selectSaveLocation()
        }

        btnSave.setOnClickListener {
            saveDataToFile()
        }

        btnViewSaved.setOnClickListener {
            viewSavedFiles()
        }
    }

    private fun selectSaveLocation() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        selectDirectoryLauncher.launch(intent)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startRecording() {
        if (accelerometer == null && gyroscope == null) {
            Toast.makeText(this, "No sensors available!", Toast.LENGTH_SHORT).show()
            return
        }

        sensorDataList.clear()
        isRecording = true
        startTime = System.currentTimeMillis()

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        btnStart.isEnabled = false
        btnStop.isEnabled = true
        btnSave.isEnabled = false
        etScenarioName.isEnabled = false
        tvStatus.text = "Status: Recording..."
        tvDataCount.text = "Data points: 0"

        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        isRecording = false
        sensorManager.unregisterListener(this)

        btnStart.isEnabled = true
        btnStop.isEnabled = false
        btnSave.isEnabled = sensorDataList.isNotEmpty()
        etScenarioName.isEnabled = true
        tvStatus.text = "Status: Stopped"

        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    lastAccelData[0] = it.values[0]
                    lastAccelData[1] = it.values[1]
                    lastAccelData[2] = it.values[2]

                    tvAccelX.text = "X: ${String.format("%.2f", it.values[0])} m/s²"
                    tvAccelY.text = "Y: ${String.format("%.2f", it.values[1])} m/s²"
                    tvAccelZ.text = "Z: ${String.format("%.2f", it.values[2])} m/s²"
                }
                Sensor.TYPE_GYROSCOPE -> {
                    lastGyroData[0] = it.values[0]
                    lastGyroData[1] = it.values[1]
                    lastGyroData[2] = it.values[2]

                    tvGyroX.text = "X: ${String.format("%.2f", it.values[0])} rad/s"
                    tvGyroY.text = "Y: ${String.format("%.2f", it.values[1])} rad/s"
                    tvGyroZ.text = "Z: ${String.format("%.2f", it.values[2])} rad/s"
                }
            }

            if (isRecording) {
                val dataPoint = SensorData(
                    timestamp = System.currentTimeMillis() - startTime,
                    accelX = lastAccelData[0],
                    accelY = lastAccelData[1],
                    accelZ = lastAccelData[2],
                    gyroX = lastGyroData[0],
                    gyroY = lastGyroData[1],
                    gyroZ = lastGyroData[2]
                )
                sensorDataList.add(dataPoint)
                tvDataCount.text = "Data points: ${sensorDataList.size}"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun saveDataToFile() {
        val scenarioName = etScenarioName.text.toString().trim()
        if (scenarioName.isEmpty()) {
            Toast.makeText(this, "Please enter a scenario name", Toast.LENGTH_SHORT).show()
            return
        }

        if (sensorDataList.isEmpty()) {
            Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val filename = "$scenarioName.csv"

            val outputStream: OutputStream? = if (saveDirectoryUri != null) {
                val treeUri = saveDirectoryUri!!
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                val newFileUri = DocumentsContract.createDocument(contentResolver, dirUri, "text/csv", filename)
                newFileUri?.let { contentResolver.openOutputStream(it) }
            } else {
                val appDirectory = File(getExternalFilesDir(null), "SensorData")
                if (!appDirectory.exists()) {
                    appDirectory.mkdirs()
                }
                val file = File(appDirectory, filename)
                file.outputStream()
            }

            outputStream?.let {
                it.bufferedWriter().use { writer ->
                    writer.append("Serial No.,Timestamp (yyyy-MM-dd HH:mm:ss.SSS),AccelX,AccelY,AccelZ,GyroX,GyroY,GyroZ\n")
                    var serialNo = 1
                    sensorDataList.forEach { data ->
                        val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                        val timestampString = timestampFormat.format(Date(startTime + data.timestamp))
                        writer.append("${serialNo++},$timestampString,${data.accelX},${data.accelY},${data.accelZ},")
                        writer.append("${data.gyroX},${data.gyroY},${data.gyroZ}\n")
                    }
                }
                Toast.makeText(this, "Data saved successfully", Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_LONG).show()
            }

            // Clear data after saving
            sensorDataList.clear()
            tvDataCount.text = "Data points: 0"
            btnSave.isEnabled = false
            etScenarioName.text.clear()
            tvStatus.text = "Status: Data saved successfully"

        } catch (e: Exception) {
            Toast.makeText(this, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }


    private fun viewSavedFiles() {
        val appDirectory = File(getExternalFilesDir(null), "SensorData")

        if (!appDirectory.exists() || appDirectory.listFiles()?.isEmpty() == true) {
            Toast.makeText(this, "No saved files found", Toast.LENGTH_SHORT).show()
            return
        }

        val files = appDirectory.listFiles()?.sortedByDescending { it.lastModified() }

        if (files.isNullOrEmpty()) {
            Toast.makeText(this, "No saved files found", Toast.LENGTH_SHORT).show()
            return
        }

        val fileNames = files.map {
            val sizeKB = it.length() / 1024
            "${it.name} (${sizeKB}KB)"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Saved Files (${files.size})")
            .setItems(fileNames) { _, which ->
                val selectedFile = files[which]
                showFileOptions(selectedFile)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showFileOptions(file: File) {
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setMessage("File location:\n${file.absolutePath}\n\nSize: ${file.length() / 1024}KB")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        if (isRecording) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isRecording) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}