package com.mongoui;

import javax.swing.*;

public class Util {

    public static String cutOf( String str, int maxLength ){
        if ( str != null ){
            return str.length() > maxLength ? str.substring( 0, maxLength-3) + "..." : str;
        }
        return str;
    }

    public static ImageIcon getIcon( String name ){
        return new ImageIcon(Util.class.getResource("/icons/" + name));
    }

}
