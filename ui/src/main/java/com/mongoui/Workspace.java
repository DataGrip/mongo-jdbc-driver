package com.mongoui;

import com.jidesoft.docking.DockableFrame;
import com.mongodb.BasicDBList;
import com.mongoui.table.ui.ResultPane;
import com.nosql.NoSqlDriver;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.jdesktop.application.Application;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;


public class Workspace extends Application {


    private static final String CURRENT_DIRECTORY = "C:/work/mongoui/samples";

    private ProjectFrame frame;
    private LayoutPane activeLayoutPane;

    @Override
    protected void startup() {
        frame = new ProjectFrame( this );
    }


    public static void main(String args[]) {
        Application.launch( Workspace.class, null );
    }

    @org.jdesktop.application.Action
    public void newLayout() {
        frame.createLayoutFrame();

    }
    @org.jdesktop.application.Action
    public void open() {
        JFileChooser chooser = new JFileChooser( CURRENT_DIRECTORY );
        if (  chooser.showOpenDialog( frame ) == JFileChooser.APPROVE_OPTION  ){
            DockableFrame df = frame.createLayoutFrame();
            LayoutPane layoutPane = (LayoutPane)df.getContentPane();
            layoutPane.setFile( chooser.getSelectedFile() );
            layoutPane.editor.setFileName( chooser.getSelectedFile().getPath() );
        }
    }

    @org.jdesktop.application.Action
    public void save() {
        if ( activeLayoutPane != null ){
            if ( activeLayoutPane.getFile() == null ){
                JFileChooser chooser = new JFileChooser( CURRENT_DIRECTORY );
                if (  chooser.showSaveDialog( frame ) == JFileChooser.APPROVE_OPTION  ){
                    activeLayoutPane.setFile( chooser.getSelectedFile());
                }
            }
            if ( activeLayoutPane.getFile() != null ) {
                try {
                    final FileWriter writer = new FileWriter(activeLayoutPane.getFile());
                    writer.write(activeLayoutPane.editor.getText());
                    writer.close();
                } catch ( IOException ex ){
                    JOptionPane.showMessageDialog( frame, ex.toString() );
                }
            }
        }
    }

    @org.jdesktop.application.Action
    public void run() {
        try {
            NoSqlDriver driver = new NoSqlDriver();
            Connection con = driver.connect("jdbc:mongodb://localhost:27017/local", null);
            Statement st = con.createStatement();
            String query = activeLayoutPane.getSelectedText();
            ResultSet rs = st.executeQuery( query );
            if ( rs != null ){
                BasicDBList result = new BasicDBList();

                while ( rs.next() ){
                    result.add( rs.getObject(1));
                }
                rs.close();
                activeLayoutPane.createAndAddResultFrame( new ResultPane( result ));
            }
        } catch ( MultipleCompilationErrorsException ex ){
            ex.printStackTrace();
            JOptionPane.showMessageDialog( frame, ex.toString() );
            //throw new Exception( ex.getLocalizedMessage() );
        } catch ( Exception ex ){
            ex.printStackTrace();
            JOptionPane.showMessageDialog( frame, ex.toString() );
        }


        /*
    try {
        Sample sample = new Sample();
        if ( activeLayoutPane != null ){
            activeLayoutPane.createAndAddResultFrame( new JSonResult( sample.execute( activeLayoutPane.getJSON() ) ));
        }
    } catch ( Exception ex ){
        JOptionPane.showMessageDialog( frame, ex.toString() );
        ex.printStackTrace();
    }
        */
    }

    public void setActiveLayoutPane( LayoutPane layoutPane ){
        this.activeLayoutPane = layoutPane;
    }


}
