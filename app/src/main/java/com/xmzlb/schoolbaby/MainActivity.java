package com.xmzlb.schoolbaby;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MEDIA_TYPE_IMAGE = 1;
    private SurfaceView surfaceSv;

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private MyHandler mHandler1; // 延时清空姓名、卡号等数据

    boolean isStartPreview = false; // 是否启动了预览
    boolean isShowPreview = true; // 是否应该显示预览
    boolean pause = false;

    private TextView time;
    private EditText edit;
    private TextView name;
    private TextView num;
    private TextView banji;
    private SimpleDraweeView image;
    private RelativeLayout progressbar;
    private RelativeLayout imarela;
    private MediaPlayer succeed;

    String serial = Build.SERIAL; //设备号
    String carNum = "";
    String imgUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 不显示标题
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏底部导航栏
        getWindow().setFlags(1024, 1024);
        Window localWindow = getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        localLayoutParams.systemUiVisibility = 2;
        localWindow.setAttributes(localLayoutParams);
        setContentView(R.layout.activity_main);

        //xutils 初始化
        x.Ext.init(MainActivity.this.getApplication());

        DisplayMetrics dm = new DisplayMetrics();
        mHandler1 = new MyHandler(this);
        mHandler1.postDelayed(new Runnable() { // 预览卡住时重启预览
            @Override
            public void run() {
                if (isShowPreview) { // 如果是应该显示预览的
                    if (!isStartPreview) { // 却没有启动预览
                        mCamera = getCamera();
                        setStartPreview(mCamera, mHolder);
                    }
                }
                mHandler1.postDelayed(this, 5000);
            }
        }, 5000);


        findById();
        initData();
    }

    /**
     * 初始化view
     */
    private void findById() {
        surfaceSv = (SurfaceView) this.findViewById(R.id.id_area_sv);
        time = (TextView) this.findViewById(R.id.time);
        edit = (EditText) findViewById(R.id.editText1);
        name = (TextView) findViewById(R.id.name);
        num = (TextView) findViewById(R.id.num);
        banji = (TextView) findViewById(R.id.banji);
        image = (SimpleDraweeView) findViewById(R.id.image);
        progressbar = (RelativeLayout) findViewById(R.id.progressbar);
        imarela = (RelativeLayout) findViewById(R.id.imarela);

        edit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 10) {
                    carNum = s.toString();
                    takePic();
                    progressbar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    /**
     * 初始化相关data
     */
    private void initData() {
        // 设置时间
        new TimeThread().start(); // 启动新的线程
        // 获得句柄
        mHolder = surfaceSv.getHolder();
        // 添加回调
        mHolder.addCallback(this);
    }

    class TimeThread extends Thread {
        @Override
        public void run() {
            do {
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = 1; // 消息(一个整型值)
                    mHandler.sendMessage(msg);// 每隔1秒发送一个msg给mHandler
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }

    // 在主线程里面处理消息并更新UI界面
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    long sysTime = System.currentTimeMillis();
                    CharSequence sysTimeStr = DateFormat.format("yyyy 年  MM 月  dd 日   EEEE  HH : mm : ss", sysTime);
                    time.setText(sysTimeStr); // 更新时间
                    break;
                default:
                    break;

            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if (this.checkCameraHardware(this) && (mCamera == null)) {
            // 打开camera
            mCamera = getCamera();
            if (mHolder != null) {
                // setStartPreview(mCamera,mHolder);
            }
        }
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private Camera getCamera() {
        Camera camera = null;
        try {

            int cameraCount = 0;
            Camera cam = null;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    try {
                        camera = Camera.open(camIdx);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            camera = null;
            Log.e(TAG, "Camera is not available (in use or does not exist)");
        }
        return camera;
    }

    @Override
    public void onPause() {
        super.onPause();
        /**
         * 记得释放camera，方便其他应用调用
         */
        releaseCamera();
        isShowPreview = false;
        pause = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isShowPreview = true;
        pause = false;
    }

    @Override
    public void onDestroy() {
        // Remove all Runnable and Message.
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * 释放mCamera
     */
    private void releaseCamera() {
        if (mCamera != null) {
            // mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// 停掉原来摄像头的预览
            isStartPreview = false;
            mCamera.release();
            mCamera = null;
        }
    }

    public void takePic() {
        // 拍照,设置相关参数
//        Camera.Parameters params = mCamera.getParameters();
//        params.setPictureFormat(ImageFormat.JPEG);
//        params.setPreviewSize(480, 640);
        // 自动对焦
//        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//        mCamera.setParameters(params);
        mCamera.takePicture(null, null, this);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
        if (mCamera == null) {
            mCamera = getCamera();
        }
        setStartPreview(mCamera, mHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
/**
 * 记得释放camera，方便其他应用调用
 */
        releaseCamera();
        holder = null;
        surfaceSv = null;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            Uri fromFile = Uri.fromFile(pictureFile);
            String url = fromFile.toString().substring(fromFile.toString().indexOf("///") + 2);

            Bitmap bitmap = BitmapFactory.decodeFile(url);
            imgUrl = url;
            isShowPreview = false;

            // 把图片旋转为正的方向并覆盖原图
            Bitmap bitmap2 = rotateImage(90, bitmap);
            saveBitmap(bitmap2, url);

            String url2 = "http://www.an-edu.com/index.php/PushCard/pushCard";
            RequestParams params = new RequestParams(url2);
            params.setMultipart(true);
            params.addBodyParameter("card_sn", carNum);
            params.addBodyParameter("cmmachime_sn", serial);
//            params.addBodyParameter("pcphoto", new File(url));
            x.http().post(params, new Callback.CommonCallback<String>() {

                @Override
                public void onCancelled(CancelledException arg0) {
                    Log.e("==", "onCancelled");
                }

                @Override
                public void onError(Throwable arg0, boolean arg1) {
                    edit.setText("");
                    isShowPreview = true;
                    if (mCamera == null && !pause) {
                        mCamera = getCamera();
                        setStartPreview(mCamera, mHolder);
                    }
                    Log.e("==", "onError");
                }

                @Override
                public void onFinished() {
                    Log.e("==", "onFinished");
                }

                @Override
                public void onSuccess(String arg0) {
                    Log.e("==", "onSuccess" + arg0);
                    new File(imgUrl).delete(); //发送成功后删除照片
                    progressbar.setVisibility(View.GONE);
                    MyData myData = GsonUtils.parseJSON(arg0, MyData.class);
                    if (myData.getStatus() == 1) {
                        banji.setText(myData.getClassname());
                        name.setText(myData.getName());
                        num.setText(carNum);
                        releaseCamera();
                        imarela.setVisibility(View.VISIBLE);
//                    image.setImageURI(Uri.parse(myData.getPhoto()));

                        // 播放音频
                        succeed = MediaPlayer.create(MainActivity.this, R.raw.su2);
                        succeed.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                // 回收对象
                                succeed.release();
                                succeed = null;
                            }
                        });
                        succeed.start();

                        // 延迟5秒后清空
                        mHandler1.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                name.setText("");
                                num.setText("");
                                banji.setText("");
                                edit.setText("");
                                GenericDraweeHierarchy hierarchy = image.getHierarchy();
                                hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_XY);
                                hierarchy.setPlaceholderImage(R.drawable.bg3);
                                imarela.setVisibility(View.GONE);
                            }
                        }, 5000);

                        mHandler1.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                isShowPreview = true;
                                if (mCamera == null && !pause) {
                                    mCamera = getCamera();
                                    setStartPreview(mCamera, mHolder);
                                }
                            }
                        }, 4000);

                    } else { //刷卡失败
                        // 播放音频
                        succeed = MediaPlayer.create(MainActivity.this, R.raw.fail);
                        succeed.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                // 回收对象
                                succeed.release();
                                succeed = null;
                            }
                        });
                        succeed.start();

                        edit.setText("");

                        GenericDraweeHierarchy hierarchy = image.getHierarchy();
                        hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_XY);
                        hierarchy.setPlaceholderImage(R.drawable.bg3);
                        imarela.setVisibility(View.GONE);

                        isShowPreview = true;
                        if (mCamera == null && !pause) {
                            mCamera = getCamera();
                            setStartPreview(mCamera, mHolder);
                        }
                    }
                }
            });

        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }


    /**
     * Create a File for saving an image or video
     */
    @SuppressLint("SimpleDateFormat")
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        // This location works best if you want the created images to be
        // shared
        // between applications and persist after your app has been
        // uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".png");
        } else {
            return null;
        }
        return mediaFile;
    }

    /**
     * 设置camera显示取景画面,并预览
     *
     * @param camera
     */

    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            camera.setDisplayOrientation(270);
            isStartPreview = true;
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    static class MyHandler extends Handler {
        WeakReference<Activity> mActivityReference;

        MyHandler(Activity activity) {
            mActivityReference = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final Activity activity = mActivityReference.get();
            if (activity != null) {
            }
        }
    }

    public void showShortToast(String str) {
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
    }

    public static Bitmap rotateImage(int angle, Bitmap bitmap) {
        // 图片旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 得到旋转后的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }

    private void saveBitmap(Bitmap bitmap, String bitName) throws IOException {
        File file = new File(bitName);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
