package com.missanye.desktop.view;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** The sign-in module fills its own compact window; restore workspace geometry after a user switch. */
public final class WindowLayout {
    private WindowLayout(){}
    public static void install(Stage stage,WorkbenchView view){
        var sizing=new Sizing(stage);
        view.workspaceOpenProperty().addListener((o,old,open)->sizing.apply(open));
        sizing.apply(view.workspaceOpenProperty().get());
    }
    private static final class Sizing {
        private final Stage stage;
        private double workspaceWidth=1120,workspaceHeight=740,workspaceX=Double.NaN,workspaceY;
        private boolean maximized,wasOpen;
        Sizing(Stage stage){this.stage=stage;}
        void apply(boolean open){
            var screens=Screen.getScreensForRectangle(stage.getX(),stage.getY(),Math.max(1,stage.getWidth()),Math.max(1,stage.getHeight()));
            Rectangle2D area=(screens.isEmpty()?Screen.getPrimary():screens.get(0)).getVisualBounds();
            if(!open&&wasOpen){maximized=stage.isMaximized();if(!maximized){workspaceWidth=stage.getWidth();workspaceHeight=stage.getHeight();workspaceX=stage.getX();workspaceY=stage.getY();}}
            stage.setMaximized(false);
            double width=Math.min(open?workspaceWidth:820,area.getWidth()-24),height=Math.min(open?workspaceHeight:570,area.getHeight()-24);
            stage.setMinWidth(Math.min(open?900:460,area.getWidth()-24));stage.setMinHeight(Math.min(open?600:440,area.getHeight()-24));
            stage.setWidth(width);stage.setHeight(height);
            double x=open&&!Double.isNaN(workspaceX)?workspaceX:area.getMinX()+(area.getWidth()-width)/2;
            double y=open&&!Double.isNaN(workspaceX)?workspaceY:area.getMinY()+(area.getHeight()-height)/2;
            stage.setX(Math.max(area.getMinX(),Math.min(x,area.getMaxX()-width)));stage.setY(Math.max(area.getMinY(),Math.min(y,area.getMaxY()-height)));
            if(open&&maximized)stage.setMaximized(true);wasOpen=open;
        }
    }
}
