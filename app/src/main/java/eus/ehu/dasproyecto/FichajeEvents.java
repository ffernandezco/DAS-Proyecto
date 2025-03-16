package eus.ehu.dasproyecto;

public class FichajeEvents {
    public interface FichajeChangeListener {
        void onFichajeChanged();
    }

    private static FichajeChangeListener listener;

    public static void setListener(FichajeChangeListener l) {
        listener = l;
    }

    public static void notifyFichajeChanged() {
        if (listener != null) {
            listener.onFichajeChanged();
        }
    }
}