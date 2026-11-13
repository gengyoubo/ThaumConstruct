package github.com.gengyoubo.TCt.client;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.Arrays;
import java.util.function.UnaryOperator;

/**
 * Removes crafting grid cells from TC4's gui_arcaneworkbench.png so only the cells that hold a slot stay.
 * A cell is a darkened, bevelled recess in the cloth; each one is brightened back to the cloth around the grid
 * row by row and column by column, which keeps the circle and hexagram lines that run through it.
 */
final class Tc4GridEraser {
    private Tc4GridEraser() { }

    private static final int GRID = 38, STEP = 24, CELL = 20, PAD = 1;

    /** @param keep cells to keep as {column, row} pairs (0..2) */
    static UnaryOperator<NativeImage> keeping(int[]... keep) {
        return image -> {
            float[] target = new float[3];
            float high = clothStats(image, target);
            float targetLum = (target[0] + target[1] + target[2]) / 3;
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    if (!contains(keep, column, row)) erase(image, GRID + STEP * column - PAD, GRID + STEP * row - PAD, target, targetLum, high);
                }
            }
            return image;
        };
    }

    private static boolean contains(int[][] keep, int column, int row) {
        for (int[] cell : keep) if (cell[0] == column && cell[1] == row) return true;
        return false;
    }

    /** Mean colour of the cloth strips between the cells; returns the 95th percentile brightness there. */
    private static float clothStats(NativeImage image, float[] mean) {
        int span = STEP * 2 + CELL;
        int[] gapStarts = {GRID + CELL, GRID + STEP + CELL};
        int gapWidth = STEP - CELL;
        float[] lums = new float[gapStarts.length * 2 * gapWidth * span];
        int n = 0;
        for (int start : gapStarts) {
            for (int g = 0; g < gapWidth; g++) {
                for (int t = 0; t < span; t++) {
                    for (int vertical = 0; vertical < 2; vertical++) {
                        int x = vertical == 0 ? start + g : GRID + t, y = vertical == 0 ? GRID + t : start + g;
                        int p = image.getPixelRGBA(x, y);
                        float r = p & 0xff, gr = (p >> 8) & 0xff, b = (p >> 16) & 0xff;
                        mean[0] += r; mean[1] += gr; mean[2] += b;
                        lums[n++] = (r + gr + b) / 3;
                    }
                }
            }
        }
        for (int c = 0; c < 3; c++) mean[c] /= n;
        Arrays.sort(lums, 0, n);
        return lums[(int) (n * 0.95f)];
    }

    private static void erase(NativeImage image, int x0, int y0, float[] target, float targetLum, float high) {
        int size = CELL + 2 * PAD;
        float[][][] c = new float[size][size][3];
        int[][] alpha = new int[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int p = image.getPixelRGBA(x0 + x, y0 + y);
                c[y][x][0] = p & 0xff; c[y][x][1] = (p >> 8) & 0xff; c[y][x][2] = (p >> 16) & 0xff;
                alpha[y][x] = p >>> 24;
            }
        }
        float[] line = new float[size];
        for (int pass = 0; pass < 3; pass++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) line[x] = lum(c[y][x]);
                scale(c[y], targetLum / Math.max(median(line), 1));
            }
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) line[y] = lum(c[y][x]);
                float gain = targetLum / Math.max(median(line), 1);
                for (int y = 0; y < size; y++) for (int k = 0; k < 3; k++) c[y][x][k] *= gain;
            }
        }
        // match the cloth colour, then cap the brightness so doubly boosted bevel corners do not glow
        float[] channel = new float[size * size];
        for (int k = 0; k < 3; k++) {
            int i = 0;
            for (float[][] row : c) for (float[] px : row) channel[i++] = px[k];
            float gain = target[k] / Math.max(median(channel), 1);
            for (float[][] row : c) for (float[] px : row) px[k] *= gain;
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float[] px = c[y][x];
                float l = lum(px);
                if (l > high) for (int k = 0; k < 3; k++) px[k] *= high / l;
                int r = clamp(px[0]), g = clamp(px[1]), b = clamp(px[2]);
                image.setPixelRGBA(x0 + x, y0 + y, alpha[y][x] << 24 | b << 16 | g << 8 | r);
            }
        }
    }

    private static void scale(float[][] row, float gain) {
        for (float[] px : row) for (int k = 0; k < 3; k++) px[k] *= gain;
    }

    private static float lum(float[] px) {
        return (px[0] + px[1] + px[2]) / 3;
    }

    private static float median(float[] values) {
        float[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }
}
