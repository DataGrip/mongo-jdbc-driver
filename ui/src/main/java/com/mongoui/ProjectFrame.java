package com.mongoui;

import com.jidesoft.action.CommandBar;
import com.jidesoft.action.DefaultDockableBarDockableHolder;
import com.jidesoft.action.DockableBarContext;
import com.jidesoft.docking.*;
import com.jidesoft.docking.event.DockableFrameAdapter;
import com.jidesoft.docking.event.DockableFrameEvent;
import com.jidesoft.grid.CellRendererManager;
import com.jidesoft.grid.ColorCellRenderer;
import com.jidesoft.swing.JideButton;
import com.jidesoft.utils.Lm;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;


public class ProjectFrame extends DefaultDockableBarDockableHolder {

    private final Workspace workspace;


    public ProjectFrame( final Workspace workspace ){
        super("MongoDb Console");
        this.setIconImage( Util.getIcon( "mongodb.png").getImage() );
        this.workspace = workspace;

        final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                label.setText(Util.cutOf(value.toString(), 30));
                label.setIcon(Util.getIcon("add.png"));
                return label;
            }
        };

        CellRendererManager.registerRenderer(BasicDBList.class, renderer);
        CellRendererManager.registerRenderer(BasicDBObject.class, renderer);


        getDockingManager().setProfileKey("DbsFrame");
        getDockingManager().setShowDividerGripper(true);
        getDockingManager().setShowInitial(false);
        getDockingManager().setFloatable(false);
        getDockingManager().setEasyTabDock(true);
        getDockingManager().setDoubleClickAction(DockingManager.DOUBLE_CLICK_TO_MAXIMIZE);
        getDockingManager().setInitSplitPriority(DefaultDockingManager.SPLIT_SOUTH_NORTH_EAST_WEST);
        getDockingManager().addDockableFrameListener(
                new DockableFrameAdapter(){

                    @Override
                    public void dockableFrameHidden(DockableFrameEvent event) {
                        if ( event.getDockableFrame().getContentPane() instanceof LayoutPane ){
                            workspace.setActiveLayoutPane( null );
                        }
                    }

                    @Override
                    public void dockableFrameActivated(DockableFrameEvent event) {
                        Component c = event.getDockableFrame().getContentPane();
                        if ( c instanceof LayoutPane ){
                            workspace.setActiveLayoutPane( (LayoutPane) c );
                        }
                    }
                }
        );

        getDockingManager().setDockableFrameFactory(new DockableFrameFactory(){
            public DockableFrame create(String key) {
                File file = new File( key );
                if ( file.exists() ){
                    DockableFrame frame = createLayoutFrame();
                    LayoutPane layoutPane = (LayoutPane)frame.getContentPane();
                    layoutPane.setFile( file );
                    layoutPane.editor.setFileName( key );
                    return null;
                }
                return null;
            }
        });

        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                getLayoutPersistence().saveLayoutData();
                System.exit(0);
            }
        } );

        CommandBar menuBar = new CommandBar();
        menuBar.setKey("menu");
        menuBar.setInitSide(DockableBarContext.DOCK_SIDE_NORTH);
        menuBar.setInitIndex(1);
        getDockableBarManager().addDockableBar(menuBar);
        menuBar.add(getAction("newLayout"));
        menuBar.add(getAction("open"));
        menuBar.add(getAction("save"));
        menuBar.add( getAction("run"));

        getLayoutPersistence().beginLoadLayoutData();
        getLayoutPersistence().loadLayoutData();
        for ( String key : getDockingManager().getAllFrames()){
            getDockingManager().showFrame( key );
        }
        getDockingManager().showInitial();

    }


    public DockableFrame createLayoutFrame() {
        LayoutPane layoutPane = new LayoutPane(ProjectFrame.this);
        final DockableFrame frame = new DockableFrame( layoutPane.id );
        frame.getContext().setInitMode( DockContext.STATE_FRAMEDOCKED );
        frame.getContext().setInitSide( DockContext.DOCK_SIDE_CENTER );
        frame.setPreferredSize( new Dimension( 600, 600 ));
        frame.setContentPane(layoutPane);
        frame.setDefaultCloseAction( DockableFrame.CLOSE_ACTION_TO_REMOVE );
        frame.setShowTitleBar( false );
        getDockingManager().addFrame( frame );
        getDockingManager().showFrame( frame.getKey() );
        return frame;
    }


    public class JideUiResourceButton extends JideButton implements UIResource {
        public JideUiResourceButton(Action action){
            super( action);
        }
    }


    public javax.swing.Action getAction( String key ) {
        return workspace.getContext().getActionMap(this).get(key) ;
    }



}
