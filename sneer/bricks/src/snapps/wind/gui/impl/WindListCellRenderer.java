package snapps.wind.gui.impl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import snapps.wind.Shout;
import sneer.kernel.container.Inject;
import sneer.pulp.contacts.Contact;
import sneer.pulp.keymanager.KeyManager;
import sneer.skin.widgets.reactive.LabelProvider;
import wheel.reactive.Signal;

class WindListCellRenderer implements ListCellRenderer {

	@Inject
	private static KeyManager _keys;

	private static final String SHOUT = "shout";
	private static final String SHOUTERS_NICK = "shoutersNick";
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("HH:mm");
	private static final int SCROLL_WIDTH = 10;
	
	private static final int SPACE_BETWEEN_LINES = 0;
	
	private final LabelProvider<Shout> _labelProvider;
	
	WindListCellRenderer(LabelProvider<Shout> labelProvider) {
		_labelProvider = labelProvider;
	}

	@Override
	public Component getListCellRendererComponent(JList jList, Object element, int ignored2, boolean isSelected, boolean cellHasFocus) {
		Shout shout = (Shout)element;
		JComponent nick = createNick(shout);
		JComponent shoutTime = createShoutTime(shout);
		JComponent shoutText = createShoutText(shout);
		JComponent root = createRootPanel(nick, shoutTime, shoutText);

//		shoutTime.setPreferredSize(new Dimension(50,15));
		Resizer.pack(shoutText, jList.getWidth() - SCROLL_WIDTH, 25);
		
		addLineSpace(root);
		return root;
	}
	
	private JComponent createNick(Shout shout) {
		if (isMyOwnShout(shout))
			return getNickAsIcon(shout);
		return getNickAsText(shout);
	}
	
	private JComponent getNickAsIcon(Shout shout) {
		Signal<Image> signalImage = _labelProvider.imageFor(shout);
		JLabel icon = new JLabel(new ImageIcon(signalImage.currentValue()), SwingConstants.LEFT);
		icon.setOpaque(false);
		return icon;
	}

	private JComponent getNickAsText(Shout shout) {
		Contact contact = _keys.contactGiven(shout.publisher);
		String nick = contact == null ? "<Unknown> " : contact.nickname().currentValue() + " ";
		JLabel icon = new JLabel(nick,  SwingConstants.LEFT);
		icon.setFont(new Font(icon.getFont().getFontName() , Font.BOLD, 11));
		icon.setForeground(Color.DARK_GRAY);
		return icon;
	}

	private JComponent createShoutTime(Shout shout) {
		JLabel icon = new JLabel(getFormatedShoutTime(shout) + " ",  SwingConstants.RIGHT);
		icon.setFont(new Font(icon.getFont().getFontName() , 0, 11));
		icon.setForeground(Color.LIGHT_GRAY);
		return icon;
	}

	private JComponent createShoutText(Shout shout) {
		Signal<String> signalText = _labelProvider.labelFor(shout);
		return createTextComponent(signalText.currentValue(), SHOUT);
	}	
	
	private JComponent createTextComponent(String msg, String style) {
		StyledDocument doc = new DefaultStyledDocument();
	    initDocumentStyles(doc);
	    appendStyledText(doc, msg, style);
		JTextPane result = new JTextPane();
		result.setDocument(doc);
		return result;
	}	
	
	private void appendStyledText(StyledDocument doc, String msg, String style) {
		try {
			doc.insertString(doc.getLength(), msg, doc.getStyle(style));
		} catch (BadLocationException e) {
			throw new wheel.lang.exceptions.NotImplementedYet(e); // Fix Handle this exception.
		}
	}

	private String getFormatedShoutTime(Shout shout) {
		return FORMAT.format(new Date(shout.publicationTime));
	}

	private boolean isMyOwnShout(Shout shout) {
		return _keys.ownPublicKey().equals(shout.publisher);
	}

	private void initDocumentStyles(StyledDocument doc) {
		Style def = StyleContext.getDefaultStyleContext().getStyle( StyleContext.DEFAULT_STYLE );

	    Style sender = doc.addStyle( SHOUTERS_NICK, def );
	    StyleConstants.setForeground(sender, Color.DARK_GRAY);
	    StyleConstants.setFontSize( sender, 11 );
	    StyleConstants.setBold(sender, true);
	    
	    doc.addStyle( SHOUT, def );
	}

	private JComponent createRootPanel(JComponent nick, JComponent time, JComponent shout) {
		JPanel root = new JPanel();
		root.setLayout(new GridBagLayout());
		root.setOpaque(false);

		root.add(nick, new GridBagConstraints(0, 0, 1, 1, 1., 0,
				GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 0), 0, 0));

		root.add(time, new GridBagConstraints(1, 0, 1, 1, 1., 0,
				GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 0), 0, 0));

		root.add(shout, new GridBagConstraints(0, 1, 2, 1, 1., 1.,
				GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));
		
//		nick.setBorder(new LineBorder(Color.BLACK));
//		time.setBorder(new LineBorder(Color.BLUE));
//		shout.setBorder(new LineBorder(Color.RED));
		return root;
	}

	private void addLineSpace(JComponent root) {
		Dimension psize = root.getPreferredSize();
		root.setPreferredSize(new Dimension(psize.width, psize.height + SPACE_BETWEEN_LINES));
	}
}