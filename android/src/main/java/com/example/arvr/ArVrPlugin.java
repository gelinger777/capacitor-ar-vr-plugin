package com.example.arvr;

import com.getcapacitor.Logger;

public class ArVrPlugin {

    public String echo(String value) {
        Logger.info("Echo", value);
        return value;
    }
}
