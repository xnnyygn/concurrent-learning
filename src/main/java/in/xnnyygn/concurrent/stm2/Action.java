package in.xnnyygn.concurrent.stm2;

import javax.annotation.Nonnull;

public interface Action {

    void run(@Nonnull Transaction transaction);

}
