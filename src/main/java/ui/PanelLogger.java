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
import java.util.HashMap;
import java.util.Map;

import static ui.PanelLogger.PanelLogAppender.StyleType.*;

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
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
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
                "%c{1} %m%n").withAlwaysWriteExceptions(true).build();

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


        enum StyleType {
            WHITE, RED, BLUE, YELLOW, GREEN,
            WHITE_BOLD, RED_BOLD, BLUE_BOLD, YELLOW_BOLD, GREEN_BOLD
        }

        Map<StyleType, Style> styles = new HashMap<>();

        protected PanelLogAppender(JTextPane logTextArea, String name, Filter filter, Layout layout) {
            super(name, filter, layout, false, Property.EMPTY_ARRAY);
            this.logTextArea = logTextArea;
            doc = logTextArea.getStyledDocument();
            addStyle(WHITE, WHITE_BOLD, new Color(PanelTree.darkText[0], PanelTree.darkText[1], PanelTree.darkText[2]));
            addStyle(RED, RED_BOLD, new Color(181, 29, 49));
            addStyle(BLUE, BLUE_BOLD, new Color(37, 126, 173));
            addStyle(YELLOW, YELLOW_BOLD, new Color(176, 156, 37));
            addStyle(GREEN, GREEN_BOLD, new Color(91, 171, 124));


        }

        int i = 0;

        @Override
        public void append(LogEvent event) {

            Style style;
            switch (event.getLevel().getStandardLevel()) {
                case OFF:
                    style = styles.get(WHITE);
                    break;
                case FATAL:
                    style = styles.get(RED_BOLD);
                    break;
                case ERROR:
                    style = styles.get(RED);
                    break;
                case WARN:
                    style = styles.get(YELLOW);
                    break;
                case INFO:
                    style = styles.get(WHITE);
                    break;
                case DEBUG:
                    style = styles.get(GREEN);
                    break;
                case TRACE:
                    style = styles.get(GREEN);
                    break;
                case ALL:
                    style = styles.get(WHITE);
                    break;
                default:
                    style = styles.get(WHITE);
            }
            try {
                doc.insertString(doc.getLength(),
                        event.getLevel().name() + '\t',
                        styles.get(RED_BOLD));

                doc.insertString(doc.getLength(),
                        event.getSource().getFileName().split("\\.")[0] + '\t',
                        styles.get(GREEN_BOLD));

                doc.insertString(doc.getLength(),
                        new String(getLayout().toByteArray(event)),
                        style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        private void addStyle(StyleType type, StyleType boldType, Color color) {
            Style style = logTextArea.addStyle(type.name(), null);
            StyleConstants.setForeground(style, color);
            styles.put(type, style);

            style = logTextArea.addStyle(boldType.name(), style);
            StyleConstants.setBold(style, true);
            styles.put(boldType, style);
        }
    }
}
