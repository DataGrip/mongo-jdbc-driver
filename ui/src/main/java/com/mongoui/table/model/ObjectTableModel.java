package com.mongoui.table.model;

import com.jidesoft.converter.ConverterContext;
import com.jidesoft.grid.*;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class ObjectTableModel extends AbstractTableModel implements HierarchicalTableModel, ContextSensitiveTableModel, HeaderStyleModel {

    private final String[] keys;
    private final DBObject object;
    private final String title;
    private final TableModel[] childModels;

    public ObjectTableModel( String title, DBObject object){
        this.object = object;
        this.title = title;

        this.keys = new String[object.keySet().size()];
        int i = 0;
        for( String key : object.keySet() ){
            keys[i++] = key;
        }
        final List<TableModel> reusable = new ArrayList<TableModel>();

        for ( String key : object.keySet() ){
            Object value = object.get( key );
            if ( value instanceof BasicDBList){
                reusable.add(new ListTableModel(key, (BasicDBList) value ));
            } else if ( value instanceof DBObject ) {
                reusable.add(new ObjectTableModel( key, (DBObject) value));
            }
        }
        if ( reusable.size() == 1 ){
            childModels = new TableModel[] { reusable.get( 0 )};
        } else if ( reusable.size()  > 1 ){
            childModels = reusable.toArray( new TableModel[ reusable.size()]);
        } else {
            childModels = null;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        final Object val = object.get(keys[columnIndex]);
        if( val != null ) return val.getClass();
        return String.class;
    }

    @Override
    public String getColumnName(int column) {
        if ( column < 0 ) return title;
        return keys[ column ];
    }

    @Override
    public int getColumnCount() {
        return keys.length;
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return object.get( keys[ columnIndex ] );
    }

    @Override
    public ConverterContext getConverterContextAt(int row, int col) {
        return null;
    }

    @Override
    public EditorContext getEditorContextAt(int row, int col) {
        return null;
    }

    @Override
    public Class<?> getCellClassAt(int row, int col) {
        final Object val = getValueAt( row, col );
        return val != null ? val.getClass() : String.class;
    }

    @Override
    public boolean hasChild(int row) {
        return childModels != null;
    }

    @Override
    public boolean isHierarchical(int row) {
        return childModels != null;
    }

    @Override
    public Object getChildValueAt(int row) {
        return childModels;
    }

    @Override
    public boolean isExpandable(int row) {
        return true;
    }

    private static final CellStyle headerStyle = new CellStyle();

    @Override
    public CellStyle getHeaderStyleAt(int row, int col) {
        boolean isTitle = title != null && row == 0 ;
        headerStyle.setForeground( isTitle ? Color.DARK_GRAY : null );
        headerStyle.setBackground( isTitle ? new Color( 0xe0e0e0) : null );
        headerStyle.setHorizontalAlignment( SwingConstants.CENTER);

        return headerStyle;
    }

    @Override
    public boolean isHeaderStyleOn() {
        return true;
    }
}
