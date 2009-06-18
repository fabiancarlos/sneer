package sneer.bricks.hardware.log.gui.impl;

import static sneer.foundation.environments.Environments.my;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import sneer.bricks.hardware.cpu.lang.Consumer;
import sneer.bricks.hardware.gui.Action;
import sneer.bricks.hardware.gui.guithread.GuiThread;
import sneer.bricks.hardware.log.gui.LogConsole;
import sneer.bricks.pulp.log.filter.LogFilter;
import sneer.bricks.pulp.log.workers.notifier.LogNotifier;
import sneer.bricks.pulp.reactive.Signals;
import sneer.bricks.pulp.reactive.collections.ListRegister;
import sneer.bricks.skin.main.dashboard.Dashboard;
import sneer.bricks.skin.main.menu.MainMenu;
import sneer.bricks.skin.main.synth.Synth;
import sneer.bricks.skin.main.synth.scroll.SynthScrolls;
import sneer.bricks.skin.widgets.reactive.ListWidget;
import sneer.bricks.skin.widgets.reactive.ReactiveWidgetFactory;
import sneer.bricks.skin.widgets.reactive.autoscroll.AutoScrolls;
import sneer.bricks.skin.windowboundssetter.WindowBoundsSetter;

class LogConsoleImpl extends JFrame implements LogConsole {

	private final Synth _synth = my(Synth.class);
	
	{_synth.load(this.getClass());}
	private final double _SPLIT_LOCATION = (((Integer) _synth.getDefaultProperty("LodConsoleImpl.splitLocationPercent")).doubleValue())/100;
	private final Integer _OFFSET_X = (Integer) _synth.getDefaultProperty("LodConsoleImpl.offsetX");
	private final Integer _OFFSET_Y = (Integer) _synth.getDefaultProperty("LodConsoleImpl.offsetY");
	private final Integer _HEIGHT = (Integer) _synth.getDefaultProperty("LodConsoleImpl.height");
	private final Integer _X = (Integer) _synth.getDefaultProperty("LodConsoleImpl.x");
		
	private final JPopupMenu _popupMenu = new JPopupMenu();
	private final MainMenu _mainMenu = my(MainMenu.class);

	@SuppressWarnings("unused")	private Object _referenceToAvoidGc;	

	{my(Dashboard.class);}

	LogConsoleImpl(){
		super("Sneer Log Console");
		addMenuAction();
		my(GuiThread.class).invokeLater(new Runnable(){ @Override public void run() {
			initGui();
		}});
	}

	private void addMenuAction() {
		Action cmd = new Action(){
			@Override public String caption() {	return "Open Log Console"; }
			@Override public void run() { open(); }
		};
		_mainMenu.getSneerMenu().addAction(cmd);
	}

	private void open() {
		setVisible(true);
	}

	private void initGui() {
		JTextArea txtLog = new JTextArea();
		txtLog.setEditable(false);

		JScrollPane scroll = newAutoScroll(txtLog);
		getContentPane().setLayout(new BorderLayout());
		
		Rectangle unused = my(WindowBoundsSetter.class).unusedArea();
		setBounds(_X , unused.height-_HEIGHT-_OFFSET_Y, unused.width-_OFFSET_X, _HEIGHT-_OFFSET_Y);

		JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, initFilterGui());
		main.setOneTouchExpandable(true);
		getContentPane().add(main, BorderLayout.CENTER);
		
		initLogReceiver(txtLog);
		initClearLogAction(txtLog);
		
		setVisible(true);
		main.setDividerLocation(_SPLIT_LOCATION);
	}

	private void initLogReceiver(final JTextArea txtLog) {
		_referenceToAvoidGc = my(Signals.class).receive(my(LogNotifier.class).loggedMessages(), new Consumer<String>() { @Override public void consume(String value) {
			txtLog.append(value);
		}});
	}

	private JPanel initFilterGui() {
		JPanel filter = new JPanel();
		_synth.attach(filter, "FilterPanel");
		filter.setLayout(new GridBagLayout());
		
		final ListRegister<String> whiteListEntries = my(LogFilter.class).whiteListEntries();
		final ListWidget<String> includes = my(ReactiveWidgetFactory.class).newList(whiteListEntries.output());
		JScrollPane scroll2 = my(SynthScrolls.class).create();
		scroll2.getViewport().add(includes.getComponent());
		scroll2.setBorder(new TitledBorder("Log Events That Contain:"));
		filter.add(scroll2, new GridBagConstraints(0,0,1,2,1.0,1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0,2,0,0), 0,0));
		
		final JTextField newInclude = new JTextField();
		newInclude.setBorder(new TitledBorder(""));
		filter.add(newInclude, new GridBagConstraints(0,2,1,1,1.0,0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0,4,2,2), 0,0));
		
		JButton addButton = new JButton();
		JButton delButton = new JButton();

		_synth.attach(addButton,"AddButton");
		_synth.attach(delButton,"DelButton");
		
		filter.add(delButton, new GridBagConstraints(1,0,1,1,0.0,0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0,0));
		filter.add(addButton, new GridBagConstraints(1,2,1,1,0.0,0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0,0));
		
		initAddFilterAction(whiteListEntries, newInclude, addButton);
		initDeleteFilterAction(whiteListEntries, includes, delButton);
		return filter;
	}

	private void initAddFilterAction(
			final ListRegister<String> whiteListEntries,
			final JTextField newInclude, JButton addButton) {
		addButton.addActionListener(new ActionListener(){ @Override public void actionPerformed(ActionEvent e) {
			String value = newInclude.getText();
			newInclude.setText("");
			if(value.length()==0) return;
			whiteListEntries.add(value);
		}});
	}

	private void initDeleteFilterAction(
			final ListRegister<String> whiteListEntries,
			final ListWidget<String> includes, JButton delButton) {
		delButton.addActionListener(new ActionListener(){ @Override public void actionPerformed(ActionEvent e) {
			Object[] values = includes.getMainWidget().getSelectedValues();
			for (Object value : values) 
				whiteListEntries.remove((String)value);
		}});
	}

	private void initClearLogAction(final JTextArea txtLog) {
		txtLog.addMouseListener(new MouseAdapter(){ 
			JMenuItem clear = new JMenuItem("Clear Log");{
				_popupMenu.add(clear);
				clear.addActionListener(new ActionListener(){ @Override public void actionPerformed(ActionEvent ae) {
					txtLog.setText("");
				}});			
			}

			@Override 
			public void mouseReleased(MouseEvent e) {
				if(e.isPopupTrigger())
					_popupMenu.show(e.getComponent(),e.getX(),e.getY());
			}
		});
	}

	private JScrollPane newAutoScroll(JComponent component) {
		JScrollPane scroll = my(AutoScrolls.class).create(my(LogNotifier.class).loggedMessages());
		scroll.getViewport().add(component);
		return scroll;
	}
}