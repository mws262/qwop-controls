package ui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.AbstractXYDataset;

import main.Node;
import main.PanelPlot;
import main.State;
import main.Utility;

/**
 * Pane for displaying plots of various state variables. Click each plot to pull up a menu for selecting data.
 * A tab.
 * @author Matt
 */
public class PanelPlot_States extends PanelPlot implements ItemListener{

	private static final long serialVersionUID = 1L;

	/** Node from which states are referenced. **/
	private Node selectedNode;
	
	/** Which plot index has an active menu. **/
	int activePlotIdx = 0;

	/** Body parts associated with each plot and axis. **/
	private State.ObjectName[] plotObjectsX = new State.ObjectName[numberOfPlots];
	private State.ObjectName[] plotObjectsY = new State.ObjectName[numberOfPlots];

	/** State variables associated with each plot and axis. **/
	private State.StateName[] plotStatesX = new State.StateName[numberOfPlots];
	private State.StateName[] plotStatesY = new State.StateName[numberOfPlots];

	/** Drop down menus for the things to plot. **/
	private JComboBox<String> objListX;
	private JComboBox<String> stateListX;
	private JComboBox<String> objListY;
	private JComboBox<String> stateListY;

	/** String names of the body parts. **/
	private final String[] objNames = new String[State.ObjectName.values().length];

	/** String names of the state variables. **/
	private final String[] stateNames = new String[State.StateName.values().length];

	/** Menu for selecting which data is displayed. **/
	private final JDialog menu;

	/** Offsets to put the data selection menu right above the correct panel. **/
	private final int menuXOffset = 30;
	private final int menuYOffset = -30;

	public PanelPlot_States(int numPlots) {
		super(numPlots);
		// Make string arrays of the body part and state variable names.
		int count = 0;
		for (State.ObjectName obj : State.ObjectName.values()) {
			objNames[count] = obj.name();
			count++;
		}
		count = 0;
		for (State.StateName st : State.StateName.values()) {
			stateNames[count] = st.name();
			count++;
		}

		// Initial plots to display			
		for (int i = 0; i < numberOfPlots; i++) {
			plotObjectsX[i] = State.ObjectName.values()[Utility.randInt(0, numberOfPlots - 1)];
			plotStatesX[i] = State.StateName.values()[Utility.randInt(0, numberOfPlots - 1)];
			plotObjectsY[i] = State.ObjectName.values()[Utility.randInt(0, numberOfPlots - 1)];
			plotStatesY[i] = State.StateName.values()[Utility.randInt(0, numberOfPlots - 1)];	
		}

		// Drop down menus
		objListX = new JComboBox<>(objNames);
		stateListX = new JComboBox<>(stateNames);
		objListY = new JComboBox<>(objNames);
		stateListY = new JComboBox<>(stateNames);

		objListX.addItemListener(this);
		stateListX.addItemListener(this);
		objListY.addItemListener(this);
		stateListY.addItemListener(this);

		menu = new JDialog(); // Pop up box for the menus.
		menu.setLayout(new GridLayout(2,4));
		menu.add(new JLabel("X-axis", SwingConstants.CENTER));
		menu.getContentPane().add(objListX);
		menu.getContentPane().add(stateListX);
		menu.add(new JLabel("Y-axis", SwingConstants.CENTER));
		menu.getContentPane().add(objListY);
		menu.getContentPane().add(stateListY);

		menu.setAlwaysOnTop(true);
		menu.pack();
		menu.setVisible(false); // Start with this panel hidden.
	}

	@Override
	public void update(Node selectedNode) {
		this.selectedNode = selectedNode;
		// Fetching new data.
		ArrayList<Node> nodesBelow = new ArrayList<Node>();
		if (selectedNode != null) {
			selectedNode.getNodesBelow(nodesBelow);

			for (int i = 0; i < numberOfPlots; i++) {
				XYPlot pl = (XYPlot)plotPanels[i].getChart().getPlot();
				LinkStateCombination statePlotDat = new LinkStateCombination(nodesBelow);
				pl.setRenderer(statePlotDat.getRenderer());
				statePlotDat.addSeries(0, plotObjectsX[i], plotStatesX[i], plotObjectsY[i], plotStatesY[i]);
				pl.setDataset(statePlotDat);
				pl.getRangeAxis().setLabel(plotObjectsX[i].toString() + " " + plotStatesX[i].toString());
				pl.getDomainAxis().setLabel(plotObjectsY[i].toString() + " " + plotStatesY[i].toString());
				JFreeChart chart = plotPanels[i].getChart();
				chart.fireChartChanged();
				//XYPlot plot = (XYPlot)(plotPanels[i].getChart().getPlot());
				//pl.getDomainAxis().setRange(new Range(statePlotDat1.xLimLo,statePlotDat1.xLimHi));
				//plot.getRangeAxis().setRange(range);
			}

			if (!plotColorsByDepth) {
				addCommandLegend((XYPlot)plotPanels[0].getChart().getPlot());
			}
		}
	}

