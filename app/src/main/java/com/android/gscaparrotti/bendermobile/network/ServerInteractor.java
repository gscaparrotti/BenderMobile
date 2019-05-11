package com.android.gscaparrotti.bendermobile.network;

import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;

import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by gscap_000 on 01/05/2016.
 */
public class ServerInteractor {

    private Socket socket;
    private boolean used = false;

    public ServerInteractor() {
        socket = new Socket();
    }

    public Object sendCommandAndGetResult(final String address, final int port, final Object input) {
        if (used) {
            throw new IllegalStateException("You cannot use a ServerInteractor twice");
        }
        used = true;
        try {
            if (!InetAddressValidator.getInstance().isValidInet4Address(address)) {
                throw new IOException("Invalid address detected: " + address);
            }
            socket.connect(new InetSocketAddress(address, port), 5000);
            socket.setSoTimeout(5000);
            socket.setTcpNoDelay(true);
            new ObjectOutputStream(socket.getOutputStream()).writeObject(input);
            final Object data = new ObjectInputStream(socket.getInputStream()).readObject();
            socket.close();
            return data;
        } catch (final IOException | ClassNotFoundException e) {
            try {
                socket.close();
                throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreSocketConChiusura));
            } catch (final IOException e1) {
                throw new BenderNetworkException(MainActivity.commonContext.getString(R.string.ErroreSocketSenzaChiusura));
            }
        }
    }

}
