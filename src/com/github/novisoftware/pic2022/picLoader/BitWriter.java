package com.github.novisoftware.pic2022.picLoader;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * ビット単位での出力ストリーム書き出し。
 */
public class BitWriter {
	/**
	 * バイナリの入力ストリーム。
	 */
	private final DataOutputStream outputStream;
	/**
	 * 読み取り済の情報。最大32bitを保持することができる。
	 */
	private int buffer = 0;
	/**
	 * いま保持している情報が何bit分なのかを保持する。
	 */
	private int nowBit = 0;

	/**
	 * プロファイル
	 */
	private int profile[] = new int[6];

	/**
	 *
	 */
	static public final int PROFILE_HEADER = 0;
	static public final int PROFILE_COLOR_CACHED = 1;
	static public final int PROFILE_COLOR_UNCACHED = 2;
	static public final int PROFILE_LENGTH = 3;
	static public final int PROFILE_CHAIN = 4;
	static public final int PROFILE_PADDING = 5;

	public void printProfile() {
		String[] profileName = {
				"header",
				"cached_color",
				"uncached_color",
				"length",
				"chain",
				"padding"
		};

		for (int i = 0; i < profile.length; i++ ) {
			System.out.printf("%15s %10d\n", profileName[i], profile[i]);
		}
	}


	/**
	 * Fileオブジェクトを引数とするコンストラクタ。
	 *
	 * @param f 出力先にするFileオブジェクト
	 * @throws FileNotFoundException
	 */
	public BitWriter(File f) throws FileNotFoundException {
		outputStream =
		          new DataOutputStream(
		              new BufferedOutputStream(
		                  new FileOutputStream(f)));
	}

	/**
	 * 入力ストリームにデータを書き出す
	 *
	 * @param bit ストリームに書き出すデータ
	 * @param bit dataが何bitか
	 * @return 読み取り結果
	 * @throws IOException
	 */
	public void write(int data, int bit, int profileIndex) throws IOException {
		profile[profileIndex] += bit;


		buffer = (buffer << bit) | (data & ((1 << bit) - 1));
		nowBit += bit;

		for( ; nowBit >= 8 ; nowBit -= 8) {
			int b = (buffer >> (nowBit - 8)) & ((1 << 8) - 1);
			outputStream.write(b);
		}
	}

	/**
	 * バッファに残っている1バイト未満のデータを書き出す。
	 */
	public void flush() throws IOException {
		if (nowBit != 0) {
			this.write(0, 8 - nowBit, BitWriter.PROFILE_PADDING);
		}
		nowBit = 0;
	}

	/**
	 * 入力ストリームを閉じる
	 */
	public void close() throws IOException {
		outputStream.close();
	}
}
