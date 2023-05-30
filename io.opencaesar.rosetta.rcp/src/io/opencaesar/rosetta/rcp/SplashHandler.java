package io.opencaesar.rosetta.rcp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.splash.BasicSplashHandler;
import org.osgi.framework.FrameworkUtil;

/**
 * Draws the version number in the bottom-right corner of the splash screen
 */
public class SplashHandler extends BasicSplashHandler {

	@Override
	public void init(Shell splash) {
		super.init(splash);
		Rectangle splashBounds = splash.getBounds();
		
		Composite overlay = new Composite(splash, SWT.NO_BACKGROUND);
		overlay.setBounds(0, 0, splashBounds.width, splashBounds.height);
		overlay.addPaintListener(e -> {
			Font font = new Font(e.gc.getDevice(), "", 12 * 72 / Display.getDefault().getDPI().y, SWT.BOLD);
			try {
				Color foreground = new Color(e.gc.getDevice(), 255, 255, 255);
				try {
					e.gc.setFont(font);
					e.gc.setForeground(foreground);
					e.gc.setTextAntialias(SWT.ON);
					
					String versionString = "Version " + FrameworkUtil.getBundle(getClass()).getVersion();
					Point textExtent = e.gc.textExtent(versionString);
					e.gc.drawText(versionString, splashBounds.width/2 - textExtent.x/2, splashBounds.height - textExtent.y - 10, true);
				} finally {
					foreground.dispose();
				}
			} finally {
				font.dispose();
			}
		});
	}

}
