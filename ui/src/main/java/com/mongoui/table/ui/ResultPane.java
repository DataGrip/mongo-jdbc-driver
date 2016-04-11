package com.mongoui.table.ui;

import com.jidesoft.grid.AutoFilterTableHeader;
import com.jidesoft.grid.HierarchicalTable;
import com.jidesoft.grid.ListSelectionModelGroup;
import com.jidesoft.grid.RowStripeTableStyleProvider;
import com.mongodb.BasicDBList;
import com.mongoui.table.model.ListTableModel;
import com.mongoui.table.model.TableFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultPane extends JPanel {

    private final ListSelectionModelGroup selectionGroup = new ListSelectionModelGroup();
    private final BasicDBList result;

    public ResultPane( ResultSet rs ) throws SQLException {
        super(new BorderLayout());
        this.result = new BasicDBList();
        while ( rs.next() ){
            result.add( rs.getObject(1));
        }
        rs.close();
    }

    public ResultPane(BasicDBList result) {
        super(new BorderLayout());
        this.result = result;
        createTable();
    }

    // create property table
    private void createTable() {
        final ListTableModel model = new ListTableModel( null, result );
        final HierarchicalTable table = new HierarchicalTable();
        table.setAutoRefreshOnRowUpdate(false);
        table.setModel(model);
        table.setName("Product Table");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setTableStyleProvider(new RowStripeTableStyleProvider());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.setComponentFactory(new TableFactory(selectionGroup));

        TableFactory.fixTableHeader(model, table);

        selectionGroup.add(table.getSelectionModel());
        AutoFilterTableHeader header = new AutoFilterTableHeader(table);
        table.setTableHeader(header);
        header.setAutoFilterEnabled(true);
        header.setUseNativeHeaderRenderer(true);
        final JScrollPane scrollPane = new JScrollPane( table );
        scrollPane.getViewport().putClientProperty("HierarchicalTable.mainViewport", Boolean.TRUE);
        setBackground( Color.YELLOW );
        this.add(scrollPane, BorderLayout.CENTER) ;
    }


}
