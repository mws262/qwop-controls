package ui;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import ui.IUserInterface.TabbedPaneActivator;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class PanelLogger extends JPanel implements TabbedPaneActivator {

    /**
     * Should this panel be drawing or is it hidden.
     */
    transient protected boolean active = false;

    private final JTextPane logTextArea;

    public PanelLogger() {
        setLayout(new BorderLayout());

//        logTextPane = new JTextPane();
        logTextArea = new JTextPane(); //JTextArea(5,15);
        logTextArea.setEditable(false);
        //EmptyBorder eb = new EmptyBorder(new Insets(10, 10, 10, 10));
        //logTextArea.setBorder(eb);
        //logTextArea.setMargin(new Insets(5, 5, 5, 5));
        logTextArea.setBackground(new Color(128, 128, 128, 60));
        logTextArea.setBorder(null);
//        EmptyBorder eb = new EmptyBorder(new Insets(10, 10, 10, 10));
//        logTextArea.setBorder(eb);
        this.setBorder(null);
        //this.setBackground(new Color(128, 128, 128, 60));

        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setPreferredSize(new Dimension(1000, 200));
        scrollPane.setBorder(null);
        scrollPane.setBackground(new Color(128, 128, 128, 60));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getVerticalScrollBar().setBackground(new Color(128, 128, 128, 128));
//        scrollPane.getVerticalScrollBar().setForeground(Color.BLUE);
//        scrollPane.getVerticalScrollBar().setBorder(BorderFactory.createLineBorder(Color.black, 1, true));
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI()
        {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(128, 128, 128, 180);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                JButton decreaseButton = new JButton();
                decreaseButton.setPreferredSize(new Dimension(22, 22));
                decreaseButton.setBackground(new Color(128, 128, 128, 20));
                return decreaseButton;
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                JButton increaseButton = new JButton();
                increaseButton.setPreferredSize(new Dimension(22, 22));
                increaseButton.setBackground(Color.BLACK);
                return increaseButton;
            }
        });
        //new SmartScroller(scrollPane);


//        Map<TextAttribute, Object> fontSettings = new HashMap<>();
//        fontSettings.put(TextAttribute.FAMILY, SANS_SERIF);
//        fontSettings.put(TextAttribute.SIZE, 18.0);
//        fontSettings.put(TextAttribute.FOREGROUND, new Color(PanelTree.darkText[0], PanelTree.darkText[1],
//                PanelTree.darkText[2]));
//
//        logTextArea.setFont(new Font(fontSettings));
        add(scrollPane);
        setVisible(true);

        LoggerContext lc = (LoggerContext) LogManager.getContext(false);

        PatternLayout layout = PatternLayout.newBuilder().withConfiguration(lc.getConfiguration()).withPattern(
                "%d{yyyy-MM-dd HH:mm:ss} [%p] %c{1} %m%n").withAlwaysWriteExceptions(true).build();

        PanelLogAppender appender = new PanelLogAppender(logTextArea, "placeholder",
                ThresholdFilter.createFilter(Level.INFO,
                Filter.Result.ACCEPT, Filter.Result.ACCEPT), layout);
        appender.start();

        // Modifying logging configuration after initialization. See:
        // https://stackoverflow.com/questions/15441477/how-to-add-log4j2-appenders-at-runtime-programmatically/
        lc.getConfiguration().addAppender(appender);
        lc.getRootLogger().addAppender(lc.getConfiguration().getAppender(appender.getName()));
        lc.updateLoggers();

    }

    @Override
    public void activateTab() {
        active = true;
    }

    @Override
    public void deactivateTab() {
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public String getName() {
        return "Logging";
    }

    public static class PanelLogAppender extends AbstractAppender {

        private JTextPane logTextArea;
        private StyledDocument doc;


        private Style style1;
        private Style style2;


        protected PanelLogAppender(JTextPane logTextArea, String name, Filter filter, Layout layout) {
            super(name, filter, layout, false, Property.EMPTY_ARRAY);
            this.logTextArea = logTextArea;
            doc = logTextArea.getStyledDocument();

            style1 = logTextArea.addStyle("s1", null);
            StyleConstants.setForeground(style1, Color.red);
            style2 = logTextArea.addStyle("s2", style1);
            StyleConstants.setBold(style2, true);

        }

        int i = 0;
        @Override
        public void append(LogEvent event) {
            try {
                doc.insertString(doc.getLength(), new String(getLayout().toByteArray(event)), (i++ % 2 == 0) ?
                        style1 : style2);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    
}
