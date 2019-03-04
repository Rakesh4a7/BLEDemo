/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.example.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class BlinkyManager extends BleManager<BlinkyManagerCallbacks> {
	private BluetoothGattCharacteristic requestCharacteristic;
    private BluetoothGattCharacteristic responseCharacteristic;
	private LogSession mLogSession;
	private boolean mSupported;

    // Create the uuid for data transfer service
    final UUID dataTransferServiceUUID = UUID.fromString("0b61c398-7697-4762-82d1-5bf490ce0a31");

    // Create the uuid's for the request and response characteristics
    final UUID requestCharacteristicUUID = UUID.fromString("0b61c399-7697-4762-82d1-5bf490ce0a31");
    final UUID responseCharacteristicUUID = UUID.fromString("0b61c39a-7697-4762-82d1-5bf490ce0a31");

    // Create UUID for the Client Characteristic Configuration for the response characteristic
    final UUID clientCharacteristicConfiguration = UUID.fromString( "00002902-0000-1000-8000-00805F9B34FB");


	public BlinkyManager(@NonNull final Context context) {
		super(context);
	}

	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {
		return mGattCallback;
	}

	/**
	 * Sets the log session to be used for low level logging.
	 * @param session the session, or null, if nRF Logger is not installed.
	 */
	public void setLogger(@Nullable final LogSession session) {
		this.mLogSession = session;
	}

	@Override
	public void log(final int priority, @NonNull final String message) {
		// The priority is a Log.X constant, while the Logger accepts it's log levels.
		Logger.log(mLogSession, LogContract.Log.Level.fromPriority(priority), message);
	}

	@Override
	protected boolean shouldClearCacheWhenDisconnected() {
		return !mSupported;
	}


	/**
	 * The LED callback will be notified when the LED state was read or sent to the target device.
	 * <p>
	 * This callback implements both {@link no.nordicsemi.android.ble.callback.DataReceivedCallback}
	 * and {@link no.nordicsemi.android.ble.callback.DataSentCallback} and calls the same
	 * method on success.
	 * <p>
	 * If the data received were invalid, the
	 * {@link BlinkyLedDataCallback#onInvalidDataReceived(BluetoothDevice, Data)} will be
	 * called.
	 */
	private final BlinkyLedDataCallback mLedCallback = new BlinkyLedDataCallback() {
		@Override
		public void onLedStateChanged(@NonNull final BluetoothDevice device,
									  final boolean on) {
			log(LogContract.Log.Level.APPLICATION, "LED " + (on ? "ON" : "OFF"));
			mCallbacks.onLedStateChanged(device, on);
		}

		@Override
		public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
										  @NonNull final Data data) {
			// Data can only invalid if we read them. We assume the app always sends correct data.
			log(Log.WARN, "Invalid data received: " + data);
		}
	};

	/**
	 * BluetoothGatt callbacks object.
	 */
	private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {
		@Override
		protected void initialize() {


            BluetoothGattDescriptor descriptor = responseCharacteristic.getDescriptor(clientCharacteristicConfiguration);
            // Enable notification
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            enableNotifications(responseCharacteristic).with(mLedCallback).enqueue();
            readDescriptor(descriptor).with(mLedCallback).enqueue();

            //setNotificationCallback(mLedCharacteristic).with(mLedCallback);
			/*readCharacteristic(mLedCharacteristic).with(mLedCallback).enqueue();
			readDescriptor(descriptor);
			enableNotifications(mLedCharacteristic).enqueue();*/
		}

		@Override
		public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(dataTransferServiceUUID);
			if (service != null) {
                responseCharacteristic = service.getCharacteristic(responseCharacteristicUUID);
				requestCharacteristic = service.getCharacteristic(requestCharacteristicUUID);
			}

			boolean writeRequest = false;
			if (responseCharacteristic != null) {
				final int rxProperties = responseCharacteristic.getProperties();
				writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
			}

			mSupported = responseCharacteristic != null && writeRequest;
			return mSupported;
		}

		@Override
		protected void onDeviceDisconnected() {
            responseCharacteristic = null;
            requestCharacteristic = null;
		}
	};

	/**
	 * Sends a request to the device to turn the LED on or off.
	 *
	 */
	public void send() {
		// Are we connected?
		if (requestCharacteristic == null)
			return;

		writeCharacteristic(requestCharacteristic, new byte[]{(byte) 0x03})
				.with(mLedCallback).enqueue();
	}
}
