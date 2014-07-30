package FileSieve.BusinessLogic.FileManagement;

/**
 * Custom exception for SwingCopyJob class. Used to convey exceptions that have occurred internally on the job's
 * background thread.
 */
public class SwingCopyJobException extends Exception {

    private final String message;
    private final Exception originalException;

    public SwingCopyJobException(String message) {
        if ((message == null) || (message.trim().isEmpty())) {
            throw new IllegalArgumentException("message parameter cannot be null");
        }

        this.message = message.trim();
        this.originalException = null;
    }

    public SwingCopyJobException(Exception originalException) {
        if (originalException == null) {
            throw new IllegalArgumentException("originalException parameter cannot be null");
        }

        this.message = "";
        this.originalException = originalException;
    }

    public SwingCopyJobException(String message, Exception originalException) {
        if (((message == null) || (message.trim().isEmpty())) && (originalException == null)) {
            throw new IllegalArgumentException("message and originalException parameters cannot both be null/empty");
        }

        this.message = message.trim();
        this.originalException = originalException;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public Exception getCause() {
        return this.originalException;
    }

} // class SwingCopyJobException extends Exception
