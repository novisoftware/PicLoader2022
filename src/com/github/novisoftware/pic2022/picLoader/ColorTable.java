package com.github.novisoftware.pic2022.picLoader;

import java.io.IOException;

/**
 * PICフォーマットの色辞書の読み込み処理
 *
 * 色情報は128色が辞書にキャッシュされている。
 * 色情報の読み込みでは、キャッシュフラグ1ビットの読み込みの後、
 * キャッシュされた色を読み込む場合は7ビットのキャッシュ番号、
 * キャッシュされていない色を読み込む場合は15ビットの色コードを読み込む。
 *
 * やなぎさわ氏( http://www.vector.co.jp/vpack/browse/person/an001322.html )による
 * PICフォーマット仕様書( http://www.vector.co.jp/soft/data/art/se003198.html )に同梱された
 * picl.c に基づきます。
 *
 *
 * このファイルはオリジナルの実装（「ＰＩＣの説明のオマケのローダー」）をほぼそのまま流用しています。
 *
 */
public class ColorTable {
	/**
	 * 色キャッシュのテーブルサイズ(7bit)。
	 */
	private static final int CACHE_TABLE_SIZE = 128;

	/**
	 * 色キャッシュテーブル。
	 */
	private short[] color;
	private int[] next;
	private int[] prev;

	/**
	 * 現在の色
	 */
	private int color_p;

	/**
	 * 読み取り処理
	 */
	private BitReader bitReader;

	/**
	 * コンストラクタ(色キャッシュの初期化)。
	 */
	public ColorTable(BitReader bitReader) {
		this.bitReader = bitReader;

		color = new short[CACHE_TABLE_SIZE];
		next = new int[CACHE_TABLE_SIZE];
		prev = new int[CACHE_TABLE_SIZE];

		for (int i = 0; i < CACHE_TABLE_SIZE; i++) {
			color[i] = 0;
			prev[i] = (i + 1) % CACHE_TABLE_SIZE;
			next[i] = (i - 1 + CACHE_TABLE_SIZE ) % CACHE_TABLE_SIZE;
		}

		color_p = 0;
	}

	/**
	 * キャッシュから色を取り出し、その色が最新になるように更新する。
	 *
	 * @param idx
	 *            インデックス
	 * @return 15bitの色コード(0 - 65534)
	 */
	private short getColor(int idx) {
		// 取り出した色が最新となるように更新する
		// (取り出す色が最新ではない場合のみ実行)
		if (color_p != idx) {
			// まず位置idxをキャッシュから切り離す
			next[prev[idx]] = next[idx];
			prev[next[idx]] = prev[idx];

			// 最新色の次にidxを新たにセット
			next[prev[color_p]] = idx;
			prev[idx] = prev[color_p];
			prev[color_p] = idx;
			next[idx] = color_p;

			// 最新色位置を更新
			color_p = idx;
		}

		// ２倍するのは、キャッシュに入っている色は bit0..14 で いるのは bit1..15 だから
		return (short)(color[idx] << 1);
	}

	/**
	 * 新しい色をキャッシュに登録。
	 *
	 * @param c
	 * @return
	 */
	private short regColor(short c) {
		color_p = prev[color_p];
		color[color_p] = c;

		// ２倍するのは、キャッシュに入っている色は bit0..14 で いるのは bit1..15 だから
		return (short)(c << 1);
	}

	/**
	 * 色の読み込み。
	 *
	 * @return 15bitの色コード(0 - 65534)
	 * @throws IOException
	 */
	public short read() throws IOException {
		if (bitReader.read(1) == 0) {
			// キャッシュミス
			return regColor((short)bitReader.read(15));
		} else {
			// キャッシュヒット
			return getColor(bitReader.read(7));
		}
	}
}