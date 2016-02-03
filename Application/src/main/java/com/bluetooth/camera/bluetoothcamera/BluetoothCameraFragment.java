package com.bluetooth.camera.bluetoothcamera;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bluetooth.camera.common.logger.Log;
import com.example.android.bluetoothchat.R;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class BluetoothCameraFragment extends Fragment implements SurfaceHolder.Callback, Camera.PreviewCallback {


    TextView testView;
    Camera camera;
    SurfaceView surfaceView;
    SurfaceView surfaceView2;
    SurfaceHolder surfaceHolder;
    SurfaceHolder surfaceHolder2;
    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;
    Camera.PictureCallback jpegCallback;
    private final String tag = "tagg";

    Button start, stop, capture;

    private static final String TAG = "BluetoothCamera";


    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;


    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private ImageView imageview;


    private String mConnectedDeviceName = null;


    private ArrayAdapter<String> mConversationArrayAdapter;


    private StringBuffer mOutStringBuffer;


    private BluetoothAdapter mBluetoothAdapter = null;


    private BluetoothCameraManager mCameraService = null;
    private boolean isCameraRunning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        } else if (mCameraService == null) {
            //setup();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraService != null) {
            mCameraService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCameraService != null) {

            if (mCameraService.getState() == BluetoothCameraManager.STATE_NONE) {

                mCameraService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_camera, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        mSendButton = (Button) view.findViewById(R.id.button_send);
        stop = (Button) view.findViewById(R.id.stop);
        capture = (Button) view.findViewById(R.id.capture);
        imageview = (ImageView) view.findViewById(R.id.previewImage);
        surfaceView = (SurfaceView) view.findViewById(R.id.surfaceview);
        surfaceView2 = (SurfaceView) view.findViewById(R.id.surfaceview2);
        setup();
    }


    private void setup() {
        Log.d(TAG, "setup()");

        mSendButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View arg0) {
                //sendMessage("start-camera".getBytes());
                //new IActionListner().onStartAction();
                start_camera();
            }
        });

        /*View view = getView();
        if (null != view) {
            stop = (Button)view.findViewById(R.id.stop);
            capture = (Button) view.findViewById(R.id.capture);
        }*/

        stop.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View arg0) {
                sendMessage("stop-camera".getBytes());
            }
        });
        capture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendMessage("take-picture".getBytes());
            }
        });


        surfaceHolder = surfaceView.getHolder();
        surfaceHolder2 = surfaceView2.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder2.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder2.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        rawCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d("Log", "onPictureTaken - raw");
               // camera.setPreviewCallback(Camera.PreviewCallback cb);
            }
        };

        shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
                Log.i("Log", "onShutter'd");
            }
        };
        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(String.format(
                            "/sdcard/%d.jpg", System.currentTimeMillis()));
                    outStream.write(data);
                    outStream.close();
                    Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                }
                Log.d("Log", "onPictureTaken - jpeg");
            }
        };


        mCameraService = new BluetoothCameraManager(getActivity(), mHandler);


        mOutStringBuffer = new StringBuffer("");
    }


    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    private void sendMessage(byte[] preview) {

        if (mCameraService.getState() != BluetoothCameraManager.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("sttatt", "sending data");
        mCameraService.write(preview);


    }


    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {

            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();

            }
            return true;
        }
    };


    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }


    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothCameraManager.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));

                            break;
                        case BluetoothCameraManager.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothCameraManager.STATE_LISTEN:
                        case BluetoothCameraManager.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.START_CAMERA_SERVICE:

                    start_camera();
                    /*byte[] readBuf = (byte[]) msg.obj;
                    String command = new String(readBuf).toString();
                    Log.d("cammy", "" + command);
                    if (command.equals("start-camera")) {
                        Log.d("cammy", "Startcam");
                        `mera();
                        Toast.makeText(getActivity(), "starting camera", Toast.LENGTH_LONG).show();

                    } else if (command.equals("stop-camera")) {
                        Log.d("cammy", "Stopcam");
                        stop_camera();
                        Toast.makeText(getActivity(), "stopping camera", Toast.LENGTH_LONG).show();
                    } else if (command.equals("take-picture")) {
                        Log.d("cammy", "takepic");
                        captureImage();
                        Toast.makeText(getActivity(), "Take picture", Toast.LENGTH_LONG).show();
                    } else {
                        Log.d("cammy", "No trigger");
                    }
*/
                    break;
                case Constants.STOP_CAMERA:

                    break;
                case Constants.TAKE_PICTURE:

                    break;
                case Constants.MESSAGE_WRITE:

                    mSendButton.setClickable(false);
                    Log.d("sttatt", "writing data");
                    break;
                case Constants.MESSAGE_READ:


                    break;
                case Constants.MESSAGE_DEVICE_NAME:

                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.CAMERA_PREVIEW:

                    byte[] data = (byte[]) msg.obj;

                    /*final int[] rgb = decodeYUV420SP(data, 176, 144);

                    Bitmap bmp = Bitmap.createBitmap(rgb, 176, 144,Bitmap.Config.ARGB_8888);
                    imageview.setImageBitmap(bmp);*/

                   /* ByteArrayOutputStream out = new ByteArrayOutputStream();
                    YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, 75, 75, null);
                    yuvImage.compressToJpeg(new Rect(0, 0, 75, 75), 20, out);
                    byte[] imageBytes = out.toByteArray();
                    Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);*/

                    Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
                    imageview.setImageBitmap(image);

                    /*Bitmap bitmap = BitmapFactory.decodeByteArray(readBuf, 0, readBuf.length);

                    if(bitmap!=null){
                        imageview.setImageBitmap(bitmap);
                    }else {
                        Log.d("immgbitt","null"+bitmap);
                    }*/

                    Log.d("immgbitt","Preview");

                    /*if(!isCameraRunning){
                        camera = Camera.open();
                    }
                    Camera.Parameters param;
                    param = camera.getParameters();

                    param.setPreviewFrameRate(10);
                    param.setPreviewSize(176, 144);
                    camera.setParameters(param);

                    Camera.Size size = param.getPreviewSize();
                    YuvImage image = new YuvImage(data, param .getPreviewFormat(),
                            size.width, size.height, null);
                    File file = new File(Environment.getExternalStorageDirectory(), "out"+System.currentTimeMillis()+".jpg");
                    FileOutputStream filecon = null;
                    try {
                        filecon = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    image.compressToJpeg(
                            new Rect(0, 0, image.getWidth(), image.getHeight()), 90,
                            filecon);
                    Picasso.with(getActivity()).load(file).
                            error(R.drawable.ic_launcher)
                            .into(imageview);*/

                    /*Log.d("cam_pree",""+readBuf.toString());
                    Bitmap bmp;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    bmp = BitmapFactory.decodeByteArray(readBuf, 0, readBuf.length, options);
                    Canvas canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawBitmap(bmp, 0, 0, null);
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }*/
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:

                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:

                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:

                if (resultCode == Activity.RESULT_OK) {
                    setup();
                } else {

                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }


    private void connectDevice(Intent data, boolean secure) {

        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        mCameraService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_camera, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {

                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {

                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {

                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }


    private void captureImage() {
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    private void start_camera() {
        try {
            camera = Camera.open();
            isCameraRunning = true;
        } catch (RuntimeException e) {
            Log.e(tag, "init_camera: " + e);
            return;
        }
        Camera.Parameters param;
        param = camera.getParameters();
        param.setPreviewFrameRate(10);
        param.setPreviewSize(176, 144);
        camera.setParameters(param);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {

                    /*Camera.Parameters parameters = camera.getParameters();
                    Camera.Size size = parameters.getPreviewSize();
                    YuvImage image = new YuvImage(data, parameters.getPreviewFormat(),
                            size.width, size.height, null);
                    File file = new File(Environment.getExternalStorageDirectory(), "out"+System.currentTimeMillis()+".jpg");
                    FileOutputStream filecon = null;
                    try {
                        filecon = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    image.compressToJpeg(
                            new Rect(0, 0, image.getWidth(), image.getHeight()), 90,
                            filecon);

                    Picasso.with(getActivity()).load(file).into(imageview);*/



                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, 176, 144, null);
                    yuvImage.compressToJpeg(new Rect(0, 0, 176, 144), 50, out);
                    byte[] imageBytes = out.toByteArray();
                    Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    imageview.setImageBitmap(image);
                    Log.d("wrrr","wriTing");
                    mCameraService.write(imageBytes);
                }
            });
            camera.startPreview();

        } catch (Exception e) {
            Log.e(tag, "init_camera: " + e);
            return;
        }
    }

    private void stop_camera() {
        camera.stopPreview();
        camera.release();
    }

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

    }

    public void surfaceCreated(SurfaceHolder holder) {

    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }


    public int[] decodeYUV420SP( byte[] yuv420sp, int width, int height) {

        final int frameSize = width * height;

        int rgb[]=new int[width*height];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
                        0xff00) | ((b >> 10) & 0xff);


            }
        }
        return rgb;
    }
}
