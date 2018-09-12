package com.example.moreillon.ble_key_beta;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.bluetooth.le.AdvertiseCallback;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private ToggleButton mToggleButton;
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver advertisingFailureReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check bluetooth stuff
        if (savedInstanceState == null) {

            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {

                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {

                        // Everything is supported and enabled, load the fragments.
                        //setupFragments();

                    } else {

                        // Bluetooth Advertisements are not supported.
                        //showErrorText(R.string.bt_ads_not_supported);
                        Toast.makeText(this, getString(R.string.bt_ads_not_supported), Toast.LENGTH_LONG).show();
                    }
                } else {

                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                }
            } else {

                // Bluetooth is not supported.
                //showErrorText(R.string.bt_not_supported);
                Toast.makeText(this, getString(R.string.bt_not_supported), Toast.LENGTH_LONG).show();
            }
        } // End of if savedInstanceState

        // Initialization of the switch by finding it in the layout using ID
        mToggleButton = (ToggleButton) findViewById(R.id.advertise_toggle_button);

        // React to switch being toggled
        mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked) {
                    startAdvertising();
                }
                else {
                    stopAdvertising();
                }
            }
        });


        // NOT UNDERSTOOD YET
        // NOT WORKING YET
        // Not quite sure what this does
        advertisingFailureReceiver = new BroadcastReceiver() {

            /**
             * Receives Advertising error codes from {@code AdvertiserService} and displays error messages
             * to the user. Sets the advertising toggle to 'false.'
             */
            @Override
            public void onReceive(Context context, Intent intent) {

                int errorCode = intent.getIntExtra(AdvertiserService.ADVERTISING_FAILED_EXTRA_CODE, -1);

                mToggleButton.setChecked(false);

                String errorMessage = getString(R.string.start_error_prefix);
                switch (errorCode) {
                    case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                        errorMessage += " " + getString(R.string.start_error_already_started);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                        errorMessage += " " + getString(R.string.start_error_too_large);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += " " + getString(R.string.start_error_unsupported);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                        errorMessage += " " + getString(R.string.start_error_internal);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        errorMessage += " " + getString(R.string.start_error_too_many);
                        break;
                    default:
                        errorMessage += " " + getString(R.string.start_error_unknown);
                }

                //Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();

            }
        }; // End of advertisingFailureReceiver = new BroadcastReceiver


    } // end of onCreate()

    // Responds to  startActivityForResult(enableBtIntent, 1); in onCreate (asking user to turn bluetooth on)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:

                if (resultCode == RESULT_OK) {

                    // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
                    // this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {

                        // Everything is supported and enabled, load the fragments.

                        //setupFragments();

                    } else {

                        // Bluetooth Advertisements are not supported.
                        Toast.makeText(this, getString(R.string.bt_ads_not_supported), Toast.LENGTH_LONG).show();
                    }
                } else {

                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    } // end of onActivityResult()

    /**
     * When app comes on screen, check if BLE Advertisements are running, set switch accordingly,
     * and register the Receiver to be notified if Advertising fails.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (AdvertiserService.running) {
            mToggleButton.setChecked(true);
        } else {
            mToggleButton.setChecked(false);
        }

        // NOT UNDERSTOOD
        IntentFilter failureFilter = new IntentFilter(AdvertiserService.ADVERTISING_FAILED);
        this.registerReceiver(advertisingFailureReceiver, failureFilter);

    }

    /**
     * When app goes off screen, unregister the Advertising failure Receiver to stop memory leaks.
     * (and because the app doesn't care if Advertising fails while the UI isn't active)
     */
    @Override
    public void onPause() {
        super.onPause();
        this.unregisterReceiver(advertisingFailureReceiver);
    }


    /**
     * Starts BLE Advertising by starting {@code AdvertiserService}.
     */
    private void startAdvertising() {
        // Intent is used to start the service
        // When the service is started, the service object's oncreate method is called
        Intent serviceIntent = new Intent(this, AdvertiserService.class);
        startService(serviceIntent);
    }

    /**
     * Stops BLE Advertising by stopping {@code AdvertiserService}.
     */
    private void stopAdvertising() {
        // Intent is used to stop the service
        // When the service is started, the service object's ondestroy method is called
        Intent serviceIntent = new Intent(this, AdvertiserService.class);
        stopService(serviceIntent);
        mToggleButton.setChecked(false);
    }

}
