package in.xnnyygn.concurrent.stm2;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

class TObjectLog {

    private static final Object EMPTY_VALUE = new Object();

    private long version;
    private Object newValue = EMPTY_VALUE;
    private LinkedList<Runnable> undoActions;

    void pushUndoAction(@Nonnull Runnable action) {
        if (undoActions == null) {
            undoActions = new LinkedList<>();
        }
        undoActions.push(action);
    }

    @Nonnull
    Iterator<Runnable> undoActionIterator() {
        return undoActions == null ? Collections.emptyIterator() : undoActions.iterator();
    }

    boolean hasNewValue() {
        return newValue != EMPTY_VALUE;
    }

    Object getNewValue() {
        return newValue;
    }

    void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

}
