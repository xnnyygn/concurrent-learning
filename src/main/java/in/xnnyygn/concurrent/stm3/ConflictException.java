package in.xnnyygn.concurrent.stm3;

public class ConflictException extends ControlFlowException {

    public ConflictException() {
    }

    public ConflictException(String message) {
        super(message);
    }
}
