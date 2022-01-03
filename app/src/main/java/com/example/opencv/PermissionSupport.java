package com.example.opencv;

import static android.Manifest.permission.CAMERA;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionSupport {
    private Context mContext;
    private Activity mActivity;

    private String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    //여기서부턴 퍼미션 관련 메소드
    private static final int MULTIPLE_PERMISSION_REQUEST_CODE = 1023;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int WRITE_PERMISSION_REQUEST_CODE = 201;
    private static final int READ_PERMISSION_REQUEST_CODE = 201;

    private List permissionList;

    public PermissionSupport(Activity activity, Context context) {
        mContext = context;
        mActivity = activity;
    }

    public boolean checkPermission() {
        int result;
        permissionList = new ArrayList<>();

        for(String permission: permissions) {
            result = ContextCompat.checkSelfPermission(mContext, permission);
            if(result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }

        if(!permissionList.isEmpty()) {
            return false;
        }
        return true;
    }

    public void requestPermission() {
        ActivityCompat.requestPermissions(mActivity, (String[]) permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSION_REQUEST_CODE);
    }

    public boolean permissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResult) {
        if(requestCode == MULTIPLE_PERMISSION_REQUEST_CODE && (grantResult.length > 0)) {
            for(int i=0; i<grantResult.length; i++) {
                if(grantResult[i] == -1) {
                    return false;
                }
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void showDialogForPermission(String msg, int requestCode) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermission();
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                mActivity.finish();
            }
        });
        builder.create().show();
    }
}
