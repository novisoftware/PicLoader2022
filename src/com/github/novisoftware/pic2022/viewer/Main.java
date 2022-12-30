package com.github.novisoftware.pic2022.viewer;

import java.io.File;

public class Main {
	static public void main(String arg[]) {
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
