package com.micah.newtransferthread;

import static com.android.providers.downloads.Constants.TAG;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import android.text.TextUtils;
import android.util.Log;

import com.android.providers.downloads.DownloadInfo.NetworkState;
import com.micah.newtransferthread.TransferThread.State;

public class DownloadThread extends TransferThread {

	public DownloadThread() {
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
    
    /**
     * Called after a successful completion to take any necessary action on the transfered file.
     */
    private void finalizeDestinationFile(State state) {
        if (state.mFilename != null) {
            // make sure the file is readable
            FileUtils.setPermissions(state.mFilename, 0644, -1, -1);
        }
    }

    /**
     * Called just before the thread finishes, regardless of status, to take any necessary action on
     * the transfered file.
     */
    private void cleanupDestination(State state, int finalStatus) {
        if (state.mFilename != null && Transfers.Impl.isStatusError(finalStatus)) {
            if (Constants.LOGVV) {
                Log.d(TAG, "cleanupDestination() deleting " + state.mFilename);
            }
            new File(state.mFilename).delete();
            state.mFilename = null;
        }
    }
    
    /**
     * Write a data buffer to the destination file.
     * @param data buffer containing the data to write
     * @param bytesRead how many bytes to write from the buffer
     */
    private void writeDataToDestination(State state, byte[] data, int bytesRead, OutputStream out)
            throws StopRequestException {
        mStorageManager.verifySpaceBeforeWritingToFile(
                mInfo.mDestination, state.mFilename, bytesRead);

        boolean forceVerified = false;
        while (true) {
            try {
                out.write(data, 0, bytesRead);
                return;
            } catch (IOException ex) {
                // TODO: better differentiate between DRM and disk failures
                if (!forceVerified) {
                    // couldn't write to file. are we out of space? check.
                    mStorageManager.verifySpace(mInfo.mDestination, state.mFilename, bytesRead);
                    forceVerified = true;
                } else {
                    throw new StopRequestException(Transfers.Impl.STATUS_FILE_ERROR,
                            "Failed to write data: " + ex);
                }
            }
        }
    }
    
    /**
     * Prepare the destination file to receive data.  If the file already exists, we'll set up
     * appropriately for resumption.
     */
    private void setupDestinationFile(State state) throws StopRequestException {
        if (!TextUtils.isEmpty(state.mFilename)) { // only true if we've already run a thread for this transfer
            if (Constants.LOGV) {
                Log.i(Constants.TAG, "have run thread before for id: " + mInfo.mId +
                        ", and state.mFilename: " + state.mFilename);
            }
            if (!Helpers.isFilenameValid(state.mFilename,
                    mStorageManager.getTransferDataDirectory())) {
                // this should never happen
                throw new StopRequestException(Transfers.Impl.STATUS_FILE_ERROR,
                        "found invalid internal destination filename");
            }
            // We're resuming a transfer that got interrupted
            File f = new File(state.mFilename);
            if (f.exists()) {
                if (Constants.LOGV) {
                    Log.i(Constants.TAG, "resuming transfer for id: " + mInfo.mId +
                            ", and state.mFilename: " + state.mFilename);
                }
                long fileLength = f.length();
                if (fileLength == 0) {
                    // The transfer hadn't actually started, we can restart from scratch
                    if (Constants.LOGVV) {
                        Log.d(TAG, "setupDestinationFile() found fileLength=0, deleting "
                                + state.mFilename);
                    }
                    f.delete();
                    state.mFilename = null;
                    if (Constants.LOGV) {
                        Log.i(Constants.TAG, "resuming transfer for id: " + mInfo.mId +
                                ", BUT starting from scratch again: ");
                    }
                } else if (mInfo.mETag == null && !mInfo.mNoIntegrity) {
                    // This should've been caught upon failure
                    if (Constants.LOGVV) {
                        Log.d(TAG, "setupDestinationFile() unable to resume transfer, deleting "
                                + state.mFilename);
                    }
                    f.delete();
                    throw new StopRequestException(Transfers.Impl.STATUS_CANNOT_RESUME,
                            "Trying to resume a transfer that can't be resumed");
                } else {
                    // All right, we'll be able to resume this transfer
                    if (Constants.LOGV) {
                        Log.i(Constants.TAG, "resuming transfer for id: " + mInfo.mId +
                                ", and starting with file of length: " + fileLength);
                    }
                    state.mCurrentBytes = (int) fileLength;
                    if (mInfo.mTotalBytes != -1) {
                        state.mContentLength = mInfo.mTotalBytes;
                    }
                    state.mHeaderETag = mInfo.mETag;
                    state.mContinuingTransfer = true;
                    if (Constants.LOGV) {
                        Log.i(Constants.TAG, "resuming transfer for id: " + mInfo.mId +
                                ", state.mCurrentBytes: " + state.mCurrentBytes +
                                ", and setting mContinuingTransfer to true: ");
                    }
                }
            }
        }
    }

}
