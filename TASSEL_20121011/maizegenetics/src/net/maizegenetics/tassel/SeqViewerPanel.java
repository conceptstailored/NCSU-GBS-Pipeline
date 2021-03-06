/*
 * SeqViewerPanel
 */
package net.maizegenetics.tassel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableColumn;

import net.maizegenetics.gui.AlignmentTableCellRenderer;
import net.maizegenetics.gui.AlignmentTableModel;
import net.maizegenetics.gui.RowHeaderRenderer;
import net.maizegenetics.gui.TableRowHeaderListModel;
import net.maizegenetics.gui.MultiTextRowHeader;
import net.maizegenetics.pal.alignment.Alignment;
import net.maizegenetics.pal.alignment.AlignmentMask;
import net.maizegenetics.pal.alignment.AlignmentMaskBoolean;
import net.maizegenetics.pal.ids.Identifier;
import net.maizegenetics.plugindef.Datum;

/**
 *
 * @author terry
 */
public class SeqViewerPanel extends JPanel implements ComponentListener, TableModelListener {

    private static final int ROW_HEADER_WIDTH = 150;
    private static final int SCROLL_BAR_WIDTH = 25;
    private static final int TABLE_COLUMN_WIDTH = 10;
    private static final int TABLE_COLUMN_MARGIN = 5;
    private static final int SLIDER_TEXT_WIDTH = 125;
    private JSlider mySlider;
    private JButton myLeftButton;
    private JButton myRightButton;
    private final JTable myTable;
    private final JPopupMenu myMenu = new JPopupMenu();
    private final AlignmentTableModel myTableModel;
    private final Alignment myAlignment;
    private final JScrollPane myScrollPane;
    private JPanel mySliderPane;
    private JLabel blankSpace;
    private JLabel searchLabel;
    private JTextField searchField;
    private JButton searchButton;
    private int start;
    private int end;
    private int startPos;
    private int endPos;
    private int siteCount;
    private final AlignmentMask[] myMasks;
    private final DataTreePanel myDataTreePanel;

    public SeqViewerPanel(Alignment alignment, DataTreePanel dataTreePanel) {
        this(alignment, null, dataTreePanel);
    }

    public SeqViewerPanel(Alignment alignment, AlignmentMask[] masks, DataTreePanel dataTreePanel) {

        setLayout(new BorderLayout());
        myAlignment = alignment;
        myMasks = masks;
        myDataTreePanel = dataTreePanel;
        myTableModel = new AlignmentTableModel(alignment);

        siteCount = myAlignment.getSiteCount();
        start = 0;
        end = siteCount - 1;
        startPos = myAlignment.getPositionInLocus(0);
        endPos = myAlignment.getPositionInLocus(end);

        mySlider = new JSlider();
        mySlider.addChangeListener(myTableModel);

        /*
        int min = myAlignment.getPositionInLocus(0);
        int max = myAlignment.getPositionInLocus(myAlignment.getSiteCount() - 1);
        int tableSize = max - min + 1;

        mySlider = new JSlider(min, max);
        mySlider.setMajorTickSpacing(tableSize / 10);
        mySlider.setPaintTicks(true);
        mySlider.setPaintLabels(true);
        mySlider.addChangeListener(myTableModel);
        mySlider.setValue(min + (max - min) / 2);
         */

        myTableModel.adjustPositionToCenter();
        myTableModel.addTableModelListener(this);
        myTable = new JTable(myTableModel);
        myTable.setUI(new MyTableUI());
        myTable.setDefaultRenderer(myTable.getColumnClass(0), new AlignmentTableCellRenderer(myTableModel, myMasks));
        myTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        JList rowHeaders = new JList(new TableRowHeaderListModel(myTableModel.getRowHeaders())) {

            public String getToolTipText(MouseEvent evt) {

                int index = locationToIndex(evt.getPoint());
                Identifier id = (Identifier) getModel().getElementAt(index);

                return id.getFullName();

            }
        };

        rowHeaders.setFixedCellWidth(ROW_HEADER_WIDTH);
        rowHeaders.setFixedCellHeight(myTable.getRowHeight());
        rowHeaders.setCellRenderer(new RowHeaderRenderer(myTable));

        myScrollPane = new JScrollPane(myTable);
        myScrollPane.addComponentListener(this);
        myScrollPane.setRowHeaderView(rowHeaders);

        add(getControls(), BorderLayout.NORTH);
        add(myScrollPane, BorderLayout.CENTER);

        boolean multipleAlignments = myAlignment.getNumLoci() > 1;

        if (multipleAlignments) {
            myTableModel.setColumnNameType(AlignmentTableModel.COLUMN_NAME_TYPE.siteNumber);
            updateSliderPhysicalPositions();
        } else {
            myTableModel.setColumnNameType(AlignmentTableModel.COLUMN_NAME_TYPE.physicalPosition);
            updateSliderPhysicalPositions();
        }

        initMenu();
    }

