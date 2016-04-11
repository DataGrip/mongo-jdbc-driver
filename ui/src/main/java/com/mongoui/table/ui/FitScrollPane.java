package com.mongoui.table.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseWheelListener;



public class FitScrollPane extends JScrollPane implements ComponentListener {
    public FitScrollPane() {
        initScrollPane();
    }

    public FitScrollPane(Component view) {
        super(view);
        initScrollPane();
    }

    public FitScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        super(view, vsbPolicy, hsbPolicy);
        initScrollPane();
    }

    public FitScrollPane(int vsbPolicy, int hsbPolicy) {
        super(vsbPolicy, hsbPolicy);
        initScrollPane();
    }

    private void initScrollPane() {
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        getViewport().getView().addComponentListener(this);
        removeMouseWheelListeners();
    }

    // remove MouseWheelListener as there is no need for it in FitScrollPane.
    private void removeMouseWheelListeners() {
        MouseWheelListener[] listeners = getMouseWheelListeners();
        for (MouseWheelListener listener : listeners) {
            removeMouseWheelListener(listener);
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        removeMouseWheelListeners();
    }

    public void componentResized(ComponentEvent e) {
        setSize(getSize().width, getPreferredSize().height);
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public Dimension getPreferredSize() {
        getViewport().setPreferredSize(getViewport().getView().getPreferredSize());
        return super.getPreferredSize();
    }

    public static void scrollRectToVisible(Component component, Rectangle aRect) {
        Container parent;
        int dx = component.getX(), dy = component.getY();

        for (parent = component.getParent();
             parent != null && (!(parent instanceof JViewport) || (((JViewport) parent).getClientProperty("HierarchicalTable.mainViewport") == null));
             parent = parent.getParent()) {
            Rectangle bounds = parent.getBounds();

            dx += bounds.x;
            dy += bounds.y;
        }

        if (parent != null) {
            aRect.x += dx;
            aRect.y += dy;

            ((JComponent) parent).scrollRectToVisible(aRect);
            aRect.x -= dx;
            aRect.y -= dy;
        }
    }


}
