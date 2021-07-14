package com.example.cameraxexample

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.rangeTo
import com.example.cameraxexample.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var isBackCameraOpened = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }


        binding.cameraCaptureButton.setOnClickListener { takePhoto() }
        binding.imageViewChangeCamera.setOnClickListener {
            if(isBackCameraOpened) {
                startFrontCamera()
                isBackCameraOpened = false
            } else {
                startCamera()
                isBackCameraOpened = true
            }
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()


    }

    private fun takePhoto() {
        val imageCap = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILE_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outPutOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCap.takePicture(
            outPutOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Toast.makeText(this@MainActivity, "$savedUri", Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, "Couldn't save picture", Toast.LENGTH_LONG)
                        .show()
                }

            }
        )
    }

    private fun startCamera() {
        val cameraProvideFuture = ProcessCameraProvider.getInstance(this)

        cameraProvideFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProvideFuture.get()

            imageCapture = ImageCapture.Builder()
                .build()

            val scaleGestureDetector = ScaleGestureDetector(this,cameraGestureListener)

            binding.viewFinder.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                return@setOnTouchListener true
            }

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview,imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Use case Binding Failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startFrontCamera() {
        val cameraProvideFuture = ProcessCameraProvider.getInstance(this)

        cameraProvideFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProvideFuture.get()

            imageCapture = ImageCapture.Builder()
                .build()


            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview,imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Use case Binding Failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Please Grant Permissions", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    val cameraGestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @SuppressLint("RestrictedApi")
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            val scale = detector?.scaleFactor?.let {
                imageCapture?.camera?.cameraInfo?.zoomState?.value?.zoomRatio?.times(
                    it
                )
            }
            imageCapture?.camera?.cameraControl?.setZoomRatio(scale!!)
            return true
        }

    }

    companion object {
        private const val TAG = "CAMERA"
        private const val FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}