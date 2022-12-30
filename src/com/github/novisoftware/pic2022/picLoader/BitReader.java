package com.github.novisoftware.pic2022.picLoader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * ビット単位での入力ストリーム読み取り。
 */
public class BitReader {
	/**
	 * バイナリの入力ストリーム。
	 */
	private final DataInputStream inputStream;
	/**
	 * 読み取り済の情報。最大32bitを保持することができる。
	 */
	private int buffer = 0;
	/**
	 * いま保持している情報が何bit分なのかを保持する。
	 */
	private int nowBit = 0;

	/**
	 * Fileオブジェクトを引数とするコンストラクタ。
	 *
	 * @param f PICファイルのFileオブジェクト
	 * @throws FileNotFoundException
	 */
	public BitReader(File f) throws FileNotFoundException {
	      inputStream =
		          new DataInputStream(
		              new BufferedInputStream(
		                  new FileInputStream(f)));
	}

	/**
	 * 入力ストリームからデータを読み取る
	 *
	 * @param requireBit 読み取るビット数
	 * @return 読み取り結果
	 * @throws IOException
	 */
	public int read(int requireBit) throws IOException {
		// マジックナンバー 8 は、1バイトが8ビットという意味。
		while (requireBit > nowBit) {
			int readValue = inputStream.read();
			if (readValue == -1) {
				throw new IOException("予期しないファイル終端です。");
			}
			buffer = (buffer << 8) + readValue;
			nowBit += 8;
		}

		int returnValue = (buffer >> (nowBit - requireBit)) & ((1 << requireBit) - 1);
		nowBit -= requireBit;

		return returnValue;
	}

	/**
	 * 入力ストリームを閉じる
	 */
	public void close() {
		try {
			inputStream.close();
		} catch (IOException e) {
			// 入力専用のストリームのクローズ時なので対処不要
			// （たぶん実際には発生することがない）
		}

	}
}
