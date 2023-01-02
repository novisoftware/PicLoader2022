package com.github.novisoftware.pic2022.picLoader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

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
	 */
	ArrayList<Integer> checkChain(boolean[][] ignore, int x, int y, short color) {
		ArrayList<Integer> chain = new ArrayList<>();

		for(y++; y < picture.height; y++) {
			if (x > 1 && picture.point(x - 2, y) == color) {
				// さらに左隣が同色の場合は連鎖データを作らなくても良い。
				if (x > 2 &&  picture.point(x - 3, y) == color) {
					break;
				}

				chain.add(-2);
				ignore[y][x-2] = true;

				x -= 2;
				continue;
			}
			else if (x > 0 && picture.point(x - 1, y) == color) {
				chain.add(-1);

				ignore[y][x-1] = true;

				x--;
				continue;
			}
			else if (picture.point(x, y) == color) {
				chain.add(0);
				ignore[y][x] = true;
				continue;
			}
			else if (x < picture.width - 1 && picture.point(x + 1, y) == color) {
				chain.add(1);

				ignore[y][x+1] = true;

				x++;
				continue;
			}
			else if (x < picture.width - 2  && picture.point(x - 2, y) == color) {
				chain.add(2);

				ignore[y][x+2] = true;
				x += 2;
				continue;
			} else {
				break;
			}
		}

		return chain;
	}

	/**
	 * 連鎖データを出力する
	 *
	 * @param chain
	 * @throws IOException
	 */
	public void writeChain(ArrayList<Integer> chain) throws IOException {
		if (chain.size() == 0) {
			// 連鎖なし
			bitWriter.write(0, 1, BitWriter.PROFILE_CHAIN);
			return;
		}

		// 連鎖あり
		bitWriter.write(1, 1, BitWriter.PROFILE_CHAIN);
		for (int code : chain) {
			if (code == -1) {  // 左下
				bitWriter.write(1, 2, BitWriter.PROFILE_CHAIN);
			} else if (code == 0) { // 真下
				bitWriter.write(2, 2, BitWriter.PROFILE_CHAIN);
			} else if (code == 1) { // 右下
				bitWriter.write(3, 2, BitWriter.PROFILE_CHAIN);
			} else if (code == -2) { // 左2ドット
				bitWriter.write(2, 4, BitWriter.PROFILE_CHAIN);
			} else if (code == 2) {  // 右2ドット
				bitWriter.write(3, 4, BitWriter.PROFILE_CHAIN);
			}
			else {
				; // ここには到達しない
			}
		}
		// 連鎖おわり
		bitWriter.write(0, 3, BitWriter.PROFILE_CHAIN);
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
	public void writeLength(int length) throws IOException {
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
	public void writeColor(ColorTable colorTable, short argColor) throws IOException {
		short color = (short)((argColor >> 1) & 0xfffe);

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
			// 現在の色を更新
			color = picture.point(x, y);
			// 連鎖データを作成
			ArrayList<Integer> chain = this.checkChain(ignore, x, y, color);

			// ランレングスを求める
			while (ignore[y][x] || picture.point(x, y) == color) {
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

			this.writeColor(colorTable, color);
			this.writeChain(chain);
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
	 * 画像データを15bitデータに変換する
	 *
	 * @param source 元画像
	 * @return 画像データ
	 */
	static public PictureData create15bitData(BufferedImage source) {
		int width = source.getWidth();
		int height = source.getHeight();

		PictureData picture = new PictureData(width, height);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = source.getRGB(x, y);
				int g = (color >> 16) & 0xFF;
				int r = (color >> 8) & 0xFF;
				int b =  color & 0xFF;
				int color15bit = ((r >> 3) << 11) | ((g >> 3) << 6) | ((b >> 3) << 1);
				picture.pset(x, y, (short)color15bit);
			}
		}

		return picture;
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

		BufferedImage image =ImageIO.read(inputFile);

		System.out.println("input:  " + inputFile.getName());
		System.out.println("output: " + outputFile.getName());
		System.out.println("width:  " + image.getWidth());
		System.out.println("height: " + image.getHeight());

		PictureData picture = PicEncoder.create15bitData(image);
		PicEncoder encoder = new PicEncoder(picture, outputFile);
		encoder.writeHeader();
		encoder.encode();

		System.out.println();
		System.out.println("data size breakdown(bit): ");
		encoder.bitWriter.printProfile();
	}
}
