package net.sorenon.openxr;

public class XrException extends Exception {

    public final int result;

    public XrException(int result, String message) {
        super(message);
        this.result = result;
    }
}
