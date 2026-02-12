package Utils;

public class VectorUtils {
    public static byte[] floatsToBytes(float[] arr) {
        if (arr == null) return null;
        byte[] bytes = new byte[arr.length * 4];
        int idx = 0;
        for (float v : arr) {
            int intBits = Float.floatToIntBits(v);
            bytes[idx++] = (byte) (intBits);
            bytes[idx++] = (byte) (intBits >> 8);
            bytes[idx++] = (byte) (intBits >> 16);
            bytes[idx++] = (byte) (intBits >> 24);
        }
        return bytes;
    }

    public static float[] bytesToFloats(byte[] bytes) {
        if (bytes == null) return null;
        int n = bytes.length / 4;
        float[] arr = new float[n];
        for (int i = 0; i < n; i++) {
            int offset = i * 4;
            int b0 = bytes[offset] & 0xff;
            int b1 = bytes[offset + 1] & 0xff;
            int b2 = bytes[offset + 2] & 0xff;
            int b3 = bytes[offset + 3] & 0xff;
            int intBits = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
            arr[i] = Float.intBitsToFloat(intBits);
        }
        return arr;
    }

    public static double dot(double[] a, double[] b) {
        if (a == null || b == null) return 0.0;
        int len = Math.min(a.length, b.length);
        if (len == 0) return 0.0;
        double s = 0.0;
        for (int i = 0; i < len; i++) s += a[i] * b[i];
        return s;
    }

    public static double dot(float[] a, float[] b) {
        if (a == null || b == null) return 0.0;
        int len = Math.min(a.length, b.length);
        if (len == 0) return 0.0;
        double s = 0.0;
        for (int i = 0; i < len; i++) s += (double) a[i] * (double) b[i];
        return s;
    }

    public static double normDouble(double[] a) {
        if (a == null) return 0.0;
        double s = 0.0;
        for (double v : a) s += v * v;
        return Math.sqrt(s);
    }

    public static double normDouble(float[] a) {
        if (a == null) return 0.0;
        double s = 0.0;
        for (float v : a) s += (double) v * (double) v;
        return Math.sqrt(s);
    }

    public static float cosine(float[] a, float[] b) {
        if (a == null || b == null) return 0f;
        int len = Math.min(a.length, b.length);
        if (len == 0) return 0f;

        // Аккумуляция в double для стабильности
        double dot = 0.0;
        double suma = 0.0;
        double sumb = 0.0;
        for (int i = 0; i < len; i++) {
            double va = a[i];
            double vb = b[i];
            dot += va * vb;
            suma += va * va;
            sumb += vb * vb;
        }

        double na = Math.sqrt(suma);
        double nb = Math.sqrt(sumb);
        final double EPS = 1e-12;
        if (na < EPS || nb < EPS) return 0f;

        double cos = dot / (na * nb);
        // защита от числовых погрешностей
        if (cos > 1.0) cos = 1.0;
        else if (cos < -1.0) cos = -1.0;
        return (float) cos;
    }

        public static float[] addScaled(float[] base, float[] vec, float scale) {
        if (vec == null) return base;
        int len = vec.length;
        if (len == 0) return base;

        if (base == null) {
            float[] out = new float[len];
            for (int i = 0; i < len; i++) out[i] = vec[i] * scale;
            return out;
        }

        if (base.length < len) {
            float[] out = new float[len];
            System.arraycopy(base, 0, out, 0, base.length);
            for (int i = 0; i < len; i++) {
                out[i] += vec[i] * scale;
            }
            return out;
        }

        // base.length >= len
        for (int i = 0; i < len; i++) {
            base[i] += vec[i] * scale;
        }
        return base;
    }

    public static void scaleInPlace(float[] a, float s) {
        for (int i = 0; i < a.length; i++) a[i] *= s;
    }
}
