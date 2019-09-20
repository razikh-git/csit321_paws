package com.example.csit321_paws;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class PermissionActivity extends AppCompatActivity {

    // Map of request codes and associated permission codes.
    protected Map<String, Integer> mCodeMap;
    protected Map<String, String> mTitleMap;
    protected Map<String, String> mMessageMap;

    // Handler method after requesting permissions.
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Identify full-allowed permission batches.
        if (requestCode == RequestCode.PERMISSION_MULTIPLE) {
            boolean areAllPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    areAllPermissionsGranted = false;
                }
            }

            // All batched permissions were granted.
            if (areAllPermissionsGranted) {
                onAllPermissionsGranted(permissions);
                return;
            }
        }

        // Handle individual permission results.
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                // A permission was granted.
                onPermissionGranted(permissions[i]);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this, permissions[i])) {
                    // A permission was denied.
                    onPermissionDenied(permissions[i]);
                } else {
                    // A permission was permanently denied.
                    onPermissionBlocked(permissions[i]);
                }
            }
        }
    }

    // Returns whether all permissions were granted.
    public boolean checkHasPermissions(int requestCode, String... permissions) {
        // Check against all required permissions.
        List<String> missingPerms = new ArrayList<>();
        if (permissions.length > 0) {
            if (requestCode == RequestCode.PERMISSION_MULTIPLE) {
                for (String perm : permissions) {
                    if (ActivityCompat.checkSelfPermission(this, perm)
                            != PackageManager.PERMISSION_GRANTED) {
                        missingPerms.add(perm);
                    }
                }
            }
        }

        // Attempt to acquire missing permissions.
        if (missingPerms.size() > 0) {
            ActivityCompat.requestPermissions(
                    this, missingPerms.toArray(new String[missingPerms.size()]), requestCode);
        }

        // Return whether all permissions were granted.
        return missingPerms.size() == 0;
    }

    protected void onPermissionDenied(String perm) {
        new AlertDialog.Builder(this)
                .setTitle(mTitleMap.get(perm))
                .setMessage(mMessageMap.get(perm))
                .setPositiveButton(getResources().getString(R.string.app_btn_perm_grant),
                        (dialog, which) -> ActivityCompat.requestPermissions(
                                this, new String[]{perm}, mCodeMap.get(perm)))
                .setNegativeButton(getResources().getString(R.string.app_btn_perm_deny),
                        (dialog, which) -> {})
                .show();
    };

    protected abstract void onPermissionGranted(String perm);
    protected abstract void onPermissionBlocked(String perm);
    protected abstract void onAllPermissionsGranted(String[] permissions);
}
