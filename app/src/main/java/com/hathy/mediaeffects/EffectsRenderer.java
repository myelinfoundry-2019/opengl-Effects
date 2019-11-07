package com.hathy.mediaeffects;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static javax.microedition.khronos.opengles.GL10.GL_UNSIGNED_BYTE;

public class EffectsRenderer implements GLSurfaceView.Renderer {

    private Bitmap photo;
    private int photoWidth, photoHeight;
    private int [] rgbimage = new int [320*180];
    private EffectContext effectContext;
    private Effect effect;
    private ByteBuffer framebuffer;
    private InputStream is;
    private Square square;
    AssetManager assetManager;

    private int textures[] = new int[2];

    public EffectsRenderer(Context context){
        super();


        photo = BitmapFactory.decodeResource(context.getResources(), R.drawable.test);
        photoWidth = photo.getWidth();
        photoHeight = photo.getHeight();
       // framebuffer = ByteBuffer.allocate(photoWidth*photoHeight*4); //Create a new buffer
        //photo.copyPixelsToBuffer(framebuffer); //Move the byte data to the bufferInputStream is = getAssets().open("test.txt");
        try {


            is = context.getAssets().open("test_1.yuv");

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);

            is.close();
            framebuffer = ByteBuffer.wrap(buffer);
            byte [] yuv = new byte[framebuffer.capacity()];
            //int [] rgbimage = new int [320*180];
            byte [] y_pixels = new byte[320*180];
            byte [] u_pixels = new byte[320/2*180/2];
            byte [] v_pixels = new byte[320/2*180/2];

            framebuffer.get(yuv, framebuffer.position(), yuv.length);
            framebuffer.rewind();
            convertYUV420PToABGR8888(yuv, 320, 180, rgbimage);

        } catch (Exception ex) {
            ex.printStackTrace();
        }




    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    }

    private void generateSquare(){


        GLES20.glGenTextures(2, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, photo, 0);
        //Buffer framebuffer;

       GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, GLES20.GL_ZERO, GLES20.GL_RGBA, 320, 180,
                GLES20.GL_ZERO, GLES20.GL_RGBA, GL_UNSIGNED_BYTE,
               /* framebuffer */IntBuffer.wrap(rgbimage));

        square = new Square();

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width, height);
        GLES20.glClearColor(0,0,0,1);
        generateSquare();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(effectContext==null) {
            effectContext = EffectContext.createWithCurrentGlContext();
        }
        if(effect!=null){
            effect.release();
        }
        //brightnessEffect();
        SharpnessEffect();
        //autofix();
       // Bitmap bmpopengl= saveTexture(textures[0],320, 180);
        saveTexture(textures[1],320, 180);
 /*      // File file = null;
        FileOutputStream fos = null;
        file = new File("/sdcard/Download/effect_bytebuffer"+".png");
//            // Get public external storage folder ( /storage/emulated/0 ).

        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (file.exists()) {
            try {
                fos = new FileOutputStream(file, false);

            //channel.write(bmp);
            //fos.close();
            bmpopengl.compress(Bitmap.CompressFormat.PNG, 100, fos);

            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
            }
        }
        square.draw(textures[1]);
   */
    }

    private void documentaryEffect(){
        EffectFactory factory = effectContext.getFactory();
        effect = factory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
        effect.apply(textures[0], photoWidth, photoHeight, textures[1]);
    }

    private void grayScaleEffect(){
        EffectFactory factory = effectContext.getFactory();
        effect = factory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
        effect.apply(textures[0], photoWidth, photoHeight, textures[1]);
    }

    private void brightnessEffect(){
        EffectFactory factory = effectContext.getFactory();
        effect = factory.createEffect(EffectFactory.EFFECT_BRIGHTNESS);
        effect.setParameter("brightness", 2f);
        effect.apply(textures[0], photoWidth, photoHeight, textures[1]);
    }
    private void SharpnessEffect(){
        EffectFactory factory = effectContext.getFactory();
        effect = factory.createEffect(EffectFactory.EFFECT_SHARPEN);
        effect.setParameter("scale", 1f);
        effect.apply(textures[0], 320, 180, textures[1]);

    }
    private void autofix() {
        EffectFactory factory = effectContext.getFactory();
        effect = factory.createEffect(EffectFactory.EFFECT_AUTOFIX);
        effect.setParameter("scale", 1f);
        effect.apply(textures[0], photoWidth, photoHeight, textures[1]);
    }
    public static void saveTexture(int texture, int width, int height) {
        int[] frame = new int[1];
        GLES20.glGenFramebuffers(1, frame, 0);
        //checkGlError("glGenFramebuffers");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frame[0]);
        //checkGlError("glBindFramebuffer");
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);
        //checkGlError("glFramebufferTexture2D");
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        File file = null;
        FileOutputStream fos = null;
//        ByteBuffer mybuf = buffer.duplicate();
        buffer.order(ByteOrder.BIG_ENDIAN);
//        System.out.println("buffer float value" + buffer.getFloat());
        try{
            file = new File("/sdcard/Download/effect_vissharp.1f" + ".argb");
//            // Get public external storage folder ( /storage/emulated/0 ).
            file.createNewFile();
            if (file.exists()) {
                fos = new FileOutputStream(file,false);
            }
            FileChannel channel = fos.getChannel();
            channel.write(buffer);
            fos.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        //checkGlError("glReadPixels");

//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
  //      bitmap.copyPixelsFromBuffer(buffer);
    //    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        //checkGlError("glBindFramebuffer");
      //  GLES20.glDeleteFramebuffers(1, frame, 0);
        //checkGlError("glDeleteFramebuffer");
      //  return bitmap;
    }
    private static void convertYUV420PToABGR8888(byte[] input, int width, int height, int[] output) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            for (int i = 0; i < width; i++, yp++) {
                int arraySize = height * width;
                int y = 0xff & input[j * width + i];
                int u = 0xff & input[(j / 2) * (width / 2) + i / 2 + arraySize];
                int v = 0xff & input[(j / 2) * (width / 2) + i / 2 + arraySize + arraySize / 4];

                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

               // output[yp] = 0xff000000 + (b << 16) + (g << 8) + r;
                output[yp] = r + (b << 16) + (g << 8) + 0xff000000;
            }
        }
    }
}
