package com.github.novisoftware.pic2022.picLoader;

/**
 * GVRAM相当。
 *
 * オリジナルの実装はGVRAMの読み書きをする。
 * それに相当するバッファ、および、バッファにアクセスするインタフェース(PSETとPOINT)を用意する。
 * バッファは各ピクセル 65536色 を記憶する。
 */
public class PictureData {
	short[][] buffer;
	int width;
	int height;

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

	public void pset(int x, int y, short color) {
		if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
			this.buffer[x][y] = color;
		}
	}

	public short point(int x, int y) {
		if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
			return this.buffer[x][y];
		}
		return 0;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}
}