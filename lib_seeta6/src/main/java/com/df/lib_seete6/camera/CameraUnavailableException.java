package com.df.lib_seete6.camera;

public class CameraUnavailableException extends RuntimeException {
    public CameraUnavailableException() {
    }

    public CameraUnavailableException(String message) {
        super(message);
    }

    public CameraUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public CameraUnavailableException(Throwable cause) {
        super(cause);
    }
}
