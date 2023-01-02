package com.github.novisoftware.pic2022.viewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.github.novisoftware.pic2022.picLoader.PicEncoder;

public class Main {
	static public void main(String arg[]) {
		if (arg.length > 0 && arg[0].equals("--encode")) {
			if (arg.length != 3) {
				System.err.println("以下の指定をするとPICエンコードを行います。");
				System.err.println("    --encode 入力画像ファイル 出力PICファイル");
				return;
			}

			try {
				PicEncoder.cui(arg[1], arg[2]);
			} catch (FileNotFoundException e) {
				// 分かりやすい FileNotFoundException の場合は toString() だけを表示する。
				System.out.println(e.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			Window w = new Window();
			w.setVisible(true);

			// 引数があった場合
			if (arg.length > 0) {
				for (String s : arg) {
					w.load(new File(s), false);
				}
				if (arg.length > 1) {
					w.history.setCurrentIndex(0);
					w.load(w.history.getCurrent(), true);
				}
			}
		}
	}
}
