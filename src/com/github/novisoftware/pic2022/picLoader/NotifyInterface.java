package com.github.novisoftware.pic2022.picLoader;

/**
 * 読み込みの様子を描画するため、JPanel の再描画を定期的に行う。
 * これは、再描画を通知するためのインタフェース。
 */
public interface NotifyInterface {
	/**
	 * 再描画タイミングの通知。
	 */
	void notifyToParent();
}