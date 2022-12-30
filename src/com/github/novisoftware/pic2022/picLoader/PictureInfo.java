package com.github.novisoftware.pic2022.picLoader;

/**
 * PICフォーマットのヘッダ部に記載される Xドット数(x_wid) Yドット数(y_wid) コメントデータ
 */
class PictureInfo {
	private int x_wid;
	private int y_wid;
	private String comment;

	PictureInfo(int x_wid, int y_wid, String comment) {
		this.x_wid = x_wid;
		this.y_wid = y_wid;
		this.comment = comment;

		/*
		System.out.println("width: " + x_wid);
		System.out.println("height: " + y_wid);
		System.out.println("comment: " + comment);
		*/
	}

	int getWidth() {
		return this.x_wid;
	}

	int getHeight() {
		return this.y_wid;
	}

	String getComment() {
		return this.comment;
	}
}