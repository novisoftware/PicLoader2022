package com.github.novisoftware.pic2022.viewer;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.github.novisoftware.pic2022.picLoader.NotifyInterface;
import com.github.novisoftware.pic2022.picLoader.PicDecodeException;
import com.github.novisoftware.pic2022.picLoader.PicLoader;
import com.github.novisoftware.pic2022.picLoader.PictureData;


class MyPanel extends JPanel implements NotifyInterface, Runnable, MouseListener, KeyListener {
	PictureData nullPicture = new PictureData(512, 512);
	private PictureData pictureData;
	final int RENDER_MODE_NUMBER = 3;
	int renderMode;
	final Window parent;

	private JScrollPane scrollPane;

	MyPanel(Window parent) {
		this.parent = parent;
		this.addMouseListener(this);
		// this.addKeyListener(this);
	}

	void setScrollPane(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	void setPicutureData(PictureData pictureData) {
		this.pictureData = pictureData;

		// 倍率
		int xw = 1, xh = 1;
		switch (renderMode) {
		case 0:
			xw = 1;
			xh = 1;
			break;
		case 1:
			xw = 2;
			xh = 2;
			break;
		case 2:
			xw = 3;
			xh = 2;
			break;
		}

		Dimension d = new Dimension(pictureData.getHeight() * xw, pictureData.getWidth() * xh);

		this.setPreferredSize(d);
		this.scrollPane.setPreferredSize(d);
//		this.parent.getContentPane().repaint();
		this.parent.getContentPane().repaint();
		this.parent.setSize(this.parent.getSize());
	}

	/**
	 * 描画
	 */
	public void paint(Graphics gOrg) {
		int renderMode = this.renderMode;
		Graphics2D g2 = (Graphics2D)gOrg;

		PictureData p = pictureData;
		if (p == null) {
			p = this.nullPicture;
		}

		int width = p.getWidth();
		int height = p.getHeight();


		Dimension nowDim = this.getSize();


		if (nowDim.width > width || nowDim.height > height) {
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, nowDim.width, nowDim.height);
		}

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int c = p.point(x, y);
				// X68000の16bitカラーを Java の Color オブジェクトに変換
				// LSB(輝度ビット。最下位ビットに位置)はPIC.Rの描画結果に含まれていないので考慮しない

				/*
				// この式の場合、下位3ビットが ALL 0 になり、少し暗くなる
				int r = ((c >> 11) & 0x1f) << 3;
				int g = ((c >>  6) & 0x1f) << 3;
				int b = ((c >>  1) & 0x1f) << 3;
				g2.setColor(new Color(g, r, b));
				*/
				int r = (c >> 11) & 0x1f;
				int r_ = (r << 3) + (r >> 2);

				int g = (c >>  6) & 0x1f;
				int g_ = (g << 3) + (g >> 2);

				int b = (c >>  1) & 0x1f;
				int b_ = (b << 3) + (b >> 2);

				g2.setColor(new Color(g_, r_, b_));
				switch (renderMode) {
				case 0:
					g2.fillRect(x, y,  1, 1);
					break;
				case 1:
					g2.fillRect(x*2, y*2,  2, 2);
					break;
				case 2:
					g2.fillRect(x*3, y*2,  3, 2);
					break;
				}
			}
		}
	}

	/**
	 * JFrame側のサイズを表示倍率に応じて変更する。
	 *
	 * @param renderMode
	 */
	void setSizeWithRenderMode(int renderMode) {
		Insets insets = parent.getInsets();

		switch (renderMode) {
		case 0:
			parent.setSize(Window.INITIAL_WIDTH + insets.left + insets.right,
					Window.INITIAL_HEIGHT + insets.top + insets.bottom);
			break;
		case 1:
			parent.setSize(Window.INITIAL_WIDTH  * 2 + insets.left + insets.right,
					Window.INITIAL_HEIGHT * 2 + insets.top + insets.bottom);
			break;
		case 2:
			parent.setSize(Window.INITIAL_WIDTH  * 3 + insets.left + insets.right,
					Window.INITIAL_HEIGHT * 2 + insets.top + insets.bottom);
			break;
		}
	}

	/**
	 * 表示倍率のモードをトグル切り換えする。
	 */
	void toggleRenderMode(boolean b) {
		if (b) {
			this.renderMode = (this.renderMode + 1) % RENDER_MODE_NUMBER;
		} else {
			this.renderMode = (this.renderMode - 1 + RENDER_MODE_NUMBER) % RENDER_MODE_NUMBER;
		}
		this.setSizeWithRenderMode(renderMode);
	}

	@Override
	public void notifyToParent() {
		try {
			// repaintを行う。
			SwingUtilities.invokeAndWait(this);
		} catch (InvocationTargetException | InterruptedException e) {
			// e.printStackTrace();
		}
	}


	// Runnable
	@Override
	public void run() {
		this.repaint();
	}

	// MouseListener

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			this.toggleRenderMode(true);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	boolean enableShiftKey = false;


	/**
	 * キーを押したときの動作
	 */
	@Override
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SHIFT:
			this.enableShiftKey = true;
			break;
		case KeyEvent.VK_SPACE:
			// スペースが押されたら、読み込みのウェイト処理を外す
		{
			PicLoader p = parent.runningPicLoader;
			if (p != null) {
				p.setNoWeight();
			}
			else {
				// 読み込み中でなかったら、次のがあれば表示する

				if (parent.history.setNext()) {
					parent.load(parent.history.getCurrent(), true);
				}
				break;
			}
		}
			break;
		case KeyEvent.VK_N:
			parent.isWeight = ! parent.isWeight;
			if (! parent.isWeight) {
				// ウェイトモードでなくなった場合は、読み込み中のウェイト動作も解除する
				PicLoader p = parent.runningPicLoader;
				if (p != null) {
					p.setNoWeight();
				}
			}

			break;
		case KeyEvent.VK_Q:
			// Q が押されたら終了する
			System.exit(0);
			break;
		case KeyEvent.VK_S:
			// S が押されたら拡大倍率を切り替える
			this.toggleRenderMode(! this.enableShiftKey);
			break;
		case KeyEvent.VK_C:
			parent.history.clear();
			parent.load(null, true);
			break;
		case KeyEvent.VK_R:
			parent.load(parent.history.getCurrent(), true);
			break;
		case KeyEvent.VK_LEFT:
			if (this.enableShiftKey) {
				if (parent.history.setFirst()) {
					parent.load(parent.history.getCurrent(), true);
				}
			} else {
				if (parent.history.setPrev()) {
					parent.load(parent.history.getCurrent(), true);
				}
			}
			break;
		case KeyEvent.VK_KP_LEFT:
			if (this.enableShiftKey) {
				if (parent.history.setFirst()) {
					parent.load(parent.history.getCurrent(), true);
				}
			} else {
				if (parent.history.setPrev()) {
					parent.load(parent.history.getCurrent(), true);
				}
			}
			break;
		case KeyEvent.VK_RIGHT:
			if (this.enableShiftKey) {
				if (parent.history.setLast()) {
					parent.load(parent.history.getCurrent(), true);
				}
				break;
			} else {
				if (parent.history.setNext()) {
					parent.load(parent.history.getCurrent(), true);
				}
			}
			break;
		case KeyEvent.VK_KP_RIGHT:
			if (this.enableShiftKey) {
				if (parent.history.setLast()) {
					parent.load(parent.history.getCurrent(), true);
				}
				break;
			} else {
				if (parent.history.setNext()) {
					parent.load(parent.history.getCurrent(), true);
				}
			}
			break;
		case KeyEvent.VK_D:
			parent.history.dropThis();
			parent.load(parent.history.getCurrent(), true);
			break;
		case KeyEvent.VK_P:
			parent.history.printState();
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SHIFT:
			this.enableShiftKey = false;
			break;
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

}

