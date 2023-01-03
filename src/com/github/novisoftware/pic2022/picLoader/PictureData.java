package com.github.novisoftware.pic2022.picLoader;

import java.awt.image.BufferedImage;

/**
 * GVRAM相当。
 *
 * オリジナルの実装はGVRAMの読み書きをする。
 * それに相当するバッファ、および、バッファにアクセスするインタフェース(PSETとPOINT)を用意する。
 * バッファは各ピクセル 65536色 を記憶する。
 */
public class PictureData {
	final short[][] buffer;
	final int width;
	final int height;

	public PictureData(int width, int height) {
		this.width = width;
		this.height = height;

		if (width < 512) {
			width = 512;
		}
		if (height < 512) {
			height = 512;
		}

		buffer = new short[width][height];
	}


	/**
	 * BufferedImageを元に画像データを作成する。
	 *
	 * @param source 元画像
	 */
	public PictureData(BufferedImage source) {
		this.width = source.getWidth();
		this.height = source.getHeight();

		int width = this.width;
		int height = this.height;
		buffer = new short[width][height];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = source.getRGB(x, y);
				int g = (color >> 16) & 0xFF;
				int r = (color >> 8) & 0xFF;
				int b =  color & 0xFF;
				int color15bit = ((r >> 3) << 11) | ((g >> 3) << 6) | ((b >> 3) << 1);
				this.pset(x, y, (short)color15bit);
			}
		}
	}


	public final void pset(int x, int y, short color) {
		if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
			this.buffer[x][y] = color;
		}
	}

	public final short point(int x, int y) {
		if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
			return this.buffer[x][y];
		}
		return 0;
	}

	public final int getWidth() {
		return this.width;
	}

	public final int getHeight() {
		return this.height;
	}
}