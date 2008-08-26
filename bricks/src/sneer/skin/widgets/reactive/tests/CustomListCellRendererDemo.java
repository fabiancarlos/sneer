package sneer.skin.widgets.reactive.tests;

import static wheel.lang.Types.cast;

import java.awt.Component;
import java.awt.Image;
import java.awt.Point;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import sneer.kernel.container.Container;
import sneer.kernel.container.ContainerUtils;
import sneer.skin.widgets.reactive.ListWidget;
import sneer.skin.widgets.reactive.RFactory;
import wheel.graphics.Images;
import wheel.reactive.Register;
import wheel.reactive.impl.RegisterImpl;

public class CustomListCellRendererDemo {

	public static void main(String[] args) throws Exception {
		Container container = ContainerUtils.getContainer();

		RFactory rfactory = container.produce(RFactory.class);
		Register<String[]> register = new RegisterImpl<String[]>(new String[]{"Klaus", "Sandro","Bamboo", "Nell"});
		
		createJFrame(register, rfactory, 0);
	}

	private static void createJFrame(Register<String[]> register, RFactory rfactory, int width) {
		JFrame f = new JFrame("Smooth List Drop");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		addReactiveListWidget(register, rfactory, f);
		
		f.pack();
		f.setLocation(new Point(width,10));
		f.setVisible(true);
	}

	private static void addReactiveListWidget(final Register<String[]> register, RFactory rfactory, JFrame f) {
		ListWidget<String> listw = cast(rfactory.newList(register.output(), register.setter()));
		final JList list = (JList) listw.getMainWidget(); 
		f.getContentPane().add(new JScrollPane(list));
		
		customizeRenderer(list);
	}

	private static void customizeRenderer(JList list) {
		
		list.setCellRenderer(new ListCellRenderer() {
			
			ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
			
			@Override
			public Component getListCellRendererComponent(JList lst,Object value, int index, boolean isSelected,boolean cellHasFocus) {
				JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(lst, value, index,isSelected, cellHasFocus);

				Image img = Images.getImage(CustomListCellRendererDemo.class.getResource("sample.png"));
				ImageIcon icon = new ImageIcon(img);
				renderer.setIcon(icon);

				return renderer;
			}
		});
	}
}