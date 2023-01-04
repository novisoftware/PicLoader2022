package com.github.novisoftware.pic2022.picLoader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * PICエンコード処理。
 *
 */
public class PicEncoder {
	PictureData picture;
	ColorTable color;
	BitWriter bitWriter;

	/**
	 * コンストラクタ
	 *
	 * @param picture エンコード対象の画像
	 * @param f 出力先ファイル
	 * @throws FileNotFoundException
	 */
	public PicEncoder(PictureData picture, File f) throws FileNotFoundException {
		this.picture = picture;
		this.color = new ColorTable();
		this.bitWriter = new BitWriter(f);
	}

	/**
	 * 連鎖をチェックする
	 *
	 * @param ignore ランレングス算出時に無視するためのビットマップ
	 * @param x 開始X座標
	 * @param y 開始Y座標
	 * @param color 色
	 * @return
	 * @throws IOException
	 */
	 private final void checkAndWriteChain(boolean[][] ignore, int x, int y, short color) throws IOException {
		boolean isFirst = true;

		for(y++; y < picture.height; y++) {
			int pos;
			if (x > 1 && picture.point(x - 2, y) == color) {
				// さらに左隣が同色の場合は連鎖データを作らなくても良い。
				if (x > 2 &&  picture.point(x - 3, y) == color) {
					break;
				}
				// または、画面右端から同色の場合、連鎖データを作らなくても良い。
				if (x == 2 && y > 1 && picture.point(picture.width - 1, y - 1) == color) {
					break;
				}

				pos = -2; // 左2ドット
			}
			else if (x > 0 && picture.point(x - 1, y) == color) {
				// 画面右端から同色の場合、連鎖データを作らなくても良い。
				if (x == 1 && y > 1 && picture.point(picture.width - 1, y - 1) == color) {
					break;
				}
				pos = -1; // 左下
			}
			else if (picture.point(x, y) == color) {
				// 画面右端から同色の場合、連鎖データを作らなくても良い。
				if (x == 0 && y > 1 && picture.point(picture.width - 1, y - 1) == color) {
					break;
				}
				pos = 0; // 真下
			}
			else if (x < picture.width - 1 && picture.point(x + 1, y) == color) {
				pos = 1; // 右下
			}
			else if (x < picture.width - 2  && picture.point(x - 2, y) == color) {
				pos = 2; // 右2ドット
			} else {
				break;
			}

			if (isFirst) {
				isFirst = false;
				bitWriter.write(1, 1, BitWriter.PROFILE_CHAIN); // 連鎖あり
			}
			switch(pos) {
			case -2:
				bitWriter.write(2, 4, BitWriter.PROFILE_CHAIN); // 左2ドット
				break;
			case -1:
				bitWriter.write(1, 2, BitWriter.PROFILE_CHAIN); // 左下
				break;
			case 0:
				bitWriter.write(2, 2, BitWriter.PROFILE_CHAIN); // 真下
				break;
			case 1:
				bitWriter.write(3, 2, BitWriter.PROFILE_CHAIN); // 右下
				break;
			case 2:
				bitWriter.write(3, 4, BitWriter.PROFILE_CHAIN); // 右2ドット
				break;
			}

			x += pos;
			ignore[y][x] = true;
		}

		if (!isFirst) {
			bitWriter.write(0, 3, BitWriter.PROFILE_CHAIN); // 連鎖おわり
		} else {
			bitWriter.write(0, 1, BitWriter.PROFILE_CHAIN); // 連鎖なし
		}
	}

/*
	static String bitDebug(int data, int bit) {
		String a = "";

		while (bit != 0) {
			a = ((data & 1)==0 ? "0" : "1") + a;
			data >>= 1;
			bit -= 1;
		}

		return a;
	}
*/

	/**
	 * 長さを出力する
	 *
	 * @return 長さ(単位: bit)
	 * @throws IOException
	 */
	private final void writeLength(int length) throws IOException {
		int binData = length - 1;
		for (int i = 1; ; i++) {
			if ((length - 1) < (2 << i) - 2) {
				int a = (1 << i);
				bitWriter.write(a - 2, i, BitWriter.PROFILE_LENGTH);
				bitWriter.write(binData, i, BitWriter.PROFILE_LENGTH);

//				System.out.printf("WROTE LENGTH %8d  %s %s   a=%d\n", length, bitDebug(a - 2, i), bitDebug(binData, i), a);
				break;
			}
			binData -= (1 << i);
		}
	}

