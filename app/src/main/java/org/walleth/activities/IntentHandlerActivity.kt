package org.walleth.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.isERC681
import org.kethereum.erc681.toERC681
import org.kethereum.model.Address
import org.kethereum.model.EthereumURI
import org.kethereum.uri.common.parseCommonURI
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import org.ligi.kaxtui.alert
import org.walleth.R
import org.walleth.data.keystore.WallethKeyStore
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.tokens.isTokenTransfer
import java.math.BigInteger.ZERO

private const val CREATE_TX_REQUEST_CODE = 10123
private const val SIGN_TX_REQUEST_CODE = 10124

fun Context.getEthereumViewIntent(ethereumString: String) = Intent(this, IntentHandlerActivity::class.java).apply {
    data = Uri.parse(ethereumString)
}

class IntentHandlerActivity : AppCompatActivity(), KodeinAware {

    var textToSign: String? = null

    override val kodein by closestKodein()
    private val keyStore: WallethKeyStore by instance()
    private val currentAddressProvider: CurrentAddressProvider by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val commonURI = EthereumURI(intent.data.toString()).parseCommonURI()
        if (commonURI.valid) {
            if (commonURI.isERC681()) {
                val erC681 = commonURI.toERC681()
                if (erC681.valid) {
                    process681(erC681)
                }
            }

            if (commonURI.prefix == "signtext") {
                textToSign = commonURI.address // TODO this is not really cool
                val intent = Intent(this, AddressBookActivity::class.java)
                startActivityForResult(intent, TO_ADDRESS_REQUEST_CODE)
            }

        } else {
            alert(getString(R.string.create_tx_error_invalid_erc67_msg, intent.data.toString()), getString(R.string.create_tx_error_invalid_erc67_title)) {
                finish()
            }
        }
    }


    private fun process681(erC681: ERC681) {
        if (erC681.address == null || erC681.isTokenTransfer() || erC681.value != null && erC681.value != ZERO) {
            startActivity(Intent(this, CreateTransactionActivity::class.java).apply {
                data = intent.data
            })
            finish()
        } else {
            AlertDialog.Builder(this)
                    .setTitle(R.string.select_action_messagebox_title)
                    .setItems(R.array.scan_hex_choices) { _, which ->
                        when (which) {
                            0 -> {
                                startCreateAccountActivity(erC681.address!!)
                                finish()
                            }
                            1 -> {
                                val intent = Intent(this, CreateTransactionActivity::class.java).apply {
                                    data = intent.data
                                }
                                startActivityForResult(intent, CREATE_TX_REQUEST_CODE)
                            }
                            2 -> alert("TODO", "add token definition") {
                                finish()
                            }

                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        finish()
                    }
                    .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SIGN_TX_REQUEST_CODE, CREATE_TX_REQUEST_CODE -> {
                setResult(resultCode, data)
                finish()
            }

            TO_ADDRESS_REQUEST_CODE -> {

                if (data?.hasExtra("HEX") == true) {
                    currentAddressProvider.setCurrent(Address(data.getStringExtra("HEX")))
                }

                val intent = Intent(this, SignTextActivity::class.java)
                intent.putExtra("TEXT", textToSign)
                startActivityForResult(intent, SIGN_TX_REQUEST_CODE)
            }
        }
    }
}