	@Override
	public void deactivateTab() {
		super.deactivateTab();
		menu.setVisible(false);	
	}

	@Override
	public void plotClicked(int plotIdx) {
		activePlotIdx = plotIdx;
		menu.setTitle("Select plot " + (plotIdx + 1) + " data.");
		menu.setLocation(plotPanels[plotIdx].getX() + menuXOffset, menuYOffset);
		objListX.setSelectedIndex(plotObjectsX[plotIdx].ordinal()); // Make the drop down menus match the current plots.
		objListY.setSelectedIndex(plotObjectsY[plotIdx].ordinal());
		stateListX.setSelectedIndex(plotStatesX[plotIdx].ordinal());
		stateListY.setSelectedIndex(plotStatesY[plotIdx].ordinal());
		menu.setVisible(true);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		int state = e.getStateChange();
		if (state == ItemEvent.SELECTED) {
			if (e.getSource() == objListX) {
				plotObjectsX[activePlotIdx] = State.ObjectName.valueOf((String)e.getItem());
			}else if (e.getSource() == objListY) {
				plotObjectsY[activePlotIdx] = State.ObjectName.valueOf((String)e.getItem());
			}else if (e.getSource() == stateListX) {
				plotStatesX[activePlotIdx] = State.StateName.valueOf((String)e.getItem());
			}else if ((e.getSource() == stateListY)) {
				plotStatesY[activePlotIdx] = State.StateName.valueOf((String)e.getItem());
			}else{
				throw new RuntimeException("Unknown item status in plots from: " + e.getSource().toString());
			}
			update(selectedNode);
		}
	}
	
	/** XYDataset gets data for ploting states vs other states here. **/
	public class LinkStateCombination extends AbstractXYDataset{

		private static final long serialVersionUID = 1L;
		
		public float xLimHi = Float.MIN_VALUE;
		public float xLimLo = Float.MAX_VALUE;

		/** Nodes to appear on the plot. **/
		private final ArrayList<Node> nodeList;

		private Map<Integer,Pair> dataSeries = new HashMap<Integer,Pair>();

		private StatePlotRenderer renderer = new StatePlotRenderer();

		public LinkStateCombination(ArrayList<Node> nodes) {
			nodeList = nodes;
		}

		public void addSeries(int plotIdx, State.ObjectName objectX, State.StateName stateX,
				State.ObjectName objectY, State.StateName stateY) {
			dataSeries.put(plotIdx, new Pair(plotIdx, objectX, stateX,
					objectY, stateY));
		}

		@Override
		public Number getX(int series, int item) {
			State state = nodeList.get(item).state; // Item is which node.
			Pair dat = dataSeries.get(series);
			float value = state.getStateVarFromName(dat.objectX, dat.stateX);

			if (value > xLimLo) {
				xLimLo = value;
			}
			if (value < xLimHi) {
				xLimLo = value;
			}

			return value;
		}
		@Override
		public Number getY(int series, int item) {
			State state = nodeList.get(item).state; // Item is which node.
			Pair dat = dataSeries.get(series);
			return state.getStateVarFromName(dat.objectY, dat.stateY);
		}

		/** State + body part name pairs for looking up data. **/
		private class Pair{
			State.ObjectName objectX;
			State.StateName stateX;
			State.ObjectName objectY;
			State.StateName stateY;

			public Pair(int plotIdx, State.ObjectName objectX, State.StateName stateX,
					State.ObjectName objectY, State.StateName stateY) {
				this.objectX = objectX;
				this.objectY = objectY;
				this.stateX = stateX;
				this.stateY = stateY;
			}
		}

		@Override
		public int getItemCount(int series) {
			return nodeList.size();
		}

		@Override
		public int getSeriesCount() {
			return dataSeries.size();
		}

		@Override
		public Comparable getSeriesKey(int series) { return null; }

		public XYLineAndShapeRenderer getRenderer() { return renderer; }

		public class StatePlotRenderer extends XYLineAndShapeRenderer {
			private static final long serialVersionUID = 1L;
			
			/** Color points by corresponding depth in the tree or by command leading to this point. **/
			public boolean colorByDepth = plotColorsByDepth;

			public StatePlotRenderer() {
				super(false, true); //boolean lines, boolean shapes
				setSeriesShape( 0, new Ellipse2D.Double( -2.0, -2.0, 2.0, 2.0 ) );
				setUseOutlinePaint(false);
			}

			@Override
			public Paint getItemPaint(int series, int item) {
				if (colorByDepth) {
					return Node.getColorFromTreeDepth(nodeList.get(item).treeDepth);
				}else{
					Color dotColor = Color.RED;
					switch (nodeList.get(item).treeDepth % 4) {
					case 0:
						dotColor = actionColor1;
						break;
					case 1:
						dotColor = actionColor2;
						break;
					case 2:
						dotColor = actionColor3;
						break;
					case 3:
						dotColor = actionColor4;
						break;
					}
					return dotColor;
				}
			}
			@Override
			public java.awt.Shape getItemShape(int row, int col) {
				return super.getItemShape(row, col);
			}
		}
	}	
}
