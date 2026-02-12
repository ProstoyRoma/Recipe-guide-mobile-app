package com.example.recipeguide;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DishClassifier {

    private Interpreter tflite;
    private int inputHeight = 169;
    private int inputWidth = 274;
    private int inputChannels = 3;

    // Загрузка TFLite-модели из assets
    public DishClassifier(AssetManager assetManager, String modelPath) throws IOException {
        MappedByteBuffer buffer = loadModelFile(assetManager, modelPath);
        tflite = new Interpreter(buffer);
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Метод для проверки, является ли изображение блюдом
    // selectedBitmap – изображение, полученное, например, через MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
    public boolean isDish(Bitmap selectedBitmap) {
        // 1. Подготовка: изменяем размер изображения до входного размера модели
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(selectedBitmap, inputWidth, inputHeight, true);

        // 2. Преобразование Bitmap в массив [1][height][width][channels] типа float, нормализуя значения в [0,1]
        float[][][][] input = new float[1][inputHeight][inputWidth][inputChannels];
        for (int y = 0; y < inputHeight; y++) {
            for (int x = 0; x < inputWidth; x++) {
                int pixel = scaledBitmap.getPixel(x, y);
                // Извлекаем компоненты R, G, B
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                input[0][y][x][0] = r / 255.0f;
                input[0][y][x][1] = g / 255.0f;
                input[0][y][x][2] = b / 255.0f;
            }
        }

        // 3. Выделяем выходной массив, модель автокодировщика восстанавливает изображение той же формы
        float[][][][] output = new float[1][inputHeight][inputWidth][inputChannels];

        // 4. Запускаем инференс
        tflite.run(input, output);

        // 5. Вычисляем для каждого пикселя среднюю абсолютную разницу между исходным и восстановленным изображением.
        // Если ошибка меньше порога (threshold), считаем, что пиксель «подтвержден»
        int totalPixels = inputHeight * inputWidth;
        int matchingPixels = 0;
        float threshold = 0.1f;  // Порог можно настроить в зависимости от модели

        for (int y = 0; y < inputHeight; y++) {
            for (int x = 0; x < inputWidth; x++) {
                float diff = 0.0f;
                for (int c = 0; c < inputChannels; c++) {
                    diff += Math.abs(input[0][y][x][c] - output[0][y][x][c]);
                }
                diff /= inputChannels;
                if (diff < threshold) {
                    matchingPixels++;
                }
            }
        }

        float percentage = (matchingPixels * 100.0f) / totalPixels;
        // Если 70% и более пикселей восстановлено с ошибкой ниже порога,
        // модель считает изображение знакомым (например, блюдом)
        return percentage >= 70.0f;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
