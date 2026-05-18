package jauri;

import jauri.ffm.Gtk3;

public class GtkMainLoop {

    public enum State { CREATED, RUNNING, STOPPED }

    private State state = State.CREATED;
    private long window = 0;

    public String getState() {
        return state.name();
    }

    public void start() {
        if (state == State.STOPPED) {
            throw new IllegalStateException("Cannot restart a stopped loop");
        }
        if (state == State.RUNNING) {
            throw new IllegalStateException("Loop is already running");
        }

        Gtk3.gtkInit(0, 0);

        window = Gtk3.gtkWindowNew(Gtk3.GTK_WINDOW_TOPLEVEL);
        if (window == 0) {
            throw new RuntimeException("Failed to create GTK window");
        }

        Gtk3.gtkWindowSetDefaultSize(window, 800, 600);

        // Connect "destroy" signal → calls gtk_main_quit
        Gtk3.gSignalConnectDestroy(window);

        Gtk3.gtkWidgetShowAll(window);

        state = State.RUNNING;

        // Blocks until gtk_main_quit is called
        Gtk3.gtkMain();

        state = State.STOPPED;
    }

    public void stop() {
        if (state == State.RUNNING) {
            Gtk3.gtkMainQuit();
        }
        state = State.STOPPED;
    }
}