	/**
	 * 色を出力する
	 *
	 * @return 長さ(単位: bit)
	 * @throws IOException
	 */
	private final void writeColor(ColorTable colorTable, short argColor) throws IOException {
		short color = (short)((argColor >> 1) & 0x7fff);

		int index = colorTable.contains(color);
		if (index != -1) {
			bitWriter.write(1, 1, BitWriter.PROFILE_COLOR_CACHED);
			bitWriter.write(index, 7, BitWriter.PROFILE_COLOR_CACHED);
		}
		else {
			colorTable.regColor(color);
			bitWriter.write(0, 1, BitWriter.PROFILE_COLOR_UNCACHED);
			bitWriter.write(color, 15, BitWriter.PROFILE_COLOR_UNCACHED);
		}
	}

	/**
	 * PICフォーマットへのエンコードを実行する
	 *
	 * @throws IOException
	 */
	public void encode() throws IOException {
		ColorTable colorTable = new ColorTable();
		colorTable.initToEncode();

		int height = picture.height;
		int width = picture.width;

		boolean[][] ignore = new boolean[height][width];

		short color = 0;

		int x = 0;
		int y = 0;
		int length = 1;

		// colorの初期値0に対するランレングスを求める
		while (picture.point(x, y) == color) {
			length ++;

			x++;
			if (x >= width) {
				x = 0;
				y++;
				if (y >= height) {
					break;
				}
			}
		}
		this.writeLength(length);
		length = 0;

		while (true) {
			// 現在の色を更新する
			color = picture.point(x, y);
			this.writeColor(colorTable, color);

			// 連鎖を調べ、無視ビットマップの更新と、連鎖データの出力を行う
			this.checkAndWriteChain(ignore, x, y, color);

			// ランレングスを求める
			while (ignore[y][x] || picture.point(x, y) == color) {
				if (ignore[y][x]) {
					color = picture.point(x, y);
				}

				length++;

				x++;
				if (x >= width) {
					x = 0;
					y++;
					if (y >= height) {
						break;
					}
				}
			}
			this.writeLength(length);
			length = 0;

			if (y >= height) {
				break;
			}
		}

		bitWriter.flush();
		bitWriter.close();
	}

	/**
	 * ヘッダを出力する。
	 *
	 * @throws IOException
	 */
	public void writeHeader() throws IOException {
		// 識別部
		bitWriter.write('P', 8, BitWriter.PROFILE_HEADER);
		bitWriter.write('I', 8, BitWriter.PROFILE_HEADER);
		bitWriter.write('C', 8, BitWriter.PROFILE_HEADER);

		// コメント部
		bitWriter.write(0x1a, 8, BitWriter.PROFILE_HEADER);
		bitWriter.write(0, 8, BitWriter.PROFILE_HEADER);
		bitWriter.write(0, 8, BitWriter.PROFILE_HEADER);

		// タイプ/モード
		// (X68000を決め打ちで指定)
		bitWriter.write(0, 8, BitWriter.PROFILE_HEADER);
		// 色数
		// (15bitカラーを決め打ちで指定)
		bitWriter.write(15, 16, BitWriter.PROFILE_HEADER);

		// 幅
		bitWriter.write(picture.width, 16, BitWriter.PROFILE_HEADER);
		bitWriter.write(picture.height, 16, BitWriter.PROFILE_HEADER);
	}

	/**
	 * CLIから呼び出されることを想定した呼び出し口。
	 * メッセージを標準出力あてに出力する。
	 *
	 * @param input 入力の画像ファイル名(ファイルパス)
	 * @param outpu 出力先PIC画像のファイル名(ファイルパス)t
	 * @throws IOException
	 */
	static public void cui(String input, String output) throws IOException {
		File inputFile = new File(input);
		File outputFile = new File(output);

		long time0 = System.nanoTime();
		BufferedImage sourceImage =ImageIO.read(inputFile);
		long time1 = System.nanoTime();

		PictureData picture = new PictureData(sourceImage);
		long time2 = System.nanoTime();

		PicEncoder encoder = new PicEncoder(picture, outputFile);
		encoder.writeHeader();
		encoder.encode();
		long time3 = System.nanoTime();

		System.out.println("input:  " + inputFile.getName());
		System.out.println("output: " + outputFile.getName());
		System.out.println("width:  " + sourceImage.getWidth());
		System.out.println("height: " + sourceImage.getHeight());

		System.out.println();
		System.out.println("data size breakdown(bit): ");
		encoder.bitWriter.printProfile();
		System.out.println();
		System.out.printf("load time                 [ms]: %8.3f\n",  (time1 - time0) / 1000000.0);
		System.out.printf("convert(15bit color) time [ms]: %8.3f\n",  (time2 - time1) / 1000000.0);
		System.out.printf("encode time               [ms]: %8.3f\n",  (time3 - time2) / 1000000.0);
	}
}
