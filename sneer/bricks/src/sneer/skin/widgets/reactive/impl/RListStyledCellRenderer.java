package sneer.skin.widgets.reactive.impl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import sneer.skin.widgets.resizer.Resizer;
import wheel.reactive.Signal;
import wheel.reactive.impl.Receiver;

class RListStyledCellRenderer<ELEMENT> extends RListSimpleCellRenderer<ELEMENT>  {

	private static final String MESSAGE = "message";
	private static final String TIME = "time";
	private static final String SENDER = "sender";
	static final int scrollWidth = 20;
	private final Resizer _resizer;
	private final SimpleDateFormat FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	
	RListStyledCellRenderer(RListImpl<ELEMENT> rlist, Resizer resizer) {
		super(rlist);
		_resizer = resizer;
	}

	@Override
	public Component getListCellRendererComponent(JList ignored, Object value,
			int ignored2, boolean isSelected, boolean cellHasFocus) {

		JComponent icon = createIcon(value);
		JComponent text = createText(value);
		JComponent root = createRootPanel(icon, text);

		new Receiver<Object>() {
			@Override
			public void consume(Object ignore) {
				_rlist.repaintList();
			}
		};

		_resizer.pack(text, _rlist.getSize().width - scrollWidth);
		addLineSpace(root);
		return root;
	}
	
	private JComponent createIcon(Object value) {
		Signal<Image> signalImage = _rlist._labelProvider.imageFor(getElement(value));
		JLabel icon = new JLabel(new ImageIcon(signalImage.currentValue()));
		icon.setOpaque(false);
		return icon;
	}
	
	private JComponent createText(Object value) {
		Signal<String> signalText = _rlist._labelProvider.labelFor(getElement(value));
		JTextPane area = new JTextPane();
		StyledDocument doc = new DefaultStyledDocument();
	    initDocumentStyles(doc);
	    
		appendSyledText(doc, getSender() + " ", SENDER);
		appendSyledText(doc, getFormatedDate() + "\n", TIME);
		appendSyledText(doc, signalText.currentValue(), MESSAGE);

		area.setDocument(doc);
		return area;
	}

	private void appendSyledText(StyledDocument doc, String msg, String style) {
		try {
			doc.insertString(doc.getLength(), msg, doc.getStyle(style));
		} catch (BadLocationException e) {
			throw new wheel.lang.exceptions.NotImplementedYet(e); // Fix Handle this exception.
		}
	}

	private String getFormatedDate() {
		//Implement get tuple date
		return FORMAT.format(new Date());
	}

	private String getSender() {
		//Implement get tuple sender
		return ("SENDER").toUpperCase();
	}

	private void initDocumentStyles(StyledDocument doc) {
		Style def = StyleContext.getDefaultStyleContext().getStyle( StyleContext.DEFAULT_STYLE );

	    Style sender = doc.addStyle( SENDER, def );
	    StyleConstants.setForeground(sender, Color.DARK_GRAY);
	    StyleConstants.setFontSize( sender, 11 );
	    StyleConstants.setBold(sender, true);
	    
	    Style time = doc.addStyle( TIME, def );
	    StyleConstants.setFontSize( time, 10 );
	    StyleConstants.setForeground(time, Color.LIGHT_GRAY);

	    doc.addStyle( MESSAGE, def );
	}

	private JComponent createRootPanel(JComponent icon, JComponent area) {
		JPanel root = new JPanel();
		root.setLayout(new GridBagLayout());
		root.setOpaque(false);

		root.add(icon, new GridBagConstraints(0, 0, 1, 1, 0, 0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));

		root.add(area, new GridBagConstraints(1, 0, 1, 1, 1., 1.,
				GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));
		return root;
	}

	private void addLineSpace(JComponent root) {
		Dimension psize = root.getPreferredSize();
		root.setPreferredSize(new Dimension(psize.width, psize.height + _rlist.getLineSpace()));
	}
}