    private void initMenu() {

        myMenu.setInvoker(this);

        JMenuItem useAsReference = new JMenuItem("Use this Taxa as Reference");
        useAsReference.addActionListener(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                int index = myTable.getSelectedRow();
                AlignmentMaskBoolean mask = AlignmentMaskBoolean.getInstanceCompareReference(myAlignment, index);
                myDataTreePanel.addDatum(new Datum(mask.toString(), mask, null));
            }
        });
        myMenu.add(useAsReference);

        myTable.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                //showPopup(e);
                myMenu.setLocation(e.getXOnScreen(), e.getYOnScreen());
                myMenu.setVisible(true);
            }

            public void mouseReleased(MouseEvent e) {
                //showPopup(e);
                myMenu.setVisible(false);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    myMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

    }

    private JPanel getControls() {

        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));

        RadioListener radioListener = new RadioListener();

        ButtonGroup buttonGroup = new ButtonGroup();

        boolean multipleAlignments = myAlignment.getNumLoci() > 1;

        JRadioButton physicalPosition = null;
        if (!multipleAlignments) {
            physicalPosition = new JRadioButton("Physical Positions");
            physicalPosition.setActionCommand(AlignmentTableModel.COLUMN_NAME_TYPE.physicalPosition.toString());
            physicalPosition.addActionListener(radioListener);
        }

        JRadioButton siteNumber = new JRadioButton("Site Numbers");
        siteNumber.setActionCommand(AlignmentTableModel.COLUMN_NAME_TYPE.siteNumber.toString());
        siteNumber.addActionListener(radioListener);

        JRadioButton locus = new JRadioButton("Locus");
        locus.setActionCommand(AlignmentTableModel.COLUMN_NAME_TYPE.locus.toString());
        locus.addActionListener(radioListener);

        JRadioButton siteName = new JRadioButton("Site Name");
        siteName.setActionCommand(AlignmentTableModel.COLUMN_NAME_TYPE.siteName.toString());
        siteName.addActionListener(radioListener);

        JRadioButton alleles = new JRadioButton("Alleles");
        alleles.setActionCommand(AlignmentTableModel.COLUMN_NAME_TYPE.alleles.toString());
        alleles.addActionListener(radioListener);

        blankSpace = new JLabel();
        blankSpace.setPreferredSize(new Dimension(70, 25));
        searchLabel = new JLabel();
        searchLabel.setPreferredSize(new Dimension(70, 25));
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(225, 25));
        searchField.setText("(Enter physical position)");
        searchButton = new JButton("Search");

        searchButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                searchButton_actionPerformed(e);
            }
        });

        JPanel selectColumnHeadings = new JPanel(new FlowLayout(FlowLayout.LEFT));

        if (!multipleAlignments) {
            buttonGroup.add(physicalPosition);
        }
        buttonGroup.add(siteNumber);
        buttonGroup.add(locus);
        buttonGroup.add(siteName);
        buttonGroup.add(alleles);
        if (multipleAlignments) {
            buttonGroup.setSelected(siteNumber.getModel(), true);
        } else {
            buttonGroup.setSelected(physicalPosition.getModel(), true);
        }

        if (!multipleAlignments) {
            selectColumnHeadings.add(physicalPosition);
        }
        selectColumnHeadings.add(siteNumber);
        selectColumnHeadings.add(locus);
        selectColumnHeadings.add(siteName);
        selectColumnHeadings.add(alleles);
        selectColumnHeadings.add(blankSpace);
        selectColumnHeadings.add(searchLabel);
        selectColumnHeadings.add(searchField);
        selectColumnHeadings.add(searchButton);

        result.add(selectColumnHeadings);

        result.add(getSliderPane());

        return result;

    }

    private void searchButton_actionPerformed(ActionEvent e) {
        try {
            int searchValue = (int)Double.parseDouble(searchField.getText().trim());
            if (myTableModel.getColumnNameType().equals(AlignmentTableModel.COLUMN_NAME_TYPE.physicalPosition)) {
                if (searchValue > endPos) {
                    JOptionPane.showMessageDialog(this.getParent(), "Physical position must be between " + startPos + " and " + endPos + ".");
                } else if (searchValue < startPos) {
                    JOptionPane.showMessageDialog(this.getParent(), "Physical position must be between " + startPos + " and " + endPos + ".");
                } else {
                    mySlider.setValue(searchValue);
                }
            } else if (myTableModel.getColumnNameType().equals(AlignmentTableModel.COLUMN_NAME_TYPE.siteNumber)) {
                if (searchValue > end) {
                    JOptionPane.showMessageDialog(this.getParent(), "Site number must be between " + start + " and " + end + ".");
                } else if (searchValue < start) {
                    JOptionPane.showMessageDialog(this.getParent(), "Site number must be between " + start + " and " + end + ".");
                } else {
                    mySlider.setValue(searchValue);
                }
            }
        } catch (Exception ex) {
            String positionType = "";
            if (myTableModel.getColumnNameType().equals(AlignmentTableModel.COLUMN_NAME_TYPE.physicalPosition)) {
                positionType = "physical position";
            } else if (myTableModel.getColumnNameType().equals(AlignmentTableModel.COLUMN_NAME_TYPE.siteNumber)) {
                positionType = "site number";
            }
            JOptionPane.showMessageDialog(this.getParent(), "Invalid " + positionType + ".");
        }
    }

    private void hideSearchFunction() {
        searchField.setVisible(false);
        searchButton.setVisible(false);
    }

    private void showSearchFunction() {
        searchField.setVisible(true);
        searchButton.setVisible(true);
    }

    private void changeTextPhysicalPosition() {
        searchField.setText("(Enter physical position)");
    }

    private void changeTextSiteNumber() {
        searchField.setText("(Enter site number)");
    }

    private JPanel getSliderPane() {

        mySliderPane = new JPanel(new BorderLayout());

        URL imageURL = SeqViewerPanel.class.getResource("left.gif");
        ImageIcon imageIcon = null;
        if (imageURL != null) {
            imageIcon = new ImageIcon(imageURL);
        }

        myLeftButton = new JButton(imageIcon);
        myLeftButton.addActionListener(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if (myTableModel.isPhysicalPosition()) {
                    int newSite = myTableModel.getHorizontalCenter() - myTableModel.getHorizontalPageSize() * 3 / 4;
                    newSite = Math.max(0, newSite);
                    mySlider.setValue(myAlignment.getPositionInLocus(newSite));
                } else {
                    int newValue = mySlider.getValue() - myTableModel.getHorizontalPageSize() * 3 / 4;
                    newValue = Math.max(mySlider.getMinimum(), newValue);
                    mySlider.setValue(newValue);
                }
            }
        });
        mySliderPane.add(myLeftButton, BorderLayout.WEST);

        mySliderPane.add(mySlider, BorderLayout.CENTER);

        imageURL = SeqViewerPanel.class.getResource("right.gif");
        imageIcon = null;
        if (imageURL != null) {
            imageIcon = new ImageIcon(imageURL);
        }

        myRightButton = new JButton(imageIcon);
        myRightButton.addActionListener(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if (myTableModel.isPhysicalPosition()) {
                    int newSite = myTableModel.getHorizontalCenter() + myTableModel.getHorizontalPageSize() * 3 / 4;
                    newSite = Math.min(myAlignment.getSiteCount() - 1, newSite);
                    mySlider.setValue(myAlignment.getPositionInLocus(newSite));
                } else {
                    int newValue = mySlider.getValue() + myTableModel.getHorizontalPageSize() * 3 / 4;
                    newValue = Math.min(mySlider.getMaximum(), newValue);
                    mySlider.setValue(newValue);
                }
            }
        });
        mySliderPane.add(myRightButton, BorderLayout.EAST);

        return mySliderPane;

    }

    private void updateSliderPhysicalPositions() {

        int min = myAlignment.getPositionInLocus(0);
        int max = myAlignment.getPositionInLocus(myAlignment.getSiteCount() - 1);
        int tableSize = max - min + 1;
        int center = myTableModel.getHorizontalCenter();
        mySlider.setMinimum(min);
        mySlider.setMaximum(max);
        mySlider.setPaintTicks(true);
        mySlider.setPaintLabels(true);
        int scrollWidth = myScrollPane.getWidth();
        int numTicks = 0;
        if (scrollWidth <= 0) {
            numTicks = 10;
        } else {
            numTicks = scrollWidth / SLIDER_TEXT_WIDTH;
        }
        int spacing = tableSize / numTicks;
        if (spacing < 1) {
            spacing = 1;
        }
        mySlider.setLabelTable(mySlider.createStandardLabels(spacing));
        mySlider.setMajorTickSpacing(spacing);
        mySlider.setValue(myAlignment.getPositionInLocus(center));
        mySlider.validate();
        mySliderPane.validate();
        mySliderPane.repaint();
        repaint();

    }

    private void updateSliderSiteNumbers() {

        int tableSize = myTableModel.getRealColumnCount();
        int center = myTableModel.getHorizontalCenter();
        mySlider.setMinimum(0);
        mySlider.setMaximum(tableSize - 1);
        mySlider.setPaintTicks(true);
        mySlider.setPaintLabels(true);
        int scrollWidth = myScrollPane.getWidth();
        int numTicks = 0;
        if (scrollWidth <= 0) {
            numTicks = 10;
        } else {
            numTicks = scrollWidth / SLIDER_TEXT_WIDTH;
        }
        int spacing = tableSize / numTicks;
        if (spacing < 1) {
            spacing = 1;
        }
        mySlider.setLabelTable(mySlider.createStandardLabels(spacing));
        mySlider.setMajorTickSpacing(spacing);
        mySlider.setValue(center);
        mySlider.validate();
        mySliderPane.validate();
        mySliderPane.repaint();
        repaint();

    }

    public void componentResized(ComponentEvent e) {
        Dimension size = e.getComponent().getSize();
        int width = (int) size.getWidth() - ROW_HEADER_WIDTH - SCROLL_BAR_WIDTH;
        int columnWidth = TABLE_COLUMN_WIDTH + TABLE_COLUMN_MARGIN * 2;
        try {
            columnWidth = myAlignment.getDataType().getFormattedString(myAlignment.getBaseChar(0, 0)).length() * TABLE_COLUMN_WIDTH + TABLE_COLUMN_MARGIN * 2;
        } catch (Exception ex) {
            // do nothing
        }
        int numColumns = width / columnWidth;
        myTableModel.setHorizontalPageSize(numColumns);

        if (myTableModel.isPhysicalPosition()) {
            updateSliderPhysicalPositions();
        } else if (!myTableModel.isPhysicalPosition()) {
            updateSliderSiteNumbers();
        }
    }

    public void componentMoved(ComponentEvent e) {
        // do nothing
    }

    public void componentShown(ComponentEvent e) {
        // do nothing
    }

    public void componentHidden(ComponentEvent e) {
        // do nothing
    }

    public void tableChanged(TableModelEvent e) {
        for (int c = 0; c < myTable.getColumnCount(); c++) {
            TableColumn col = myTable.getColumnModel().getColumn(c);
            col.setHeaderRenderer(new MultiTextRowHeader(1, '.'));
        }

        if (myTableModel.isPhysicalPosition()) {
            updateSliderPhysicalPositions();
        } else if (!myTableModel.isPhysicalPosition()) {
            updateSliderSiteNumbers();
        }
    }

    class RadioListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(AlignmentTableModel.COLUMN_NAME_TYPE.physicalPosition.toString())) {
                myTableModel.setColumnNameType(AlignmentTableModel.COLUMN_NAME_TYPE.physicalPosition);
                updateSliderPhysicalPositions();
                showSearchFunction();
                changeTextPhysicalPosition();
            } else if (e.getActionCommand().equals(AlignmentTableModel.COLUMN_NAME_TYPE.siteNumber.toString())) {
                myTableModel.setColumnNameType(AlignmentTableModel.COLUMN_NAME_TYPE.siteNumber);
                updateSliderSiteNumbers();
                showSearchFunction();
                changeTextSiteNumber();
            } else if (e.getActionCommand().equals(AlignmentTableModel.COLUMN_NAME_TYPE.locus.toString())) {
                myTableModel.setColumnNameType(AlignmentTableModel.COLUMN_NAME_TYPE.locus);
                hideSearchFunction();
            } else if (e.getActionCommand().equals(AlignmentTableModel.COLUMN_NAME_TYPE.alleles.toString())) {
                myTableModel.setColumnNameType(AlignmentTableModel.COLUMN_NAME_TYPE.alleles);
                hideSearchFunction();
            } else if (e.getActionCommand().equals(AlignmentTableModel.COLUMN_NAME_TYPE.siteName.toString())) {
                myTableModel.setColumnNameType(AlignmentTableModel.COLUMN_NAME_TYPE.siteName);
                hideSearchFunction();
            }
        }
    }

    public class MyTableUI extends BasicTableUI {

        private boolean justClicked = false;

        protected MouseInputListener createMouseInputListener() {
            return (new MyMouseInputListener());
        }

        public class MyMouseInputListener extends BasicTableUI.MouseInputHandler {

            public void mousePressed(MouseEvent e) {

                JTable table = (JTable) e.getSource();

                if (!table.isRowSelected(table.rowAtPoint(e.getPoint()))) {

                    int mod = e.getModifiers();

                    if ((mod & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK) {
                        mod = mod ^ MouseEvent.BUTTON3_MASK;
                        mod = mod | MouseEvent.BUTTON1_MASK;
                        e = new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), mod, e.getX(), e.getY(), e.getClickCount(), true);
                    }

                    super.mousePressed(e);

                    justClicked = true;

                }

            }

            public void mouseClicked(MouseEvent e) {

                if (!justClicked) {
                    super.mousePressed(e);
                } else {
                    justClicked = false;
                }

            }

            public void mouseDragged(MouseEvent e) {
            }
        }
    }
}
