package com.hadders.camerareverse;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.IOException;

/**
 * @author Steven Hadley
 *         <p/>
 *         Shows an image preview NOT mirrored.
 *         This is useful if you want to capture or scan images from a front-facing camera
 */
public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private Camera mCamera;
    private TextureView mTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_screen);
        mTextureView = (TextureView) findViewById(R.id.camera_texture);
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }

        if (null == mCamera) {
            Log.e("camera-reverse", "no front facing camera");
            Toast toast = Toast.makeText(this.getApplicationContext(), "No front facing camera", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }

        /*
           This doesn't work
           setCameraDisplayOrientation(info, mCamera);
         */

        //make portrait
        mCamera.setDisplayOrientation(90);
        //create a matrix to invert the x-plane
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        //move it back to in view otherwise it'll be off to the left.
        matrix.postTranslate(width, 0);
        mTextureView.setTransform(matrix);

        try {
            /* Tell the camera to write onto our textureView mTextureView */
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        } catch (IOException ioe) {
            Log.e("camera-reverse", ioe.getMessage());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (null != mCamera) {
            mCamera.stopPreview();
            mCamera.release();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        //This is where you get the image to check for barcode

        // read the image from the SurfaceTexture
        Bitmap barcodeBmp = mTextureView.getBitmap();

        //get pixel array
        int width = barcodeBmp.getWidth();
        int height = barcodeBmp.getHeight();
        int[] pixels = new int[barcodeBmp.getHeight() * barcodeBmp.getWidth()];
        barcodeBmp.getPixels(pixels, 0, width, 0, 0, width, height);

        /*
        If using zbar barcode processing library.

        // create a barcode image
		Image barcode = new Image(width, height, "RGB4");
		barcode.setData(pixels);
		int result = mScanner.scanImage(barcode.convert("Y800"));
         */

    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * From http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation%28int%29
     *
     * @param info
     * @param camera
     */
    public void setCameraDisplayOrientation(
            Camera.CameraInfo info, android.hardware.Camera camera) {

        int rotation = this.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        result = (info.orientation + degrees) % 360;
        result = (360 - result) % 360;  // compensate the mirror

        camera.setDisplayOrientation(result);
    }
}
