package com.github.novisoftware.pic2022.picLoader;

import java.io.File;
import java.io.IOException;

/**
 * PIC読み込み処理。
 *
 * やなぎさわ氏( http://www.vector.co.jp/vpack/browse/person/an001322.html )による
 * PICフォーマット仕様書( http://www.vector.co.jp/soft/data/art/se003198.html )に同梱された
 * PICの説明のオマケのローダーである  picl.c に基づきます。
 *
 */
public class PicLoader {
	/**
	 * サイズ制限(X方向ドット数)
	 */
	final int LIMIT_OF_WIDTH = 1024 * 10;
	/**
	 * サイズ制限(Y方向ドット数)
	 */
	final int LIMIT_OF_HEIGHT = 1024 * 10;

	private BitReader bitReader;
	private ColorTable colorTable;
	private PictureData picture;
	private PictureInfo pictureInfo;
	private Weight weight;
	private boolean isWeight = true;

	/**
	 * コンストラクタ。
	 *
	 * @param file 読み込むPICファイルのFileオブジェクト。
	 * @param notifyTo 表示更新の通知先オブジェクト。
	 * @throws IOException
	 * @throws PicDecodeException
	 */
	public PicLoader(File file, NotifyInterface notifyTo, boolean isWeight) throws IOException, PicDecodeException {
		this.weight = new Weight(notifyTo);
		this.isWeight = isWeight;

		BitReader bitReader = null;
		try {
			bitReader = new BitReader(file);

			this.bitReader = bitReader;
			this.colorTable = new ColorTable(bitReader);
			PictureInfo info = this.readHeader();
			this.pictureInfo = info;
			this.picture = new PictureData(info.getWidth(), info.getHeight());
		} catch (IOException e) {
			if (bitReader != null) {
				bitReader.close();
				bitReader = null;
			}
			throw e;
		} catch (PicDecodeException e) {
			if (bitReader != null) {
				bitReader.close();
				bitReader = null;
			}
			throw e;
		}
	}

	/**
	 * 読み込み実行。
	 *
	 * @throws IOException
	 */
	public void load() throws IOException {
		try {
			this.expand(this.picture);
		} finally {
			if (bitReader != null) {
				bitReader.close();
			}
		}
	}

	/**
	 * 読み込み中に別のファイルの読み込みを開始する場合がある。
	 * 読み込み中だったら、それ以上は読み込みの動作を継続する必要がないため、中断させる。
	 */
	public void setDispose() {
		this.weight.setDispose();
	}

	/**
	 * ウェイト処理をやめる
	 */
	public void setNoWeight() {
		this.isWeight = false;
	}

	/**
	 * 画像バッファオブジェクトを取得
	 *
	 * @return
	 */
	public PictureData getPicture() {
		return picture;
	}

	/**
	 * 画像の情報を表現する文字列を取得する。
	 *
	 * @return
	 */
	public String getInfoString() {
		if (picture != null && pictureInfo != null) {
			return picture.width + "x" + picture.getHeight() + "  " + pictureInfo.getComment();
		}
		return "";
	}

	/**
	 * コメント文字列のみ取得。
	 *
	 * @return
	 */
	public String getComment() {
		if (pictureInfo == null) {
			return "";
		}
		return pictureInfo.getComment();
	}

	/**
	 * 長さの読み込み。
	 *
	 * オリジナルの実装をほぼそのまま流用。
	 *
	 * @return 長さ(単位: bit)
	 * @throws IOException
	 */
	public int readLength() throws IOException {
		int a = 1;
		while (bitReader.read(1) != 0) {
			a++;
		}
		return bitReader.read(a) + (1 << a) - 1;
	}

