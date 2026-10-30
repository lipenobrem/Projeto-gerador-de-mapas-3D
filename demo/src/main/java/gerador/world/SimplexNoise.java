package gerador.world;
import java.util.Random;

public class SimplexNoise {
    private final int[] p = new int[512];

    public SimplexNoise(long seed) {
        Random rand = new Random(seed);
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) permutation[i] = i;

        // Embaralha com base na Seed
        for (int i = 255; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            int temp = permutation[index];
            permutation[index] = permutation[i];
            permutation[i] = temp;
        }
        for (int i = 0; i < 512; i++) p[i] = permutation[i & 255];
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    // Ruído 3D -> Retorna valor entre -1.0 e 1.0
    public double noise3D(double x, double y, double z) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;
        x -= Math.floor(x); y -= Math.floor(y); z -= Math.floor(z);
        double u = fade(x), v = fade(y), w = fade(z);
        int A = p[X] + Y, AA = p[A] + Z, AB = p[A + 1] + Z;
        int B = p[X + 1] + Y, BA = p[B] + Z, BB = p[B + 1] + Z;

        return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z), grad(p[BA], x - 1, y, z)),
                               lerp(u, grad(p[AB], x, y - 1, z), grad(p[BB], x - 1, y - 1, z))),
                       lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1), grad(p[BA + 1], x - 1, y, z - 1)),
                               lerp(u, grad(p[AB + 1], x, y - 1, z - 1), grad(p[BB + 1], x - 1, y - 1, z - 1))));
    }

    // Ruído 2D com Oitavas 
    public double getHeightNoise(double x, double z) {
        double total = 0;
        total += noise3D(x * 0.03, 0, z * 0.03) * 1.0;   // Montanhas grandes
        total += noise3D(x * 0.08, 10, z * 0.08) * 0.35; // Detalhes nas colinas
        return total;
    }
}