public class Window extends JFrame implements DragGestureListener, DropTargetListener {
	static final int INITIAL_WIDTH = 512;
	static final int INITIAL_HEIGHT = 512;
	MyPanel innerPanel;
	HistoryList history;
	boolean isWeight = true;

	public static final String RESOURCE_FRAME_ICON = "/com/github/novisoftware/pic2022/viewer/sharpxlogo.png";

	Window() {
		this.history = new HistoryList();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.setTitle("画像ファイルをドラッグ&ドロップしてください");
		DropTarget dt = new DropTarget(this,DnDConstants.ACTION_COPY, this);
		this.setDropTarget(dt);
		this.innerPanel = new MyPanel(this);
		this.innerPanel.setSizeWithRenderMode(0);
		this.addKeyListener(this.innerPanel);

		Container contentPane = getContentPane();
	    JScrollPane scrollPane = new JScrollPane(this.innerPanel);
	    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    contentPane.add(scrollPane);

	    this.innerPanel.setScrollPane(scrollPane);


	    // ウィンドウのアイコンを設定
		try {
			this.setIconImage(ImageIO.read(this.getClass().getResource(
					Window.RESOURCE_FRAME_ICON)));
		} catch (Exception e) {
			// 特段の処理は不要
			// e.printStackTrace();
		}
	}

