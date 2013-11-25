package com.micah.newtransferthread;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import com.micah.newtransferthread.TransferThread.State;

public class UploadThread extends TransferThread {

	public UploadThread() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void runInternal() {
	}
	
	@Override
    public  void executeTransfer(State state) throws StopRequestException {
    }

    @Override
    public void transferData(State state, HttpURLConnection conn) throws StopRequestException {
    }

    @Override
    public void transferData(State state, InputStream in, OutputStream out) throws StopRequestException {
    }
}
