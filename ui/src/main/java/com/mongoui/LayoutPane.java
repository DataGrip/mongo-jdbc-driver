package com.mongoui;


import com.jidesoft.action.DefaultDockableBarDockableHolder;
import com.jidesoft.action.DockableBarDockableHolderPanel;
import com.jidesoft.docking.DefaultDockingManager;
import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import com.jidesoft.editor.CodeEditor;
import com.jidesoft.editor.tokenmarker.PythonTokenMarker;
import com.jidesoft.editor.tokenmarker.Token;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class LayoutPane extends DockableBarDockableHolderPanel {

    public final DockingManager dockingManager;

    public final String id;
    public final CodeEditor editor = new CodeEditor();
    private File file;


    // https://github.com/poiati/gmongo

    public LayoutPane(DefaultDockableBarDockableHolder frame){
        super ();
        this.id = "Layout_" + System.currentTimeMillis()%100000;
        editor.setText("{$or:[{age:20}, {age:21} ]}");
        editor.setText(
                "import com.nosql.mongo.JMongoClient\n" +
                "mongoClient = new JMongoClient('localhost', 27017)\n" +
                "db = mongoClient.getDB('local')\n" +
                "db.getCollection('user').find()");



        final PythonTokenMarker pythonTokenMarker = new PythonTokenMarker();
        PythonTokenMarker.getKeywords().add("println", Token.KEYWORD2 );
        PythonTokenMarker.getKeywords().add("toString", Token.KEYWORD2);
        PythonTokenMarker.getKeywords().add("db", Token.KEYWORD2);

        editor.setTokenMarker(pythonTokenMarker);
        dockingManager = new DefaultDockingManager( frame, this );
        getDockingManager().setProfileKey( "LayoutPane" );
        getDockingManager().loadLayoutData();
        getDockingManager().setShowTitleBar(false);
        getDockingManager().setShowDividerGripper( true );
        getDockingManager().setEasyTabDock(true);
        getDockingManager().setFloatable(false);
        getDockingManager().setDoubleClickAction( DockingManager.DOUBLE_CLICK_TO_MAXIMIZE );

        final DockableFrame diagramFrame = new DockableFrame("Diagram_"){
            @Override
            public boolean shouldVetoHiding() {
                return false;
            }

            @Override
            public boolean shouldVetoRemoving() {
                return false;
            }
        };
        diagramFrame.getContext().setInitMode( DockContext.STATE_FRAMEDOCKED );
        diagramFrame.getContext().setInitSide( DockContext.DOCK_SIDE_CENTER );
        diagramFrame.add( editor, BorderLayout.CENTER);
        getDockingManager().addFrame( diagramFrame );

        diagramFrame.setShowTitleBar( false );
        diagramFrame.setVisible( true );
    }


    public DockingManager getDockingManager() {
        return dockingManager;
    }


    public void createAndAddResultFrame( JComponent resultPane ) {
        int frameId = 1;
        while ( getDockingManager().getFrame( "Result " + frameId ) != null ){
            frameId ++;
        }
        String key = "Result " + frameId;
        final DockableFrame frame = new DockableFrame( key, null );
        frame.getContext().setInitMode( DockContext.STATE_FRAMEDOCKED );
        frame.getContext().setInitSide( DockContext.DOCK_SIDE_SOUTH );
        frame.getContext().setInitPosition( true );
        frame.getContext().setInitIndex( 0 );
        frame.setContentPane( resultPane );
        frame.setTabTitle( key );
        frame.setDefaultCloseAction( DockableFrame.CLOSE_ACTION_TO_REMOVE );
        frame.setPreferredSize( new Dimension( 500, 400) );
        getDockingManager().addFrame( frame );
        getDockingManager().activateFrame( key );
    }


    public String getSelectedText(){
        final String selectedText = editor.getSelectedText();
        return ( selectedText != null ) ? selectedText : editor.getText();
    }

    public BasicDBObject getJSON(){
        String query = getSelectedText();
        if ( query != null ){
            return (BasicDBObject)JSON.parse( query );
        }
        return null;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        Component cmp = this;
        while ( cmp != null ){
            if( cmp instanceof DockableFrame ){
                ((DockableFrame)cmp).setKey( file.getPath());
            }
            cmp = cmp.getParent();
        }
    }


}



