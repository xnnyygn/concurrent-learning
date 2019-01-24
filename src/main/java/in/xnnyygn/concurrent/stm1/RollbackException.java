package in.xnnyygn.concurrent.stm1;

class RollbackException extends RuntimeException {

    RollbackException() {
    }

    RollbackException(String message) {
        super(message);
    }

}
