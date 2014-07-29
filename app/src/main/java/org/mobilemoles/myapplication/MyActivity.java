package org.mobilemoles.myapplication;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import org.mobilemoles.myapplication.R;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.net.Uri;


public class MyActivity extends Activity implements SurfaceHolder.Callback, OnClickListener{

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;
    private LayoutInflater controlInflater = null;
    private String mCurrentPhotoPath;
    private Button buttonTakePicture;
    private ImageView imageViewCamera;
    private File imageFileFolder, imageFileName;
    private MediaScannerConnection msConn;
    private int[] pictureIntArray;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView)findViewById(R.id.camerapreview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        //surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.control, null);
        @SuppressWarnings("deprecation")
        LayoutParams layoutParamsControl
                = new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);

        buttonTakePicture = (Button)findViewById(R.id.takepicture);
        buttonTakePicture.setOnClickListener(this);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        //If you want, the MIME type will also be decoded (if possible)
        String type = options.outMimeType;

    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        if(previewing){
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null){
            try {
                camera.setDisplayOrientation(90);
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera = Camera.open();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        //camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
        //Giving errors trying to switch
        //setContentView(R.layout.afterpicture);
    }



    @Override
    public void onClick(View v) {
        camera.takePicture(null, null, capturedIt);
    }

    private PictureCallback capturedIt = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmapFirst = BitmapFactory.decodeByteArray(data, 0, data.length);

            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap bitmap = Bitmap.createBitmap(bitmapFirst, 0, 0,
                    bitmapFirst.getWidth(), bitmapFirst.getHeight(),
                    matrix, true);

            if (bitmap == null) {
                Toast.makeText(getApplicationContext(), "not taken", Toast.LENGTH_SHORT).show();
            } else {
                bitmap = changeBitmapContrastBrightness(bitmap, (float) .4, 200);
                int numFingers = numFingers(bitmap);
                savePhoto(bitmap);
                storeByteArray(bitmap);
                Toast.makeText(getApplicationContext(),"Number of Fingers: " + numFingers + ".", Toast.LENGTH_LONG).show();
                Log.i("MyApp", "NumFingers: " + numFingers);
            }
            camera.startPreview();
        }
    };


    private void storeByteArray(Bitmap bitmap) {
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        //draw the Bitmap, unchanged, in the ImageView
        //iv_originalImage.setImageBitmap(bmp);

        //Initialize the intArray with the same size as the number of pixels on the image
        pictureIntArray = new int[bitmap.getWidth()*bitmap.getHeight()];

        //copy pixel data from the Bitmap into the 'intArray' array
        bitmap.getPixels(pictureIntArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int palmColor = findPalmColor(pictureIntArray,bitmap.getWidth(), bitmap.getHeight());
    }

    public static Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness) {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }

    private int findPalmColor(int [] pixels, int width, int height)
    {
        int picsize = width * height;
        int[] colors = new int[255];
        int[] numColors = new int[255];
        for (int i = 0; i < 255; i++)
        {
            numColors[i] = 0;
        }
        int numberOfColors = 0;
        colors[0] = pixels[0];
        numColors[0] = 1;
        for(int j = 0; j < width; j++)
        {
            for(int k = 0; k < height; k++)
            {
                int index = j + k * width;

                if((k >= (2*height/3))&&(k <= (8*height/9))&&(j >=(width/3)&&(j <= (2*width/3))))
                {

                    if(isNewColor(pixels[index], colors))
                    {
                        numberOfColors++;
                        if(numberOfColors < 255)
                        {
                            colors[numberOfColors] = pixels[index];
                        }
                    }
                    else
                    {
                        for(int i = 0; i < 255; i++)
                        {
                            if(colors[i] == pixels[index])
                            {
                                numColors[i]++;
                            }
                        }
                    }
                }
            }
        }
        int primaryColorIndex = 0;
        int primaryColor = 0;
        for(int i = 0; i < 255; i++)
        {
            if(numColors[i] > primaryColor)
            {
                primaryColor = numColors[i];
                primaryColorIndex = i;
            }
        }

        System.out.println("Number of colors: " + numberOfColors);

        return pixels[primaryColorIndex];
    }



    boolean isNewColor(int color, int []colors)
    {
        for(int i = 0; i < 255; i++)
        {
            if(colors[i] == color)
            {
                return false;
            }
        }
        return true;
    }

    public int[] getIntArray(){
        return pictureIntArray;
    }


    public void savePhoto(Bitmap bmp)
    {
        imageFileFolder = new File(Environment.getExternalStorageDirectory(),"Rotate");
        imageFileFolder.mkdir();
        FileOutputStream out = null;
        Calendar c = Calendar.getInstance();
        String date = fromInt(c.get(Calendar.MONTH))
                + fromInt(c.get(Calendar.DAY_OF_MONTH))
                + fromInt(c.get(Calendar.YEAR))
                + fromInt(c.get(Calendar.HOUR_OF_DAY))
                + fromInt(c.get(Calendar.MINUTE))
                + fromInt(c.get(Calendar.SECOND));
        imageFileName = new File(imageFileFolder, date.toString() + ".jpg");

        try
        {
            out = new FileOutputStream(imageFileName);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            scanPhoto(imageFileName.toString());
            out = null;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public String fromInt(int val)
    {
        return String.valueOf(val);
    }


    public void scanPhoto(final String imageFileName)
    {
        msConn = new MediaScannerConnection(MyActivity.this,new MediaScannerConnectionClient(){

            public void onMediaScannerConnected()
            {
                msConn.scanFile(imageFileName, null);
                Log.i("msClient obj  in Photo Utility","connection established");
            }
            public void onScanCompleted(String path, Uri uri)
            {
                msConn.disconnect();
                Log.i("msClient obj in Photo Utility","scan completed");
            }
        });
        msConn.connect();
    }

    public int numFingers(Bitmap img)
    {
        int x1p = 710, x2p = 1450, y1p = 1600, y2p = 2100;
        int x1t = 1610, x2t = 1870, y1t = 1340, y2t = 1410;
        int x1f = 1350, x2f = 1500, y1f = 1180, y2f = 700;
        int x1m = 1090, x2m = 1260, y1m = 650, y2m = 1090;
        int x1r = 890, x2r = 1020, y1r = 770, y2r = 1200;
        int x1pi = 630, x2pi = 750, y1pi = 1100, y2pi = 1260;

        int numMatches = 0;
        int palm = majorColor(img, x1p, x2p, y1p, y2p);
        int thumb = majorColor(img, x1t, x2t, y1t, y2t);
        int fore = majorColor(img, x1f, x2f, y1f, y2f);
        int middle = majorColor(img, x1m, x2m, y1m, y2m);
        int ring = majorColor(img, x1r, x2r, y1r, y2r);
        int pinkie = majorColor(img, x1pi, x2pi, y1pi, y2pi);
		/*
		System.out.println("Major color: "+Integer.toHexString(palm));
		System.out.println("Major color: "+Integer.toHexString(thumb));
		System.out.println("Major color: "+Integer.toHexString(fore));
		System.out.println("Major color: "+Integer.toHexString(middle));
		System.out.println("Major color: "+Integer.toHexString(ring));
		System.out.println("Major color: "+Integer.toHexString(pinkie));
		*/
        if(isWithinRGBRange(palm, thumb))
        {
            numMatches++;
        }
        if(isWithinRGBRange(palm, fore))
        {
            numMatches++;
        }
        if(isWithinRGBRange(palm, middle))
        {
            numMatches++;
        }
        if(isWithinRGBRange(palm, ring))
        {
            numMatches++;
        }
        if(isWithinRGBRange(palm, pinkie))
        {
            numMatches++;
        }

        return numMatches;
    }
    public int majorColor(Bitmap img, int x1, int x2, int y1, int y2)
    {
        int rgb;
        int red;
        int green;
        int blue;

        int numberOfColors = 1;
        int[] colors = new int[1000];
        int[] numColors = new int[1000];
        for(int i = 0; i < 255; i++)
        {
            numColors[i] = 0;
        }
        colors[0] = img.getPixel(0,0);
        //System.out.println("first color: "+Integer.toHexString(colors[0]));
        numColors[0] = 1;

        for (int h = y1; h<y2; h++)
        {
            for (int w = x1; w<x2; w++)
            {
                rgb = img.getPixel(w, h);
                if(isNewColor(rgb, colors, numColors))
                {
                    numberOfColors ++;
                    if(numberOfColors <=1000)
                    {
                        colors[numberOfColors-1] = rgb;
                    }
                }
            }
        }
        return mostCommonColor(numColors, colors);
    }
    public static boolean isNewColor(int color, int[] colors, int[] numColors)
    {
        for(int i = 0; i<255; i++)
        {
            if(color == colors[i])
            {
                numColors[i]++;
                return false;
            }
        }
        //.out.println("New Color: "+Integer.toHexString(color));
        return true;
    }
    public int mostCommonColor(int[] numColors, int[] colors)
    {
        int most = 0;
        int mostIndex = - 99;
        for(int i = 0 ; i < 1000; i++)
        {
            if(numColors[i] > most)
            {
                most = numColors[i];
                mostIndex = i;
            }
        }
        int rgb = colors[mostIndex];
        int red = (rgb >> 16 ) & 0x000000FF;
        int green = (rgb >> 8 ) & 0x000000FF;
        int blue = (rgb) & 0x000000FF;

        System.out.println("red: "+red+" green: "+green+" blue: "+green);
        return colors[mostIndex];
    }
    boolean isWithinRGBRange(int rgb1, int rgb2)
    {
        int tolerance = 42;

        int red1 = (rgb1 >> 16 ) & 0x000000FF;
        int green1 = (rgb1 >> 8 ) & 0x000000FF;
        int blue1 = (rgb1) & 0x000000FF;


        int red2 = (rgb2 >> 16 ) & 0x000000FF;
        int green2 = (rgb2 >> 8 ) & 0x000000FF;
        int blue2 = (rgb2) & 0x000000FF;

        int difR = abs(red1 , red2);
        int difG = abs(green1, green2);
        int difB = abs(blue1, blue2);

        if((difR < tolerance)&&(difG < tolerance)&&(difB < tolerance))
        {
            return true;
        }
        else
        {
            return false;
        }

    }
    public int abs(int one, int two)
    {
        int difference = one - two;
        if(difference < 0)
        {
            return -1 * difference;
        }
        else
        {
            return difference;
        }
    }


}