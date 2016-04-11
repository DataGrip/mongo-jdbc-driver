package com.mongoui.table.model;

import com.jidesoft.grid.*;
import com.jidesoft.swing.JideSwingUtilities;
import com.mongoui.table.ui.FitScrollPane;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

public class TableFactory implements HierarchicalTableComponentFactory {

    private final ListSelectionModelGroup selectionGroup;

    public TableFactory(ListSelectionModelGroup _selectionGroup){
        this.selectionGroup = _selectionGroup;
    }

    @Override
    public Component createChildComponent(HierarchicalTable table, Object value, int row) {
        if ( value instanceof HierarchicalTableModel) {
            return createHierarchicalTable((TableModel) value, row);
        } else if ( value instanceof TableModel ) {
            return createFlatTable((TableModel) value, row);
        } else if ( value instanceof TableModel[] ) {
            final JPanel panel = new JPanel();
            final BoxLayout boxLayout = new BoxLayout( panel, BoxLayout.Y_AXIS );
            panel.setLayout( boxLayout );
            for ( TableModel val : (TableModel[])value) {
                if ( val instanceof HierarchicalTableModel) {
                    panel.add(createHierarchicalTable(val, row));
                } else {
                    panel.add(createFlatTable(val, row));
                }
            }
            return panel;
        }
        return new JPanel();
    }


    @Override
    public void destroyChildComponent(HierarchicalTable table, Component component, int row) {
        Component t = JideSwingUtilities.getFirstChildOf(JTable.class, component);
        if (t instanceof JTable) {
            selectionGroup.remove(((JTable) t).getSelectionModel());
        }
    }


    private JComponent createHierarchicalTable(final TableModel model, final int row){
        final HierarchicalTable childTable = new HierarchicalTable(model) {
            @Override
            public void scrollRectToVisible(Rectangle aRect) {
                FitScrollPane.scrollRectToVisible(this, aRect);
            }
        };
        childTable.setOpaque(false);
        childTable.setFillsRight( true);
        childTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF);
        childTable.setName("Detail Table");
        childTable.setComponentFactory(this);
        selectionGroup.add(childTable.getSelectionModel());
        fixTableHeader(model, childTable);
        return new DetailPanel( childTable );
    }


    private JComponent createFlatTable(final TableModel model, final int row) {
        SortableTable sortableTable = new SortableTable(model) {
            @Override
            public void scrollRectToVisible(Rectangle aRect) {
                FitScrollPane.scrollRectToVisible(this, aRect);
            }
        };
        sortableTable.setFillsRight( true );
        sortableTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        selectionGroup.add(sortableTable.getSelectionModel());
        fixTableHeader(model, sortableTable);
        return new DetailPanel( sortableTable ) ;
    }

    private class DetailPanel extends TreeLikeHierarchicalPanel{

        public DetailPanel(CategorizedTable childTable){
            super(new FitScrollPane(childTable));
            setOpaque( false );
        }
    }

    public static void fixTableHeader(TableModel model, SortableTable childTable) {
        childTable.setNestedTableHeader(true);
        TableColumnGroup food = new TableColumnGroup(model.getColumnName( -1 ));
        for ( int i = 0; i < childTable.getColumnCount(); i++){
            food.add( childTable.getColumnModel().getColumn(i));
        }

        if ( childTable.getTableHeader() instanceof NestedTableHeader) {
            NestedTableHeader header = (NestedTableHeader) childTable.getTableHeader();
            header.addColumnGroup(food);
        }
        TableHeaderPopupMenuInstaller installer = new TableHeaderPopupMenuInstaller( childTable);
        installer.addTableHeaderPopupMenuCustomizer(new TableColumnChooserPopupMenuCustomizer());

        TableUtils.autoResizeAllColumns(childTable);
    }


}