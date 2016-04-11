package com.mongoui.table.model;

import com.jidesoft.converter.ConverterContext;
import com.jidesoft.grid.*;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class ListTableModel extends AbstractTableModel implements HierarchicalTableModel, ContextSensitiveTableModel, HeaderStyleModel {

    private final BasicDBList data;
    private final List<String> keys = new ArrayList<String>();
    private final Object[] childModels;
    private final String title;

    public ListTableModel(String title, BasicDBList data){
        this.data = data;
        this.title = title;

        for ( Object record : data ){
            if ( record instanceof DBObject ){
                for ( String key : ((DBObject)record).keySet() ){
                    if ( !keys.contains( key ) ) keys.add( key );
                }
            } else if ( !keys.contains("value")){
                keys.add( "value");

            }

        }
        this.childModels = new Object[data.size()];

        for ( int row = 0; row < data.size(); row++ ){
            final DBObject record = (DBObject)data.get( row );
            final List<TableModel> reusable = new ArrayList<TableModel>();

            for ( String key : record.keySet() ){
                Object value = record.get( key );
                if ( isDbListOfDbObject( value ) ){
                    reusable.add(new ListTableModel(key, (BasicDBList) value ));
                } else if ( value instanceof DBObject ) {
                    reusable.add(new ObjectTableModel( key, (DBObject) value));
                }
            }
            if ( reusable.size() == 1 ){
                childModels[row] = reusable.get( 0 );
            } else if ( reusable.size()  > 1 ){
                childModels[row] = reusable.toArray( new TableModel[ reusable.size()]);
            }
        }
    }

    private boolean isDbListOfDbObject( Object obj ){
        if ( obj instanceof BasicDBList ){
            for ( Object r : (BasicDBList)obj ){
                if ( !( r instanceof BasicDBObject )) return false;
            }
            return true;
        }
        return false;
    }

    public boolean hasChild(int row) {
        return childModels[row] != null;
    }

    public boolean isExpandable(int row) {
        return childModels[row] != null;
    }

    public boolean isHierarchical(int row) {
        return childModels[row] != null;
    }

    @Override
    public Object getChildValueAt(int row) {
        return childModels[ row ];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int column) {
        if ( column < 0 ) return title;
        return keys.get( column );
    }

    @Override
    public int getColumnCount() {
        return keys.size();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return ((DBObject)data.get( rowIndex )).get( keys.get( columnIndex ));
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