	/**
	 * タイトルバーに文字列を設定する
	 */
	public void setTitle(String title) {
		super.setTitle("Pic Loader  2022  --  " + title);
	}

	PicLoader runningPicLoader = null;

	/**
	 * 画像をロードする。
	 *
	 * @param f 画像ファイル
	 */
	public void load(File f, boolean fromHistory) {
		if (f == null) {
			this.innerPanel.setPicutureData(this.innerPanel.nullPicture);
			this.innerPanel.repaint();
			this.setTitle("");
			return;
		}

		final Window this_ = this;

		try {
			PicLoader picLoader = new PicLoader(f, this.innerPanel, this.isWeight);
			this.innerPanel.setPicutureData(picLoader.getPicture());
			this.runningPicLoader = picLoader;
			if (!fromHistory) {
				this.history.add(f);
			}
			this.setTitle(history.getStatusString() + f.getName() + "  " +picLoader.getInfoString());
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						picLoader.load();
						// イベントディスパッチスレッドでrepaint()させる
						this_.innerPanel.notifyToParent();
					} catch (IOException e) {
						this_.history.remove(f);
						this_.setTitle("Error: " + e.toString());
					} finally {
						this_.runningPicLoader = null;
					}
				}
			}).start();
		} catch (IOException e) {
			this.setTitle(f.getName() + " 【エラー】" + e.toString());
			this.innerPanel.setPicutureData(this.innerPanel.nullPicture);
			this.innerPanel.repaint();
		} catch (PicDecodeException e) {
			this.setTitle(f.getName() + " 【エラー】" + e.getMessage());
			this.innerPanel.setPicutureData(this.innerPanel.nullPicture);
			this.innerPanel.repaint();
		}
	}

	/**
	 * ウィンドウへのアイコンドロップ時の処理
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void drop(DropTargetDropEvent e) {
        e.acceptDrop(DnDConstants.ACTION_COPY);
        Transferable tr = e.getTransferable();
		try {
			int oldSize = this.history.getCurrentSize();

			Object o = tr.getTransferData(DataFlavor.javaFileListFlavor);
			if (o instanceof List) {
				List list = (List)o;
				for (Object member: list) {
					if (member instanceof File) {
						File file = (File)member;

						this.load(file, false);
			        	// System.out.println( file.getName());
					} else {
						// ファイルでなかった場合(成立しないはず)。
					}
				}
			} else {
				// 取得したものが List オブジェクトでなかった場合(成立しないはず)。
			}

			// 複数の画像ファイルをドラッグドロップした場合、履歴を1個目に戻す。
			// (裏では野放図に資源を浪費している)
			int newSize = this.history.getCurrentSize();
			if (newSize - oldSize > 1) {
				this.history.setCurrentIndex(oldSize);
				this.load(this.history.getCurrent(), true);
			}
		} catch (UnsupportedFlavorException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		// 特に処理は行わない
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		// 特に処理は行わない
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
		// 特に処理は行わない
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		// 特に処理は行わない
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		// 特に処理は行わない
	}
}

