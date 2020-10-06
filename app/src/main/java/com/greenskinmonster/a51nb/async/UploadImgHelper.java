package com.greenskinmonster.a51nb.async;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.greenskinmonster.a51nb.bean.HiSettingsHelper;
import com.greenskinmonster.a51nb.okhttp.OkHttpHelper;
import com.greenskinmonster.a51nb.utils.CursorUtils;
import com.greenskinmonster.a51nb.utils.HiUtils;
import com.greenskinmonster.a51nb.utils.ImageFileInfo;
import com.greenskinmonster.a51nb.utils.Logger;
import com.greenskinmonster.a51nb.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadImgHelper {
    private final static int MAX_QUALITY = 90;
    private final static int MAX_IMAGE_FILE_SIZE = 400 * 1024;
    private static final int THUMB_SIZE = 256;

    private final static int MAX_PIXELS = 1600 * 900;

    private UploadImgListener mListener;

    private String mHash;
    private Context mCtx;
    private Uri[] mUris;

    private String mMessage = "";
    private Bitmap mThumb;
    private int mTotal;
    private int mCurrent;
    private String mCurrentFileName = "";

    public UploadImgHelper(Context ctx, UploadImgListener v, String hash, Uri[] uris) {
        mCtx = ctx;
        mListener = v;
        mHash = hash;
        mUris = uris;
    }

    public interface UploadImgListener {
        void updateProgress(int total, int current, int percentage);
        void itemComplete(Uri uri, int total, int current, String currentFileName, String message, String imgId, Bitmap thumbtail);
    }

    public void upload() {
        mTotal = mUris.length;

        int i = 0;
        for (Uri uri : mUris) {
            mCurrent = i++;
            mListener.updateProgress(mTotal, mCurrent, -1);
            try {
                String imgId = uploadImage(uri);
                mListener.itemComplete(uri, mTotal, mCurrent, mCurrentFileName, mMessage, imgId, mThumb);
            } catch (Exception e) {
                mListener.itemComplete(uri, mTotal, mCurrent, mCurrentFileName, e.getMessage(), "", mThumb);
            }
        }
    }

    private String uploadImage(Uri uri) {
        mThumb = null;
        mMessage = "";
        mCurrentFileName = "";

        ImageFileInfo imageFileInfo = CursorUtils.getImageFileInfo(mCtx, uri);
        mCurrentFileName = imageFileInfo.getFileName();

        ByteArrayOutputStream baos = compressImage(uri, imageFileInfo);
        if (baos == null) {
            mMessage = "处理图片发生错误";
            return null;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd_HHmm", Locale.US);
        String suffix = "." + Utils.getImageFileSuffix(imageFileInfo.getMime());
        String fileName = "51NB_" + formatter.format(new Date()) + suffix;

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        String uid = HiSettingsHelper.getInstance().getUid();
        builder.addFormDataPart("uid", uid);
        builder.addFormDataPart("hash", mHash);
        builder.addFormDataPart("Filename", fileName);
        builder.addFormDataPart("type", "filetype");
        builder.addFormDataPart("filetype", suffix);

        RequestBody requestBody = RequestBody.create(MediaType.parse(imageFileInfo.getMime()), baos.toByteArray());
        builder.addFormDataPart("Filedata", fileName, requestBody);

        Request request = new Request.Builder()
                .url(HiUtils.UploadImgUrl)
                .post(builder.build())
                .build();

        String imgId = null;
        try {
            OkHttpClient client = OkHttpHelper.getInstance().getClient();
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful())
                throw new IOException(OkHttpHelper.ERROR_CODE_PREFIX + response.networkResponse().code());

            String responseText = response.body().string();
            if (HiUtils.isValidId(responseText) && TextUtils.isDigitsOnly(responseText))
                imgId = responseText;
            else
                mMessage = "无法获取图片 ID";
        } catch (Exception e) {
            Logger.e(e);
            mMessage = OkHttpHelper.getErrorMessage(e, false).getMessage();
        } finally {
            try {
                baos.close();
            } catch (IOException ignored) {
            }
        }
        return imgId;
    }

    private ByteArrayOutputStream compressImage(Uri uri, ImageFileInfo imageFileInfo) {
        if (imageFileInfo.isGif()
                && imageFileInfo.getFileSize() > HiSettingsHelper.getInstance().getMaxUploadFileSize()) {
            mMessage = "GIF 图片大小不能超过" + Utils.toSizeText(HiSettingsHelper.getInstance().getMaxUploadFileSize());
            return null;
        }

        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(mCtx.getContentResolver(), uri);
        } catch (Exception e) {
            mMessage = "无法获取图片: " + e.getMessage();
            return null;
        }

        // gif or very long/wide image or small image or filePath is null
        if (isDirectUploadable(imageFileInfo)) {
            mThumb = ThumbnailUtils.extractThumbnail(bitmap, THUMB_SIZE, THUMB_SIZE);
            bitmap.recycle();
            return readFileToStream(imageFileInfo.getFilePath());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, MAX_QUALITY, baos);
        bitmap.recycle();
        bitmap = null;

        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(isBm, null, opts);

        int width = opts.outWidth;
        int height = opts.outHeight;

        // inSampleSize is needed to avoid OOM
        int be = (int) (Math.max(width, height) * 1.0 / 1500);
        if (be <= 0)
            be = 1; // be=1 表示不缩放

        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = false;
        newOpts.inSampleSize = be;

        isBm = new ByteArrayInputStream(baos.toByteArray());
        Bitmap newbitmap = BitmapFactory.decodeStream(isBm, null, newOpts);

        width = newbitmap.getWidth();
        height = newbitmap.getHeight();

        // scale bitmap so later compress could run less times, once is the best result
        // rotate if needed
        if (width * height > MAX_PIXELS || imageFileInfo.getOrientation() > 0) {
            float scale = 1.0f;
            if (width * height > MAX_PIXELS)
                scale = (float) Math.sqrt(MAX_PIXELS * 1.0 / (width * height));

            Matrix matrix = new Matrix();
            if (imageFileInfo.getOrientation() > 0)
                matrix.postRotate(imageFileInfo.getOrientation());
            matrix.postScale(scale, scale);

            Bitmap scaledBitmap = Bitmap.createBitmap(newbitmap, 0, 0, newbitmap.getWidth(),
                    newbitmap.getHeight(), matrix, true);

            newbitmap.recycle();
            newbitmap = scaledBitmap;
        }

        int quality = MAX_QUALITY;
        baos.reset();
        newbitmap.compress(CompressFormat.JPEG, quality, baos);
        while (baos.size() > MAX_IMAGE_FILE_SIZE) {
            quality -= 10;
            if (quality <= 0) {
                mMessage = "无法压缩图片至指定大小 " + Utils.toSizeText(MAX_IMAGE_FILE_SIZE);
                return null;
            }
            baos.reset();
            newbitmap.compress(CompressFormat.JPEG, quality, baos);
        }

        mThumb = ThumbnailUtils.extractThumbnail(newbitmap, THUMB_SIZE, THUMB_SIZE);
        newbitmap.recycle();
        newbitmap = null;

        System.gc();
        return baos;
    }

    private boolean isDirectUploadable(ImageFileInfo imageFileInfo) {
        long fileSize = imageFileInfo.getFileSize();
        int w = imageFileInfo.getWidth();
        int h = imageFileInfo.getHeight();

        if (TextUtils.isEmpty(imageFileInfo.getFilePath()))
            return false;

        if (imageFileInfo.getOrientation() > 0)
            return false;

        // gif image
        if (imageFileInfo.isGif() && fileSize <= HiSettingsHelper.getInstance().getMaxUploadFileSize())
            return true;

        // very long or wide image
        if (w > 0 && h > 0 && fileSize <= HiSettingsHelper.getInstance().getMaxUploadFileSize()) {
            if (Math.max(w, h) * 1.0 / Math.min(w, h) >= 3)
                return true;
        }

        // normal image
        return fileSize <= MAX_IMAGE_FILE_SIZE && w * h <= MAX_PIXELS;
    }

    private static ByteArrayOutputStream readFileToStream(String file) {
        FileInputStream fileInputStream = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            fileInputStream = new FileInputStream(file);

            int readedBytes;
            byte[] buf = new byte[1024];
            while ((readedBytes = fileInputStream.read(buf)) > 0)
                bos.write(buf, 0, readedBytes);

            return bos;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (fileInputStream != null)
                    fileInputStream.close();
            } catch (Exception ignored) {
            }
        }
    }
}
