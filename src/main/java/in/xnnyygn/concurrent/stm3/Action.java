package in.xnnyygn.concurrent.stm3;

import javax.annotation.Nonnull;

public interface Action {

    void run(@Nonnull Transaction transaction);

}
