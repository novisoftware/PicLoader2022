package com.github.novisoftware.pic2022.viewer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 読み込んだファイルを簡易的にぐるぐる表示できるようにする
 *
 */
public class HistoryList {
	List<File> list;
	int index;

	HistoryList() {
		this.list = new ArrayList<File>();
	}

	void clear() {
		synchronized( list ) {
			list.clear();
		}
	}

	void add(File f) {
		synchronized( list ) {
			list.add(f);
			index = list.size() - 1;
		}
	}

	void remove(File f) {
		synchronized( list ) {
			list.remove(f);
			index = list.size() - 1;
		}
	}

	boolean setPrev() {
		synchronized( list ) {
			if (list.size() == 0) {
				return false;
			}

			index --;
			if (index < 0) {
				index = 0;
				return false;
			}
			return true;
		}
	}

	boolean setNext() {
		synchronized( list ) {
			if (list.size() == 0) {
				return false;
			}

			index ++;
			if (index >= list.size()) {
				index = list.size() - 1;
				return false;
			}
			return true;
		}
	}

	boolean setFirst() {
		synchronized( list ) {
			if (list.size() == 0) {
				return false;
			}

			if (index == 0) {
				return false;
			}
			index = 0;
			return true;
		}
	}

	boolean setLast() {
		synchronized( list ) {
			if (list.size() == 0) {
				return false;
			}

			if (index == list.size() - 1) {
				return false;
			}
			index = list.size() - 1;
			return true;
		}
	}


	File getCurrent() {
		synchronized( list ) {
			if (list.size() == 0) {
				return null;
			}

			return list.get(index);
		}
	}

	void dropThis() {
		synchronized( list ) {
			if (list.size() == 0) {
				return;
			}

			list.remove(index);
			index --;
			if (index < 0) {
				index = 0;
			}
		}
	}

	String getStatusString() {
		synchronized( list ) {
			if (list.size() <= 1) {
				return "";
			}
			return "[" + (this.index + 1) + "/" + list.size() + "] ";
		}
	}

	void printState() {
		System.out.println();
		synchronized( list ) {
			for (int i = 0 ; i < list.size(); i++) {
				System.out.format("%3d %s %s\n", i, index == i ? "*" : " ", list.get(i).getName() );
			}
		}
	}

	int getCurrentSize() {
		synchronized( list ) {
			return list.size();
		}
	}

	void setCurrentIndex(int index) {
		synchronized( list ) {
			if (index >= list.size()) {
				return;
			}

			this.index = index;
		}
	}

}