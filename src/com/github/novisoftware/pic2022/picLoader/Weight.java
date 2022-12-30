package com.github.novisoftware.pic2022.picLoader;

import java.io.IOException;

/**
 * ウェイト処理。
 *
 * オリジナルの実装には当然存在しない処理。
 * ロード処理は適当なタイミングでウェイト処理を呼ぶ。
 *
 */
public class Weight {
	private int weightCounter = 0;
//	private int weightPeriod = 20;
	private int weightPeriod = 70;
	private int weightMilliSec = 10;

	private boolean isDisposed = false;
	NotifyInterface notifyTo;


	public Weight(NotifyInterface notifyTo) {
		this.notifyTo = notifyTo;
	}

	/**
	 * 打ち切りを通知する。
	 *
	 * 読み込み中に別のファイルの読み込みを開始する場合がある。
	 * 読み込み中だったら、それ以上は読み込みの動作を継続する必要がないため、中断させる
	 */
	public void setDispose() {
		this.isDisposed = true;
	}

	/**
	 * ウェイト処理は weightPeriod 回に1回 weightMilliSec ミリ秒 sleepする。
	 * sleepの後、描画処理に対して再描画を通知する。
	 * 打ち切り通知を検出した場合は IOException を throw する。
	 *
	 * @throws IOException
	 */
	public void weightFunction() throws IOException {
		++ weightCounter;
		if (weightCounter % weightPeriod == 0) {
			try {
				Thread.sleep(weightMilliSec);
			} catch(InterruptedException e) {
				// 処理不要
			}
			if (this.isDisposed) {
				// IOException を流用する
				throw new IOException("Loading Cancel");
			}
			if (this.notifyTo != null) {
				// 再描画を通知
				this.notifyTo.notifyToParent();
			}
		}
	}
}