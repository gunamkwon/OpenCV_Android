package com.example.opencv;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.features2d.MSER;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import com.example.opencv.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "opencv";
    private Mat matInput;
    private Mat matResult;

    private CameraBridgeViewBase mOpenCvCameraBackView;
    private int mCameraId = 0;
    private int takeImage;
    public long cascadeClassifier_face, cascadeClassifier_eye;

    PermissionSupport permission;


    public native void convertRGBtoGray(long matAddrInput, long matAddrResult);
    public native void detectAndDraw(long cascadeClassifier_face, long cascadeClassifier_eye, double scale, boolean tryFlip, long overlayImageAddr, long matAddrInput, long matAddrOutput);
    public native void detect(long cascadeClassifier_face, long cascadeClassifier_eye, long matAddrInput, long matAddrResult);
    public native long loadCascade(String cascadeFileName);
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraBackView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swapCamera();
            }
        });
        binding.btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(takeImage == 0)
                    takeImage = 1;
                else
                    takeImage = 0;
            }
        });

        binding.btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"));
                startActivity(mIntent);
            }
        });


        mOpenCvCameraBackView = binding.surfaceViewBack;
        mOpenCvCameraBackView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraBackView.setCvCameraViewListener(this);
        mOpenCvCameraBackView.setCameraIndex(mCameraId); // front-camera(1),  back-camera(0)
    }

    private void swapCamera() {
        mCameraId = mCameraId^1;

        mOpenCvCameraBackView.disableView();
        mOpenCvCameraBackView.setCameraIndex(mCameraId);
        mOpenCvCameraBackView.enableView();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraBackView != null)
            mOpenCvCameraBackView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraBackView != null)
            mOpenCvCameraBackView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        matInput = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        matInput.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();
        /*ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());*/

        // Flip Camera
        if(mCameraId == 1) {
            Core.flip(matInput,matInput,-1);
        }

        // Take Picture
        takeImage = takePictureRGB(takeImage, matInput);

        Mat matOverlay = Imgcodecs.imread("/storage/emulated/0/sunglasses.png", Imgcodecs.IMREAD_UNCHANGED);
        if ( matResult == null )
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        //detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        detectAndDraw(cascadeClassifier_face, cascadeClassifier_eye, 1, false,
                matOverlay.getNativeObjAddr(), matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        return matResult;
    }

    private int takePictureRGB(int takeImage, Mat input) {
        if(takeImage==1) {
            // Create New mat
            Mat saveMat = new Mat();
            // Rotate Image
            Core.flip(input.t(), saveMat, 1);
            // Convert Image RGBA to BGRA
            Imgproc.cvtColor(saveMat, saveMat, Imgproc.COLOR_RGBA2BGRA);

            // Save Image
            File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/ImagePro");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String currentDateAndTime = sdf.format(new Date());
            String fileName = Environment.getExternalStorageDirectory().getPath() + "/ImagePro" +
                currentDateAndTime + ".jpg";

            Log.d("OpenCV","Save: " + fileName);
            Imgcodecs.imwrite(fileName, saveMat);

            takeImage = 0;
        }

        return takeImage;
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraBackView);
    }


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();

                readCascadeFile();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean haveCameraPermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permission = new PermissionSupport(this,this);

                if(!permission.checkPermission()) {
                    permission.requestPermission();
            }
        }

        if (haveCameraPermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if(!permission.permissionResult(requestCode, permissions, grantResults)) {
            permission.showDialogForPermission("앱을 실행하려면 권한을 허가하셔야합니다.", requestCode);
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void copyFile(String fileName) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + fileName;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = assetManager.open(fileName);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while((read = inputStream.read(buffer)) != -1 ) {
                outputStream.write(buffer, 0 ,read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile:: 파일 복사 중 예외 발생" + e.toString());
        }
    }

    private void readCascadeFile() {
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");
        copyFile("sunglasses.png");

        cascadeClassifier_face = loadCascade("haarcascade_frontalface_alt.xml");
        cascadeClassifier_eye = loadCascade("haarcascade_eye_tree_eyeglasses.xml");
    }

}