	/**
	 * 連鎖の展開。
	 *
	 * オリジナルの実装（「ＰＩＣの説明のオマケのローダー」）をほぼそのまま流用。
	 *
	 * @param picture 画像バッファ
	 * @param x 現在のx位置
	 * @param y 現在のy位置
	 * @param c 現在の色
	 * @throws IOException
	 */
	private void expand_chain(PictureData picture, int x, int y, short c) throws IOException {
		for (;;) {
			switch (bitReader.read(2)) {
			case 0:
				if (bitReader.read(1) == 0) {
					return; // 終わり
				}
				if (bitReader.read(1) == 0) {
					x -= 2; // 左２つ
				} else {
					x += 2; // 右２つ
				}
				break;
			case 1:
				x--;
				break; // 左１つ
			case 2:
				break; // 真下
			case 3:
				x++;
				break; // 右１つ
			}
			y++;

			// 連鎖を書き込む。
			//  " | 1"するのは c = 0 の時にも連鎖だと判るように。
			picture.pset(x, y, (short)(c | 1));
		}
	}

	/**
	 * 展開するぞ。
	 *
	 * オリジナルの実装（「ＰＩＣの説明のオマケのローダー」）をほぼそのまま流用。
	 *
	 * @param picture 画像バッファ
	 * @throws IOException
	 */
	private void expand(PictureData picture) throws IOException {
		int x_wid = picture.getWidth();
		int y_wid = picture.getHeight();

		long length; // 変化点間の長さ

		int x = -1;// 展開中の位置 X
		int y = 0; // 展開中の位置 Y
		short c = 0; // 現在の色。(初期値0)
		for (;;) {
			length = readLength(); // 長さ読み込み

			// 次の変化点まで繰り返す
			while (--length != 0) {
				// 右端の処理
				if (++x == x_wid) {
					if (++y == y_wid)
						return; // (^_^;)
					x = 0;
				}
				// 連鎖点上を通過した時は、現在の色を変更
				short a = picture.point(x, y);
				if (a != 0) {
					c = (short)(a & 0xfffe);
				}
				// 現在の色を書き込む
				picture.pset(x, y, c);
			}
			// 右端の処理
			if (++x == x_wid) {
				if (++y == y_wid)
					return; /* (^_^;) */
				x = 0;
			}
			// 新しい色の読み込み
			c = this.colorTable.read();

			// それを書いて
			picture.pset(x, y, c);

			// 連鎖ありなら、連鎖の展開
			if (bitReader.read(1) != 0) {
				expand_chain(picture, x, y, c);
			}

			// 描画の様子がわかるように、遅くする。
			if (isWeight) {
				this.weight.weightFunction();
			}
		}
	}

	/**
	 * ヘッダのよみこみ。
	 *
	 * オリジナルの実装（「ＰＩＣの説明のオマケのローダー」）をほぼそのまま流用。
	 *
	 * @return 画像のメタ情報相当
	 * @throws IOException
	 * @throws PicDecodeException
	 */
	public PictureInfo readHeader() throws IOException, PicDecodeException
	{
		// 識別部
		if ( bitReader.read(8) != 'P' ||  bitReader.read(8) != 'I' ||  bitReader.read(8) != 'C' ) {
			throw new PicDecodeException("picではありませんよ");
		}

		// コメント部
		StringBuilder sb = new StringBuilder();
		int	c;
		while ((c = bitReader.read(8)) != 26) {
			char ch = (char)c;
			sb.append(ch);
		}
		String comment = sb.toString();

		// ここは読み飛ばす
		while (bitReader.read(8) != 0) {
			; // null loop
		}

		// ここは 0 のはず
		if (bitReader.read(8) != 0) {
			throw new PicDecodeException("picではありませんよ");
		}

		// タイプ/モードともに 0しか対応していない
		if (bitReader.read(8) != 0 ) {
			throw new PicDecodeException("ごめんね対応していないの");
		}

		// 15bit色しか対応していない
		if (bitReader.read(16) != 15 ) {
			throw new PicDecodeException("ごめんね対応していないの");
		}
		int x_wid= bitReader.read(16);
		if (x_wid> LIMIT_OF_WIDTH ) {
			throw new PicDecodeException("大きくて読めないよ");
		}
		int y_wid = bitReader.read(16);
		if (y_wid > LIMIT_OF_HEIGHT) {
			throw new PicDecodeException("大きくて読めないよ");
		}

		return new PictureInfo(x_wid, y_wid, comment);
	}

}
