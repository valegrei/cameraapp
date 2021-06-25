package com.example.mycameraapp;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.math.MathUtils;
import androidx.fragment.app.Fragment;

import com.example.mycameraapp.databinding.FragmentCamaraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CamaraFragment extends Fragment {

    private ImageCapture imageCapture;
    private File outputDirectory;
    private ExecutorService cameraExecutor;
    private static final String TAG = "CameraXBasic";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final int REQUEST_CODE_PERMISSION = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private Camera mCamera;
    private FragmentCamaraBinding binding;
    private CamaraFragmentListener mListener;

    public CamaraFragment() {
        // Required empty public constructor
    }

    public static CamaraFragment newInstance() {
        return new CamaraFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentCamaraBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Request camera permissions
        if(allPermissionsGranted()){
            startCamera();
        }else{
            mPermissionResult.launch(Manifest.permission.CAMERA);
        }

        // Set up the listener for take photo button
        binding.cameraCaptureButton.setOnClickListener(v -> takePhoto());

        setupViewFinderGestureControls();
        setUpFlashButton();
        //updateButtonsUi();

        outputDirectory = getOutputDirectory();

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }
        // Create time-stamped output file to hold the image
        File photoFile = new File(
                outputDirectory,
                new SimpleDateFormat(FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".jpg");
        // Crea objeto de opciones de salida, el cual contiene file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Establece el listener de captura de imagen, el cual es accionado despues
        // que la foto fue tomada
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
                String msg = "Photo capture succeeded: "+savedUri.toString();
                //Toast.makeText(context(), msg, Toast.LENGTH_SHORT).show();
                Log.d(TAG, msg);
                mListener.mostrarFoto(photoFile.getAbsolutePath());
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: "+exception.getMessage(), exception);
            }
        });
    }

    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context());

        cameraProviderFuture.addListener(() -> {
            try {
                // Se usa para enlazar el ciclo de vida de las camaras al ciclo de vida poseedor
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
                // Selecciona la camara trasera por defecto
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Inicia use case de Captura de imagen
                imageCapture = new ImageCapture.Builder().build();

                // Evento de cambio de orientacion
                OrientationEventListener orientationEventListener = new OrientationEventListener(context()) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        int rotation;

                        // Monitors orientation values to determine the target rotation value
                        if (orientation >= 45 && orientation < 135) {
                            rotation = Surface.ROTATION_270;
                        } else if (orientation >= 135 && orientation < 225) {
                            rotation = Surface.ROTATION_180;
                        } else if (orientation >= 225 && orientation < 315) {
                            rotation = Surface.ROTATION_90;
                        } else {
                            rotation = Surface.ROTATION_0;
                        }

                        imageCapture.setTargetRotation(rotation);

                    }
                };
                // Habilita evento de cambio de orientacion
                orientationEventListener.enable();

                // Desenlaza casos de usos antes de reenlazar
                cameraProvider.unbindAll();

                // Enlaza los casos de uso a la camara
                mCamera = cameraProvider.bindToLifecycle(
                        CamaraFragment.this,cameraSelector,preview,imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        },ContextCompat.getMainExecutor(context()));
    }

    private void setupViewFinderGestureControls() {
        GestureDetector tapGestureDetector = new GestureDetector(context(), onTapGestureListener);
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context(),mScaleGestureListener);
        binding.viewFinder.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean tapEventProcessed = tapGestureDetector.onTouchEvent(event);
                boolean scaleEventProcessed = scaleGestureDetector.onTouchEvent(event);
                return tapEventProcessed || scaleEventProcessed;
            }
        });
    }

    // Listen to pinch gestures
    private ScaleGestureDetector.SimpleOnScaleGestureListener mScaleGestureListener =
            new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                @SuppressLint("RestrictedApi")
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    if(mCamera==null){
                        return true;
                    }

                    CameraInfo cameraInfo = mCamera.getCameraInfo();
                    CameraControl cameraControl = mCamera.getCameraControl();
                    float newZoom =
                            cameraInfo.getZoomState().getValue().getZoomRatio()
                                    * detector.getScaleFactor();
                    float clampedNewZoom  = MathUtils.clamp(newZoom,
                            cameraInfo.getZoomState().getValue().getMinZoomRatio(),
                            cameraInfo.getZoomState().getValue().getMaxZoomRatio());

                    Log.d(TAG,"setZoomRatio ratio: " + clampedNewZoom );
                    //showNormalZoomRatio()
                    ListenableFuture<Void> listenableFuture = cameraControl.setZoomRatio(clampedNewZoom );
                    Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            Log.d(TAG, "setZoomRatio onSuccess: " + clampedNewZoom);
                            //showZoomRatioIsAlive();
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.d(TAG, "setZoomRatio failed, " + t);
                        }
                    },ContextCompat.getMainExecutor(context()));

                    return true;
                }
            };

    GestureDetector.OnGestureListener onTapGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @SuppressLint("RestrictedApi")
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    // Since we are showing full camera preview we will be using
                    // DisplayOrientedMeteringPointFactory to map the view's (x, y) to a
                    // metering point.
                    MeteringPointFactory factory =
                            new DisplayOrientedMeteringPointFactory(
                                    binding.viewFinder.getDisplay(),
                                    mCamera.getCameraInfo(),
                                    binding.viewFinder.getWidth(),
                                    binding.viewFinder.getHeight());
                    FocusMeteringAction action = new FocusMeteringAction.Builder(
                            factory.createPoint(e.getX(), e.getY())
                    ).build();

                    animateFocusRing(e.getX(),e.getY());

                    Futures.addCallback(
                            mCamera.getCameraControl().startFocusAndMetering(action),
                            new FutureCallback<FocusMeteringResult>() {
                                @Override
                                public void onSuccess(FocusMeteringResult result) {
                                    Log.d(TAG, "Focus and metering succeeded.");
                                }
                                @Override
                                public void onFailure(Throwable t) {
                                    Log.e(TAG, "Focus and metering failed.", t);
                                }
                            },
                            CameraXExecutors.mainThreadExecutor());
                    return true;
                }
            };

    private void animateFocusRing(float x, float y) {

        // Move the focus ring so that its center is at the tap location (x, y)
        float width = binding.focusRing.getWidth();
        float height = binding.focusRing.getHeight();
        binding.focusRing.setX(x - width / 2);
        binding.focusRing.setY(y - height / 2);

        // Show focus ring
        binding.focusRing.setVisibility(View.VISIBLE);
        binding.focusRing.setAlpha(1F);

        // Animate the focus ring to disappear
        binding.focusRing.animate()
                .setStartDelay(500)
                .setDuration(300)
                .alpha(0F)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        binding.focusRing.setVisibility(View.GONE);
                    }
                    // The rest of AnimatorListener's methods.
                    @Override
                    public void onAnimationStart(Animator animation) {}
                    @Override
                    public void onAnimationCancel(Animator animation) {}
                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });
    }

    private void updateButtonsUi(){
        // Flash button
        binding.btnFlash.setEnabled(isFlashAvailable());
        switch (imageCapture.getFlashMode()) {
            case ImageCapture.FLASH_MODE_ON:
                binding.btnFlash.setImageResource(R.drawable.ic_flash_on);
                break;
            case ImageCapture.FLASH_MODE_OFF:
                binding.btnFlash.setImageResource(R.drawable.ic_flash_off);
                break;
            case ImageCapture.FLASH_MODE_AUTO:
                binding.btnFlash.setImageResource(R.drawable.ic_flash_auto);
                break;
        }
    }

    private void setUpFlashButton() {
        binding.btnFlash.setOnClickListener(v -> {
            @ImageCapture.FlashMode int flashMode = imageCapture.getFlashMode();
            if (flashMode == ImageCapture.FLASH_MODE_ON) {
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_OFF);
            } else if (flashMode == ImageCapture.FLASH_MODE_OFF) {
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_AUTO);
            } else if (flashMode == ImageCapture.FLASH_MODE_AUTO) {
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON);
            }
            updateButtonsUi();
        });
    }

    private boolean isFlashAvailable() {
        CameraInfo cameraInfo = mCamera.getCameraInfo();
        return cameraInfo != null && cameraInfo.hasFlashUnit();
    }

    private boolean allPermissionsGranted(){
        return ContextCompat.checkSelfPermission(context(),Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED;

    }

    private final ActivityResultLauncher<String> mPermissionResult = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if(result){
                    startCamera();
                }else {
                    mListener.cerrar();
                }
            }
    );

    private File getOutputDirectory(){
        return context().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CamaraFragmentListener) {
            mListener = (CamaraFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement CamaraFragmentListener");
        }
    }

    public Context context(){
        return getContext();
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface CamaraFragmentListener {
        void cerrar();
        void mostrarFoto(String filePath);
    }
}