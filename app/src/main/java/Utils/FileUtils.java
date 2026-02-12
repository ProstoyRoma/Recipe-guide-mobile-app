package Utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

// FileUtils.java
public class FileUtils {
    public static String copyAssetToFiles(Context ctx, String assetPath, String outFileName) throws IOException {
        File outFile = new File(ctx.getFilesDir(), outFileName);
        if (outFile.exists()) return outFile.getAbsolutePath();
        try (InputStream is = ctx.getAssets().open(assetPath);
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
        return outFile.getAbsolutePath();
    }
}

