package com.indalec.walletservice.exception;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(String message) {

        super(message); //super calls the constructor of the superclass (RunTimeException)
    }
}
